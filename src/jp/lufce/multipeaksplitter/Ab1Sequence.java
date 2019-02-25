package jp.lufce.multipeaksplitter;

import java.io.File;
import java.io.IOException;

import org.biojava.bio.program.abi.ABITrace;
import org.biojava.bio.seq.DNATools;

public class Ab1Sequence extends ABITrace {

	private boolean[][] workingMap;
	private int[] workingBasecall;
	private int[][] workingIntensity;
	private int[][] workingBasecallIntensity;

	private boolean[][] map;
	private boolean[][] revcomMap;
	private int[] basecall;
	private int[][] multiBasecallIntensity;
	private int[][] multiAllIntensity;
	private int[] revcomBasecall;
	private int[][] revcomMultiBasecallIntensity;
	private int[][] revcomMultiAllIntensity;

	private boolean isRevcom = false;
	private int seqLength = 0;
	private int traceLength = 0;
	private int traceEnd = 0;

	final private int A = 0;
	final private int C = 1;
	final private int G = 2;
	final private int T = 3;

	public Ab1Sequence(File ABIFile) throws IOException {
		super(ABIFile);

		seqLength = super.getSequenceLength();
		traceLength = super.getTraceLength();

		basecall = super.getBasecalls();
		revcomBasecall = new int[seqLength];

		traceEnd = basecall[seqLength-1];

		for(int n = 0; n < seqLength; n++) {
			revcomBasecall[n] = traceEnd - basecall[seqLength - n - 1];
		}

		multiBasecallIntensity = new int[4][seqLength];
		multiAllIntensity = new int[4][traceLength];
		revcomMultiBasecallIntensity = new int[4][seqLength];
		revcomMultiAllIntensity = new int[4][traceLength];
	}

	public void makeMap(int cutoff){
		//Multipeakを検出して二次元boolean配列を返す
		//もしcutoffが0-100の間でなければnullを返す。
		//TODO 例外を返すようにしないといけない

		if(cutoff < 0 && 100 < cutoff) {
			return;
		}

		this.getAllTrace();
		map = new boolean[4][seqLength];
		revcomMap = new boolean[4][seqLength];

		int maxpeak = 0;

		for(int m = 0; m < seqLength; m++){

			//最大値の取得
			for(int n=0; n<4; n++){
				if(maxpeak < multiBasecallIntensity[n][m]){
					maxpeak = multiBasecallIntensity[n][m];
				}
			}

			//Multi peakの検出
			for(int n = 0; n<4; n++){
				if(maxpeak != 0 && (double)multiBasecallIntensity[n][m]/maxpeak >= (double)cutoff/100){
					map[n][m]= true;
					revcomMap[3-n][seqLength -1 -m] = true;
				}else{
					map[n][m]= false;
					revcomMap[3-n][seqLength -1 -m] = false;
				}
			}

			//maxpeakの初期化
			maxpeak = 0;
		}

		//isRevcomフラグに基づいて、必要とされている(Workingの)情報を登録
		this.setWorkings();
	}

	public int[] getWorkingBasecall() {
		return this.workingBasecall;
	}

	public boolean[][] getMap(){
		return map;
	}

	public boolean[][] getRevcomMap(){
		return revcomMap;
	}

	public boolean[][] getWorkingMap(){
		return this.workingMap;
	}

	public int[][] getWorkingMultiBasecallIntensity(){
		return this.workingBasecallIntensity;
	}

	public int[][] getWorkingMultiAllIntensity(){
		return this.workingIntensity;
	}

//	public int[][] getMultiBasecallIntensity(){
//		return multiBasecallIntensity;
//	}
//
//	public int[][] getMultiAllIntensity(){
//		return multiAllIntensity;
//	}
//
//	public int[][] getRevcomMultiBasecallIntensity(){
//		return revcomMultiBasecallIntensity;
//	}
//
//	public int[][] getRevcomMultiAllIntensity(){
//		return revcomMultiAllIntensity;
//	}

	public double getLocalMaxIntensity(int start, int end) {
		double localMax = Double.MIN_VALUE;

		for(int m = start; m < end; m++) {
			for(int n = 0; n < 4; n++) {
				if(workingIntensity[n][m] > localMax) {
					localMax = workingIntensity[n][m];
				}
			}
		}
		return localMax;
	}

	public int[][] getSubarrayMultiAllIntensity(int start, int end) throws ArrayIndexOutOfBoundsException{
		if(start < 0 || traceEnd < end) {
			System.out.println("out of bounds");
			throw new ArrayIndexOutOfBoundsException();
		}

		int[][] subarray = new int[4][end-start+1];

		for(int m = 0; m < subarray[1].length; m++) {
			for(int n = 0; n < 4; n++) {
				subarray[n][m] = this.workingIntensity[n][m+start];
			}
		}
		return subarray;
	}

	public int[][] getSubarrayMultiBasecallIntensity(int start, int end) throws ArrayIndexOutOfBoundsException{
		if(start < 0 || traceEnd < end) {
			System.out.println("out of bounds");
			throw new ArrayIndexOutOfBoundsException();
		}

		int[][] subarray = new int[4][end-start+1];

		for(int m = 0; m < subarray[1].length; m++) {
			for(int n = 0; n < 4; n++) {
				subarray[n][m] = this.workingBasecallIntensity[n][m];
			}
		}
		return subarray;
	}

	public int getTraceEnd() {
		return this.traceEnd;
	}

	private void getAllTrace() {
		//各塩基ごとのベースコール座標でのトレースデータを含んだ二次元配列を返す
		//第一index 0:A 1:C 2:G 3:T

		try {

			multiAllIntensity[A] = super.getTrace(DNATools.a());
			multiAllIntensity[C] = super.getTrace(DNATools.c());
			multiAllIntensity[G] = super.getTrace(DNATools.g());
			multiAllIntensity[T] = super.getTrace(DNATools.t());

			for(int n = 0; n < traceEnd; n++) {
				revcomMultiAllIntensity[T][n] = multiAllIntensity[A][traceEnd - n - 1];
				revcomMultiAllIntensity[G][n] = multiAllIntensity[C][traceEnd - n - 1];
				revcomMultiAllIntensity[C][n] = multiAllIntensity[G][traceEnd - n - 1];
				revcomMultiAllIntensity[A][n] = multiAllIntensity[T][traceEnd - n - 1];
			}

			for(int n = 0; n < seqLength; n++) {
				multiBasecallIntensity[A][n] = multiAllIntensity[A][basecall[n]];
				multiBasecallIntensity[C][n] = multiAllIntensity[C][basecall[n]];
				multiBasecallIntensity[G][n] = multiAllIntensity[G][basecall[n]];
				multiBasecallIntensity[T][n] = multiAllIntensity[T][basecall[n]];

				revcomMultiBasecallIntensity[T][seqLength - 1 - n] = multiAllIntensity[A][basecall[n]];
				revcomMultiBasecallIntensity[G][seqLength - 1 - n] = multiAllIntensity[C][basecall[n]];
				revcomMultiBasecallIntensity[C][seqLength - 1 - n] = multiAllIntensity[G][basecall[n]];
				revcomMultiBasecallIntensity[A][seqLength - 1 - n] = multiAllIntensity[T][basecall[n]];

			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void setRevcom(boolean boo) {
		this.isRevcom = boo;
		this.setWorkings();
	}

	private void setWorkings() {
		if(this.isRevcom) {
			this.workingBasecallIntensity  = this.revcomMultiBasecallIntensity;
			this.workingBasecall           = this.revcomBasecall;
			this.workingIntensity          = this.revcomMultiAllIntensity;
			this.workingMap                = this.revcomMap;
		}else {
			this.workingBasecallIntensity  = this.multiBasecallIntensity;
			this.workingBasecall           = this.basecall;
			this.workingIntensity          = this.multiAllIntensity;
			this.workingMap                = this.map;
		}
	}

}
