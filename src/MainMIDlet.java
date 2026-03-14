import java.util.Calendar;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.midlet.MIDlet;

public class MainMIDlet extends MIDlet {
    // 2011 roughly marks when Android/iOS became the clear mainstream mobile focus.
    private static final int J2ME_INFLECTION_YEAR = 2011;

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

        private final Command nextStepCommand;
        private final Command exitCommand;

        private final int[] particleX;
        private final int[] particleY;
        private final int[] particleVX;
        private final int[] particleVY;
        private final int[] particleSize;
        private final int[] particlePhase;

        private Thread animationThread;
        private boolean running;
        private int frameTick;
        private int interactionCount;
        private int yearsSinceInflection;

        private int cachedWidth;
        private int cachedHeight;

        StarterCanvas() {
            nextStepCommand = new Command("Next Step", Command.OK, 1);
            exitCommand = new Command("Exit", Command.EXIT, 2);

            particleX = new int[PARTICLE_COUNT];
            particleY = new int[PARTICLE_COUNT];
            particleVX = new int[PARTICLE_COUNT];
            particleVY = new int[PARTICLE_COUNT];
            particleSize = new int[PARTICLE_COUNT];
            particlePhase = new int[PARTICLE_COUNT];

            yearsSinceInflection = Calendar.getInstance().get(Calendar.YEAR) - J2ME_INFLECTION_YEAR;
            if (yearsSinceInflection < 0) {
                yearsSinceInflection = 0;
            }

            interactionCount = 0;
            frameTick = 0;
            cachedWidth = 0;
            cachedHeight = 0;

            addCommand(nextStepCommand);
            addCommand(exitCommand);
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
                    // Keep loop simple for MIDP runtimes.
                }
            }
        }

        protected void sizeChanged(int width, int height) {
            initializeParticles(width, height);
        }

        protected void paint(Graphics graphics) {
            int width = getWidth();
            int height = getHeight();

            if (width != cachedWidth || height != cachedHeight) {
                initializeParticles(width, height);
            }

            drawBackground(graphics, width, height);
            drawParticles(graphics, width, height);
            drawGlowCard(graphics, width, height);
            drawContent(graphics, width, height);
        }

        public void commandAction(Command command, Displayable displayable) {
            if (command == nextStepCommand) {
                interactionCount += 1;
                repaint();
                return;
            }

            if (command == exitCommand) {
                stopAnimation();
                notifyDestroyed();
            }
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

        private void drawParticles(Graphics graphics, int width, int height) {
            int index;
            for (index = 0; index < PARTICLE_COUNT; index += 1) {
                int twinkle = (frameTick + particlePhase[index]) % 24;
                int cyan = 120 + (twinkle * 4);
                if (cyan > 220) {
                    cyan = 220;
                }
                int blue = 170 + (twinkle * 3);
                if (blue > 255) {
                    blue = 255;
                }

                graphics.setColor((20 << 16) | (cyan << 8) | blue);
                graphics.fillRect(particleX[index], particleY[index], particleSize[index], particleSize[index]);
            }

            graphics.setColor(0x112B3A);
            graphics.drawLine(0, height - 1, width, height - 1);
        }

        private void drawGlowCard(Graphics graphics, int width, int height) {
            int cardX = 4;
            int cardY = 4;
            int cardWidth = width - 8;
            int cardHeight = height - 8;

            // "Glass" panel effect: striped fill keeps the background particles visible.
            graphics.setColor(0x072535);
            int row;
            for (row = cardY + 2; row < cardY + cardHeight - 2; row += 3) {
                graphics.drawLine(cardX + 2, row, cardX + cardWidth - 3, row);
            }

            graphics.setColor(0x0E4E63);
            graphics.drawRoundRect(cardX - 1, cardY - 1, cardWidth + 1, cardHeight + 1, 14, 14);

            graphics.setColor(0x22B2CF);
            graphics.drawRoundRect(cardX, cardY, cardWidth - 1, cardHeight - 1, 12, 12);

            graphics.setColor(0x57DFFF);
            graphics.drawRoundRect(cardX + 1, cardY + 1, cardWidth - 3, cardHeight - 3, 10, 10);
        }

        private void drawContent(Graphics graphics, int width, int height) {
            Font titleFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
            Font bodyFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            Font accentFont = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_SMALL);

            int x = 10;
            int y = 10;
            int textWidth = width - 20;
            int bodyLimitY = height - (accentFont.getHeight() + bodyFont.getHeight() + 14);

            graphics.setFont(titleFont);
            graphics.setColor(0x9EF4FF);
            graphics.drawString("J2ME Starter", x, y, Graphics.TOP | Graphics.LEFT);
            y += titleFont.getHeight() + 5;

            graphics.setFont(bodyFont);
            graphics.setColor(0xD0F6FF);
            y = drawWrappedParagraph(
                    graphics,
                    bodyFont,
                    "Congratulations. You successfully set up your J2ME dev environment.",
                    x,
                    y,
                    textWidth,
                    bodyLimitY);
            y = drawWrappedParagraph(
                    graphics,
                    bodyFont,
                    "You are taking time to explore a legacy platform most people overlook, "
                            + yearsSinceInflection
                            + " years after it stopped being mainstream.",
                    x,
                    y,
                    textWidth,
                    bodyLimitY);
            y = drawWrappedParagraph(
                    graphics,
                    bodyFont,
                    "Fork this project. Start with src/MainMIDlet.java and app.jad.",
                    x,
                    y,
                    textWidth,
                    bodyLimitY);

            graphics.setFont(accentFont);
            graphics.setColor(0x92FFE8);
            y = height - (accentFont.getHeight() + bodyFont.getHeight() + 9);
            graphics.drawString("Next Step count: " + interactionCount, x, y, Graphics.TOP | Graphics.LEFT);

            graphics.setFont(bodyFont);
            graphics.setColor(0x8ED7E8);
            graphics.drawString("Use Next Step or Exit.", x, height - bodyFont.getHeight() - 5, Graphics.TOP | Graphics.LEFT);
        }

        private int drawWrappedParagraph(
                Graphics graphics,
                Font font,
                String paragraph,
                int x,
                int y,
                int maxWidth,
                int maxY) {
            int lineHeight = font.getHeight();
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
                if (font.stringWidth(candidate) <= maxWidth || line.length() == 0) {
                    line = candidate;
                } else {
                    if (y + lineHeight > maxY) {
                        return y;
                    }
                    graphics.drawString(line, x, y, Graphics.TOP | Graphics.LEFT);
                    y += lineHeight;
                    line = word;
                }

                cursor = wordEnd + 1;
            }

            if (line.length() > 0 && y + lineHeight <= maxY) {
                graphics.drawString(line, x, y, Graphics.TOP | Graphics.LEFT);
                y += lineHeight;
            }

            return y + 2;
        }
    }
}
