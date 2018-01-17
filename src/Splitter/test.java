package Splitter;

import java.io.*;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.program.abi.*;
import org.biojavax.bio.seq.io.*;

public class test {
	
	private static int cutoff = 10;  //Dotplotのノイズとしてカットする%
	private static int window = 7;  //Dotplotを整えるときののウィンドウサイズ
	
	public static void main(String argv[]){
		String filepath = "C:\\Users\\Shohei\\eclipse-workspace\\MultipeakSplitter\\file\\";
//		String fileIn1 = "20171129Nozaki-1_A07.ab1";
		String fileIn2 = "20170214-Nozaki-2_B09.ab1";
		String fileRef = "BBS1refseq.fa";
//		String fileOut1 = "single.jpg";
//		String fileOut2 = "multi.jpg";
		
		try {
			File i1 = new File(filepath+fileIn2);
			File fref = new File(filepath+fileRef);
			BufferedReader bref = new BufferedReader(new FileReader(filepath+fileRef));
			
//			File o1 = new File(filepath+"out.csv");
//			File o2 = new File(filepath+"basecall.csv");

			//トレースデータを読み込む
			ABITrace trace = new ABITrace(i1);
			
			//RefSeq配列を読み込む
			FastaFormat faref = new FastaFormat();
			SimpleRichSequenceBuilder refseqBuilder = new SimpleRichSequenceBuilder();
			
			if(faref.canRead(fref)==false) {
				System.out.println("Cannot read the reference sequence file");
				bref.close();
				return;
			}
			faref.readSequence(bref, faref.guessSymbolTokenization(fref), refseqBuilder);
			
			Sequence refseq = refseqBuilder.makeSequence();
			System.out.println(refseq.seqString());
			
//			System.out.println(trace.getSequenceLength());
//			System.out.println(trace.getTraceLength());
//			
//			int[][] dna = getAllBasecallTrace(trace);
			
//			String output2 = BaseLocationCSV(base,a_tr,c_tr,g_tr,t_tr);
//			
//			FileWriter fw2 = new FileWriter(o2);
//			fw2.write(output2);
//			fw2.close();
			
		}catch(Exception e) {
			System.out.println(e);
		}
		
		System.out.println("end");
	}
	
	private static String OutputCSV(int base[],int a[],int c[], int g[], int t[]) {
		String crlf = System.getProperty("line.separator");
		
		String out = "basecall,";
		for (int i =0; i < base.length; i++) {
			out = out + String.valueOf(base[i])+ ",";
		}
		out.substring(0, out.length() -1 );
		out = out + crlf;
		
		out = out + "A,";
		for (int i =0; i < a.length; i++) {
			out = out + String.valueOf(a[i])+ ",";
		}
		out.substring(0, out.length() -1 );
		out = out + crlf;
		
		out = out + "C,";
		for (int i =0; i < c.length; i++) {
			out = out + String.valueOf(c[i])+ ",";
		}
		out.substring(0, out.length() -1 );
		out = out + crlf;
		
		out = out + "G,";
		for (int i =0; i < g.length; i++) {
			out = out + String.valueOf(g[i])+ ",";
		}
		out.substring(0, out.length() -1 );
		out = out + crlf;
		
		out = out + "T,";
		for (int i =0; i < t.length; i++) {
			out = out + String.valueOf(t[i])+ ",";
		}
		out.substring(0, out.length() -1 );
		
		return out;
	}
	
	private static String BaseLocationCSV(int base[],int a[],int c[], int g[], int t[]) {
		String crlf = System.getProperty("line.separator");
		
		String out = "Basecall_A,";
		for (int i =0; i < base.length; i++) {
			out = out + String.valueOf(a[base[i]])+ ",";
		}
		out.substring(0, out.length() -1 );
		out = out + crlf;
		
		out = out + "Basecall_C,";
		for (int i =0; i < base.length; i++) {
			out = out + String.valueOf(c[base[i]])+ ",";
		}
		out.substring(0, out.length() -1 );
		out = out + crlf;
		
		out = out + "Basecall_G,";
		for (int i =0; i < base.length; i++) {
			out = out + String.valueOf(g[base[i]])+ ",";
		}
		out.substring(0, out.length() -1 );
		out = out + crlf;
		
		out = out + "Basecall_T,";
		for (int i =0; i < base.length; i++) {
			out = out + String.valueOf(t[base[i]])+ ",";
		}
		out.substring(0, out.length() -1 );
		
		return out;
	}

	private static void CreateDotPlot(ABITrace trace, Sequence refseq) {
		int[][] dna;
		boolean[][] multi, ref, dotmap, trimedmap;
		
		dna = getAllBasecallTrace(trace);
		
		System.out.println(dna[1].length);
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
}