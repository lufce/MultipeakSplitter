package jp.lufce.multipeaksplitter;

import java.io.File;
import java.io.IOException;

import org.biojava.bio.program.abi.ABITrace;
import org.biojava.bio.seq.DNATools;

public class Ab1Sequence extends ABITrace {
	
	private boolean[][] multi;
	private boolean[][] revcomMulti;
	private int[][] multiBasecallIntensity;
	private int[][] multiAllIntensity;
	private int[][] revcomMultiBasecallIntensity;
	private int[][] revcomMultiAllIntensity;
	
	final private int A = 0;
	final private int C = 1;
	final private int G = 2;
	final private int T = 3;
	
	public Ab1Sequence(File ABIFile) throws IOException {
		super(ABIFile);
		multiBasecallIntensity = new int[4][super.getSequenceLength()];
		multiAllIntensity = new int[4][super.getTraceLength()];
		revcomMultiBasecallIntensity = new int[4][super.getSequenceLength()];
		revcomMultiAllIntensity = new int[4][super.getTraceLength()];
	}
	
	public void makeMultipeak(int cutoff){
		//Multipeakを検出して二次元boolean配列を返す
		//もしcutoffが0-100の間でなければnullを返す。
		//TODO 例外を返すようにしないといけない
		
		if(cutoff < 0 && 100 < cutoff) {
			return;
		}
		
		this.getAllTrace();
		multi = new boolean[4][super.getSequenceLength()];
		revcomMulti = new boolean[4][super.getSequenceLength()];
		
		int maxpeak = 0;
		
		for(int m = 0; m<multiBasecallIntensity[0].length; m++){
			
			//最大値の取得
			for(int n=0; n<4; n++){
				if(maxpeak < multiBasecallIntensity[n][m]){
					maxpeak = multiBasecallIntensity[n][m];
				}
			}
			
			//Multi peakの検出
			for(int n = 0; n<4; n++){
				if(maxpeak != 0 && (double)multiBasecallIntensity[n][m]/maxpeak >= (double)cutoff/100){
					multi[n][m]= true;
					revcomMulti[3-n][multiBasecallIntensity[0].length -1 -m] = true;
				}else{
					multi[n][m]= false;
					revcomMulti[3-n][multiBasecallIntensity[0].length -1 -m] = false;
				}
			}
			
			//maxpeakの初期化
			maxpeak = 0;
		}
	}

	public boolean[][] getMultipeakMap(){
		return multi;
	}
	
	public boolean[][] getRevcomMultipeakMap(){
		return revcomMulti;
	}
	
	public int[][] getMultiBasecallIntensity(){
		return multiBasecallIntensity;
	}
	
	public int[][] getMultiAllIntensity(){
		return multiAllIntensity;
	}
	
	public int[][] getRevcomMultiBasecallIntensity(){
		return revcomMultiBasecallIntensity;
	}
	
	public int[][] getRevcomMultiAllIntensity(){
		return revcomMultiAllIntensity;
	}
	
	public double getLocalMaxIntensity(int start, int end) {
		double localMax = Double.MIN_VALUE;
		
		for(int m = start; m < end; m++) {
			for(int n = 0; n < 4; n++) {
				if(multiAllIntensity[n][m] > localMax) {
					localMax = multiAllIntensity[n][m];
				}
			}
		}
		return localMax;
	}
	
	public int[][] getSubarrayMultiAllIntensity(int start, int end) throws ArrayIndexOutOfBoundsException{
		if(start < 0 || this.multiAllIntensity[1].length < end) {
			System.out.println("out of bounds");
			throw new ArrayIndexOutOfBoundsException();
		}
		
		int[][] subarray = new int[4][end-start+1];
		
		for(int m = 0; m < subarray[1].length; m++) {
			for(int n = 0; n < 4; n++) {
				subarray[n][m] = this.multiAllIntensity[n][m+start];
			}
		}
		return subarray;
	}
	
	public int[][] getSubarrayMultiBasecallIntensity(int start, int end) throws ArrayIndexOutOfBoundsException{
		if(start < 0 || this.multiBasecallIntensity[1].length < end) {
			System.out.println("out of bounds");
			throw new ArrayIndexOutOfBoundsException();
		}
		
		int[][] subarray = new int[4][end-start+1];
		
		for(int m = 0; m < subarray[1].length; m++) {
			for(int n = 0; n < 4; n++) {
				subarray[n][m] = this.multiBasecallIntensity[n][m];
			}
		}
		return subarray;
	}
		
	private void getAllTrace() {
		//各塩基ごとのベースコール座標でのトレースデータを含んだ二次元配列を返す
		//第一index 0:A 1:C 2:G 3:T
		
		try {
			int[] basecall = super.getBasecalls();
			
			multiAllIntensity[A] = super.getTrace(DNATools.a());
			multiAllIntensity[C] = super.getTrace(DNATools.c());
			multiAllIntensity[G] = super.getTrace(DNATools.g());
			multiAllIntensity[T] = super.getTrace(DNATools.t());
			
			for(int n = 0; n < super.getTraceLength(); n++) {
				
			}
			
			for(int n = 0; n < super.getSequenceLength(); n++) {
				multiBasecallIntensity[A][n] = multiAllIntensity[A][basecall[n]];
				multiBasecallIntensity[C][n] = multiAllIntensity[C][basecall[n]];
				multiBasecallIntensity[G][n] = multiAllIntensity[G][basecall[n]];
				multiBasecallIntensity[T][n] = multiAllIntensity[T][basecall[n]];
				
				revcomMultiBasecallIntensity[T][super.getSequenceLength() - 1 - n] = multiAllIntensity[A][basecall[n]];
				revcomMultiBasecallIntensity[G][super.getSequenceLength() - 1 - n] = multiAllIntensity[C][basecall[n]];
				revcomMultiBasecallIntensity[C][super.getSequenceLength() - 1 - n] = multiAllIntensity[G][basecall[n]];
				revcomMultiBasecallIntensity[A][super.getSequenceLength() - 1 - n] = multiAllIntensity[T][basecall[n]];
				
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
}
