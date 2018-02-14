package jp.lufce.multipeaksplitter;

public class MultipeakDotplot {
	//TODO get系のFunctionで、makeDotplot前の例外をつくらないといけない
	//TODO これってstaticでいいのでは。
	
	private boolean[][] rawDotplot, windowedDotplot;
	
	private int window = -1;
	
	public MultipeakDotplot(boolean[][] sampleMap, boolean[][] refseqMap, int windowSize){
		//TODO 例外を返すようにしないといけない。

		if(0 < windowSize && windowSize < sampleMap[0].length && windowSize < refseqMap[0].length ) {
			window = windowSize;
			rawDotplot = booleanDotplot(sampleMap, refseqMap);
			windowedDotplot = this.windowDotplot(rawDotplot, windowSize);	
		}		
	}
	
	public int getWindowSize() {
		return window;
	}
	
	public boolean[][] getRawDotPlot(){
		return rawDotplot;
	}
	
	public boolean[][] getWindowedDotPlot(){
		return windowedDotplot;
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
	
//	private boolean[][] windowDotplot(boolean[][] dotplot, int window){
//		//window sizeがdotplotの大きさよりも大きいときに例外を投げるようにする必要がある
//		
//		boolean[][] trimed = new boolean[dotplot.length - window + 1][dotplot[0].length - window + 1];
//		boolean point;
//		
//		for(int m = 0; m < trimed.length; m++) {
//			for(int n = 0; n < trimed[0].length; n++) {
//				
//				point = true;
//				
//				for(int w = 0; w < window; w++) {
//					if(dotplot[m+w][n+w] == false) {
//						point = false;
//						break;
//					}
//				}
//				
//				trimed[m][n] = point;
//				
//			}
//		}
//		
//		return trimed;
//	}
	
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
			
			if(point == false && m > window) {
				
				point = true;
				
				for(int w = 0; w < window; w++) {
					if(dotplot[m-w][n+w] == false) {
						point = false;
						break;
					}
				}
			}
				
			trimed[m][n] = point;
			
		}
	}
	
	return trimed;
}

}
