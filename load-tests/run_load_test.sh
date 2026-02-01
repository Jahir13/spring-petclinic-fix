#!/bin/bash

set -e

HOST="${HOST:-http://localhost:8080}"
TOOL="${1:-locust}"
OUTPUT_DIR="results_$(date +%Y%m%d_%H%M%S)"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "======================================"
echo "PetClinic Load Test - Stepped Execution"
echo "======================================"
echo "Host: $HOST"
echo "Tool: $TOOL"
echo "Output: $OUTPUT_DIR"
echo ""

mkdir -p "$OUTPUT_DIR"

echo -n "Verificando conexión a $HOST... "
if curl -s -o /dev/null -w "%{http_code}" "$HOST" | grep -q "200"; then
    echo -e "${GREEN}OK${NC}"
else
    echo -e "${RED}FAILED${NC}"
    echo "Error: No se puede conectar a $HOST"
    echo "Asegúrate de que PetClinic esté corriendo:"
    echo "  mvn spring-boot:run"
    exit 1
fi

if [ "$TOOL" == "locust" ]; then
    if ! command -v locust &> /dev/null; then
        echo -e "${RED}Locust no está instalado${NC}"
        echo "Instalar con: pip install locust"
        exit 1
    fi

    LOCUST_FILE="$(dirname "$0")/locust/locustfile.py"

    echo ""
    echo "Ejecutando pruebas escalonadas con Locust..."
    echo ""

    USER_STEPS=(10 25 50 75 100 125 150 175 200)
    SPAWN_RATE=5
    RUN_TIME="2m"

    SUMMARY_FILE="$OUTPUT_DIR/summary.csv"
    echo "users,requests,failures,avg_response_time,p99_response_time,error_rate,throughput" > "$SUMMARY_FILE"

    for users in "${USER_STEPS[@]}"; do
        echo -e "${YELLOW}Testing with $users users...${NC}"

        CSV_PREFIX="$OUTPUT_DIR/results_${users}users"

        locust -f "$LOCUST_FILE" \
            --host="$HOST" \
            --users "$users" \
            --spawn-rate "$SPAWN_RATE" \
            --run-time "$RUN_TIME" \
            --headless \
            --csv="$CSV_PREFIX" \
            --only-summary 2>&1 | tee "$OUTPUT_DIR/log_${users}users.txt"

        if [ -f "${CSV_PREFIX}_stats.csv" ]; then
            STATS=$(grep "Aggregated" "${CSV_PREFIX}_stats.csv" || echo "")

            if [ -n "$STATS" ]; then
                REQUESTS=$(echo "$STATS" | cut -d',' -f3)
                FAILURES=$(echo "$STATS" | cut -d',' -f4)
                AVG_TIME=$(echo "$STATS" | cut -d',' -f6)
                P99_TIME=$(echo "$STATS" | cut -d',' -f12)
                THROUGHPUT=$(echo "$STATS" | cut -d',' -f10)

                if [ "$REQUESTS" -gt 0 ]; then
                    ERROR_RATE=$(echo "scale=2; ($FAILURES / $REQUESTS) * 100" | bc)
                else
                    ERROR_RATE="0"
                fi

                echo "$users,$REQUESTS,$FAILURES,$AVG_TIME,$P99_TIME,$ERROR_RATE,$THROUGHPUT" >> "$SUMMARY_FILE"

                if (( $(echo "$ERROR_RATE > 2" | bc -l) )); then
                    echo -e "${RED}⚠️  Error rate ($ERROR_RATE%) exceeds 2% at $users users!${NC}"
                    echo ""
                    echo "======================================"
                    echo -e "${RED}PUNTO DE QUIEBRE ENCONTRADO: $users usuarios${NC}"
                    echo "======================================"
                    break
                else
                    echo -e "${GREEN}✓ Error rate: $ERROR_RATE% (within threshold)${NC}"
                fi
            fi
        fi

        echo ""
        sleep 5
    done

    echo ""
    echo "======================================"
    echo "RESUMEN DE RESULTADOS"
    echo "======================================"
    column -s',' -t "$SUMMARY_FILE"
    echo ""
    echo "Resultados guardados en: $OUTPUT_DIR/"

elif [ "$TOOL" == "jmeter" ]; then
    if ! command -v jmeter &> /dev/null; then
        echo -e "${RED}JMeter no está instalado${NC}"
        echo "Instalar con: brew install jmeter (macOS) o apt install jmeter (Ubuntu)"
        exit 1
    fi

    JMX_FILE="$(dirname "$0")/jmeter/petclinic_loadtest.jmx"

    echo ""
    echo "Ejecutando prueba de carga con JMeter..."
    echo ""

    RESULTS_FILE="$OUTPUT_DIR/results.jtl"
    REPORT_DIR="$OUTPUT_DIR/report"

    jmeter -n \
        -t "$JMX_FILE" \
        -l "$RESULTS_FILE" \
        -e \
        -o "$REPORT_DIR" \
        -JBASE_URL="${HOST#http://}" \
        -JPORT=8080

    echo ""
    echo "======================================"
    echo "PRUEBA COMPLETADA"
    echo "======================================"
    echo "Reporte HTML: $REPORT_DIR/index.html"
    echo ""

    if command -v xdg-open &> /dev/null; then
        xdg-open "$REPORT_DIR/index.html" 2>/dev/null &
    elif command -v open &> /dev/null; then
        open "$REPORT_DIR/index.html" 2>/dev/null &
    fi

else
    echo "Uso: $0 [locust|jmeter]"
    exit 1
fi

echo ""
echo -e "${GREEN}¡Prueba de carga completada!${NC}"
