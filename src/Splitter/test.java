package Splitter;

import java.io.*;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.program.abi.*;
import org.biojavax.bio.seq.RichSequence;
import org.biojavax.bio.seq.io.*;

public class test {
	
	private static double cutoff = 0.1;  //Dotplotのカットオフ用
	private static int window = 7;  //Dotplotウィンドウサイズ
	
	public static void main(String argv[]){
		String filepath = "C:\\Users\\Shohei\\eclipse-workspace\\MultipeakSplitter\\file\\";
//		String filepath = "C:\\Users\\shohei_desk\\git\\MultipeakSplitter\\file\\";
//		String fileIn2 = "20171129Nozaki-1_A07.ab1";
//		String fileIn2 = "20170214-Nozaki-2_B09.ab1";
		String fileIn2 = "sec8.ab1";
//		String fileRef = "BBS1refseq.fa";
		String fileRef = "sec8.fa";
//		String fileOut1 = "single.jpg";
//		String fileOut2 = "multi.jpg";
		
		try {

			File i1 = new File(filepath+fileIn2);
			File fref = new File(filepath+fileRef);
			BufferedReader bref = new BufferedReader(new FileReader(filepath+fileRef));
			
			File o1 = new File(filepath+"out2.csv");
//			File o2 = new File(filepath+"basecall.csv");

			//トレースデータを読み込む
			ABITrace trace = new ABITrace(i1);
			
			System.out.println("basecallLength:"+trace.getBasecalls().length);
			
			//RefSeq読み込み
			FastaFormat faref = new FastaFormat();
			SimpleRichSequenceBuilder refseqBuilder = new SimpleRichSequenceBuilder();

			
			if(faref.canRead(fref)==false) {
				System.out.println("Cannot read the reference sequence file");
				bref.close();
				return;
			}
			faref.readSequence(bref, RichSequence.IOTools.getDNAParser(), refseqBuilder);
			
			Sequence refseq = refseqBuilder.makeSequence();
			
			boolean[][] dotplot = CreateDotPlot(trace,refseq);
			String[] strDotplot;
			
			FileWriter fw = new FileWriter(o1);
			strDotplot = BooleanDotplotCSV(dotplot);
			for(int n = 0; n < strDotplot.length; n++) {
				fw.write(strDotplot[n]);
			}
			fw.close();
			
//			String output2 = BaseLocationCSV(base,a_tr,c_tr,g_tr,t_tr);
//			
//			FileWriter fw2 = new FileWriter(o2);
//			fw2.write(output2);
//			fw2.close();
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("end");
	}

	private static String BooleanBaseCSV(boolean[][] boo){
		String crlf = System.getProperty("line.separator");	
		
		String out = "A,C,G,T"+crlf;
		String[] base = new String[4];
		base[0]="A";
		base[1]="C";
		base[2]="G";
		base[3]="T";
		
		for (int m=0; m < boo[1].length; m++){
			for(int n=0; n<4; n++){
				if(boo[n][m]){
					out = out+base[n]+",";
				}
			}
			out = out+crlf;
		}
		
		return out;
	}
	
	private static String[] BooleanDotplotCSV(boolean[][] dotplot) {
		String crlf = System.getProperty("line.separator");	
		String[] out = new String[dotplot.length];
		
		for (int m=0; m < dotplot.length; m++){
			System.out.println(m);
			out[m] = "";	//初期化
			for(int n=0; n < dotplot[0].length; n++){
				if(dotplot[m][n]){
					out[m] = out[m]+"1,";
				}else {
					out[m] = out[m]+"0,";
				}
			}
			out[m] = out[m]+crlf;
		}
		
		return out;
	}
	
	private static boolean[][] CreateDotPlot(ABITrace trace, Sequence refseq) {
		
		boolean[][] multi, ref;
		
		multi = getMultiBase(getAllBasecallTrace(trace));
		ref = Refseq2Boolean(refseq.seqString());
		
		return windowDotplot(booleanDotplot(multi,ref));
		
	}
	
	private static int[][] getAllBasecallTrace(ABITrace trace) {
		//各塩基ごとのベースコール座標でのトレースデータを含んだ二次元配列を返す
		//第一index 0:A 1:C 2:G 3:T
		
		int[][] dna = new int[4][trace.getSequenceLength()];
		
		try {
			int[] basecall = trace.getBasecalls();
			
			int[] a = trace.getTrace(DNATools.a());
			int[] c = trace.getTrace(DNATools.c());
			int[] g = trace.getTrace(DNATools.g());
			int[] t = trace.getTrace(DNATools.t());
			
			for(int n = 0; n < trace.getSequenceLength(); n++) {
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
	
	private static boolean[][] getMultiBase(int[][] dna){
		int maxpeak = 0;
		
		boolean[][] multi = new boolean[4][dna[1].length];
		
		for(int m = 0; m<dna[1].length; m++){
			
			//最大値の取得
			for(int n=0; n<4; n++){
				if(maxpeak < dna[n][m]){
					maxpeak = dna[n][m];
				}
			}
			
			//Multi peakの検出
			for(int n = 0; n<4; n++){
				if(maxpeak != 0 && (double)dna[n][m]/maxpeak >= cutoff){
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

	private static boolean[][] Refseq2Boolean(String refseq){

		boolean[][] boo = new boolean[4][refseq.length()];
		char base;
		
		//初期化
		for(int n=0; n< refseq.length(); n++) {
			boo[0][n] = false;
			boo[1][n] = false;
			boo[2][n] = false;
			boo[3][n] = false;
		}
		
		//塩基配列をbooleanの二次元配列に変換
		for(int n=0; n< refseq.length(); n++) {
			base = refseq.charAt(n);
			if(base == 'A' || base == 'a') {boo[0][n] = true;}
			else if(base == 'C' || base == 'c') {boo[1][n] = true;}
			else if(base == 'G' || base == 'g') {boo[2][n] = true;}
			else if(base == 'T' || base == 't') {boo[3][n] = true;}
		}
		
		return boo;
	}
	
	private static boolean[][] booleanDotplot(boolean[][] multi, boolean[][] refseq){
		boolean[][] dotplot = new boolean[multi[1].length][refseq[1].length];

		for(int m=0; m<multi[1].length ;m++) {
			for(int n=0; n<refseq[1].length; n++) {
				if(multi[0][m] && refseq[0][n]) {dotplot[m][n]=true;}
				else if(multi[1][m] && refseq[1][n]) {dotplot[m][n]=true;}
				else if(multi[2][m] && refseq[2][n]) {dotplot[m][n]=true;}
				else if(multi[3][m] && refseq[3][n]) {dotplot[m][n]=true;}
				else {dotplot[m][n]=false;}
			}
		}
		
		return dotplot;
	}
	
	private static boolean[][] windowDotplot(boolean[][] dotplot){
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
	
//	private static String OutputCSV(int base[],int a[],int c[], int g[], int t[]) {
//	String crlf = System.getProperty("line.separator");
//	
//	String out = "basecall,";
//	for (int i =0; i < base.length; i++) {
//		out = out + String.valueOf(base[i])+ ",";
//	}
//	out.substring(0, out.length() -1 );
//	out = out + crlf;
//	
//	out = out + "A,";
//	for (int i =0; i < a.length; i++) {
//		out = out + String.valueOf(a[i])+ ",";
//	}
//	out.substring(0, out.length() -1 );
//	out = out + crlf;
//	
//	out = out + "C,";
//	for (int i =0; i < c.length; i++) {
//		out = out + String.valueOf(c[i])+ ",";
//	}
//	out.substring(0, out.length() -1 );
//	out = out + crlf;
//	
//	out = out + "G,";
//	for (int i =0; i < g.length; i++) {
//		out = out + String.valueOf(g[i])+ ",";
//	}
//	out.substring(0, out.length() -1 );
//	out = out + crlf;
//	
//	out = out + "T,";
//	for (int i =0; i < t.length; i++) {
//		out = out + String.valueOf(t[i])+ ",";
//	}
//	out.substring(0, out.length() -1 );
//	
//	return out;
//}

//private static String BaseLocationCSV(int base[],int a[],int c[], int g[], int t[]) {
//	String crlf = System.getProperty("line.separator");
//	
//	String out = "Basecall_A,";
//	for (int i =0; i < base.length; i++) {
//		out = out + String.valueOf(a[base[i]])+ ",";
//	}
//	out.substring(0, out.length() -1 );
//	out = out + crlf;
//	
//	out = out + "Basecall_C,";
//	for (int i =0; i < base.length; i++) {
//		out = out + String.valueOf(c[base[i]])+ ",";
//	}
//	out.substring(0, out.length() -1 );
//	out = out + crlf;
//	
//	out = out + "Basecall_G,";
//	for (int i =0; i < base.length; i++) {
//		out = out + String.valueOf(g[base[i]])+ ",";
//	}
//	out.substring(0, out.length() -1 );
//	out = out + crlf;
//	
//	out = out + "Basecall_T,";
//	for (int i =0; i < base.length; i++) {
//		out = out + String.valueOf(t[base[i]])+ ",";
//	}
//	out.substring(0, out.length() -1 );
//	
//	return out;
//}
	
}