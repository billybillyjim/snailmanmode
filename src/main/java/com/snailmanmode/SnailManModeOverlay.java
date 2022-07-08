package com.snailmanmode;

import com.google.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;

import java.awt.*;

import static com.snailmanmode.SnailManModePlugin.snailXPosition;
import static com.snailmanmode.SnailManModePlugin.snailYPosition;

public class SnailManModeOverlay  extends Overlay {
    private final Client client;
    @javax.inject.Inject
    private SnailManModeConfig config;

    @Inject
    private SnailManModeOverlay(Client client)
    {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if(config.showPath()){
            if(SnailManModePlugin.path.size() > 0){
                for(WorldPoint wp:
                        SnailManModePlugin.path){
                    renderTile(graphics, LocalPoint.fromWorld(client, wp.getX(), wp.getY()), config.color(), 2, config.fillColor());
                }
            }
        else{
            for(WorldPoint wp:
                        SnailManModePlugin.getDirectPath()){
                    if(wp != null){
                        renderTile(graphics, LocalPoint.fromWorld(client, wp.getX(), wp.getY()), config.color(), 2, config.fillColor());
                    }
                }
            }
        }

        if(config.showTile() == false){
            return null;
        }
            WorldPoint wp = new WorldPoint(snailXPosition, snailYPosition, 0);
            if (wp == null)
            {
                return null;
            }
            LocalPoint lp = LocalPoint.fromWorld(client, wp);

            if (lp == null)
            {
                return null;
            }

            renderTile(graphics, lp, config.color(), 2, config.fillColor());

        return null;
    }

    private void renderTile(final Graphics2D graphics, final LocalPoint dest, final Color color, final double borderWidth, final Color fillColor)
    {
        if (dest == null)
        {
            return;
        }

        final Polygon poly = Perspective.getCanvasTilePoly(client, dest);

        if (poly == null)
        {
            return;
        }

        OverlayUtil.renderPolygon(graphics, poly, color, fillColor, new BasicStroke((float) borderWidth));
    }
}
