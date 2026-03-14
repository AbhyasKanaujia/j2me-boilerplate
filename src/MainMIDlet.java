// Starter source: github.com/AbhyasKanaujia/j2me-boilerplate
import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.midlet.MIDlet;

public class MainMIDlet extends MIDlet {
    private final Display display;
    private final StarterCanvas canvas;

    public MainMIDlet() {
        display = Display.getDisplay(this);
        canvas = new StarterCanvas();
    }

    public void startApp() {
        display.setCurrent(canvas);
        canvas.startAnimation();
    }

    public void pauseApp() {
        canvas.stopAnimation();
    }

    public void destroyApp(boolean unconditional) {
        canvas.stopAnimation();
    }

    private final class StarterCanvas extends Canvas implements CommandListener, Runnable {
        private static final int FRAME_DELAY_MS = 80;
        private static final int PARTICLE_COUNT = 22;
        private static final int PANEL_MARGIN = 4;
        private static final int MODE_MAIN = 0;
        private static final int MODE_ABOUT = 1;

        private final Command countCommand;
        private final Command aboutCommand;
        private final Command backCommand;
        private final Command exitCommand;
        private final int[] particleX;
        private final int[] particleY;
        private final int[] particleVX;
        private final int[] particleVY;
        private final int[] particleSize;
        private final int[] particlePhase;
        private final Font titleFont;
        private final Font subtitleFont;
        private final Font bodyFont;
        private final Font accentFont;

        private Thread animationThread;
        private boolean running;
        private int frameTick;
        private int interactionCount;
        private int cachedWidth;
        private int cachedHeight;
        private int scrollLine;
        private int visibleBodyLines;
        private int screenMode;
        private String[] bodyLines;

        StarterCanvas() {
            countCommand = new Command("Count", Command.OK, 1);
            aboutCommand = new Command("About", Command.SCREEN, 2);
            backCommand = new Command("Back", Command.BACK, 1);
            exitCommand = new Command("Exit", Command.EXIT, 3);

            particleX = new int[PARTICLE_COUNT];
            particleY = new int[PARTICLE_COUNT];
            particleVX = new int[PARTICLE_COUNT];
            particleVY = new int[PARTICLE_COUNT];
            particleSize = new int[PARTICLE_COUNT];
            particlePhase = new int[PARTICLE_COUNT];

            titleFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
            subtitleFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_SMALL);
            bodyFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            accentFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_SMALL);

            bodyLines = new String[0];
            frameTick = 0;
            interactionCount = 0;
            cachedWidth = 0;
            cachedHeight = 0;
            scrollLine = 0;
            visibleBodyLines = 1;
            screenMode = MODE_MAIN;

            configureCommands();
            setCommandListener(this);
        }

        void startAnimation() {
            if (running) {
                return;
            }

            running = true;
            animationThread = new Thread(this);
            animationThread.start();
        }

        void stopAnimation() {
            running = false;
            animationThread = null;
        }

        public void run() {
            while (running) {
                updateParticles();
                repaint();
                serviceRepaints();

                try {
                    Thread.sleep(FRAME_DELAY_MS);
                } catch (InterruptedException ignored) {
                    // Keep the loop predictable on MIDP runtimes.
                }
            }
        }

        protected void sizeChanged(int width, int height) {
            updateLayout(width, height);
        }

        protected void keyPressed(int keyCode) {
            int action = 0;

            try {
                action = getGameAction(keyCode);
            } catch (IllegalArgumentException ignored) {
                action = 0;
            }

            if (action == Canvas.UP || keyCode == KEY_NUM2) {
                scrollBy(-1);
                return;
            }

            if (action == Canvas.DOWN || keyCode == KEY_NUM8) {
                scrollBy(1);
                return;
            }

            if ((action == Canvas.FIRE || keyCode == KEY_NUM5) && screenMode == MODE_MAIN) {
                advanceCounter();
            }
        }

        protected void paint(Graphics graphics) {
            int width = getWidth();
            int height = getHeight();

            if (width != cachedWidth || height != cachedHeight) {
                updateLayout(width, height);
            }

            drawBackground(graphics, width, height);
            drawParticles(graphics);
            drawPanel(graphics, width, height);
            drawHeader(graphics);
            drawBody(graphics, width, height);
            drawFooter(graphics, width, height);
            drawScrollBar(graphics, width, height);
        }

        public void commandAction(Command command, Displayable displayable) {
            if (command == countCommand && screenMode == MODE_MAIN) {
                advanceCounter();
                return;
            }

            if (command == aboutCommand) {
                showAboutScreen();
                return;
            }

            if (command == backCommand) {
                showMainScreen();
                return;
            }

            if (command == exitCommand) {
                stopAnimation();
                notifyDestroyed();
            }
        }

        private void showMainScreen() {
            screenMode = MODE_MAIN;
            scrollLine = 0;
            configureCommands();
            rebuildBodyLines(cachedWidth, cachedHeight);
            repaint();
        }

        private void showAboutScreen() {
            screenMode = MODE_ABOUT;
            scrollLine = 0;
            configureCommands();
            rebuildBodyLines(cachedWidth, cachedHeight);
            repaint();
        }

        private void configureCommands() {
            removeCommand(countCommand);
            removeCommand(aboutCommand);
            removeCommand(backCommand);
            removeCommand(exitCommand);

            if (screenMode == MODE_MAIN) {
                addCommand(countCommand);
                addCommand(aboutCommand);
                addCommand(exitCommand);
            } else {
                addCommand(backCommand);
                addCommand(exitCommand);
            }
        }

        private void advanceCounter() {
            interactionCount += 1;
            repaint();
        }

        private void scrollBy(int delta) {
            int maxScroll = getMaxScroll();
            int nextScroll = scrollLine + delta;

            if (nextScroll < 0) {
                nextScroll = 0;
            }

            if (nextScroll > maxScroll) {
                nextScroll = maxScroll;
            }

            if (nextScroll != scrollLine) {
                scrollLine = nextScroll;
                repaint();
            }
        }

        private int getMaxScroll() {
            int maxScroll = bodyLines.length - visibleBodyLines;
            return maxScroll > 0 ? maxScroll : 0;
        }

        private void updateLayout(int width, int height) {
            initializeParticles(width, height);
            rebuildBodyLines(width, height);
        }

        private void initializeParticles(int width, int height) {
            int safeWidth = width > 0 ? width : 1;
            int safeHeight = height > 0 ? height : 1;

            cachedWidth = safeWidth;
            cachedHeight = safeHeight;

            for (int index = 0; index < PARTICLE_COUNT; index += 1) {
                particleX[index] = (index * 37 + 11) % safeWidth;
                particleY[index] = (index * 53 + 17) % safeHeight;
                particleVX[index] = 1 + (index % 2);
                particleVY[index] = 1 + (index % 3 == 0 ? 1 : 0);
                particleSize[index] = 1 + (index % 2);
                particlePhase[index] = (index * 19) % 31;
            }
        }

        private void rebuildBodyLines(int width, int height) {
            Vector lines = new Vector();
            int textWidth = width - 28;
            int bodyHeight = getBodyHeight(height);
            int lineStep = bodyFont.getHeight() + 1;

            if (screenMode == MODE_MAIN) {
                addParagraph(lines, "You are now running a MIDlet inside a classic feature phone emulator, the same kind of environment that powered millions of devices.", textWidth);
                addBlankLine(lines);
                addParagraph(lines, "This project is a minimal starting point. You can build your own MIDlets and run them instantly.", textWidth);
                addBlankLine(lines);
                addRawLine(lines, "Try this:");
                addParagraph(lines, "- Open src/MainMIDlet.java", textWidth);
                addParagraph(lines, "- Change the text on this screen", textWidth);
                addParagraph(lines, "- Run make run again", textWidth);
                addBlankLine(lines);
                addParagraph(lines, "Press Count to test the demo.", textWidth);
            } else {
                addParagraph(lines, "Built from the forkable starter: AbhyasKanaujia/j2me-boilerplate", textWidth);
                addBlankLine(lines);
                addParagraph(lines, "GitHub: github.com/AbhyasKanaujia/j2me-boilerplate", textWidth);
                addBlankLine(lines);
                addParagraph(lines, "Get started fast: clone it, run make setup, then make run.", textWidth);
                addBlankLine(lines);
                addParagraph(lines, "Start editing in src/MainMIDlet.java and app.jad when you fork your own copy.", textWidth);
            }

            bodyLines = new String[lines.size()];
            for (int index = 0; index < lines.size(); index += 1) {
                bodyLines[index] = (String) lines.elementAt(index);
            }

            visibleBodyLines = bodyHeight / lineStep;
            if (visibleBodyLines < 1) {
                visibleBodyLines = 1;
            }

            if (scrollLine > getMaxScroll()) {
                scrollLine = getMaxScroll();
            }
        }

        private void addParagraph(Vector lines, String paragraph, int maxWidth) {
            int length = paragraph.length();
            int cursor = 0;
            String line = "";

            while (cursor < length) {
                while (cursor < length && paragraph.charAt(cursor) == ' ') {
                    cursor += 1;
                }

                if (cursor >= length) {
                    break;
                }

                int wordEnd = cursor;
                while (wordEnd < length && paragraph.charAt(wordEnd) != ' ') {
                    wordEnd += 1;
                }

                String word = paragraph.substring(cursor, wordEnd);
                String candidate = line.length() == 0 ? word : line + " " + word;

                if (bodyFont.stringWidth(candidate) <= maxWidth) {
                    line = candidate;
                } else if (line.length() > 0) {
                    addRawLine(lines, line);
                    line = appendToken(lines, word, maxWidth);
                } else {
                    line = appendToken(lines, word, maxWidth);
                }

                cursor = wordEnd + 1;
            }

            if (line.length() > 0) {
                addRawLine(lines, line);
            }
        }

        private void addBlankLine(Vector lines) {
            addRawLine(lines, "");
        }

        private String appendToken(Vector lines, String token, int maxWidth) {
            if (bodyFont.stringWidth(token) <= maxWidth) {
                return token;
            }

            while (token.length() > 0) {
                int split = findTokenBreak(token, maxWidth);
                if (split >= token.length()) {
                    return token;
                }

                addRawLine(lines, token.substring(0, split));
                token = token.substring(split);
            }

            return "";
        }

        private int findTokenBreak(String token, int maxWidth) {
            int lastFit = 1;
            int preferredBreak = -1;
            int index;

            for (index = 1; index <= token.length(); index += 1) {
                String candidate = token.substring(0, index);
                if (bodyFont.stringWidth(candidate) > maxWidth) {
                    break;
                }

                lastFit = index;
                if (isPreferredBreakChar(token.charAt(index - 1))) {
                    preferredBreak = index;
                }
            }

            if (preferredBreak > 0) {
                return preferredBreak;
            }

            return lastFit;
        }

        private boolean isPreferredBreakChar(char value) {
            return value == '/' || value == '-' || value == '.' || value == '_' || value == ':';
        }

        private void addRawLine(Vector lines, String line) {
            lines.addElement(line);
        }

        private int getHeaderHeight() {
            return titleFont.getHeight() + subtitleFont.getHeight() + 16;
        }

        private int getFooterHeight() {
            return accentFont.getHeight() + (bodyFont.getHeight() * 2) + 18;
        }

        private int getBodyTop() {
            return PANEL_MARGIN + getHeaderHeight();
        }

        private int getBodyHeight(int height) {
            int bodyHeight = height - getHeaderHeight() - getFooterHeight() - (PANEL_MARGIN * 2) - 4;
            return bodyHeight > bodyFont.getHeight() ? bodyHeight : bodyFont.getHeight();
        }

        private void updateParticles() {
            if (cachedWidth <= 0 || cachedHeight <= 0) {
                return;
            }

            frameTick += 1;

            for (int index = 0; index < PARTICLE_COUNT; index += 1) {
                particleX[index] += particleVX[index];
                particleY[index] += particleVY[index];

                if (particleX[index] >= cachedWidth) {
                    particleX[index] = 0;
                }

                if (particleY[index] >= cachedHeight) {
                    particleY[index] = 0;
                }
            }
        }

        private void drawBackground(Graphics graphics, int width, int height) {
            int row;
            for (row = 0; row < height; row += 2) {
                int mix = (row * 160) / (height == 0 ? 1 : height);
                int red = 4;
                int green = 14 + (mix / 8);
                int blue = 26 + (mix / 2);
                graphics.setColor((red << 16) | (green << 8) | blue);
                graphics.fillRect(0, row, width, 2);
            }
        }

        private void drawParticles(Graphics graphics) {
            int index;
            for (index = 0; index < PARTICLE_COUNT; index += 1) {
                int twinkle = (frameTick + particlePhase[index]) % 24;
                int cyan = 120 + (twinkle * 4);
                int blue = 170 + (twinkle * 3);

                if (cyan > 220) {
                    cyan = 220;
                }

                if (blue > 255) {
                    blue = 255;
                }

                graphics.setColor((20 << 16) | (cyan << 8) | blue);
                graphics.fillRect(particleX[index], particleY[index], particleSize[index], particleSize[index]);
            }
        }

        private void drawPanel(Graphics graphics, int width, int height) {
            int panelX = PANEL_MARGIN;
            int panelY = PANEL_MARGIN;
            int panelWidth = width - (PANEL_MARGIN * 2);
            int panelHeight = height - (PANEL_MARGIN * 2);

            graphics.setColor(0x0A3A4A);
            graphics.fillRoundRect(panelX + 6, panelY + 6, panelWidth - 12, 20, 10, 10);

            drawGlassScanlines(graphics, panelX + 3, panelY + 28, panelWidth - 6, panelHeight - 34, 0x083244, 4);

            graphics.setColor(0x0E4E63);
            graphics.drawRoundRect(panelX - 1, panelY - 1, panelWidth + 1, panelHeight + 1, 14, 14);
            graphics.setColor(0x22B2CF);
            graphics.drawRoundRect(panelX, panelY, panelWidth - 1, panelHeight - 1, 12, 12);
            graphics.setColor(0x57DFFF);
            graphics.drawRoundRect(panelX + 1, panelY + 1, panelWidth - 3, panelHeight - 3, 10, 10);
        }

        private void drawHeader(Graphics graphics) {
            int x = PANEL_MARGIN + 8;
            int y = PANEL_MARGIN + 8;

            graphics.setFont(titleFont);
            graphics.setColor(0xA7F5FF);
            if (screenMode == MODE_MAIN) {
                graphics.drawString("J2ME Starter", x, y, Graphics.TOP | Graphics.LEFT);
            } else {
                graphics.drawString("About This Starter", x, y, Graphics.TOP | Graphics.LEFT);
            }

            y += titleFont.getHeight() + 4;
            graphics.setFont(subtitleFont);
            graphics.setColor(0x93FFE4);
            if (screenMode == MODE_MAIN) {
                graphics.drawString("You are a J2ME developer now!", x, y, Graphics.TOP | Graphics.LEFT);
            } else {
                graphics.drawString("Forkable source + quick start", x, y, Graphics.TOP | Graphics.LEFT);
            }
        }

        private void drawBody(Graphics graphics, int width, int height) {
            int x = PANEL_MARGIN + 8;
            int y = getBodyTop();
            int bodyWidth = width - 24;
            int bodyHeight = getBodyHeight(height);
            int lineStep = bodyFont.getHeight() + 1;

            graphics.setClip(x, y, bodyWidth, bodyHeight);
            graphics.setFont(bodyFont);
            graphics.setColor(0xD4F8FF);

            int visibleIndex;
            for (visibleIndex = 0; visibleIndex < visibleBodyLines; visibleIndex += 1) {
                int lineIndex = scrollLine + visibleIndex;
                if (lineIndex >= bodyLines.length) {
                    break;
                }

                graphics.drawString(bodyLines[lineIndex], x, y + (visibleIndex * lineStep), Graphics.TOP | Graphics.LEFT);
            }

            graphics.setClip(0, 0, width, height);
        }

        private void drawFooter(Graphics graphics, int width, int height) {
            int footerX = PANEL_MARGIN + 6;
            int footerHeight = getFooterHeight() - 6;
            int footerY = height - PANEL_MARGIN - footerHeight - 2;
            int footerWidth = width - 12 - (PANEL_MARGIN * 2);

            drawGlassScanlines(graphics, footerX, footerY, footerWidth, footerHeight, 0x0A3140, 3);
            graphics.setColor(0x1A7086);
            graphics.drawRoundRect(footerX, footerY, footerWidth - 1, footerHeight - 1, 10, 10);

            graphics.setFont(accentFont);
            graphics.setColor(0x9CFFF0);
            if (screenMode == MODE_MAIN) {
                graphics.drawString("Counter: " + interactionCount, footerX + 6, footerY + 4, Graphics.TOP | Graphics.LEFT);
            } else {
                graphics.drawString("Forkable starter info", footerX + 6, footerY + 4, Graphics.TOP | Graphics.LEFT);
            }

            graphics.setFont(bodyFont);
            graphics.setColor(0x8ED7E8);
            graphics.drawString("Up/Down scroll", footerX + 6, footerY + 4 + accentFont.getHeight(), Graphics.TOP | Graphics.LEFT);
            if (screenMode == MODE_MAIN) {
                graphics.drawString("Count adds 1", footerX + 6, footerY + 5 + accentFont.getHeight() + bodyFont.getHeight(), Graphics.TOP | Graphics.LEFT);
            } else {
                graphics.drawString("Back returns", footerX + 6, footerY + 5 + accentFont.getHeight() + bodyFont.getHeight(), Graphics.TOP | Graphics.LEFT);
            }
        }

        private void drawGlassScanlines(Graphics graphics, int x, int y, int width, int height, int color, int stride) {
            int row;

            graphics.setColor(color);
            for (row = y + 1; row < y + height - 1; row += stride) {
                graphics.drawLine(x + 2, row, x + width - 3, row);
            }
        }

        private void drawScrollBar(Graphics graphics, int width, int height) {
            int maxScroll = getMaxScroll();
            if (maxScroll <= 0) {
                return;
            }

            int trackX = width - 10;
            int trackY = getBodyTop();
            int trackHeight = getBodyHeight(height);
            int thumbHeight = (trackHeight * visibleBodyLines) / bodyLines.length;
            int thumbY;

            if (thumbHeight < 10) {
                thumbHeight = 10;
            }

            thumbY = trackY + ((trackHeight - thumbHeight) * scrollLine) / maxScroll;

            graphics.setColor(0x0A3140);
            graphics.fillRect(trackX, trackY, 3, trackHeight);
            graphics.setColor(0x7AE9FF);
            graphics.fillRect(trackX, thumbY, 3, thumbHeight);
        }
    }
}
