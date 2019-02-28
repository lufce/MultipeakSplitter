package jp.lufce.multipeaksplitter;

public class MultipeakDotplot {
	//TODO get系のFunctionで、makeDotplot前の例外をつくらないといけない
	//TODO これってstaticでいいのでは。
	//TODO もしseqTopとseqLeftの両方が波形データだった場合、どういうふうにDotplotを作ると良いのだろうか。

	private boolean[][] rawDotplot, windowedDotplot;
//	private boolean[][] maxWindowedDotplot;

	private int window = -1;

//========================initialization======================================

	public MultipeakDotplot(boolean[][] map1, boolean[][] map2, int windowSize, int thresholdSize){
		//TODO 例外を返すようにしないといけない。

		if(0 < windowSize && windowSize < map1[0].length && windowSize < map2[0].length ) {
			window = windowSize;
			rawDotplot = booleanDotplot(map1, map2);
			windowedDotplot = this.windowDotplot(rawDotplot, windowSize, thresholdSize);
//			maxWindowedDotplot = this.maximizeWindowdDotplot(map1, map2, windowedDotplot);
		}
	}

//========================get methods======================================

	public int getWindowSize() {
		return window;
	}

	public boolean[][] getRawDotPlot(){
		return rawDotplot;
	}

	public boolean[][] getWindowedDotPlot(){
		return windowedDotplot;
	}

//	public boolean[][] getMaxWindowedDotPlot(){
//		return this.maxWindowedDotplot;
//	}

//========================Dotmap calculation======================================

	private boolean[][] booleanDotplot(boolean[][] sample, boolean[][] refseq){
		boolean[][] dotplot = new boolean[sample[1].length][refseq[1].length];

		for(int m=0; m<sample[1].length ;m++) {
			for(int n=0; n<refseq[1].length; n++) {
				if     (sample[0][m] && refseq[0][n]) {dotplot[m][n]=true;}
				else if(sample[1][m] && refseq[1][n]) {dotplot[m][n]=true;}
				else if(sample[2][m] && refseq[2][n]) {dotplot[m][n]=true;}
				else if(sample[3][m] && refseq[3][n]) {dotplot[m][n]=true;}
				else                                  {dotplot[m][n]=false;}
			}
		}

		return dotplot;
	}

	private boolean[][] windowDotplot(boolean[][] dotplot, int window, int threshold){
	//TODO window sizeがdotplotの大きさよりも大きいときに例外を投げるようにする必要がある

	boolean[][] trimed = new boolean[dotplot.length - window + 1][dotplot[0].length - window + 1];
	boolean point;
	int count;

	for(int m = 0; m < trimed.length; m++) {
		for(int n = 0; n < trimed[0].length; n++) {

			point = false;
			count = 0;

			for(int w = 0; w < window; w++) {
				if(dotplot[m+w][n+w] == true) {
					count++;
				}
				if(threshold <= count) {
					point = true;
					break;
				}
			}

			//逆位についての解析。このやり方でいいのか？
			if(point == false && m > window) {

				count = 0;

				for(int w = 0; w < window; w++) {
					if(dotplot[m-w][n+w] == true) {
						count++;
					}
					if(threshold <= count) {
						point = true;
						break;
					}
				}
			}

			trimed[m][n] = point;

		}
	}

	return trimed;
	}

//	private boolean[][] maximizeWindowdDotplot(boolean[][] sample, boolean[][] refseq, boolean[][] windowed){
	//なんでこれつくったのかよくわからない。

//		boolean[][] maximized = new boolean[sample[1].length][refseq[1].length];
//		for(int m=0; m<windowed.length; m++) {
//			for(int n=0; n<windowed[1].length; n++) {
//				maximized[m][n] = windowed[m][n];
//			}
//		}
//
//		for(int m=1; m<windowed.length-1; m++) {
//			for(int n=1; n<windowed[1].length-1; n++) {
//				if(windowed[m][n]==true) {
//					if(windowed[m+1][n+1]==false) {
//						for(int k=1; m+k < rawDotplot.length && n+k < rawDotplot[1].length; k++) {
//							if(rawDotplot[m+k][n+k]==true) {
//								maximized[m+k][n+k] = true;
//							}else {
//								break;
//							}
//						}
//					}
//
//					if(windowed[m-1][n-1]==false) {
//						for(int k=1; m-k >= 0 && n-k >= 0; k++) {
//							if(rawDotplot[m-k][n-k]==true) {
//								maximized[m-k][n-k] = true;
//							}else {
//								break;
//							}
//						}
//					}
//				}
//			}
//		}
//		return maximized;
//	}

//===========================utilities=================================

}
