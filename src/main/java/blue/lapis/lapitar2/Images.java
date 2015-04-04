package blue.lapis.lapitar2;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;

import javax.swing.JButton;

public class Images {

	public static BufferedImage toBuffered(Image img) throws InterruptedException {
		if (img instanceof BufferedImage) return (BufferedImage) img;
		MediaTracker tracker = new MediaTracker(new JButton("Dummy"));
		tracker.addImage(img, 0);
		tracker.waitForID(0);
		BufferedImage buf = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g2d = buf.createGraphics();
		g2d.drawImage(img, 0, 0, null);
		g2d.dispose();
		return buf;
	}

}
