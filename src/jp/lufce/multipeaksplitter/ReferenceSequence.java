package jp.lufce.multipeaksplitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.biojava.bio.BioException;
import org.biojava.bio.seq.Sequence;
import org.biojavax.bio.seq.RichSequence;
import org.biojavax.bio.seq.io.FastaFormat;
import org.biojavax.bio.seq.io.SimpleRichSequenceBuilder;

public class ReferenceSequence {

	private File f;
	FastaFormat format = new FastaFormat();
	private Sequence seq;
	private boolean[][] refseq;
	
	public ReferenceSequence(File file){
		f = file;
		try {
			this.makeSequence();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BioException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean[][] getRefseqMap(){
		if(seq != null) {
			return refseq;
		}else {
			return null;
		}
	}
	
	public String getSeqString(){
		if(seq != null) {
			return seq.seqString();
		}else {
			return "";
		}
	}
	
	public int getSequenceLength() {
		if(seq != null) {
			return seq.seqString().length();
		}else {
			return 0;
		}
	}
	
	public String getSeqSubstring(int start, int length) {
		if(length > 0 && start > 0 && start < seq.seqString().length()) {
			return seq.subStr(start, start + length -1);
		}else {
			return "";
		}
	}
	
	public boolean canRead() throws IOException {
		//ファイルがfasta形式か調べる。
		
		if(format.canRead(f)) {
			return true;
		}else {
			return false;
		}
	}
	
	private void makeSequence() throws IOException, BioException {
		//Sequence形式の塩基配列と、boolean配列形式の塩基配列を作成する。
		
		if(this.canRead() == false) return;
		
		SimpleRichSequenceBuilder builder = new SimpleRichSequenceBuilder();
		format.readSequence(new BufferedReader(new FileReader(f.getAbsolutePath())), RichSequence.IOTools.getDNAParser(), builder);
		seq = builder.makeSequence();
		refseq = refseq2Boolean(seq.seqString());
		
		return;
	}
	
	private static boolean[][] refseq2Boolean(String refseqString){

		boolean[][] boo = new boolean[4][refseqString.length()];
		char base;
		
		//初期化
		for(int n=0; n< refseqString.length(); n++) {
			boo[0][n] = false;
			boo[1][n] = false;
			boo[2][n] = false;
			boo[3][n] = false;
		}
		
		//塩基配列をbooleanの二次元配列に変換
		for(int n=0; n< refseqString.length(); n++) {
			base = refseqString.charAt(n);
			if(base == 'A' || base == 'a') {boo[0][n] = true;}
			else if(base == 'C' || base == 'c') {boo[1][n] = true;}
			else if(base == 'G' || base == 'g') {boo[2][n] = true;}
			else if(base == 'T' || base == 't') {boo[3][n] = true;}
		}
		
		return boo;
	}
}
