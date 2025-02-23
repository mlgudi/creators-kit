package com.creatorskit.swing.timesheet.sheets;

import com.creatorskit.Character;
import com.creatorskit.swing.Folder;
import com.creatorskit.swing.ToolBoxFrame;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.timesheet.AttributePanel;
import com.creatorskit.swing.timesheet.keyframe.KeyFrame;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

@Getter
@Setter
public class SummarySheet extends TimeSheet
{
    private ManagerTree tree;
    private AttributePanel attributePanel;

    public SummarySheet(ToolBoxFrame toolBox, ManagerTree tree, AttributePanel attributePanel)
    {
        super(toolBox, tree, attributePanel);
        this.tree = tree;
        this.attributePanel = attributePanel;
    }

    @Override
    public void drawHighlight(Graphics g)
    {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, getSelectedIndex() * ROW_HEIGHT - ROW_HEIGHT_OFFSET - getVScroll(), this.getWidth(), ROW_HEIGHT);
    }

    @Override
    public void drawKeyFrames(Graphics g)
    {
        BufferedImage image = getKeyframeImage();
        int yImageOffset = (image.getHeight() - ROW_HEIGHT) / 2;
        int xImageOffset = image.getWidth() / 2;

        ArrayList<DefaultMutableTreeNode> nodes = new ArrayList<>();
        nodes.add(tree.getRootNode());
        tree.getAllNodes(tree.getRootNode(), nodes);
        int index = -2;

        for (DefaultMutableTreeNode node : nodes)
        {
            index++;

            TreePath path = tree.getPathForRow(index);
            if (path == null)
            {
                continue;
            }

            if (node.getUserObject() instanceof Folder)
            {
                continue;
            }

            Character character = (Character) node.getUserObject();
            KeyFrame[][] frames = character.getFrames();
            double[] ticks = new double[0];
            for (int i = 0; i < frames.length; i++)
            {
                KeyFrame[] keyFrames = frames[i];
                if (keyFrames == null)
                {
                    continue;
                }

                for (int e = 0; e < keyFrames.length; e++)
                {
                    KeyFrame k = keyFrames[e];
                    if (ticks.length == 0)
                    {
                        ticks = ArrayUtils.add(ticks, k.getTick());
                    }

                    double tick = k.getTick();
                    boolean contains = false;
                    for (double d : ticks)
                    {
                        if (tick == d)
                        {
                            contains = true;
                            break;
                        }
                    }

                    if (contains)
                    {
                        continue;
                    }

                    ticks = ArrayUtils.add(ticks, tick);
                }
            }

            for (int e = 0; e < ticks.length; e++)
            {
                double d = ticks[e];

                double zoomFactor = this.getWidth() / getZoom();
                g.drawImage(
                        image,
                        (int) ((d + getHScroll()) * zoomFactor - xImageOffset),
                        (index * ROW_HEIGHT) - ROW_HEIGHT_OFFSET - yImageOffset - getVScroll(),
                        null);
            }
        }
    }
}
