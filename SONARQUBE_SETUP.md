# Guía de Configuración y Análisis con SonarQube

Esta guía documenta cómo configurar y ejecutar el análisis de calidad de código del proyecto Spring PetClinic utilizando SonarQube.

## 📋 Requisitos Previos

- Docker instalado y funcionando
- Maven 3.6+
- Java 17+
- Proyecto Spring PetClinic clonado

## 🚀 Paso 1: Configurar SonarQube con Docker

### 1.1 Iniciar el contenedor de SonarQube

```bash
docker run -d --name sonarqube \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
  -p 9000:9000 \
  sonarqube:latest
```

**Explicación de los parámetros:**

- `-d`: Ejecuta el contenedor en modo "detached" (segundo plano)
- `--name sonarqube`: Asigna el nombre "sonarqube" al contenedor
- `-e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true`: Deshabilita las comprobaciones de bootstrap de Elasticsearch (necesario para entornos de desarrollo)
- `-p 9000:9000`: Mapea el puerto 9000 del contenedor al puerto 9000 del host
- `sonarqube:latest`: Usa la última versión de la imagen de SonarQube

### 1.2 Verificar que SonarQube está funcionando

Espera unos 2-3 minutos para que SonarQube inicie completamente, luego accede a:

```
http://localhost:9000
```

**Credenciales por defecto:**

- Usuario: `admin`
- Contraseña: `admin`

> ⚠️ **Importante:** Al primer acceso, SonarQube te pedirá cambiar la contraseña.

### 1.3 Verificar el estado del contenedor

```bash
# Ver logs del contenedor
docker logs -f sonarqube

# Verificar que el contenedor está corriendo
docker ps | grep sonarqube
```

## 🔑 Paso 2: Generar Token de Autenticación

### 2.1 Crear un User Token en SonarQube

1. Inicia sesión en SonarQube: http://localhost:9000
2. Ve a tu perfil de usuario (esquina superior derecha)
3. Selecciona **"My Account"** → **"Security"** → **"Tokens"**
4. En el campo "Generate Tokens":
   - **Name**: `spring-petclinic-analysis` (o el nombre que prefieras)
   - **Type**: Selecciona **"User Token"**
   - **Expires in**: `30 days` (o según tu preferencia)
5. Haz clic en **"Generate"**
6. **¡IMPORTANTE!** Copia el token generado inmediatamente (no podrás verlo de nuevo)

### 2.2 Configurar el token en el archivo .env

Crea un archivo `.env` en la raíz del proyecto:

```bash
cd /home/jrocha/university/vv/spring-petclinic-fix
touch .env
```

Añade el token al archivo `.env`:

```env
SONAR_TOKEN=squ_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

> 🔒 **Seguridad:** El archivo `.env` está incluido en `.gitignore`, por lo que no se subirá al repositorio.

## 🔧 Paso 3: Configuración del Proyecto

### 3.1 Configuración en pom.xml

El archivo `pom.xml` ya contiene la configuración necesaria para SonarQube:

```xml
<properties>
    <!-- ... otras propiedades ... -->
    <sonar.host.url>http://localhost:9000</sonar.host.url>
    <sonar.token>${env.SONAR_TOKEN}</sonar.token>
</properties>
```

Esta configuración:

- Define la URL del servidor SonarQube
- Lee el token desde la variable de entorno `SONAR_TOKEN`

### 3.2 Archivo .gitignore

Se agregó `.env` al `.gitignore` para proteger el token:

```gitignore
target
.metals
.vscode
.env
```

## 📊 Paso 4: Crear el Proyecto en SonarQube

### 4.1 Opción A: Crear manualmente desde la UI

1. Accede a http://localhost:9000
2. Haz clic en **"Create Project"**
3. Selecciona **"Manually"**
4. Ingresa:
   - **Project key**: `ec.edu.epn:spring-petclinic-fix`
   - **Display name**: `spring-petclinic-fix`
5. Haz clic en **"Set Up"**

### 4.2 Opción B: Crear automáticamente vía API

```bash
curl -X POST -u squ_xxxxxx: \
  "http://localhost:9000/api/projects/create?project=ec.edu.epn:spring-petclinic-fix&name=spring-petclinic-fix"
```

> 💡 El proyecto se crea automáticamente en el primer análisis si no existe.

## 🏃 Paso 5: Ejecutar el Análisis

### 5.1 Cargar las variables de entorno

```bash
# Cargar las variables del archivo .env
set -a && source .env && set +a
```

### 5.2 Compilar el proyecto

```bash
mvn clean compile
```

### 5.3 Ejecutar el análisis de SonarQube

```bash
mvn sonar:sonar
```

**Salida esperada:**

```
[INFO] ANALYSIS SUCCESSFUL, you can find the results at: http://localhost:9000/dashboard?id=ec.edu.epn%3Aspring-petclinic-fix
[INFO] BUILD SUCCESS
```

### 5.4 Ver los resultados

Accede al dashboard de SonarQube:

```
http://localhost:9000/dashboard?id=ec.edu.epn%3Aspring-petclinic-fix
```

## 🔄 Paso 6: Ejecutar Análisis Posteriores

Para análisis futuros, simplemente ejecuta:

```bash
# Cargar variables de entorno
set -a && source .env && set +a

# Ejecutar análisis
mvn sonar:sonar
```

O en un solo comando:

```bash
set -a && source .env && set +a && mvn sonar:sonar
```

## ⚠️ Solución de Problemas

### Error: "SonarQube server cannot be reached"

**Causa:** El servidor SonarQube no está corriendo.

**Solución:**

```bash
# Verificar si el contenedor está corriendo
docker ps | grep sonarqube

# Si no está corriendo, iniciarlo
docker start sonarqube

# Esperar 2-3 minutos y verificar
curl http://localhost:9000/api/system/status
```

### Error: "Not authorized"

**Causa:** El token no está configurado correctamente o ha expirado.

**Solución:**

1. Verifica que el archivo `.env` existe y contiene el token
2. Carga las variables de entorno: `set -a && source .env && set +a`
3. Verifica el token: `echo $SONAR_TOKEN`
4. Si el token expiró, genera uno nuevo en SonarQube

### Error: "Failed to execute goal... Compilation failure"

**Causa:** Errores de compilación en el código.

**Solución:**

```bash
# Compilar primero para ver los errores
mvn clean compile

# Corregir los errores y volver a intentar
mvn sonar:sonar
```

## 🛑 Detener y Limpiar

### Detener SonarQube

```bash
docker stop sonarqube
```

### Reiniciar SonarQube

```bash
docker start sonarqube
```

### Eliminar el contenedor (⚠️ Esto borrará todos los datos)

```bash
docker stop sonarqube
docker rm sonarqube
```

### Eliminar el contenedor y la imagen

```bash
docker stop sonarqube
docker rm sonarqube
docker rmi sonarqube:latest
```

## 📚 Recursos Adicionales

- [Documentación oficial de SonarQube](https://docs.sonarqube.org/)
- [SonarQube Maven Plugin](https://docs.sonarqube.org/latest/analyzing-source-code/scanners/sonarscanner-for-maven/)
- [Clean Code con SonarQube](https://rules.sonarsource.com/java/)

## 💡 Mejores Prácticas

1. **Ejecuta el análisis regularmente**: Idealmente antes de cada commit o merge
2. **Mantén el Quality Gate en verde**: No permitas que se acumulen issues
3. **Revisa los Security Hotspots**: Aunque no sean vulnerabilidades confirmadas, requieren revisión
4. **Configura Quality Gates personalizados**: Ajusta los umbrales según las necesidades de tu proyecto
5. **Usa ramas en SonarQube**: Para analizar branches y pull requests por separado

---

**Fecha de última actualización:** Enero 29, 2026
**Versión del proyecto:** 4.0.0-SNAPSHOT
**Versión de SonarQube:** Latest (26.1.0.118079)
