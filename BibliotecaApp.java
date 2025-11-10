import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;


// EXCEPCIONES PERSONALIZADAS

class LibroNoDisponibleException extends Exception {
    public LibroNoDisponibleException(String message) {
        super(message);
    }
}

class UsuarioSinCupoException extends Exception {
    public UsuarioSinCupoException(String message) {
        super(message);
    }
}


// ENUM DE ESTADOS DE PRÉSTAMO

enum EstadoPrestamo {
    ACTIVO,
    DEVUELTO,
    VENCIDO
}


// CLASE LIBRO

class Libro {
    private final String isbn;
    private final String titulo;
    private final String autor;
    private final int año;
    private int ejemplaresTotales;
    private int ejemplaresDisponibles;

    public Libro(String isbn, String titulo, String autor, int año, int ejemplaresTotales) {
        validarIsbn(isbn);
        validarAño(año);
        if (ejemplaresTotales < 0) throw new IllegalArgumentException("Ejemplares totales debe ser >= 0");

        this.isbn = isbn;
        this.titulo = titulo;
        this.autor = autor;
        this.año = año;
        this.ejemplaresTotales = ejemplaresTotales;
        this.ejemplaresDisponibles = ejemplaresTotales;
    }

    private void validarIsbn(String isbn) {
        if (isbn == null || !isbn.matches("\\d{13}")) {
            throw new IllegalArgumentException("ISBN debe tener 13 dígitos.");
        }
    }

    private void validarAño(int año) {
        int actual = java.time.Year.now().getValue();
        if (año <= 0 || año > actual) {
            throw new IllegalArgumentException("Año inválido.");
        }
    }

    public synchronized void prestar() throws LibroNoDisponibleException {
        if (!estaDisponible()) {
            throw new LibroNoDisponibleException("Libro no disponible: " + titulo);
        }
        ejemplaresDisponibles--;
    }

    public synchronized void devolver() {
        if (ejemplaresDisponibles < ejemplaresTotales) {
            ejemplaresDisponibles++;
        }
    }

    public synchronized boolean estaDisponible() {
        return ejemplaresDisponibles > 0;
    }

    public String getIsbn() { return isbn; }
    public String getTitulo() { return titulo; }
    public String getAutor() { return autor; }
    public int getAño() { return año; }
    public synchronized int getEjemplaresTotales() { return ejemplaresTotales; }
    public synchronized int getEjemplaresDisponibles() { return ejemplaresDisponibles; }

    @Override
    public String toString() {
        return String.format("ISBN:%s | %s - %s (%d) [%d/%d disp]",
                isbn, titulo, autor, año, ejemplaresDisponibles, ejemplaresTotales);
    }
}

// CLASE USUARIO

class Usuario {
    private static final AtomicInteger SEQ = new AtomicInteger(1);
    private final int id;
    private final String nombre;
    private final String email;
    private final Set<String> librosPrestadosIsbn = new HashSet<>();
    private BigDecimal multas = BigDecimal.ZERO;

    public static final int MAX_LIBROS = 3;
    public static final BigDecimal MULTA_MAXIMA = new BigDecimal("5000");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public Usuario(String nombre, String email) {
        if (nombre == null || nombre.trim().isEmpty()) throw new IllegalArgumentException("Nombre requerido");
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) throw new IllegalArgumentException("Email inválido");
        this.id = SEQ.getAndIncrement();
        this.nombre = nombre.trim();
        this.email = email.trim();
    }

    public synchronized boolean puedePedirPrestado() {
        return librosPrestadosIsbn.size() < MAX_LIBROS && multas.compareTo(MULTA_MAXIMA) < 0;
    }

    public synchronized void agregarLibroPrestado(String isbn) {
        librosPrestadosIsbn.add(isbn);
    }

    public synchronized void quitarLibroPrestado(String isbn) {
        librosPrestadosIsbn.remove(isbn);
    }

    public synchronized void agregarMulta(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) return;
        multas = multas.add(monto);
        if (multas.compareTo(MULTA_MAXIMA) > 0) multas = MULTA_MAXIMA;
    }

    public synchronized void pagarMultas(BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) return;
        multas = multas.subtract(monto);
        if (multas.compareTo(BigDecimal.ZERO) < 0) multas = BigDecimal.ZERO;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public synchronized BigDecimal getMultas() { return multas; }
    public synchronized int getCantidadPrestados() { return librosPrestadosIsbn.size(); }

    @Override
    public String toString() {
        return String.format("Usuario[%d] %s | email: %s | Prestados: %d | Multas: $%s",
                id, nombre, email, getCantidadPrestados(), multas.toPlainString());
    }
}


// CLASE PRESTAMO

class Prestamo {
    private static final int DIAS_PRESTAMO = 14;
    private static final BigDecimal MULTA_POR_DIA = new BigDecimal("500");

    private final String isbnLibro;
    private final int usuarioId;
    private final LocalDate fechaPrestamo;
    private LocalDate fechaDevolucion;
    private EstadoPrestamo estado;

    public Prestamo(String isbnLibro, int usuarioId) {
        this.isbnLibro = isbnLibro;
        this.usuarioId = usuarioId;
        this.fechaPrestamo = LocalDate.now();
        this.estado = EstadoPrestamo.ACTIVO;
    }

    public LocalDate fechaVencimiento() {
        return fechaPrestamo.plusDays(DIAS_PRESTAMO);
    }

    public BigDecimal calcularMulta() {
        long diasRetraso;
        if (estado == EstadoPrestamo.DEVUELTO && fechaDevolucion != null) {
            diasRetraso = ChronoUnit.DAYS.between(fechaVencimiento(), fechaDevolucion);
        } else {
            diasRetraso = ChronoUnit.DAYS.between(fechaVencimiento(), LocalDate.now());
        }
        if (diasRetraso <= 0) return BigDecimal.ZERO;
        return MULTA_POR_DIA.multiply(BigDecimal.valueOf(diasRetraso));
    }

    public void marcarDevuelto() {
        this.fechaDevolucion = LocalDate.now();
        this.estado = fechaDevolucion.isAfter(fechaVencimiento()) ? EstadoPrestamo.VENCIDO : EstadoPrestamo.DEVUELTO;
    }

    public String getIsbnLibro() { return isbnLibro; }
    public int getUsuarioId() { return usuarioId; }
    public EstadoPrestamo getEstado() { return estado; }

    @Override
    public String toString() {
        return String.format("Prestamo{usuario=%d, isbn=%s, inicio=%s, venc=%s, estado=%s, multaActual=$%s}",
                usuarioId, isbnLibro, fechaPrestamo, fechaVencimiento(), estado, calcularMulta().toPlainString());
    }
}


// CLASE BIBLIOTECA

class Biblioteca {
    private final Map<String, Libro> libros = new ConcurrentHashMap<>();
    private final Map<Integer, Usuario> usuarios = new ConcurrentHashMap<>();
    private final List<Prestamo> prestamos = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Integer> contadorPrestamos = new ConcurrentHashMap<>();

    public synchronized void agregarLibro(Libro libro) { libros.put(libro.getIsbn(), libro); }
    public synchronized void registrarUsuario(Usuario usuario) { usuarios.put(usuario.getId(), usuario); }

    public synchronized Prestamo realizarPrestamo(int usuarioId, String isbn)
            throws UsuarioSinCupoException, LibroNoDisponibleException {
        Usuario u = usuarios.get(usuarioId);
        Libro l = libros.get(isbn);
        if (u == null || l == null) throw new IllegalArgumentException("Usuario o libro no encontrado.");
        if (!u.puedePedirPrestado()) throw new UsuarioSinCupoException("El usuario no puede pedir más libros.");
        l.prestar();
        Prestamo p = new Prestamo(isbn, usuarioId);
        prestamos.add(p);
        u.agregarLibroPrestado(isbn);
        contadorPrestamos.merge(isbn, 1, Integer::sum);
        return p;
    }

    public synchronized BigDecimal devolverLibro(int usuarioId, String isbn) {
        Usuario u = usuarios.get(usuarioId);
        Libro l = libros.get(isbn);
        if (u == null || l == null) throw new IllegalArgumentException("Usuario o libro no encontrado.");
        Optional<Prestamo> pr = prestamos.stream()
                .filter(x -> x.getUsuarioId() == usuarioId && x.getIsbnLibro().equals(isbn) && x.getEstado() == EstadoPrestamo.ACTIVO)
                .findFirst();
        if (pr.isEmpty()) return BigDecimal.ZERO;
        Prestamo p = pr.get();
        p.marcarDevuelto();
        BigDecimal multa = p.calcularMulta();
        if (multa.compareTo(BigDecimal.ZERO) > 0) u.agregarMulta(multa);
        l.devolver();
        u.quitarLibroPrestado(isbn);
        return multa;
    }

    public List<Libro> listarLibrosDisponibles() {
        return libros.values().stream().filter(Libro::estaDisponible).toList();
    }

    public List<Usuario> obtenerUsuariosConMultas() {
        return usuarios.values().stream()
                .filter(u -> u.getMultas().compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    public List<Libro> obtenerTopLibrosPrestados(int n) {
        return contadorPrestamos.entrySet().stream()
                .sorted(Map.Entry.<String,Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(n)
                .map(e -> libros.get(e.getKey()))
                .filter(Objects::nonNull)
                .toList();
    }

    public List<Prestamo> obtenerPrestamosPorUsuario(int id) {
        return prestamos.stream().filter(p -> p.getUsuarioId() == id).toList();
    }

    public Collection<Usuario> listarUsuarios() { return usuarios.values(); }
}


// CLASE PRINCIPAL CON MENÚ

public class BibliotecaApp {
    private static final Scanner sc = new Scanner(System.in);
    private static final Biblioteca bib = new Biblioteca();

    public static void main(String[] args) {
        precargar();
        int op;
        do {
            menu();
            op = leerInt("Opción: ");
            try {
                switch (op) {
                    case 1 -> agregarLibro();
                    case 2 -> registrarUsuario();
                    case 3 -> prestar();
                    case 4 -> devolver();
                    case 5 -> listarDisponibles();
                    case 6 -> prestamosUsuario();
                    case 7 -> usuariosConMultas();
                    case 8 -> topLibros();
                    case 9 -> System.out.println("Hasta luego.");
                    default -> System.out.println("Opción inválida.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        } while (op != 9);
    }

    private static void menu() {
        System.out.println("\n=== SISTEMA DE BIBLIOTECA ===");
        System.out.println("1. Agregar libro");
        System.out.println("2. Registrar usuario");
        System.out.println("3. Realizar préstamo");
        System.out.println("4. Devolver libro");
        System.out.println("5. Consultar libros disponibles");
        System.out.println("6. Consultar préstamos de usuario");
        System.out.println("7. Listar usuarios con multas");
        System.out.println("8. Top 5 libros más prestados");
        System.out.println("9. Salir");
    }

    private static void agregarLibro() {
        System.out.println("== Agregar libro ==");
        String isbn = leer("ISBN (13 dígitos): ");
        String titulo = leer("Título: ");
        String autor = leer("Autor: ");
        int año = leerInt("Año: ");
        int tot = leerInt("Ejemplares totales: ");
        bib.agregarLibro(new Libro(isbn, titulo, autor, año, tot));
    }

    private static void registrarUsuario() {
        System.out.println("== Registrar usuario ==");
        String nombre = leer("Nombre: ");
        String email = leer("Email: ");
        Usuario u = new Usuario(nombre, email);
        bib.registrarUsuario(u);
        System.out.println("Usuario creado: " + u);
    }

    private static void prestar() throws Exception {
        System.out.println("== Realizar préstamo ==");
        int id = leerInt("ID usuario: ");
        String isbn = leer("ISBN: ");
        Prestamo p = bib.realizarPrestamo(id, isbn);
        System.out.println("Préstamo realizado: " + p);
    }

    private static void devolver() {
        System.out.println("== Devolver libro ==");
        int id = leerInt("ID usuario: ");
        String isbn = leer("ISBN: ");
        BigDecimal multa = bib.devolverLibro(id, isbn);
        if (multa.compareTo(BigDecimal.ZERO) > 0)
            System.out.println("Libro devuelto con multa: $" + multa);
        else
            System.out.println("Libro devuelto sin multa.");
    }

    private static void listarDisponibles() {
        System.out.println("== Libros disponibles ==");
        bib.listarLibrosDisponibles().forEach(System.out::println);
    }

    private static void prestamosUsuario() {
        int id = leerInt("ID usuario: ");
        bib.obtenerPrestamosPorUsuario(id).forEach(System.out::println);
    }

    private static void usuariosConMultas() {
        System.out.println("== Usuarios con multas ==");
        bib.obtenerUsuariosConMultas().forEach(System.out::println);
    }

    private static void topLibros() {
        System.out.println("== Top 5 libros más prestados ==");
        bib.obtenerTopLibrosPrestados(5).forEach(System.out::println);
    }

    private static String leer(String msg) {
        System.out.print(msg);
        return sc.nextLine().trim();
    }

    private static int leerInt(String msg) {
        System.out.print(msg);
        while (!sc.hasNextInt()) {
            sc.next();
            System.out.print("Ingrese un número: ");
        }
        int n = sc.nextInt();
        sc.nextLine();
        return n;
    }

    private static void precargar() {
        bib.agregarLibro(new Libro("9780306406157", "Las aventuras del mago", "Rodrigo Alfonso ", 1980, 300));
        bib.agregarLibro(new Libro("9783161484100", "Antes de morir", "Jorge Salamar", 2000, 600));
        bib.registrarUsuario(new Usuario("Felipe", "felipe@gmail.com"));
        bib.registrarUsuario(new Usuario("Tatiana", "tatiana@gmail.com"));
    }
}

