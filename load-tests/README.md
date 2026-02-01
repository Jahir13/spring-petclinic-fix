# Pruebas de Carga para PetClinic

## Guía Completa con JMeter y Locust

---

## 📋 Índice

1. [Requisitos Previos](#requisitos-previos)
2. [Configuración de Locust (Python)](#configuración-de-locust)
3. [Configuración de JMeter](#configuración-de-jmeter)
4. [Extracción del ID del Owner](#extracción-del-id-del-owner)
5. [Métricas y Reportes](#métricas-y-reportes)
6. [Identificar Punto de Fallo (>2% Error)](#identificar-punto-de-fallo)

---

## 🔧 Requisitos Previos

### Iniciar la Aplicación PetClinic

```bash
# Desde el directorio raíz del proyecto
mvn spring-boot:run

# O con el JAR compilado
java -jar target/spring-petclinic-fix-4.0.0-SNAPSHOT.jar
```

La aplicación estará disponible en: `http://localhost:8080`

---

## 🐍 Configuración de Locust

### Instalación

```bash
# Instalar Locust
pip install locust

# Verificar instalación
locust --version
```

### Ejecutar las Pruebas

#### Modo con Interfaz Web (Recomendado para pruebas iniciales)

```bash
cd load-tests/locust
locust -f locustfile.py --host=http://localhost:8080
```

Abrir `http://localhost:8089` y configurar:

- **Number of users**: 100 (usuarios concurrentes máximos)
- **Spawn rate**: 10 (usuarios nuevos por segundo - ramp-up gradual)

#### Modo Headless (Para CI/CD o ejecución automatizada)

```bash
locust -f locustfile.py \
    --host=http://localhost:8080 \
    --users 100 \
    --spawn-rate 10 \
    --run-time 5m \
    --headless \
    --csv=results \
    --html=report.html
```

### Parámetros de Ramp-up Gradual

| Parámetro      | Valor | Descripción                 |
| -------------- | ----- | --------------------------- |
| `--users`      | 100   | Total de usuarios virtuales |
| `--spawn-rate` | 10    | Usuarios nuevos por segundo |
| `--run-time`   | 5m    | Duración total del test     |

Con estos valores, alcanzarás 100 usuarios en **10 segundos** (100/10).

---

## ☕ Configuración de JMeter

### Instalación

```bash
# Ubuntu/Debian
sudo apt-get install jmeter

# MacOS
brew install jmeter

# O descargar de: https://jmeter.apache.org/download_jmeter.cgi
```

### Ejecutar las Pruebas

#### Modo GUI (Para diseño y debug)

```bash
jmeter -t load-tests/jmeter/petclinic_loadtest.jmx
```

#### Modo Línea de Comandos (Recomendado para ejecución)

```bash
cd load-tests/jmeter

# Ejecutar y generar reporte HTML
jmeter -n \
    -t petclinic_loadtest.jmx \
    -l results.jtl \
    -e \
    -o report/
```

### Configuración del Thread Group

El archivo `.jmx` incluye la siguiente configuración:

```
Thread Group Configuration:
├── Number of Threads (users): 100
├── Ramp-up Period: 60 segundos
├── Loop Count: Infinite
├── Duration: 300 segundos (5 minutos)
└── Scheduler: Enabled
```

**Fórmula de Ramp-up:**

- Usuarios por segundo = Total Usuarios / Ramp-up Time
- 100 / 60 = **~1.67 usuarios/segundo**

---

## 🔍 Extracción del ID del Owner

### El Problema

El flujo de creación de Pet necesita el ID del Owner creado previamente:

1. `POST /owners/new` → Crea owner, redirige a `/owners/{id}`
2. `POST /owners/{id}/pets/new` → Necesita el `{id}` del paso anterior

### Solución en Locust

```python
# En locustfile.py
with self.client.post(
    "/owners/new",
    data=owner_data,
    allow_redirects=False,  # ¡Importante! No seguir redirect
    catch_response=True
) as response:

    if response.status_code == 302:
        # Extraer ID del header Location
        location = response.headers.get('Location', '')
        match = re.search(r'/owners/(\d+)', location)

        if match:
            self.owner_id = match.group(1)  # Guardar para usar después
```

### Solución en JMeter

1. **Regular Expression Extractor** (Post-Processor):

   ```
   Reference Name: OWNER_ID
   Regular Expression: Location: .*/owners/(\d+)
   Template: $1$
   Match No.: 1
   Default Value: NOT_FOUND
   Field to check: Response Headers
   ```

2. **Uso en siguiente request**:
   ```
   Path: /owners/${OWNER_ID}/pets/new
   ```

**Diagrama del Flujo:**

```
┌─────────────────┐     302 Redirect      ┌─────────────────┐
│ POST            │ ──────────────────────▶│ Location:       │
│ /owners/new     │                        │ /owners/123     │
└─────────────────┘                        └────────┬────────┘
                                                    │
                                          Extract: 123
                                                    │
                                                    ▼
                                          ┌─────────────────┐
                                          │ POST            │
                                          │ /owners/123/    │
                                          │ pets/new        │
                                          └─────────────────┘
```

---

## 📊 Métricas y Reportes

### Métricas Clave

| Métrica                      | Descripción                        | Cómo Obtener            |
| ---------------------------- | ---------------------------------- | ----------------------- |
| **Latencia (Response Time)** | Tiempo desde envío hasta respuesta | Promedio, P50, P90, P99 |
| **Throughput**               | Transacciones por segundo (TPS)    | Requests/segundo        |
| **Error Rate**               | Porcentaje de requests fallidos    | Errores/Total × 100     |
| **Concurrent Users**         | Usuarios activos simultáneos       | Thread count            |

### Locust: Ver Métricas

**Interfaz Web (`http://localhost:8089`):**

- Tab **Statistics**: Latencia (Avg, Min, Max, P50, P90, P99)
- Tab **Charts**: Throughput en tiempo real
- Tab **Failures**: Errores detallados

**Archivos CSV generados:**

```bash
results_stats.csv        # Estadísticas por endpoint
results_stats_history.csv  # Métricas en el tiempo
results_failures.csv     # Detalle de fallos
results_exceptions.csv   # Excepciones
```

### JMeter: Ver Métricas

**Reporte HTML (recomendado):**

```bash
# El reporte se genera en report/
open report/index.html
```

**Listeners en el Test Plan:**

1. **Summary Report**: Vista general con Throughput
2. **Aggregate Report**: Latencia detallada (Avg, P90, P95, P99)
3. **Response Time Graph**: Gráfico de latencia en el tiempo

---

## 🎯 Identificar Punto de Fallo (>2% Error)

### Estrategia de Prueba Escalonada

Para encontrar el punto exacto donde la tasa de error supera el 2%:

#### Opción 1: Stepped Thread Group (JMeter Plugin)

Instalar plugin y configurar:

```
Initial Users: 10
Step Users: 10
Step Duration: 60 seconds
Max Users: 200
```

Esto aumentará usuarios: 10 → 20 → 30 → ... hasta encontrar el punto de fallo.

#### Opción 2: Script de Locust Escalonado

```bash
# Ejecutar con diferentes cargas
for users in 10 25 50 75 100 125 150 175 200; do
    echo "Testing with $users users..."
    locust -f locustfile.py \
        --host=http://localhost:8080 \
        --users $users \
        --spawn-rate 5 \
        --run-time 2m \
        --headless \
        --csv=results_${users}users \
        --only-summary
done
```

### Análisis del Punto de Quiebre

**En Locust:**

```python
# El script ya incluye detección automática
@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    # Calcula y alerta si error_rate > 2%
```

**En JMeter - Assertion para Error Rate:**

```groovy
// En un JSR223 Assertion
def errorRate = (prev.getErrorCount() / prev.getSampleCount()) * 100
if (errorRate > 2) {
    AssertionResult.setFailure(true)
    AssertionResult.setFailureMessage(
        "Error rate ${errorRate}% exceeds 2% threshold"
    )
}
```

### Interpretación de Resultados

```
Ejemplo de salida esperada:
==========================================================
| Users | Throughput | Avg Latency | P99 Latency | Error % |
|-------|------------|-------------|-------------|---------|
|    10 |     50 TPS |       45 ms |      120 ms |    0.0% |
|    25 |    120 TPS |       52 ms |      150 ms |    0.1% |
|    50 |    230 TPS |       68 ms |      220 ms |    0.5% |
|    75 |    310 TPS |       95 ms |      380 ms |    1.2% |
|   100 |    380 TPS |      145 ms |      620 ms |    1.8% |
|   125 |    420 TPS |      280 ms |     1200 ms |    3.5% | ← PUNTO DE QUIEBRE
|   150 |    390 TPS |      520 ms |     2500 ms |    8.2% |
==========================================================

⚠️  El punto de quiebre está entre 100-125 usuarios concurrentes
```

---

## 📁 Estructura de Archivos

```
load-tests/
├── locust/
│   ├── locustfile.py          # Script principal de Locust
│   ├── results_stats.csv      # (generado)
│   └── report.html            # (generado)
│
└── jmeter/
    ├── petclinic_loadtest.jmx # Test plan de JMeter
    ├── results.jtl            # (generado)
    └── report/                # (generado)
        └── index.html
```

---

## 🚀 Comandos Rápidos

```bash
# === LOCUST ===
# Instalar
pip install locust

# Ejecutar con UI
cd load-tests/locust && locust -f locustfile.py --host=http://localhost:8080

# Ejecutar headless (5 min, 100 usuarios)
locust -f locustfile.py --host=http://localhost:8080 \
    --users 100 --spawn-rate 10 --run-time 5m --headless --csv=results

# === JMETER ===
# Ejecutar con reporte HTML
cd load-tests/jmeter && jmeter -n -t petclinic_loadtest.jmx -l results.jtl -e -o report/

# Ver reporte
open report/index.html  # MacOS
xdg-open report/index.html  # Linux
```

---

## ⚠️ Notas Importantes

1. **No ejecutar en producción** sin autorización
2. **Monitorear recursos** del servidor durante las pruebas (CPU, RAM, conexiones DB)
3. **Base de datos**: Las pruebas crearán muchos registros. Considera limpiar después:
   ```sql
   DELETE FROM pets WHERE name LIKE '%_%';
   DELETE FROM owners WHERE last_name LIKE '%_%';
   ```
4. **Conexiones**: Ajustar `hikari.maximum-pool-size` si hay errores de conexión
