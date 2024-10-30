package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.Character;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

@Getter
@Setter
public class TimeSheet extends JPanel
{
    private ToolBoxFrame toolBox;

    private final BufferedImage keyframeImage = ImageUtil.loadImageResource(getClass(), "/Keyframe.png");

    private double zoom = 50;
    private double hScroll = 0;
    private int vScroll = 0;
    private int selectedIndex = 0;

    private double currentTime = 0;
    private double previewTime = 0;
    private boolean timeIndicatorPressed = false;
    private boolean drawingRectangleSelect = false;
    private Point rectangleSelectStart = new Point(0, 0);

    public final int ROW_HEIGHT = 24;
    public final int ROW_HEIGHT_OFFSET = 1;
    public final int TEXT_HEIGHT_OFFSET = 5;
    private int indexBuffers = 1;

    public final int SHOW_5_ZOOM = 200;
    public final int SHOW_1_ZOOM = 50;

    private KeyFrame[] visibleKeyFrames = new KeyFrame[0];
    private Character selectedCharacter;

    public TimeSheet(ToolBoxFrame toolBox)
    {
        this.toolBox = toolBox;

        setBorder(new LineBorder(ColorScheme.MEDIUM_GRAY_COLOR, 1));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setLayout(new BorderLayout());
        setFocusable(true);
        requestFocusInWindow();

        setKeyBindings();
        setMouseListeners(this);

        revalidate();
        repaint();
    }

    public void addVisibleKeyFrame(KeyFrame keyFrame)
    {
        visibleKeyFrames = ArrayUtils.add(visibleKeyFrames, keyFrame);
    }

    public void removeVisibleKeyFrame(KeyFrame keyFrame)
    {
        visibleKeyFrames = ArrayUtils.removeElement(visibleKeyFrames, keyFrame);
    }

    public void onVerticalScrollEvent(int scroll)
    {
        vScroll = scroll;
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(1));
        drawHighlight(g2);
        drawBackgroundLines(g2);
        drawRectangleSelect(g2);
        drawTextHeader(g2);
        drawTimeIndicator(g2);
        drawPreviewTimeIndicator(g2);
        drawKeyFrames(g2);
        revalidate();
        repaint();
    }

    private void drawRectangleSelect(Graphics2D g)
    {
        if (!drawingRectangleSelect)
        {
            return;
        }

        Point absoluteMouse = MouseInfo.getPointerInfo().getLocation();

        int x1 = (int) rectangleSelectStart.getX();
        int x2 = (int) (absoluteMouse.getX() - getLocationOnScreen().getX());
        int y1 = (int) rectangleSelectStart.getY();
        int y2 = (int) (absoluteMouse.getY() - getLocationOnScreen().getY());

        int startX;
        int startY;
        int endX;
        int endY;

        if (x1 < x2)
        {
            startX = x1;
            endX = x2;
        }
        else
        {
            startX = x2;
            endX = x1;
        }

        if (y1 < y2)
        {
            startY = y1;
            endY = y2;
        }
        else
        {
            startY = y2;
            endY = y1;
        }

        int buffer = 1;

        if (startX < buffer)
        {
            startX = buffer;
        }

        if (endX > getWidth() - 2)
        {
            endX = getWidth() - 2;
        }

        if (startY < buffer)
        {
            startY = buffer;
        }

        if (endY > getHeight() - 2)
        {
            endY = getHeight() - 2;
        }

        g.setColor(new Color(93, 93, 93));
        Composite composite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2F));
        g.fillRect(startX, startY, endX - startX, endY - startY);

        g.setColor(new Color(255, 255, 255));
        g.drawRect(startX, startY, endX - startX, endY - startY);
        g.setComposite(composite);
    }

    private void drawHighlight(Graphics g)
    {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, (selectedIndex + indexBuffers) * ROW_HEIGHT + ROW_HEIGHT_OFFSET - vScroll, this.getWidth(), ROW_HEIGHT);
    }

    private void drawBackgroundLines(Graphics g)
    {
        if (zoom <= SHOW_1_ZOOM)
        {
            g.setColor(ColorScheme.DARKER_GRAY_COLOR);

            double spacing = this.getWidth() / zoom;
            double startOffset = hScroll;
            int firstIteration = (int) Math.ceil(-1 * startOffset);

            for (int i = firstIteration; i < zoom + firstIteration; i++)
            {
                g.drawLine((int) (i * spacing + startOffset * spacing), 0, (int) (i * spacing + startOffset * spacing), this.getHeight());
            }
        }

        if (zoom <= SHOW_5_ZOOM)
        {
            g.setColor(ColorScheme.DARKER_GRAY_COLOR.darker());

            double iterations = zoom / 5;
            double spacing = this.getWidth() / iterations;
            double startOffset = hScroll / 5;
            int firstIteration = (int) Math.ceil(-1 * startOffset);

            for (int i = firstIteration; i < iterations + firstIteration; i++)
            {
                g.drawLine((int) (i * spacing + startOffset * spacing), 0, (int) (i * spacing + startOffset * spacing), this.getHeight());

            }
        }

        g.setColor(ColorScheme.BORDER_COLOR.darker());

        double iterations = zoom / 5;
        double spacing = this.getWidth() / iterations;
        double startOffset = hScroll / 5;
        int firstIteration = (int) Math.ceil(-1 * startOffset);
        boolean skip5Line = firstIteration % 2 != 0;

        for (int i = firstIteration; i < iterations + firstIteration; i++)
        {
            if (skip5Line)
            {
                skip5Line = false;
                continue;
            }

            g.drawLine((int) (i * spacing + startOffset * spacing), 0, (int) (i * spacing + startOffset * spacing), this.getHeight());
            skip5Line = true;
        }
    }

    private void drawTimeIndicator(Graphics g)
    {
        double x = (currentTime + hScroll) * this.getWidth() / zoom;
        char[] c = ("" + currentTime).toCharArray();
        int width = g.getFontMetrics().charsWidth(c, 0, c.length);
        int textBuffer = 16;

        g.setColor(new Color(74, 121, 192));
        g.drawLine((int) x, 0, (int) x, this.getHeight());

        g.fillRoundRect((int) (x - (width + textBuffer) / 2), 0, width + textBuffer, ROW_HEIGHT, 10, 10);

        g.setColor(Color.WHITE);
        g.drawChars(c, 0, c.length, (int) (x - width / 2), ROW_HEIGHT - TEXT_HEIGHT_OFFSET);

    }

    private void drawPreviewTimeIndicator(Graphics g)
    {
        if (!timeIndicatorPressed)
        {
            return;
        }

        double x = (previewTime + hScroll) * this.getWidth() / zoom;
        char[] c = ("" + previewTime).toCharArray();
        int width = g.getFontMetrics().charsWidth(c, 0, c.length);
        int textBuffer = 16;

        g.setColor(new Color(49, 84, 128));
        g.drawLine((int) x, 0, (int) x, this.getHeight());

        g.fillRoundRect((int) (x - (width + textBuffer) / 2), 0, width + textBuffer, ROW_HEIGHT, 10, 10);

        g.setColor(Color.WHITE);
        g.drawChars(c, 0, c.length, (int) (x - width / 2), ROW_HEIGHT - TEXT_HEIGHT_OFFSET);

    }

    private void drawTextHeader(Graphics g)
    {
        g.setColor(Color.WHITE);

        if (zoom <= SHOW_1_ZOOM)
        {
            g.setColor(Color.WHITE.darker().darker());
            g.setFont(FontManager.getRunescapeSmallFont());
            FontMetrics fontMetrics = g.getFontMetrics();

            double spacing = this.getWidth() / zoom;
            double startOffset = hScroll;
            int firstIteration = (int) Math.ceil(-1 * startOffset);

            for (int i = firstIteration; i < zoom + firstIteration; i++)
            {
                if (i % 5 == 0)
                {
                    continue;
                }

                char[] c = ("" + i).toCharArray();
                int width = fontMetrics.charsWidth(c, 0, c.length) / 2;
                g.drawChars(c, 0, c.length, (int) (i * spacing - width + startOffset * spacing), ROW_HEIGHT - TEXT_HEIGHT_OFFSET);
            }
        }

        if (zoom <= SHOW_5_ZOOM)
        {
            g.setColor(Color.WHITE.darker());
            g.setFont(FontManager.getRunescapeSmallFont());
            FontMetrics fontMetrics = g.getFontMetrics();

            double iterations = zoom / 5;
            double spacing = this.getWidth() / iterations;
            double startOffset = hScroll / 5;
            int firstIteration = (int) Math.ceil(-1 * startOffset);

            for (int i = firstIteration; i < iterations + firstIteration; i++)
            {
                int draw = i * 5;
                if (draw % 10 == 0)
                {
                    continue;
                }

                char[] c = ("" + draw).toCharArray();
                int width = fontMetrics.charsWidth(c, 0, c.length) / 2;
                g.drawChars(c, 0, c.length, (int) (i * spacing - width + startOffset * spacing), ROW_HEIGHT - TEXT_HEIGHT_OFFSET);
            }
        }

        g.setColor(Color.WHITE);
        g.setFont(FontManager.getRunescapeFont());
        FontMetrics fontMetrics = g.getFontMetrics();

        double iterations = zoom / 10;
        double spacing = this.getWidth() / iterations;
        double startOffset = hScroll / 10;
        int firstIteration = (int) Math.ceil(-1 * startOffset);

        for (int i = firstIteration; i < iterations + firstIteration; i++)
        {
            char[] c = ("" + i * 10).toCharArray();
            int width = fontMetrics.charsWidth(c, 0, c.length) / 2;
            g.drawChars(c, 0, c.length, (int) (i * spacing - width + startOffset * spacing), ROW_HEIGHT - TEXT_HEIGHT_OFFSET);
        }
    }

    public void drawKeyFrames(Graphics g)
    {
        /*
        if (selectedCharacter == null)
        {
            return;
        }

        int yImageOffset = (keyframeImage.getHeight() - ROW_HEIGHT) / 2;
        int xImageOffset = keyframeImage.getWidth() / 2;
        KeyFrame[][] frames = selectedCharacter.getFrames();
        for (int i = 0; i < frames.length; i++)
        {
            KeyFrame[] keyFrames = frames[i];
            for (int e = 0; e < keyFrames.length; e++)
            {
                double zoomFactor = this.getWidth() / zoom;
                g.drawImage(
                        keyframeImage,
                        (int) ((keyFrames[e].getTick() + hScroll) * zoomFactor - xImageOffset),
                        ROW_HEIGHT_OFFSET + ROW_HEIGHT + ROW_HEIGHT * i - yImageOffset,
                        null);
            }
        }

         */
    }

    private void setKeyBindings()
    {
        ActionMap actionMap = getActionMap();
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "VK_LEFT");
        actionMap.put("VK_LEFT", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                getTimeSheetPanel().setCurrentTime(currentTime - 0.1);
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "VK_RIGHT");
        actionMap.put("VK_RIGHT", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                getTimeSheetPanel().setCurrentTime(currentTime + 0.1);
            }
        });
    }

    private void setMouseListeners(TimeSheet timeSheet)
    {
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                super.mousePressed(e);
                requestFocusInWindow();

                if (e.getButton() != MouseEvent.BUTTON1)
                {
                    return;
                }

                Point mousePosition = getMousePosition();

                if (mousePosition.getY() < ROW_HEIGHT)
                {
                    timeIndicatorPressed = true;

                    TimeSheetPanel timeSheetPanel = getTimeSheetPanel();
                    double previewTime = getTimeIndicatorPosition();
                    timeSheetPanel.setPreviewTime(previewTime);
                }
                else
                {
                    drawingRectangleSelect = true;
                    rectangleSelectStart = mousePosition;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                super.mouseClicked(e);
                requestFocusInWindow();

                if (e.getButton() != MouseEvent.BUTTON1)
                {
                    return;
                }

                drawingRectangleSelect = false;

                if (!timeIndicatorPressed)
                {
                    return;
                }

                TimeSheetPanel timeSheetPanel = getTimeSheetPanel();
                double time = getTimeIndicatorPosition();
                timeSheetPanel.setCurrentTime(time);
                timeIndicatorPressed = false;
            }
        });

        addMouseMotionListener(new MouseAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent e)
            {
                super.mouseDragged(e);
                requestFocusInWindow();

                TimeSheetPanel timeSheetPanel = getTimeSheetPanel();
                double previewTime = getTimeIndicatorPosition();
                timeSheetPanel.setPreviewTime(previewTime);
            }
        });

        addMouseWheelListener(new MouseAdapter()
        {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e)
            {
                super.mouseWheelMoved(e);
                int amount = e.getWheelRotation();

                if (e.isAltDown())
                {
                    getTimeSheetPanel().onZoomEvent(amount, timeSheet);
                    return;
                }

                getTimeSheetPanel().onHorizontalScrollEvent(amount);
            }
        });
    }

    private double getTimeIndicatorPosition()
    {
        double absoluteMouseX = MouseInfo.getPointerInfo().getLocation().getX();
        double x = absoluteMouseX - getLocationOnScreen().getX();

        double time = TimeSheetPanel.round(x / getWidth() * zoom - hScroll);

        if (time < -hScroll)
        {
            time = -hScroll;
        }

        double max = TimeSheetPanel.round(zoom - hScroll);
        if (time > max)
        {
            time = max;
        }

        return time;
    }

    private TimeSheetPanel getTimeSheetPanel()
    {
        return toolBox.getTimeSheetPanel();
    }
}
