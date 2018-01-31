package Splitter;

import java.io.File;
import java.io.IOException;

import org.biojava.bio.BioException;

public class MultipeakDotplot {
	private Ab1Sequence sample;
	private ReferenceSequence refseq;
	private boolean[][] rawDotplot, windowedDotplot;
	
	private int cutoffSize = -1;
	private int windowSize = -1;
	
	public MultipeakDotplot(File sampleFile, File refseqFile) throws IOException, BioException{
		sample = new Ab1Sequence(sampleFile);
		refseq = new ReferenceSequence(refseqFile);
		
		if(refseq.makeSequence() == false) throw new IOException();
	}
	
	public boolean[][] getRawDotPlot(){
		return rawDotplot;
	}
	
	public boolean[][] getWindowedDotPlot(){
		return windowedDotplot;
	}
	
	public int getCutoff() {
		return cutoffSize;
	}
	
	public int getWindowSize() {
		return windowSize;
	}
	
	public void makeMultipeakDotplot(int cutoff, int window) {
		if(cutoff < 0 || 100 < cutoff) {
			return;
		}
		
		if(window < 1 || sample.getSequenceLength() <= window || refseq.getSequenceLength() <= window) {
			return;
		}
		
		rawDotplot = booleanDotplot(sample.getMultipeak(cutoff), refseq.getRefseq());
		windowedDotplot = this.windowDotplot(rawDotplot, window);
	}

	private boolean[][] booleanDotplot(boolean[][] sample, boolean[][] refseq){
		boolean[][] dotplot = new boolean[sample[1].length][refseq[1].length];

		for(int m=0; m<sample[1].length ;m++) {
			for(int n=0; n<refseq[1].length; n++) {
				if(sample[0][m] && refseq[0][n]) {dotplot[m][n]=true;}
				else if(sample[1][m] && refseq[1][n]) {dotplot[m][n]=true;}
				else if(sample[2][m] && refseq[2][n]) {dotplot[m][n]=true;}
				else if(sample[3][m] && refseq[3][n]) {dotplot[m][n]=true;}
				else {dotplot[m][n]=false;}
			}
		}
		
		return dotplot;
	}
	
	private boolean[][] windowDotplot(boolean[][] dotplot, int window){
		//window sizeがdotplotの大きさよりも大きいときに例外を投げるようにする必要がある
		
		boolean[][] trimed = new boolean[dotplot.length - window + 1][dotplot[0].length - window + 1];
		boolean point;
		
		for(int m = 0; m < trimed.length; m++) {
			for(int n = 0; n < trimed[0].length; n++) {
				
				point = true;
				
				for(int w = 0; w < window; w++) {
					if(dotplot[m+w][n+w] == false) {
						point = false;
						break;
					}
				}
				
				trimed[m][n] = point;
				
			}
		}
		
		return trimed;
	}

}
