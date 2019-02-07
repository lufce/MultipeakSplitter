package jp.lufce.multipeaksplitter;

import java.io.File;
import java.io.IOException;

import org.biojavax.bio.seq.io.FastaFormat;

public class SequenceMaster{

	private int dataType = 0;			//このシークエンスデータがFASTAなのかABIなのか未定義なのかを決める。
										// 0:未定義　1:FASTA　2:ABI　とする

	static final public int typeUndef = 0;
	static final public int typeFasta = 1;
	static final public int typeAb1   = 2;

	private FastaFormat fastaFormat = new FastaFormat();

	protected FastaSequence fastaSeq;
	protected Ab1Sequence ab1Seq;

	public SequenceMaster(File file) throws IOException {
		if(fastaFormat.canRead(file)) {
			fastaSeq = new FastaSequence(file);
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
			map = fastaSeq.getMap();break;
		case typeAb1:
			map = ab1Seq.getMap();break;
		}

		return map;
	}

	public boolean[][] getRevcomMap(){
		boolean[][] map = null;

		switch(dataType) {
		case typeFasta:
			map = fastaSeq.getRevcomMap();break;
		case typeAb1:
			map = ab1Seq.getRevcomMap();break;
		}

		return map;
	}

	public int getSequenceLength() {
		int sequenceLength = 0;

		switch(dataType) {
		case typeFasta:
			sequenceLength = fastaSeq.getSequenceLength();break;
		case typeAb1:
			sequenceLength = ab1Seq.getSequenceLength();break;
		}

		return sequenceLength;

	}

}
