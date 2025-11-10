# Parcial 3 - Sistema de Gestión de Biblioteca

Este proyecto es un programa hecho en Java que sirve para manejar una biblioteca.  
Permite registrar libros, usuarios y hacer préstamos con control de fechas, multas y reportes.

---

##  Para qué sirve
El sistema ayuda a organizar el préstamo de libros en una biblioteca.  
Controla los libros disponibles, los usuarios registrados y las multas por retrasos.  
También se pueden ver los libros más prestados y los usuarios que tienen multas pendientes.

---

##  Cómo funciona
El programa corre por consola y muestra un menú con varias opciones.  
El usuario elige qué quiere hacer escribiendo el número de la opción.  
Cada parte está hecha con clases separadas para aplicar la Programación Orientada a Objetos (OOP).

### Menú principal
1. Agregar libro  
2. Registrar usuario  
3. Realizar préstamo  
4. Devolver libro  
5. Consultar libros disponibles  
6. Consultar préstamos de usuario  
7. Listar usuarios con multas  
8. Top 5 libros más prestados  
9. Salir  

---

##  Clases del sistema

- **Libro:** guarda el ISBN, título, autor, año y cantidad de ejemplares.  
  Valida que el ISBN tenga 13 dígitos y el año sea válido.  
  Tiene métodos para prestar, devolver y saber si está disponible.

- **Usuario:** maneja el nombre, correo, libros prestados y las multas.  
  Solo puede tener máximo 3 libros prestados y multas menores a $5000.  
  Valida que el correo sea correcto y usa un ID automático.

- **Prestamo:** guarda las fechas del préstamo y calcula las multas.  
  Tiene 14 días de plazo y cobra $500 por día de retraso.  
  Usa un enum llamado **EstadoPrestamo** (ACTIVO, DEVUELTO, VENCIDO).

- **Biblioteca:** junta todo. Guarda las listas de libros, usuarios y préstamos.  
  Tiene métodos para agregar libros, registrar usuarios, hacer préstamos y reportes.

- **BibliotecaApp:** es la clase principal.  
  Muestra el menú, llama los métodos de la biblioteca y controla el flujo del programa.

- **Excepciones personalizadas:**  
  - `LibroNoDisponibleException` → cuando no hay ejemplares disponibles.  
  - `UsuarioSinCupoException` → cuando el usuario ya no puede pedir más libros.

---

##  Reglas del sistema

Estas son las reglas que cumple el programa según la guía del parcial:

### Libros
- El ISBN debe tener exactamente **13 dígitos** (por ejemplo: 2334568790012).  
- El año debe ser real (no puede ser del futuro).  
- Cada libro tiene una cantidad total y disponible de ejemplares.  
- No se puede prestar un libro si no hay ejemplares disponibles.

### Usuarios
- Cada usuario tiene un **ID automático**, nombre y correo válido.  
- Puede tener **máximo 3 libros prestados al mismo tiempo**.  
- No puede pedir más libros si tiene **multas mayores o iguales a $5000**.  
- Puede pagar sus multas o devolver libros para recuperar su cupo.

### Préstamos
- Cada préstamo dura **14 días** desde la fecha de préstamo.  
- Si el libro se devuelve después de esos 14 días, se cobra una **multa de $500 por día de retraso**.  
- El estado del préstamo puede ser:
  - `ACTIVO`: cuando el usuario aún no devuelve el libro.
  - `DEVUELTO`: cuando se devuelve a tiempo.
  - `VENCIDO`: cuando se devuelve tarde o aún no se devuelve y ya pasó la fecha.

###  Reportes
- El sistema muestra:
  - Los libros más prestados (Top 5).  
  - Los usuarios que tienen multas pendientes.  
  - Los libros disponibles para préstamo.

---

##  Multas y control
- Se calculan automáticamente al devolver un libro.  
- Se acumulan hasta un máximo de **$5000** por usuario.  
- Si se supera ese valor, el usuario queda bloqueado hasta que pague o devuelva.  

---

##  Tecnologías usadas
- Lenguaje: Java  
- Colecciones: HashMap, ArrayList, ConcurrentHashMap  
- Tipos de datos: BigDecimal (para dinero)  
- Concurrencia: synchronized y AtomicInteger  
- Validaciones: Regex para email y formato de ISBN  
- Streams: para ordenar, filtrar y generar reportes


