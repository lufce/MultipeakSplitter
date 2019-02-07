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


public class FastaSequence {

	//TODO 今はFASTAファイルしか対応していないが、Plainテキストとかマルチピークのテキスト出力ファイルとかを読み込めるようにしたい。

	private File f;
	FastaFormat format = new FastaFormat();
	private Sequence seq;
	private boolean[][] map;
	private boolean[][] revcomMap;

	final static private int A = 0;
	final static private int C = 1;
	final static private int G = 2;
	final static private int T = 3;

	FastaSequence(File file){
		f = file;
		try {
			this.makeSequence();
			map = new boolean[4][seq.length()];
			revcomMap = new boolean[4][seq.length()];
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BioException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean[][] getMap(){
		if(seq != null) {
			return map;
		}else {
			return null;
		}
	}

	public boolean[][] getRevcomMap(){
		if(seq != null) {
			return revcomMap;
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

		return;
	}

	public void makeMap() {
		String seqString = seq.seqString();
		char base;

		//塩基配列をbooleanの二次元配列に変換
		for(int n=0; n< seqString.length(); n++) {
			base = seqString.charAt(n);

			switch(base) {
			case 'A':
			case 'a':
				map[A][n] = true;
				revcomMap[T][n] = true;
				break;
			case 'C':
			case 'c':
				map[C][n] = true;
				revcomMap[G][n] = true;
				break;
			case 'G':
			case 'g':
				map[G][n] = true;
				revcomMap[C][n] = true;
				break;
			case 'T':
			case 't':
				map[T][n] = true;
				revcomMap[A][n] = true;
				break;
			case 'N':
			case 'n':
				for(int i = 0; i < 4; i++) {
					map[i][n] = true;
					revcomMap[i][n] = true;
				}
				break;
			}
		}
	}
}
