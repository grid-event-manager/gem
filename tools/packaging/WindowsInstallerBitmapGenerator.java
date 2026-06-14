package org.gem.tools.packaging;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class WindowsInstallerBitmapGenerator {
    private WindowsInstallerBitmapGenerator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            throw new IllegalArgumentException(
                "Usage: WindowsInstallerBitmapGenerator <logo-svg> <visual-properties> <output-dir>"
            );
        }

        SvgBrandMark logo = readBrandMark(new File(args[0]));

        Properties visual = new Properties();
        try (FileInputStream input = new FileInputStream(args[1])) {
            visual.load(input);
        }

        File outputDir = new File(args[2]);
        if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
            throw new IOException("Unable to create output directory: " + outputDir);
        }

        writeDialog(logo, visual, new File(outputDir, "gem-installer-dialog.bmp"));
        writeBanner(logo, visual, new File(outputDir, "gem-installer-banner.bmp"));
    }

    private static void writeDialog(SvgBrandMark logo, Properties visual, File output) throws IOException {
        BufferedImage dialog = new BufferedImage(
            requiredInt(visual, "installer.dialog.width"),
            requiredInt(visual, "installer.dialog.height"),
            BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = dialog.createGraphics();
        try {
            fill(graphics, Color.WHITE, dialog.getWidth(), dialog.getHeight());
            graphics.setColor(requiredColor(visual, "installer.leftStripColor"));
            graphics.fillRect(0, 0, requiredInt(visual, "installer.dialog.leftStripWidth"), dialog.getHeight());
            drawLogoFrame(
                graphics,
                logo,
                requiredInt(visual, "installer.dialog.logoBoxX"),
                requiredInt(visual, "installer.dialog.logoBoxY"),
                requiredInt(visual, "installer.dialog.logoBoxSize"),
                requiredInt(visual, "installer.dialog.logoContentSize"),
                visual
            );
        } finally {
            graphics.dispose();
        }
        writeBmp(dialog, output);
    }

    private static void writeBanner(SvgBrandMark logo, Properties visual, File output) throws IOException {
        BufferedImage banner = new BufferedImage(
            requiredInt(visual, "installer.banner.width"),
            requiredInt(visual, "installer.banner.height"),
            BufferedImage.TYPE_INT_RGB
        );
        Graphics2D graphics = banner.createGraphics();
        try {
            fill(graphics, Color.WHITE, banner.getWidth(), banner.getHeight());
            drawLogoFrame(
                graphics,
                logo,
                requiredInt(visual, "installer.banner.logoBoxX"),
                requiredInt(visual, "installer.banner.logoBoxY"),
                requiredInt(visual, "installer.banner.logoBoxSize"),
                requiredInt(visual, "installer.banner.logoContentSize"),
                visual
            );
        } finally {
            graphics.dispose();
        }
        writeBmp(banner, output);
    }

    private static void drawLogoFrame(
        Graphics2D graphics,
        SvgBrandMark logo,
        int x,
        int y,
        int size,
        int logoSize,
        Properties visual
    ) {
        int cornerRadius = requiredInt(visual, "installer.logoBoxCornerRadius");
        int borderWidth = requiredInt(visual, "installer.logoBoxBorderWidth");
        Color background = requiredColor(visual, "installer.logoBoxBackground");
        Color border = requiredColor(visual, "installer.logoBoxBorder");

        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        RoundRectangle2D frame = new RoundRectangle2D.Float(
            x,
            y,
            size,
            size,
            cornerRadius,
            cornerRadius
        );
        graphics.setColor(background);
        graphics.fill(frame);

        int logoX = x + (size - logoSize) / 2;
        int logoY = y + (size - logoSize) / 2;
        drawBrandMark(graphics, logo, logoX, logoY, logoSize);

        graphics.setColor(border);
        graphics.setStroke(new BasicStroke(borderWidth));
        graphics.draw(new RoundRectangle2D.Float(
            x + borderWidth / 2.0f,
            y + borderWidth / 2.0f,
            size - borderWidth,
            size - borderWidth,
            cornerRadius,
            cornerRadius
        ));
    }

    private static void drawBrandMark(Graphics2D graphics, SvgBrandMark mark, int x, int y, int size) {
        Graphics2D brandGraphics = (Graphics2D) graphics.create();
        try {
            brandGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            brandGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            brandGraphics.translate(x, y);
            double scale = size / Math.max(mark.viewBoxWidth, mark.viewBoxHeight);
            brandGraphics.scale(scale, scale);
            brandGraphics.translate(-mark.viewBoxMinX, -mark.viewBoxMinY);
            brandGraphics.setColor(mark.strokeColor);
            brandGraphics.setStroke(new BasicStroke(
                (float) mark.strokeWidth,
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND
            ));
            for (Path2D.Double path : mark.paths) {
                brandGraphics.draw(path);
            }
        } finally {
            brandGraphics.dispose();
        }
    }

    private static void fill(Graphics2D graphics, Color color, int width, int height) {
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
    }

    private static void writeBmp(BufferedImage image, File output) throws IOException {
        if (!ImageIO.write(image, "bmp", output)) {
            throw new IOException("Unable to write BMP: " + output);
        }
    }

    private static SvgBrandMark readBrandMark(File svgFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        Element root = factory.newDocumentBuilder().parse(svgFile).getDocumentElement();
        String[] viewBoxParts = requiredAttribute(root, "viewBox").trim().split("\\s+");
        if (viewBoxParts.length != 4) {
            throw new IllegalArgumentException("Expected four-part SVG viewBox in " + svgFile);
        }

        Element group = firstElement(root.getElementsByTagName("g"), "g");
        Color strokeColor = requiredColorValue(requiredAttribute(group, "stroke"));
        double strokeWidth = Double.parseDouble(requiredAttribute(group, "stroke-width"));

        List<Path2D.Double> paths = new ArrayList<>();
        NodeList pathNodes = group.getElementsByTagName("path");
        for (int index = 0; index < pathNodes.getLength(); index++) {
            Element path = (Element) pathNodes.item(index);
            paths.add(parseSvgPath(requiredAttribute(path, "d")));
        }
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("Central SVG mark contains no paths: " + svgFile);
        }

        return new SvgBrandMark(
            Double.parseDouble(viewBoxParts[0]),
            Double.parseDouble(viewBoxParts[1]),
            Double.parseDouble(viewBoxParts[2]),
            Double.parseDouble(viewBoxParts[3]),
            strokeColor,
            strokeWidth,
            paths
        );
    }

    private static Element firstElement(NodeList nodes, String name) {
        if (nodes.getLength() == 0) {
            throw new IllegalArgumentException("Expected SVG " + name + " element");
        }
        return (Element) nodes.item(0);
    }

    private static String requiredAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Missing SVG attribute " + name);
        }
        return value;
    }

    private static Color requiredColorValue(String value) {
        if (!value.matches("#[0-9A-Fa-f]{6}")) {
            throw new IllegalArgumentException("Invalid SVG color: " + value);
        }
        return new Color(
            Integer.parseInt(value.substring(1, 3), 16),
            Integer.parseInt(value.substring(3, 5), 16),
            Integer.parseInt(value.substring(5, 7), 16)
        );
    }

    private static Path2D.Double parseSvgPath(String pathData) {
        SvgPathCursor cursor = new SvgPathCursor(pathData);
        Path2D.Double path = new Path2D.Double();
        char command = 0;
        double currentX = 0.0;
        double currentY = 0.0;
        double startX = 0.0;
        double startY = 0.0;

        while (cursor.hasMore()) {
            if (cursor.nextIsCommand()) {
                command = cursor.readCommand();
            }
            if (command == 0) {
                throw new IllegalArgumentException("SVG path data starts without command: " + pathData);
            }

            switch (command) {
                case 'M':
                case 'm': {
                    boolean relative = command == 'm';
                    double x = cursor.readNumber();
                    double y = cursor.readNumber();
                    if (relative) {
                        x += currentX;
                        y += currentY;
                    }
                    path.moveTo(x, y);
                    currentX = x;
                    currentY = y;
                    startX = x;
                    startY = y;
                    command = relative ? 'l' : 'L';
                    break;
                }
                case 'L':
                case 'l': {
                    boolean relative = command == 'l';
                    double x = cursor.readNumber();
                    double y = cursor.readNumber();
                    if (relative) {
                        x += currentX;
                        y += currentY;
                    }
                    path.lineTo(x, y);
                    currentX = x;
                    currentY = y;
                    break;
                }
                case 'H':
                case 'h': {
                    double x = cursor.readNumber();
                    if (command == 'h') {
                        x += currentX;
                    }
                    path.lineTo(x, currentY);
                    currentX = x;
                    break;
                }
                case 'V':
                case 'v': {
                    double y = cursor.readNumber();
                    if (command == 'v') {
                        y += currentY;
                    }
                    path.lineTo(currentX, y);
                    currentY = y;
                    break;
                }
                case 'Z':
                case 'z':
                    path.closePath();
                    currentX = startX;
                    currentY = startY;
                    command = 0;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported SVG path command: " + command);
            }
        }
        return path;
    }

    private static int requiredInt(Properties properties, String key) {
        String value = required(properties, key);
        return Integer.parseInt(value);
    }

    private static Color requiredColor(Properties properties, String key) {
        String value = required(properties, key);
        if (!value.matches("#[0-9A-Fa-f]{6}")) {
            throw new IllegalArgumentException("Invalid color for " + key + ": " + value);
        }
        return new Color(
            Integer.parseInt(value.substring(1, 3), 16),
            Integer.parseInt(value.substring(3, 5), 16),
            Integer.parseInt(value.substring(5, 7), 16)
        );
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing visual property: " + key);
        }
        return value;
    }

    private static final class SvgBrandMark {
        final double viewBoxMinX;
        final double viewBoxMinY;
        final double viewBoxWidth;
        final double viewBoxHeight;
        final Color strokeColor;
        final double strokeWidth;
        final List<Path2D.Double> paths;

        SvgBrandMark(
            double viewBoxMinX,
            double viewBoxMinY,
            double viewBoxWidth,
            double viewBoxHeight,
            Color strokeColor,
            double strokeWidth,
            List<Path2D.Double> paths
        ) {
            this.viewBoxMinX = viewBoxMinX;
            this.viewBoxMinY = viewBoxMinY;
            this.viewBoxWidth = viewBoxWidth;
            this.viewBoxHeight = viewBoxHeight;
            this.strokeColor = strokeColor;
            this.strokeWidth = strokeWidth;
            this.paths = paths;
        }
    }

    private static final class SvgPathCursor {
        private final String text;
        private int index = 0;

        SvgPathCursor(String text) {
            this.text = text;
        }

        boolean hasMore() {
            skipSeparators();
            return index < text.length();
        }

        boolean nextIsCommand() {
            skipSeparators();
            return index < text.length() && Character.isLetter(text.charAt(index));
        }

        char readCommand() {
            skipSeparators();
            if (index >= text.length() || !Character.isLetter(text.charAt(index))) {
                throw new IllegalArgumentException("Expected SVG path command near: " + text);
            }
            return text.charAt(index++);
        }

        double readNumber() {
            skipSeparators();
            int start = index;
            if (index < text.length() && (text.charAt(index) == '+' || text.charAt(index) == '-')) {
                index++;
            }
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            if (index < text.length() && text.charAt(index) == '.') {
                index++;
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            }
            if (index < text.length() && (text.charAt(index) == 'e' || text.charAt(index) == 'E')) {
                index++;
                if (index < text.length() && (text.charAt(index) == '+' || text.charAt(index) == '-')) {
                    index++;
                }
                while (index < text.length() && Character.isDigit(text.charAt(index))) {
                    index++;
                }
            }
            if (start == index) {
                throw new IllegalArgumentException("Expected SVG path number near: " + text);
            }
            return Double.parseDouble(text.substring(start, index));
        }

        private void skipSeparators() {
            while (index < text.length()) {
                char next = text.charAt(index);
                if (!Character.isWhitespace(next) && next != ',') {
                    return;
                }
                index++;
            }
        }
    }
}
