package com.gameminers.visage.benchmark;

import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import com.gameminers.visage.Visage;
import com.gameminers.visage.slave.render.PlayerRenderer;
import com.gameminers.visage.slave.render.Renderer;

public class VisageBenchmark {
	private static final long TARGET = 5000000000L;
	private int num = 1;
	private NumberFormat format = NumberFormat.getInstance();
	private int totalTotal = 0;
	public void start() {
		Renderer renderer = new PlayerRenderer();
		try {
			Visage.log.warning("VISAGE IS NOT AN ACCURATE HARDWARE BENCHMARK. This benchmark is to give you an idea of how well this machine would work as a Visage slave.");
			Visage.log.info("Loading skin...");
			float total = 0;
			BufferedImage skin = ImageIO.read(ClassLoader.getSystemResource("test_skin.png"));
			
			format.setMinimumFractionDigits(2);
			format.setMaximumFractionDigits(2);
			
			renderer.init(1);
			renderer.setSkin(skin);
			total += bench(renderer,  1, 512, true) / 4f;
			total += bench(renderer,  1, 128, true) / 5f;
			total += bench(renderer,  1,  32, true) / 6f;
			
			renderer.init(2);
			renderer.setSkin(skin);
			total += bench(renderer,  2, 512, true) / 3f;
			total += bench(renderer,  2, 128, true) / 4f;
			total += bench(renderer,  2,  32, true) / 5f;
			
			renderer.init(3);
			renderer.setSkin(skin);
			total += bench(renderer,  3, 512, true) / 2f;
			total += bench(renderer,  3, 128, true) / 3f;
			total += bench(renderer,  3,  32, true) / 4f;
			
			renderer.init(4);
			renderer.setSkin(skin);
			total += bench(renderer,  4, 512, true) / 1f;
			total += bench(renderer,  4, 128, true) / 2f;
			total += bench(renderer,  4,  32, true) / 3f;
			
			renderer.init(1);
			renderer.setSkin(skin);
			total += bench(renderer,  1, 512, false) / 12f;
			total += bench(renderer,  1, 128, false) / 15f;
			total += bench(renderer,  1,  32, false) / 18f;
			
			renderer.init(2);
			renderer.setSkin(skin);
			total += bench(renderer,  2, 512, false) / 9f;
			total += bench(renderer,  2, 128, false) / 12f;
			total += bench(renderer,  2,  32, false) / 15f;
			
			renderer.init(3);
			renderer.setSkin(skin);
			total += bench(renderer,  3, 512, false) / 6f;
			total += bench(renderer,  3, 128, false) / 9f;
			total += bench(renderer,  3,  32, false) / 12f;
			
			renderer.init(4);
			renderer.setSkin(skin);
			total += bench(renderer,  4, 512, false) / 3f;
			total += bench(renderer,  4, 128, false) / 6f;
			total += bench(renderer,  4,  32, false) / 9f;
			
			renderer.destroy();
			Visage.log.info("Done. Performed "+totalTotal+" renders.");
			Visage.log.info("Your arbitrary number is "+(total / (num-1)));
		} catch (Exception e) {
			Visage.log.log(Level.SEVERE, "Unexpected error occured during benchmark", e);
		}
	}
	
	private int bench(Renderer renderer, int supersampling, int size, boolean readpixels) throws Exception {
		int csize = size*supersampling;
		Visage.log.info("--");
		Visage.log.info("Starting benchmark #"+(num++)+" - "+supersampling+"x supersampling, "+size+"x"+size+" result, "+csize+"x"+csize+" canvas, "+(readpixels ? "with" : "without")+" readpixels)");
		int count = 0;
		long renderTime = 0;
		long readTime = 0;
		System.gc();
		long start = System.nanoTime();
		while (System.nanoTime()-start < TARGET) {
			long renderStart = System.nanoTime();
			renderer.render(csize, csize);
			renderTime += System.nanoTime()-renderStart;
			if (readpixels) {
				long readStart = System.nanoTime();
				renderer.readPixels(csize, csize);
				readTime += System.nanoTime()-readStart;
			}
			count++;
		}
		long runtime = System.nanoTime()-start;
		double runtimeS = (runtime/1000000000D);
		double renderTimeM = ((renderTime/(double)count)/1000000D);
		Visage.log.fine("Rendered "+count+" players in "+format.format(runtimeS)+" seconds");
		Visage.log.fine("Avg. "+format.format(renderTimeM)+" millis per render");
		if (readpixels) {
			double readTimeM = ((readTime/(double)count)/1000000D);
			Visage.log.fine("Avg. "+format.format(readTimeM)+" millis per readPixels");
		}
		totalTotal += count;
		return count;
	}

}
