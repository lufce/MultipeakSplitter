package Splitter;

import java.io.File;
import java.io.IOException;

import org.biojava.bio.program.abi.ABITrace;
import org.biojava.bio.seq.DNATools;

public class Ab1Sequence extends ABITrace {
	
	public Ab1Sequence(File ABIFile) throws IOException {
		super(ABIFile);
	}
	
	public boolean[][] getMultipeak(int cutoff){
		//Multipeakを検出して二次元boolean配列を返す
		//もしcutoffが0-100の間でなければnullを返す。
		
		if(cutoff < 0 && 100 < cutoff) {
			return null;
		}
		
		int dna[][] = this.getAllBasecallTrace();
		boolean[][] multi = new boolean[4][super.getSequenceLength()];
		
		int maxpeak = 0;
		
		for(int m = 0; m<dna[1].length; m++){
			
			//最大値の取得
			for(int n=0; n<4; n++){
				if(maxpeak < dna[n][m]){
					maxpeak = dna[n][m];
				}
			}
			
			//Multi peakの検出
			for(int n = 0; n<4; n++){
				if(maxpeak != 0 && (double)dna[n][m]/maxpeak >= (double)cutoff/100){
					multi[n][m]= true;
				}else{
					multi[n][m]= false;
				}
			}
			
			//maxpeakの初期化
			maxpeak = 0;
		}
		
		return multi;
	}

	private int[][] getAllBasecallTrace() {
		//各塩基ごとのベースコール座標でのトレースデータを含んだ二次元配列を返す
		//第一index 0:A 1:C 2:G 3:T
		
		int[][] dna = new int[4][super.getSequenceLength()];
		
		try {
			int[] basecall = super.getBasecalls();
			
			int[] a = super.getTrace(DNATools.a());
			int[] c = super.getTrace(DNATools.c());
			int[] g = super.getTrace(DNATools.g());
			int[] t = super.getTrace(DNATools.t());
			
			for(int n = 0; n < super.getSequenceLength(); n++) {
				dna[0][n] = a[basecall[n]];
				dna[1][n] = c[basecall[n]];
				dna[2][n] = g[basecall[n]];
				dna[3][n] = t[basecall[n]];
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		return dna;
	}
	
}
