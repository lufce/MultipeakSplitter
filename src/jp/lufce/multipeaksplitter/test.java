package jp.lufce.multipeaksplitter;
//package jp.lufce.multipeaksplitter;
//
//import java.io.*;
//import org.biojava.bio.seq.DNATools;
//import org.biojava.bio.seq.Sequence;
//import org.biojava.bio.program.abi.*;
//import org.biojavax.bio.seq.RichSequence;
//import org.biojavax.bio.seq.io.*;
//
//public class test {
//	
//	private static int cutoff = 10;  //Dotplotのカットオフ用
//	private static int window = 7;  //Dotplotウィンドウサイズ
//	
//	public static void main(String argv[]){
//		String filepath = "C:\\Users\\Shohei\\eclipse-workspace\\MultipeakSplitter\\file\\";
////		String filepath = "C:\\Users\\shohei_desk\\git\\MultipeakSplitter\\file\\";
////		String fileIn2 = "20171129Nozaki-1_A07.ab1";
////		String fileIn2 = "20170214-Nozaki-2_B09.ab1";
//		String fileIn2 = "sec8.ab1";
////		String fileRef = "BBS1refseq.fa";
//		String fileRef = "sec8.fa";
////		String fileOut1 = "single.jpg";
////		String fileOut2 = "multi.jpg";
//		
//		try {
//
//			File i1 = new File(filepath+fileIn2);
//			File fref = new File(filepath+fileRef);
//			
//			File o1 = new File(filepath+"out3.csv");
////			File o2 = new File(filepath+"basecall.csv");
//
//			//配列を読み込む
//			Ab1Sequence sampleSequence = new Ab1Sequence(i1);
//			ReferenceSequence refseq = new ReferenceSequence(fref);
//			if(refseq.makeSequence() == false) {
//				System.out.println("error");
//				return;
//			}
//			
//			boolean[][] dotplot = CreateDotPlot(sampleSequence.getMultipeak(cutoff),refseq.getRefseq());
//			String[] strDotplot;
//			
//			FileWriter fw = new FileWriter(o1);
//			strDotplot = BooleanDotplotCSV(dotplot);
//			for(int n = 0; n < strDotplot.length; n++) {
//				fw.write(strDotplot[n]);
//			}
//			fw.close();
//			
////			String output2 = BaseLocationCSV(base,a_tr,c_tr,g_tr,t_tr);
////			
////			FileWriter fw2 = new FileWriter(o2);
////			fw2.write(output2);
////			fw2.close();
//			
//		}catch(Exception e) {
//			e.printStackTrace();
//		}
//		
//		System.out.println("end");
//	}
//
//	private static String BooleanBaseCSV(boolean[][] boo){
//		String crlf = System.getProperty("line.separator");	
//		
//		String out = "A,C,G,T"+crlf;
//		String[] base = new String[4];
//		base[0]="A";
//		base[1]="C";
//		base[2]="G";
//		base[3]="T";
//		
//		for (int m=0; m < boo[1].length; m++){
//			for(int n=0; n<4; n++){
//				if(boo[n][m]){
//					out = out+base[n]+",";
//				}
//			}
//			out = out+crlf;
//		}
//		
//		return out;
//	}
//	
//	private static String[] BooleanDotplotCSV(boolean[][] dotplot) {
//		String crlf = System.getProperty("line.separator");	
//		String[] out = new String[dotplot.length];
//		
//		for (int m=0; m < dotplot.length; m++){
//			
//			out[m] = "";	//初期化
//			for(int n=0; n < dotplot[0].length; n++){
//				if(dotplot[m][n]){
//					out[m] = out[m]+"1,";
//				}else {
//					out[m] = out[m]+"0,";
//				}
//			}
//			out[m] = out[m]+crlf;
//		}
//		
//		return out;
//	}
//	
//	private static boolean[][] CreateDotPlot(boolean[][] multi, boolean[][] ref) {
//
//		return windowDotplot(booleanDotplot(multi,ref));
//		
//	}
//	
//
//	private static boolean[][] booleanDotplot(boolean[][] multi, boolean[][] refseq){
//		boolean[][] dotplot = new boolean[multi[1].length][refseq[1].length];
//
//		for(int m=0; m<multi[1].length ;m++) {
//			for(int n=0; n<refseq[1].length; n++) {
//				if(multi[0][m] && refseq[0][n]) {dotplot[m][n]=true;}
//				else if(multi[1][m] && refseq[1][n]) {dotplot[m][n]=true;}
//				else if(multi[2][m] && refseq[2][n]) {dotplot[m][n]=true;}
//				else if(multi[3][m] && refseq[3][n]) {dotplot[m][n]=true;}
//				else {dotplot[m][n]=false;}
//			}
//		}
//		
//		return dotplot;
//	}
//	
//	private static boolean[][] windowDotplot(boolean[][] dotplot){
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
//}