# MultipeakSplitter設計
## 1. 目的
配列サンプルとしてab1ファイルを、リファレンスファイルとしてfastaファイルを読み込み、アラインメントをドットプロット画像として出力する。  
その画像を見ながら、任意のアラインメント配列をクリックして抜き出せるようにしたい。
## 2. 機能
- ab1ファイルを読み込んでマルチピークを検出する。
- fastaファイルを読み込んでDNA配列を取得する。
- マルチピークを全て考慮してドットプロット画像を作成する。
- マルチピーク画像をクリックすることで、任意のアラインメント配列をテキストとして抜き出す。

## 3. クラス設計
### Ab1Sequenceクラス
ab1ファイルのRAWデータやマルチピークのデータを保持する。

**クラス変数**  
int private cutoff: マルチピークを検出する際に、ノイズとして無視するシグナル強度の値。各baseCall地点での最大ピーク値に対する割合で示す。  
int private window: ドットプロットを描く際のウィンドウサイズ。  
int final private static a = 0: アデニン。ベースコール値などの引数に使う用。  
int final private static c = 1: シトシン。  
int final private static g = 2: グアニン。   
int final private static t = 3: チミン。  
int final private rawLength: シグナル強度の配列の長さ。  
int final private sequenceLnegth: 読めた配列の長さ。ベースコールの長さ。    
int final private beseCall\[4] \[sequenceLength]: ベースコール値。 第一引数は塩基。0:A 1:C 2:G 3:T  
int final private intensity\[4] \[rawLength]: シグナル強度。 第一引数は塩基。0:A 1:C 2:G 3:T  
boolean private peak\[4] \[sequenceLength]: ピークとして認められたかどうか。第一引数は塩基。  

