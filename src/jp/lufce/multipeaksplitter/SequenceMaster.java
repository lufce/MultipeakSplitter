package jp.lufce.multipeaksplitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.biojavax.bio.seq.io.FastaFormat;

public class SequenceMaster{

	private int dataType = 0;			//このシークエンスデータがFASTAなのかABIなのか未定義なのかを決める。
										// 0:未定義　1:FASTA　2:ABI　とする
	protected boolean[][] map;

	//final private int typeUndef = 0;
	final private int typeFasta = 1;
	final private int typeAb1   = 2;

	private FastaFormat fastaFormat = new FastaFormat();

	protected TextSequence textSeq;
	protected Ab1Sequence ab1Seq;

	public SequenceMaster(File file) throws IOException {
		if(fastaFormat.canRead(file)) {
			textSeq = new TextSequence(file);
			dataType = typeFasta;
		}else {
			ab1Seq = new Ab1Sequence(file);
			dataType = typeAb1;
		}
	}

	public int getDataType() {
		return dataType;
	}

	public boolean[][] getMap(){
		boolean[][] map = null;

		switch(dataType) {
		case typeFasta:
			map = textSeq.getMap();break;
		case typeAb1:
			map = ab1Seq.getMultipeakMap();break;
		}

		return map;
	}


}
