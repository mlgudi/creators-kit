package com.creatorskit.swing;

import com.creatorskit.CreatorsPlugin;
import com.creatorskit.swing.manager.ManagerPanel;
import com.creatorskit.swing.manager.ManagerTree;
import com.creatorskit.swing.timesheet.TimeSheet;
import com.creatorskit.swing.timesheet.TimeSheetPanel;
import com.creatorskit.swing.timesheet.TimeTree;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

@Getter
public class ToolBoxFrame extends JFrame
{
    private ClientThread clientThread;
    private final Client client;
    private final CreatorsPlugin plugin;
    private final CreatorsPanel creatorsPanel;
    private final ConfigManager configManager;
    private final ManagerPanel managerPanel;
    private final ModelOrganizer modelOrganizer;
    private final ModelAnvil modelAnvil;
    private final ProgrammerPanel programPanel;
    private final TransmogPanel transmogPanel;
    private final TimeSheetPanel timeSheetPanel;
    private final BufferedImage ICON = ImageUtil.loadImageResource(getClass(), "/panelicon.png");

    @Inject
    public ToolBoxFrame(Client client, ClientThread clientThread, CreatorsPlugin plugin, ConfigManager configManager, ModelOrganizer modelOrganizer, ModelAnvil modelAnvil, ProgrammerPanel programPanel, TransmogPanel transmogPanel)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.plugin = plugin;
        this.creatorsPanel = plugin.getCreatorsPanel();
        this.configManager = configManager;
        this.modelOrganizer = modelOrganizer;
        this.modelAnvil = modelAnvil;
        this.programPanel = programPanel;
        this.transmogPanel = transmogPanel;

        Folder rootFolder = new Folder("Master Folder", FolderType.MASTER, null, null, null, null);
        DefaultMutableTreeNode managerRootNode = new DefaultMutableTreeNode(rootFolder);
        DefaultMutableTreeNode timeRootNode = new DefaultMutableTreeNode(rootFolder);
        rootFolder.setLinkedManagerNode(managerRootNode);
        rootFolder.setLinkedTimeSheetNode(timeRootNode);

        Folder sidePanelFolder = new Folder("Side Panel", FolderType.SIDE_PANEL, null, null, managerRootNode, timeRootNode);
        Folder managerPanelFolder = new Folder("Manager", FolderType.MANAGER, null, null, managerRootNode, timeRootNode);
        DefaultMutableTreeNode timeSideNode = new DefaultMutableTreeNode(sidePanelFolder);
        DefaultMutableTreeNode timeManagerNode = new DefaultMutableTreeNode(managerPanelFolder);
        DefaultMutableTreeNode managerSideNode = new DefaultMutableTreeNode(sidePanelFolder);
        DefaultMutableTreeNode managerManagerNode = new DefaultMutableTreeNode(managerPanelFolder);
        sidePanelFolder.setLinkedManagerNode(managerSideNode);
        sidePanelFolder.setLinkedTimeSheetNode(timeSideNode);
        managerPanelFolder.setLinkedManagerNode(managerManagerNode);
        managerPanelFolder.setLinkedTimeSheetNode(timeManagerNode);

        TimeSheet timeSheet = new TimeSheet();
        TimeTree timeTree = new TimeTree(timeSheet, timeRootNode, timeSideNode, timeManagerNode);
        this.timeSheetPanel = new TimeSheetPanel(client, plugin, clientThread, timeSheet, timeTree);

        JPanel objectHolder = new JPanel();
        ManagerTree managerTree = new ManagerTree(this, plugin, objectHolder, managerRootNode, managerSideNode, managerManagerNode, timeTree);
        this.managerPanel = new ManagerPanel(client, plugin, objectHolder, managerTree);

        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setTitle("Creator's Kit Toolbox");
        setIconImage(ICON);

        try
        {
            String string = configManager.getConfiguration("creatorssuite", "toolBoxSize");
            String[] dimensions = string.split(",");
            int width = Integer.parseInt(dimensions[0]);
            int height = Integer.parseInt(dimensions[1]);
            if (width < 150)
                width = 150;
            if (height < 150)
                height = 150;
            setPreferredSize(new Dimension(width, height));
        }
        catch (Exception e)
        {
            setPreferredSize(new Dimension(1500, 800));
        }

        addComponentListener(new ComponentAdapter()
        {
            public void componentResized(ComponentEvent componentEvent)
            {
                Dimension dimension = getSize();
                configManager.setConfiguration("creatorssuite", "toolBoxSize", (int) dimension.getWidth() + "," + (int) dimension.getHeight());
            }
        });

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(FontManager.getRunescapeBoldFont());
        tabbedPane.addTab("Manager", managerPanel);
        tabbedPane.addTab("Model Organizer", modelOrganizer);
        tabbedPane.addTab("Model Anvil", modelAnvil);
        tabbedPane.addTab("Programmer", programPanel);
        tabbedPane.addTab("Transmogger", transmogPanel);
        tabbedPane.addTab("Timesheet", timeSheetPanel);
        tabbedPane.setToolTipTextAt(0, "Manage and organize all your Objects");
        tabbedPane.setToolTipTextAt(1, "Organize Custom Models you've loaded from the cache or Forged");
        tabbedPane.setToolTipTextAt(2, "Create Custom Models by modifying and merging different models together");
        tabbedPane.setToolTipTextAt(3, "Change your Object Programs' animations, speeds, and more");
        tabbedPane.setToolTipTextAt(4, "Set animations for Transmogging your player character");

        //Move the FolderTree between the Manager and Programmer tabs when the given tab is selected
        tabbedPane.addChangeListener(e -> {
            if (e.getSource() instanceof JTabbedPane)
            {
                JTabbedPane jTabbedPane = (JTabbedPane) e.getSource();
                if (jTabbedPane.getSelectedComponent() == managerPanel)
                {
                    managerPanel.getScrollPanel().add(managerTree, BorderLayout.LINE_START);
                    setHeaderButtonsVisible(true);
                }

                if (jTabbedPane.getSelectedComponent() == programPanel)
                {
                    programPanel.getScrollPanel().add(managerPanel.getManagerTree(), BorderLayout.LINE_START);
                    setHeaderButtonsVisible(false);
                }
            }
            repaint();
            revalidate();
        });

        add(tabbedPane);
        pack();
        revalidate();
    }

    private void setHeaderButtonsVisible(boolean setVisible)
    {
        JButton[] headerButtons = managerPanel.getManagerTree().getHeaderButtons();
        for (JButton button : headerButtons)
            button.setVisible(setVisible);
    }
}
