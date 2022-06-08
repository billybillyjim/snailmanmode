package com.snailmanmode;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TextComponent;

import javax.inject.Inject;
import java.awt.*;

public class SnailManModeLoseOverlay extends OverlayPanel {

    private final SnailManModePlugin plugin;

    @Inject
    private Client client;
    @Inject
    private SnailManModeLoseOverlay(SnailManModePlugin plugin, Client client)
    {
        this.plugin = plugin;
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setClearChildren(false);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if(plugin.lostGame){
            graphics.setFont(FontManager.getRunescapeBoldFont());
            final TextComponent textComponent = new TextComponent();
            final WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
            final LocalPoint localPos = LocalPoint.fromWorld(client, playerPos);
            Point textPoint = Perspective.getCanvasTextLocation(client,
                    graphics,
                    localPos,
                    "SLIMED",
                    50);
            textComponent.setPosition(new java.awt.Point(textPoint.getX(), textPoint.getY()));
            textComponent.setColor(Color.GREEN);
            textComponent.setText("SLIMED");

            textComponent.render(graphics);
        }

        return super.render(graphics);
    }
}
