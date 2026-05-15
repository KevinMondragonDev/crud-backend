# CRUD Backend - API REST de Gestión de Productos

API REST desarrollada en Java 21 con Spring MVC, Hibernate y MySQL para gestión de productos (CRUD completo).

## Stack Tecnológico

- **Java 21** + **Spring MVC 6.1.8** (sin Spring Boot)
- **Hibernate 6.4.4** (JPA) + **MySQL 8.x**
- **HikariCP 5.1.0** (Connection Pool)
- **Jackson 2.17** (JSON + Java Time)
- **Hibernate Validator 8.0.1** (Jakarta Bean Validation)
- **Apache Tomcat 10.1** (WAR deployment)
- **Docker** (multi-stage build)

## Endpoints

| Método | Ruta | Descripción | Código |
|--------|------|-------------|--------|
| GET | `/api/products` | Listar todos los productos | 200 |
| GET | `/api/products/{id}` | Consultar producto por ID | 200 / 404 |
| POST | `/api/products` | Crear producto | 201 |
| PUT | `/api/products/{id}` | Actualizar producto | 200 / 404 |
| DELETE | `/api/products/{id}` | Eliminar producto | 200 / 404 |

### Ejemplo de body (POST/PUT)

```json
{
  "name": "Laptop HP",
  "description": "Laptop 15 pulgadas",
  "price": 15999.99,
  "quantity": 10
}
```

## Requisitos

- **JDK 21**
- **Maven 3.9+**
- **MySQL 8.x** con base de datos `crud_db`

## Variables de Entorno

| Variable | Default | Descripción |
|----------|---------|-------------|
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/crud_db` | URL de conexión JDBC |
| `MYSQL_USER` | `root` | Usuario MySQL |
| `MYSQL_PASSWORD` | `root123` | Contraseña MySQL |

## Compilar y Ejecutar

### Con Maven + Tomcat

```bash
mvn clean package -DskipTests
# Desplegar crud-backend.war en Tomcat 10.1
```

### Con Docker

```bash
docker build -t crud-backend .
docker run -p 8080:8080 \
  -e MYSQL_URL=jdbc:mysql://host.docker.internal:3306/crud_db \
  -e MYSQL_USER=root \
  -e MYSQL_PASSWORD=root123 \
  crud-backend
```

## Estructura del Proyecto

```
src/main/java/com/crud/backend/
├── config/
│   ├── AppConfig.java              # DataSource, EntityManager, TransactionManager
│   ├── AppInitializer.java         # Servlet Initializer, UTF-8 filter
│   └── WebConfig.java              # CORS, Jackson, MessageConverters
├── controller/
│   ├── ProductController.java      # REST endpoints CRUD
│   └── GlobalExceptionHandler.java # Manejo centralizado de errores
├── service/
│   └── ProductService.java         # Lógica de negocio, transacciones
├── repository/
│   └── ProductRepository.java      # Acceso a datos JPA/EntityManager
└── model/
    └── Product.java                # Entidad JPA con validación
```

## Documentación Automatizada (DDR/DDS)

```bash
pip install python-docx
python generar_plantillas.py
python inyector_word.py DDR_plantilla.docx DDR_Final.docx datos_documentacion.json
python inyector_word.py DDS_plantilla.docx DDS_Final.docx datos_documentacion.json
```
