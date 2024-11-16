import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.Desktop;

public class HoughTransform {
    private int width; // Ancho de la imagen
    private int height; // Altura de la imagen
    private int[][] houghSpace; // Espacio de Hough donde se acumulan las detecciones
    private int maxRadius; // Radio máximo posible en el espacio de Hough
    private double[] sinTable; // Tabla precalculada de valores de seno
    private double[] cosTable; // Tabla precalculada de valores de coseno
    private static final int THETA_STEP = 180; // Tamaño del paso para theta en grados

    // Constructor que inicializa el transformador de Hough
    public HoughTransform(BufferedImage image) {
        this.width = image.getWidth();
        this.height = image.getHeight();
        this.maxRadius = (int) Math.hypot(width, height); // Calcula el radio máximo
        this.houghSpace = new int[THETA_STEP][2 * maxRadius]; // Inicializa el espacio de Hough

        // Precalcula los valores de seno y coseno para cada ángulo theta
        sinTable = new double[THETA_STEP];
        cosTable = new double[THETA_STEP];
        for (int t = 0; t < THETA_STEP; t++) {
            double theta = Math.toRadians(t);
            sinTable[t] = Math.sin(theta);
            cosTable[t] = Math.cos(theta);
        }

        // Llama al método para detectar bordes y poblar el espacio de Hough
        populateHoughSpace(image);
    }

    // Método para detectar bordes y poblar el espacio de Hough
    private void populateHoughSpace(BufferedImage image) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Verifica si el píxel actual es un borde
                if (isEdgePixel(image, x, y)) {
                    // Para cada ángulo theta, calcula el radio correspondiente
                    for (int t = 0; t < THETA_STEP; t++) {
                        int r = (int) (x * cosTable[t] + y * sinTable[t]);
                        r += maxRadius; // Ajusta el radio a un rango positivo
                        if (r >= 0 && r < 2 * maxRadius) {
                            houghSpace[t][r]++; // Incrementa el acumulador en el espacio de Hough
                        }
                    }
                }
            }
        }
    }

    // Detector simple de bordes basado en un umbral
    private boolean isEdgePixel(BufferedImage image, int x, int y) {
        int color = image.getRGB(x, y); // Obtiene el color del píxel
        int brightness = (color >> 16) & 0xff; // Usa el canal rojo como brillo
        return brightness < 128; // Umbral para considerar el píxel como borde
    }

    // Encuentra líneas en el espacio de Hough que superan un umbral
    public ArrayList<Line> findLines(int threshold) {
        ArrayList<Line> lines = new ArrayList<>();
        for (int t = 0; t < THETA_STEP; t++) {
            for (int r = 0; r < 2 * maxRadius; r++) {
                // Si el valor acumulado supera el umbral, se considera una línea
                if (houghSpace[t][r] > threshold) {
                    double theta = Math.toRadians(t);
                    int radius = r - maxRadius; // Ajusta el radio al rango original
                    lines.add(new Line(radius, theta)); // Agrega la línea detectada
                }
            }
        }
        return lines;
    }

    public static void main(String[] args) {
        try {
            // Carga una imagen desde el archivo especificado
            BufferedImage image = ImageIO.read(new File("input.png"));

            // Asegura que la imagen tenga el tipo ARGB
            BufferedImage colorImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            colorImage.getGraphics().drawImage(image, 0, 0, null);

            HoughTransform houghTransform = new HoughTransform(colorImage);
            ArrayList<Line> lines = houghTransform.findLines(50); // Umbral de 50

            // Dibuja las líneas detectadas en la imagen
            for (Line line : lines) {
                line.drawLine(colorImage, new Color(255, 0, 0, 255)); // Color rojo con alfa completo
            }
            
            // Guarda el resultado en un archivo
            File outputFile = new File("output.png");
            ImageIO.write(colorImage, "png", outputFile);
            
            // Abre automáticamente la imagen de salida
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(outputFile);
            } else {
                System.out.println("Abrir la imagen automáticamente no es compatible en este sistema.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// Clase que representa una línea detectada
class Line {
    private int radius; // Radio de la línea
    private double theta; // Ángulo theta de la línea

    public Line(int radius, double theta) {
        this.radius = radius;
        this.theta = theta;
    }

    // Dibuja la línea en la imagen
    public void drawLine(BufferedImage image, Color color) {
        int width = image.getWidth();
        int height = image.getHeight();
        int redColor = color.getRGB(); // Color con alfa

        // Dibuja la línea en el eje x
        for (int x = 0; x < width; x++) {
            int y = (int) ((radius - x * Math.cos(theta)) / Math.sin(theta));
            if (y >= 0 && y < height) {
                image.setRGB(x, y, redColor);
            }
        }

        // Dibuja la línea en el eje y
        for (int y = 0; y < height; y++) {
            int x = (int) ((radius - y * Math.sin(theta)) / Math.cos(theta));
            if (x >= 0 && x < width) {
                image.setRGB(x, y, redColor);
            }
        }
    }
}
