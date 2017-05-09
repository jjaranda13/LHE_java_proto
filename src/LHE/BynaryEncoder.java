package LHE;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import huffman.*;


/**
 *
 * 
 * @author josejavg
 *
 *This class provides methods for binary encoding of LHE hops and grid's PR values
 *taking into account spatial redundancies
 *
 *
 *
 */
public class BynaryEncoder {
    boolean DEBUG=true;
	int width=0;
	int height=0;
	
	int totalhops=0;
	int[] stathops;// estadisticas de hops
	int[] bits_len;// longitudes de bit asignadas a los hops
	int freq_sym[]; // apariciones de cada simbolo
	StringBuffer txt;//cadena de simbolos
	int compressed_len;//longitud final en bits
	int num_sym=0;
	int num_sym2bit=0;
	int freq_sym2bit[]=new int[81];
	int compressed_len2bit;//longitud final en bits
	int compressed_len3bit;
	int compressed_len4bit;
	int statup=0;
	
	int numunos=0;
	
	
	
	public int[][] tr=new int[9][9];
	public int[][] tr_prediction=new int[9][9];
	public int[][] tr_prediction_len=new int[9][9];
	
	public int[] down_stats=new int [10];//del 0 al 9 
	
	public int[][] down_stats_saco=new int [4][10];//del 0 al 9 
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	/*
	 * constructor
	 * 
	 * 
	 * 
	 */
	BynaryEncoder(int width, int height)
	{
		this.width=width;
		this.height=height;
		totalhops=0;
		
		
		//array para estadisticas de hops
		stathops=new int [9];
		
		//esta asignacion es innecesaria
		stathops[0]=0;//000000001
		stathops[1]=0;//0000001
		stathops[2]=0;//00001
		stathops[3]=0;//001
		stathops[4]=0;//1
		stathops[5]=0;//01
		stathops[6]=0;//0001
		stathops[7]=0;//000001
		stathops[8]=0;//00000001
		
		//asignacion de longitud de codigos a hops
		//cada 0 descarta un hop. si hay 8 ceros hay 8 descartes, no hay confusion, pero para huffman es necesario diferenciar
		//ya que vamos a usar la longitud como simbolo
		
		//como al final no uso huffman, no hace falta que el simbolo mas largo tenga 9 bit
		bits_len=new int[9];
		bits_len[0]=8;//000000000    
		bits_len[1]=7;//0000001
		bits_len[2]=5;//00001
		bits_len[3]=3;//001
		bits_len[4]=1;//1
		bits_len[5]=2;//01
		bits_len[6]=4;//0001
		bits_len[7]=6;//000001
		bits_len[8]=8;//00000001
		
		//apariciones de cada symbol
		freq_sym=new int[10];//el simbolo 0 mide 1, el 3 mide 4, etc. son 9 simbolos. pero consideraremos un simbolo extra, el 0, para bloques nulos
		txt=new StringBuffer("");//cadena de simbolos de la imagen
		
		
		//longitud final en bits de los simbolos
		compressed_len=0;
		
		//numero de ocurrencias de redundancia vertical
		statup=0;
		
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	
	/*
	 * hops to bits
	 * inputs: array of hops and limits
	 * 
	 * 0 1 2 3 4 5 6 7 8
	 *      <--|--->
	 *      
	 *      esta funcion retorna el numero de bits, nada mas
	 */

	public int hopsToBits(int[] hops, int xini, int yini, int xfin, int yfin, float PRx,float PRy)
	{
		
		//las prx y pry son promedio
		int lx=xfin-xini;
		int ly=yfin-yini;
		//char[] symbols_block=new char[lx*ly];
		
		int bits_counter=0;
		int pos=yini*width+xini;
		int[] discard=new int[9];
		
		
		//setLenCodesPrediction();
		int hops_no_nulos=0;
		
		int init=0;
		
		
		
		//int result[]=new int[2];
		//for (int intento=0;intento<2;intento++)
		//{bits_counter=0;
		if (PRx==0.0f && PRy==0.0f) //OPTIMIZACION
			{
			//System.out.println(" OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
			//return 0;
			//addSymbol(1);
			
			}
		for (int y=yini; y<=yfin;y++)
		{
			for (int x=xini; x<=xfin; x++)
			{
				totalhops++;
				
				int symbol_len=0;//longitud del simbolo a insertar
				
				boolean found=false;
				for (int i=0;i<=8;i++) discard[i]=0;//hops no discarded
				pos=y*width+x;
				int hop=hops[pos];
			
				stathops[hop]++;
				if (hop!=4) hops_no_nulos++;
				
				
				
				int up=-1;
				if (y>yini) up=hops[pos-width];
				
				int left=-1;
				if (x>xini) {left=hops[pos-1];}//left=left-4;if (left<0) left=left+1; else if (left>0) left=left-1;left=left +4;} 
				
				int upi=-1;
				if (y>yini) up=hops[pos-width];
				
				
				//para las estadisticas
				if (x>xini && y>yini)	upi=hops[pos-width-1];
				//-------------------------------------
				
				
				
				
				
				//if (lx<ly) //esta mejora es minima pero es algo
				if (PRx<=PRy) //esta mejora es minima pero es algo
				//if (1==1)
					{	
					
					    //redundancia horizontal
						int predicted_hop=4;
						//if (left!=-1)	predicted_hop=tr_prediction[left][0];
						if (hop==predicted_hop) {bits_counter++;found=true;symbol_len++;addSymbol(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalizacion

						//redundancia vertical	
						if (up!=-1) predicted_hop=up;
						if (hop==predicted_hop) {bits_counter++;found=true;symbol_len++;addSymbol(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalizacion

						
						/*predicted_hop=4;
						if (upi!=-1)	predicted_hop=upi;
						if (hop==predicted_hop) {bits_counter++;found=true;continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;discard[predicted_hop]=1;}//penalizacion
                         */
						
						//disminuciones de hop paulatinas
						if (left!=-1 && left >5) predicted_hop=left-1;
						else if (left!=-1 && left <3) predicted_hop=left+1;
						if (hop==predicted_hop) {bits_counter++;found=true;symbol_len++;addSymbol(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalizacion
						
						
						
						/*
						if (left!=-1 && left >6) predicted_hop=left-1;
						else if (left!=-1 && left <2) predicted_hop=left+1;
						if (hop==predicted_hop) {bits_counter++;found=true;continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;discard[predicted_hop]=1;}//penalizacion
						
						if (left!=-1 && left >7) predicted_hop=left-1;
						else if (left!=-1 && left <1) predicted_hop=left+1;
						if (hop==predicted_hop) {bits_counter++;found=true;continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;discard[predicted_hop]=1;}//penalizacion
						*/
						
						
						if (up!=-1 && up >5) predicted_hop=up-1;
						else if (up!=-1 && up <3) predicted_hop=up+1;
						if (hop==predicted_hop) {bits_counter++;found=true;statup++;symbol_len++;addSymbol(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalizacion
						
						
						
						/*
						if (up!=-1 && up >6) predicted_hop=up-2;
						else if (up!=-1 && up <2) predicted_hop=up+2;
						if (hop==predicted_hop) {bits_counter++;found=true;continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;discard[predicted_hop]=1;}//penalizacion
						
						if (up!=-1 && up >7) predicted_hop=up-3;
						else if (up!=-1 && up <1) predicted_hop=up+3;
						if (hop==predicted_hop) {bits_counter++;found=true;continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;discard[predicted_hop]=1;}//penalizacion
						*/
					}
					else
					{
						

						//redundancia V				
						if (up!=-1 && hop==up ){bits_counter++;found=true;statup++;symbol_len++;addSymbol(symbol_len);continue;}
						else if (up!=-1 && discard[up]!=1 ) {bits_counter++;symbol_len++;discard[up]=1;}//penalizacion
						
						
						
						
						//redundancia H
						int predicted_hop=4;
						//if (left!=-1)	predicted_hop=tr_prediction[left][0];
						if (hop==predicted_hop) {bits_counter++;found=true;symbol_len++;addSymbol(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalizacion
						
						
						
						//disminuciones de hops paulatinas
						if (up!=-1 && up >5) predicted_hop=up-1;
						else if (up!=-1 && up <3) predicted_hop=up+1;
						if (hop==predicted_hop) {bits_counter++;statup++;found=true;symbol_len++;addSymbol(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalizacion
						
						
						
						/*
						if (up!=-1 && up >6) predicted_hop=up-2;
						else if (up!=-1 && up <2) predicted_hop=up+2;
						if (hop==predicted_hop) {bits_counter++;found=true;continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;discard[predicted_hop]=1;}//penalizacion
						
						if (up!=-1 && up >7) predicted_hop=up-3;
						else if (up!=-1 && up <1) predicted_hop=up+3;
						if (hop==predicted_hop) {bits_counter++;found=true;continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;discard[predicted_hop]=1;}//penalizacion
						*/
						
						
						if (left!=-1 && left >5) predicted_hop=left-1;
						else if (left!=-1 && left <3) predicted_hop=left+1;
						if (hop==predicted_hop) {bits_counter++;found=true;symbol_len++;addSymbol(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalizacion
						
						
						
						
						/*
						if (left!=-1 && left >6) predicted_hop=left-1;
						else if (left!=-1 && left <2) predicted_hop=left+1;
						if (hop==predicted_hop) {bits_counter++;found=true;continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;discard[predicted_hop]=1;}//penalizacion
						
						if (left!=-1 && left >7) predicted_hop=left-1;
						else if (left!=-1 && left <1) predicted_hop=left+1;
						if (hop==predicted_hop) {bits_counter++;found=true;continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;discard[predicted_hop]=1;}//penalizacion
						*/
					}
				
				
				
				
				//ahora asignamos el codigo
				int len_hop=bits_len[hop];
				//if (left!=-1) len_hop=tr_prediction_len[left][hop];
				
				int discount=0;
				for (int j=0;j<=8;j++)
				{
					int lenj=bits_len[j];
					//if (left!=-1) lenj=tr_prediction_len[left][j];
					
					if (lenj<len_hop && discard[j]==1) discount++;
					//if (j==up) System.out.println("discard UP:"+discard[j]+"-->discount:"+discount);
				}
				
				//if (!found) sobra 
					bits_counter=bits_counter+len_hop-discount;
					//symbol_len=len_hop-discount;
					symbol_len+=len_hop-discount;
					addSymbol(symbol_len);
				
			}//x
			//pos+=width;
		}//y
		
		//if (hops_no_nulos==0) bits_counter=0;
		//else bits_counter++;
		//result[intento]=bits_counter;
		//}//intento
		//return result[1];
		//if (result[0]<result[1]) {System.out.println("mejor 0");return result[0]+1;}
		//else {System.out.println("mejor 1");return result[1]+1;}
		
		//symbols.append(symbols_block);
		//System.out.println(" PRx="+PRx+"PRy="+PRy);
		
		if (PRx==0.0f && PRy==0.0f && hops_no_nulos==0) 
		{
			//bits_counter-=4;
			
			System.out.println(txt.length());
			txt.delete(txt.length()-4, txt.length());
			freq_sym[0]-=4;
			System.out.println(" "+txt.length());
			
		}
		if (PRx==0.0f && PRy==0.0f ) 
		{
			addSymbol(1);
			//bits_counter++;
		}
		return bits_counter;
	}
	
	//%%%%%%%%%%%%%%%%%%%%%
	public void printStatHops()
	{
	//	if (1==1) return;
		System.out.println("----------------STATS HOPS---------------------------");
		
		for (int i=0;i<9;i++)
		{
			System.out.println(" hops["+i+"]="+stathops[i]+"   %="+((float)stathops[i]*100/totalhops));
		}
		System.out.println("UPS:"+statup);
		
System.out.println("----------------STATS SYMBOLS---------------------------");
		
		for (int i=0;i<9;i++)
		{
			System.out.println(" freq_sym["+i+"]="+freq_sym[i]+"   %="+((float)freq_sym[i]*100/totalhops));
		}
		System.out.println("longitud en simbolos:"+txt.length());
		
		ajust_compressed_len();
		
		System.out.println("compressed_len:"+compressed_len+ "     bppHop="+((float)compressed_len/totalhops));
		
		System.out.println("compressed_len:"+compressed_len+ "     bpp="+((float)compressed_len/(width*height)));
		
		
		
System.out.println("----------------STATS SYMBOLS 2bit---------------------------");
		/*
		for (int i=0;i<81;i++)
		{
			if (freq_sym2bit[i]>0)
			System.out.println(" freq_sym2bit["+i+"]="+freq_sym2bit[i]+"   %="+((float)freq_sym2bit[i]*100/((float)totalhops/2f)));
		}
		*/
		//Huffman huff=new Huffman();
		
		//huff.getTranslateCodes(freq_sym2bit);
		System.out.println("compressed_len2bit:"+compressed_len2bit+ "     bpp="+((float)compressed_len2bit/(width*height)));
		resumeTXT();
		System.out.println("compressed_len3bit:"+compressed_len3bit+ "     bpp="+((float)compressed_len3bit/(width*height))+"   bbpphop="+(float)compressed_len3bit/(float)totalhops);
		resumeTXT2();
		System.out.println("compressed_len4bit:"+compressed_len4bit+ "     bpp="+((float)compressed_len4bit/(width*height))+"   bbpphop="+(float)compressed_len4bit/(float)totalhops);
		
		TXT2BIN();
		/*
		System.out.println("---------------- TR STATS---------------------------");
		
		for (int i=0;i<9;i++)
		{
		  for (int j=0;j<9;j++)
		  {
			System.out.println(" tr["+i+"]["+j+"]"+tr[i][j]);
			
			
		  }//j
		}//i
		
		
		
		System.out.println("----------------codigos---------------------------");
		for (int i=0;i<9;i++)
		{
		//  for (int j=0;j<9;j++)
		  {
		   int len=1;
		   for (int l=0;l<9;l++)
		    {
			int max=-1;
			int n=-1;
			//System.out.println("max:"+max);
			for (int k=0;k<9;k++)
		      {
				//System.out.println("  tr["+i+"]["+k+"]"+tr[i][k]);
		      if (tr[i][k]>max) {max=tr[i][k];n=k;}//n es el escogido, el mayor en cada vuelta
		      }//k
			//System.out.println(" mejor es tr["+i+"]["+n+"]"+tr[i][n]);
		    tr_prediction_len[i][n]=len;
		    tr_prediction[i][len-1]=n;
		    tr[i][n]=-1000;
		    len++;
		    
		    }//l
		  }//j
		}//i
		//-------
		for (int i=0;i<9;i++)
		{
		  for (int j=0;j<9;j++)
		  {
			  System.out.println(" tr_prediction["+i+"]["+j+"]="+tr_prediction[i][j]+";");
			  System.out.println(" tr_prediction_len["+i+"]["+j+"]="+tr_prediction_len[i][j]+";");
			}//j
	    }//i
		*/
}


public void setLenCodesPrediction()
{
	//esto es para rellenar con
	 tr_prediction[0][0]=1;
	 tr_prediction_len[0][0]=8;
	 
	 //etc
	 
}
public void compressHopsHuffman()
{
	
	
	Huffman huff=new Huffman();
	
	huff.getTranslateCodes(freq_sym2bit);
	
	
}
public void addSymbol(int s)
{
	freq_sym[s-1]++;
	num_sym++;
	
	//txt.append((char)(s));
	//txt.append("s");
	//System.out.println(""+s);
	txt.append(s);
	//if (num_sym%2==0 && num_sym>0) addSymbol2bit((int)(txt.charAt(num_sym-2)),(int)(txt.charAt(num_sym-1)));
	//System.out.println("simbolo"+s);
	compressed_len+=s;//longitud en bits
}
public void ajust_compressed_len()
{compressed_len=0;
	for (int i=0;i<txt.length();i++)
	{
		int lens1=txt.charAt(i)-48;
		compressed_len+=lens1;
	}
}


public void resumeTXT()
{
	System.out.println(" la longitud inicial es "+txt.length()+" simbolos, y mide "+compressed_len);
	for (int i=0;i<txt.length();i+=2)
	{
		int lens1=txt.charAt(i)-48;
		int lens2=0;
		if (i<txt.length()-1) lens2=(int)(txt.charAt(i+1)-48);
		
		int lens3=0;
		if (i<txt.length()-2) lens3=(int)(txt.charAt(i+2)-48);
		
		int lens4=0;
		if (i<txt.length()-3) lens4=(int)(txt.charAt(i+3)-48);
		//System.out.println(""+lens1+""+lens2);//+""+lens3);
		if (lens1==1 && lens2==1)// && lens3>1)// && lens4==1)
		{
			compressed_len3bit++;
			//compressed_len3bit=compressed_len3bit+1+lens3;
			//i++;
			continue;
		}
		else compressed_len3bit+=1+lens1+lens2;//+lens3;//pej 01-1
			
		
		
	}
	System.out.println(" la longitud final es "+compressed_len3bit);	
}

public StringBuffer txt2bin(StringBuffer txt)
{
StringBuffer txtbin=new StringBuffer();
	
	for (int i=0;i<txt.length();i+=1)
	{
		int lens1=(int)(txt.charAt(i)-48);
		for (int j=1;j<lens1;j++)
		{
			txtbin.append("0");
			//System.out.println("0");
		}
		txtbin.append("1");
		//System.out.println("1");
	}
	System.out.println("lenbin:"+txtbin.length());
	return txtbin;
}

public void TXT2BIN(){
	System.out.println("............entramos en TXT2BIN.................");
	int lenbin=0;
	StringBuffer txtbin=new StringBuffer();
	
	for (int i=0;i<txt.length();i+=1)
	{
		int lens1=(int)(txt.charAt(i)-48);
		for (int j=1;j<lens1;j++)
		{
			txtbin.append("0");
			//System.out.println("0");
		}
		txtbin.append("1");
		//System.out.println("1");
	}
	System.out.println("lenbin:"+txtbin.length());
	
	int[] freqbin2=new int[4];
	for (int i=0;i<txtbin.length();i+=2)
	{
		int lens1=(int)(txtbin.charAt(i)-48);
		int lens2=0;if (i<txtbin.length()-1) lens2=(int)(txtbin.charAt(i+1)-48);
		freqbin2[lens2*2+lens1]++;
	}
	for (int i=0;i<4;i++)
		System.out.println("freqbin2["+i+"]="+freqbin2[i]);

	//StringBuffer rle=new StringBuffer();
	int lenrle=0;
	int paso=8;
	for (int i=0;i<txtbin.length();i+=paso)
	{
		int bitrle=1;
		
		int bitini=1;//txtbin.charAt(i)-48;
		for (int j=i;j<i+paso;j++)
		{
			if (j<txtbin.length() && txtbin.charAt(j)-48!=bitini) bitrle=0;
		}
		lenrle+=paso;
		int paso2=16;
		if (bitrle==1) 
		{
			int cuantos=0;
			for (int j=i+paso;j<i+paso+paso2;j++)
			{
				if (j<txtbin.length() && txtbin.charAt(j)-48!=bitini) {bitrle=0; break;}
				else cuantos++;
			}
			lenrle+=4;i+=cuantos;
		}
	}
	System.out.println("bin: " +txtbin.length()+"   RLE1:"+lenrle);
	
	paso=8;
	
	lenrle=0;
	for (int i=0;i<txt.length();i+=paso)
	{
		int symini=(int)(txt.charAt(i)-48);
		
		int bitrle=1;
		int lensegment=0;
		for (int j=i;j<i+paso;j++)
		{
			if (j<txt.length() && txt.charAt(j)-48!=symini) bitrle=0;
			if (j<txt.length() ) lensegment+=txt.charAt(j)-48;
		}
		if (bitrle==1) lenrle+=1+4;
		else lenrle+=lensegment+1;
	}
	System.out.println("bin: " +txtbin.length()+"   RLE:"+lenrle);
	
	paso=8;
	int lenfinal=0;
	for (int i=0;i<txt.length();i+=paso)
	{
		int lens1=(int)(txt.charAt(i)-48);
		lenfinal+=lens1;
		if (lens1==1)
		{
		
			int cuantos=0;
			for (int j=i+1;j<txt.length();j++)
			{
				int symlen=(int)(txt.charAt(j)-48);
				if (symlen!=1) break;
				cuantos++;
				if (cuantos==4) break;
			}
			lenfinal+=2;
			i+=cuantos;
			
			
		}
		
	}
	System.out.println("bin: " +txtbin.length()+"   RLE:"+lenfinal);
	System.out.println("............salimos de TXT2BIN.................");
}


public void resumeTXT2()
{
	int state=1;
	int lenfinal=0;
	
	int l=0;
	int ant=0;
	//System.out.println(txt.toString());
	int[] freq2=new int[81];
	for (int i=0;i<txt.length();i+=2)
	{
		int lens1=(int)(txt.charAt(i)-48);
	
		// l=l-1;
		//ant=lens1;
		//System.out.println(""+lens1);
		//System.out.println(""+i);
		
		int lens2=0;
		if (i<txt.length()-1) lens2=(int)(txt.charAt(i+1)-48);
		
		if (lens1==1 && lens2!=1) lenfinal+=lens1+lens2-1;
		else if (lens1==1 && lens2==1)lenfinal+=2;
		else if (lens1!=1)lenfinal+=1+lens1+lens2;
		//freq2[lens1*10+lens2]++;
	/*	
		if (state==1){ 
		//idea: se pueden borrar los "11"?
	    if (lens1!=1 )//101->11	
	    {lenfinal+=lens1+lens2;}//pej 0101->011... start with 0
	    
	    
	    
	    else  if (lens1==1 && lens2==1 ) 
	    		{lenfinal+=1;} //11->101
	    else {lenfinal+=lens1+lens2;}   
	    //else  if (lens1==1){lenfinal+=lens2;}// pej 101->01 start with 0 again...
		
		}
		
		if (state==2)
		{
			if (lens1==1 && lens2==1)	lenfinal+=1;
			else  {lenfinal+=lens1+lens2;state=1;}
			
			
		}
		*/
	}
	//huff.getTranslateCodes( freq2);
	compressed_len4bit=lenfinal;//27869;
	System.out.println(" la len final es "+lenfinal);
	//if (1<2) System.exit(0);
}



public void addSymbol2bit(int s1, int s2)
{
	
	s1=s1-48;
	s2=s2-48;
	
	//System.out.println(""+s1+""+s2);
	freq_sym2bit[s1+9*s2]++;
	if (s1==1 && s2==1) compressed_len2bit++;
	//else if (s1==0) compressed_len2bit+=s1+s2;
	else compressed_len2bit+=1+s1+s2;
}

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public int gridToBitsOLD(Grid grid)
{
	if (DEBUG) System.out.println("---------------comprimiendo la grid------------------");
	int[] freq_pr=new int[7];
	bits_len=new int[5];
	bits_len[0]=5;//000000001    
	bits_len[1]=1;//0000001
	bits_len[2]=2;//00001
	bits_len[3]=3;//001
	bits_len[4]=4;//001
	int[] discard=new int[5];
	
	
	
	//primero transformo en cuantos de 0 a 4
	//--------------------------------------
	int[][] qx=new int[grid.number_of_blocks_V+1][grid.number_of_blocks_H+1];
	int[][] qy=new int[grid.number_of_blocks_V+1][grid.number_of_blocks_H+1];
	for ( int y=0 ; y<grid.number_of_blocks_V+1;y++)
	{
		for ( int x=0 ; x<grid.number_of_blocks_H+1;x++)
		{
			PRblock b=grid.prbl[y][x];
			int c=0;
			if (b.PRx==0.125f) c=1;
			else if(b.PRx==0.25f) c=2;
			else if(b.PRx==0.5f) c=3;
			else if(b.PRx==1f) c=4;
			qx[y][x]=c;
			freq_pr[c]++;
			c=0;
			if (b.PRy==0.125f) c=1;
			else if(b.PRy==0.25f) c=2;
			else if(b.PRy==0.5f) c=3;
			else if(b.PRy==1f) c=4;
			qy[y][x]=c;
			freq_pr[c]++;
		}
	}
	/*
	//ahora asignamos codigos segun frecuencia 19-01-2015
	int bits=0;
	int bits_ini=1;
	for (int k=0; k<5;k++)
	{
		int max=0;
		int j=-1;	
	for (int i=0;i<5;i++)
	{
		if (freq_pr[i]>=max) {max=freq_pr[i];j=i;}
	}
	bits+=freq_pr[j]*bits_ini;
	freq_pr[j]=0;
	if (bits_ini<4)bits_ini++;
	}
	*/
	//---------------
	
	
	
	if (DEBUG) for (int i=0;i<5;i++) System.out.println("PR["+i+"]="+freq_pr[i]);
	freq_pr=new int[7];
	if (DEBUG) System.out.println("");
	//--------
	for ( int y=0 ; y<grid.number_of_blocks_V+1;y++)
	{
		
		//primero hago x y luego y
		for ( int x=0 ; x<grid.number_of_blocks_H+1;x++)
		{
			discard=new int[5];//limpiamos
			
			int upx=-1;
			int upy=-1;
			int leftx=-1;
			int lefty=-1;
			if (x>0) {leftx=qx[y][x-1];lefty=qy[y][x-1];}
			if (y>0) {upx=qy[y-1][x];upy=qy[y-1][x];}
			int c=qx[y][x];
			
			int len=1;
			int prediccion=1;
			if (leftx!=-1) prediccion=leftx;
			else if (upx!=-1) prediccion=upx;
			
			if (c==prediccion) {freq_pr[len]++; continue;} 
			else {discard[prediccion]=1;len++;}//penaliza y descarta
			
			if (upx!=-1 && discard[upx]!=1) prediccion=upx;
			if (c==prediccion) {freq_pr[len]++; continue;} 
			else {discard[prediccion]=1;len++;}//penaliza y descarta
			
			//System.out.println(" aqui no llegamos?");
			int len_pr=bits_len[c];
			int discount=0;
			for (int i=0;i<c;i++) if (bits_len[i]<len_pr && discard[i]==1) discount++;
			len_pr=len_pr-discount;
			freq_pr[len_pr]++;
		}
	     //ahora y
		//System.out.println(" haciendo PRy["+y+"]...");
		for ( int x=0 ; x<grid.number_of_blocks_H+1;x++)
		{
			discard=new int[5];//limpiamos
			
			int upx=-1;
			int upy=-1;
			int leftx=-1;
			int lefty=-1;
			if (x>0) {leftx=qx[y][x-1];lefty=qy[y][x-1];}
			if (y>0) {upx=qy[y-1][x];upy=qy[y-1][x];}
			int c=qy[y][x];
			
			int len=1;
			int prediccion=1;
			if (lefty!=-1) prediccion=lefty;
			else if (upy!=-1) prediccion=upy;
			
			if (upy!=-1) prediccion=upy;
			else if (upy!=-1) prediccion=lefty;
			
			if (c==prediccion) {freq_pr[len]++; continue;} 
			else {discard[prediccion]=1;len++;}//penaliza y descarta
			
			//if (upy!=-1 && discard[upy]!=1) prediccion=upy;
			if (lefty!=-1 && discard[lefty]!=1) prediccion=lefty;
			if (c==prediccion) {freq_pr[len]++; continue;} 
			else {discard[prediccion]=1;len++;}//penaliza y descarta
			
			int len_pr=bits_len[c];
			int discount=0;
			for (int i=0;i<c;i++) if (bits_len[i]<len_pr && discard[i]==1) discount++;
			len_pr=len_pr-discount;
			freq_pr[len_pr]++;
		}		
			
		
		//System.out.println("suma x="+sumax+ "  %="+(float)sumax/(float)(grid.number_of_blocks_H+1));
		
	}
	
	if (DEBUG) for (int i=0;i<5;i++) System.out.println("freq_pr["+i+"]="+freq_pr[i]);
	
	//PASO DE HUFFMAN
    //Huffman huff=new Huffman(5);
	//huff.getTranslateCodes(freq_pr);
	
	
	//como solo son 5 vamos a asignar de forma voraz
		int bits=0;
		int bits_ini=1;
		for (int k=0; k<5;k++)
		{
			int max=0;
			int j=-1;	
		for (int i=0;i<5;i++)
		{
			if (freq_pr[i]>=max) {max=freq_pr[i];j=i;}
		}
		bits+=freq_pr[j]*bits_ini;
		freq_pr[j]=0;
		if (bits_ini<4)bits_ini++;
		}
		System.out.println("bits grid:"+bits);
		return bits;
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	/*************************************************************
	 * hops to bits
	 * inputs: array of hops and limits
	 * 
	 * 0 1 2 3 4 5 6 7 8
	 *      <--|--->
	 *      
	 * hops are the raw symbols
	 * hops are  transformed into symbols based on spatial redundancy     
	 * symbols are from 0 to 8, but percentages are different from hops.
	 *      
	 * output: 
	 *   bits_counter : returned number of bits taken by this block. 
	 *   txt : (global stringbuffer of  symbols)
	 *   stathops[]: statistics of hops 
	 *   
	 **************************************************************/

	public int hopsToBits_v2(int[] hops, int xini, int yini, int xfin, int yfin, float PRx,float PRy)
	{
		
		StringBuffer symblock=new StringBuffer();//symbols of this block
		int bits_counter=0;//bits taken by the symbols of this block
		int pos=yini*width+xini;//initial position of the block
		int[] discard=new int[9];//hops discarded during symbol asignment		
		int not_null_symbols=0;//number of not null symbols. useful for blocks filled of 1s
		
		int last_redundancy=0;
		//int last_symbol=-1;
		// 0 is none, 1 is H (null hop) 2 is V (up)
		int lasthop=4;
		//bucle for scan hops
		//----------------------
		for (int y=yini; y<=yfin;y++)
		{
			for (int x=xini; x<=xfin; x++)
			{
				totalhops++;
				
				int symbol_len=0;//length of this symbol. bits_counter is the counter for the set of symbols of the block
				
				//boolean found=false;
				
				for (int i=0;i<=8;i++) discard[i]=0;//hops not discarded
				pos=y*width+x;//hop position
				int hop=hops[pos];
				stathops[hop]++;//statistics of hops
				
				int up=-1;//hop located at up position
				if (y>yini) up=hops[pos-width];
				
				int left=-1;//hop located at left position
				if (x>xini) left=hops[pos-1]; 
				
				int upi=-1;//hop located at left position
				//if (x>xini && y>yini) upi=hops[pos-1-width]; UPI NO MEJORA NADA. EMPEORA
				
				
				
				
				
				
				if (lasthop>=4)
				
				//if (up!=-1 && up>4 && left!=-1 && left>4)	
				{
					bits_len[0]=8;//000000000    
					bits_len[1]=7;//0000001
					bits_len[2]=5;//00001
					bits_len[3]=3;//001
					bits_len[4]=1;//1
					bits_len[5]=2;//01
					bits_len[6]=4;//0001
					bits_len[7]=6;//000001
					bits_len[8]=8;//00000001
				}
				else //if (lasthop<4)
					//if (up!=-1 && up<4 && left!=-1 && left<4)
				{
					bits_len[0]=8;//000000000    
					bits_len[1]=6;//0000001
					bits_len[2]=4;//00001
					bits_len[3]=2;//001
					bits_len[4]=1;//1
					bits_len[5]=3;//01
					bits_len[6]=5;//0001
					bits_len[7]=7;//000001
					bits_len[8]=8;//00000001	
				}
				
				
				
				/*
				if (PRx==0 && x>xini) //tras el primer hop no hay cambios de signo al scanear los hops de cada fila
				{
					bits_len[0]=4;//000000000    
					bits_len[1]=4;//0000001
					bits_len[2]=3;//00001
					bits_len[3]=2;//001
					bits_len[4]=1;//1
					bits_len[5]=2;//01
					bits_len[6]=3;//0001
					bits_len[7]=4;//000001
					bits_len[8]=4;//00000001	
				}
				*/
				
				
				lasthop=hop;
			
				
				//hago que se mire segun la ultima redundancia. else se mira sgun pr
				if (last_redundancy==1)
				{
				int predicted_hop=4;
				if (hop==predicted_hop) {last_redundancy=1;bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
				else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalty
				//last_symbol=0;
				}
				else if (last_redundancy==2 && up!=-1)
				{
					int predicted_hop=up;
					if (hop==predicted_hop) {last_redundancy=2;bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
					else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalty
					//last_symbol=0;
				}
				/*
				else if (last_redundancy==3 && left!=-1)
				{
					int predicted_hop=left;
					if (hop==predicted_hop) {last_redundancy=3;bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
					else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalty
					//last_symbol=0;
				}
				*/
				
				
				
				
				if (PRx<=PRy) //optimization: first H, then V redundancies
				//if (1<2) //optimization: first H, then V redundancies
					{	
					    //horizontal redundancy
						int predicted_hop=4;
						if (hop==predicted_hop) {last_redundancy=1;bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalty
						//last_symbol=0;
						not_null_symbols++;//symbol is not "1"
						
						
						
						
						//vertical redundancy
						if (up!=-1) predicted_hop=up;
						if (hop==predicted_hop) {last_redundancy=2;bits_counter++;statup++;symbol_len++;symblock.append(symbol_len);continue ;}
						else if ( discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalty

						//left
						/*
						if (left!=-1) predicted_hop=left;
						if (hop==predicted_hop) {last_redundancy=3;bits_counter++;statup++;symbol_len++;symblock.append(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalty
                        */
						
						
						//softing: hops decreasing
						
						if (left!=-1 && left >5) predicted_hop=left-1;
						else if (left!=-1 && left <3) predicted_hop=left+1;
						if (hop==predicted_hop) {bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalty
						
						if (up!=-1 && up >5) predicted_hop=up-1;
						else if (up!=-1 && up <3) predicted_hop=up+1;
						if (hop==predicted_hop) {bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalizacion
						
					}
					else //PRx >PRy
					{
						
						int predicted_hop=4;
						//vertical redundancy
						
						if (up!=-1) predicted_hop=up;
						if (hop==predicted_hop) {last_redundancy=2;bits_counter++;statup++;symbol_len++;symblock.append(symbol_len);continue;}
						else if ( discard[predicted_hop]!=1 ) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalty
						
						not_null_symbols++;
						
						//horizontal redundancy
						predicted_hop=4;
						if (hop==predicted_hop) {last_redundancy=1;bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalizacion
						
						
						//left
						/*
						 if (left!=-1) predicted_hop=left;
						if (hop==predicted_hop) {last_redundancy=3;bits_counter++;statup++;symbol_len++;symblock.append(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalty
                          */          
						
						 
						//softing: hops decreasing
						
						if (up!=-1 && up >5) predicted_hop=up-1;
						else if (up!=-1 && up <3) predicted_hop=up+1;
						if (hop==predicted_hop) {bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalizacion
						
						if (left!=-1 && left >5) predicted_hop=left-1;
						else if (left!=-1 && left <3) predicted_hop=left+1;
						if (hop==predicted_hop) {bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalizacion
						
					}
				
				//redundancy has failed. Asignment of symbol code taking into account discarded hops
				//----------------------------------------------------------------------------------
				last_redundancy=0;
				
				int len_hop=bits_len[hop];
				
				int discount=0;//discounts per discarded hops
				for (int j=0;j<=8;j++)
				{
					int lenj=bits_len[j];
					if (lenj<len_hop && discard[j]==1) discount++;// es <= por si no uso huffman
				}
					
					symbol_len+=len_hop-discount;//penalties included
					symblock.append(symbol_len);//
					
					bits_counter+=len_hop-discount;//penalties included
			}//x
			//pos+=width;
		}//y
		
		//System.out.println(bits_counter);
		//three cases for return
		//-------------------------
		//if (2>1)
		
		{
			//symblock=processSymblock5(symblock,PRx,PRy);
			//bits_counter=getlen(symblock);
			txt.append (symblock); 
			
			return bits_counter;//this block takes bits_counter bits


		}
		
		/*
		if (PRx==0.0f && PRy==0.0f && not_null_symbols==0) 
		{
			//System.out.println("-------------------------------------------------null block!!!!!!!");
			txt.append (0);	
			return 1;//this block only takes 1 bit 
		}
		else if (PRx==0.0f && PRy==0.0f) 
		{
		  txt.append (1);
		  txt.append (symblock); 
		  return 1+bits_counter;
		}
		
		txt.append (symblock); 
		return bits_counter;//this block takes bits_counter bits
		*/
		
	}
	
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	/*
	 * this function examines txt buffer and print occurrence of symbols and more informations
	 * 
	 * 
	 */
public void statSymbols()
{
	System.out.println("");
	System.out.println(" Enter in statSymbols....");
	System.out.println(" --------------------------");
	
	
	System.out.println(" statistics of hops:");
	
	for (int i=0;i<9;i++)
	{
		System.out.println("   hops["+i+"]="+stathops[i]+"   %="+((float)stathops[i]*100/totalhops));
	}
	System.out.println("   UP occurrences:"+statup);
	
	System.out.println("----------------------------------");
	System.out.println(" statistics of symbols:");
	
	compressed_len=0;
	freq_sym=new int[10];//clean
	System.out.println("");
	for (int i=0;i<txt.length();i++)
	{
		int sym=(int)(txt.charAt(i)-48);
		//System.out.println(sym);
		//if (sym==0) System.out.println ("kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk");
		int len=sym;
		if (len==0){
			System.out.println("ERROR en statSymbols");
			System.exit(0);
		}
		//if (len==0) len=1;//symbol '0' takes 1 bit. it is used only in null blocks
		compressed_len+=len;
		freq_sym[sym]++;
		
	}
	System.out.println("");
	System.out.println("   freq_sym[0]="+freq_sym[0]+"    %="+((float)freq_sym[0]*100/totalhops)+"    <-- null blocks");
	for (int i=1;i<10;i++)
	{
		System.out.println("   freq_sym["+i+"]="+freq_sym[i]+"    %="+((float)freq_sym[i]*100/totalhops));
		
	}
	System.out.println("----------------------------------");
	System.out.println(" number of symbols or hops:"+txt.length());
	System.out.println(" compressed len:"+compressed_len+"  bits.");
	int image_size=width*height;
	float bpp=(float)compressed_len/(float)image_size;
	float bph=(float)compressed_len/(float)txt.length();
	System.out.println(" bpp:"+bpp+"      bph:"+bph);
	
	System.out.println("---------compression huffman------------");
    Huffman huff=new Huffman(10);
    huff.getTranslateCodes(freq_sym);
	
	
	/*
	System.out.println("----------- LZW desde txtbin-----------------------");
	
	StringBuffer txtbin=txt2bin(txt);
	for (int i=0;i<txtbin.length();i++)
	{
		int sym=(int)(txtbin.charAt(i)-48);
		//System.out.println(sym);
		
	}
	LZW lzw=new LZW();
	List<Integer> result = lzw.compress(txtbin.toString());
	System.out.println(result.size());
	String de=lzw.decompress(result);
	System.out.println(de.length());
    System.out.println(result.toString());
	
	System.out.println("----------- -----------------------");
	//processTXT();
	 
	 */
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void processTXT()
{
	StringBuffer txt2=new StringBuffer();
	int lastsym=0;
	int totlen=0;
	for (int i=0;i<txt.length();i++)
	{
		
		int sym=(int)(txt.charAt(i)-48);
		if (sym==1 && lastsym==1) {txt2.append(1);lastsym=0;totlen++;}//11->1 y reset de lastsym
		else if (sym==1 && lastsym!=1) lastsym=1;//1X el X ya esta añadido. aguanto el 1 a la siguiente iteracion
		else if (sym!=1 && lastsym==1) {lastsym=0;txt2.append(2);txt2.append(sym);totlen+=2+sym;} //X1 metemos 0X y 01
		else //XX
		{
			lastsym=0;txt2.append(sym+1);totlen+=sym+1;
		}
	}
	System.out.println("nueva len:"+txt2.length()+" symbols   bin:"+totlen);
	System.out.println("");
	int image_size=width*height;
	float bpp=(float)totlen/(float)image_size;
	float bph=(float)totlen/(float)txt.length();
	System.out.println(" bpp:"+bpp+"      bph:"+bph);
}

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public StringBuffer processSymblock(StringBuffer sb)
{
	StringBuffer sb2=new StringBuffer();
	int head=0;
	int cola=0;
	for (int i=0;i<sb.length();i++)
	{
		
		int sym=(int)(sb.charAt(i)-48);
	if (sym!=1) break;
	head++;
	}
	for (int i=sb.length()-1;i>=0;i--)
	{
		
		int sym=(int)(sb.charAt(i)-48);
	if (sym!=1) break;
	cola++;
	}
	if (head<10 && cola<10) 
		{
		//System.out.println("nada");
		return sb;
		
		}
	
	else if (head>cola)
	{
		sb2.append(10);
		sb2.append(sb.substring(head));
		//System.out.println(" ahorrados "+head);
	}
	else //cola>head
	{
		//System.out.println(" ahorrados "+cola);	
		sb2.append(sb.substring(0,sb.length()-cola));
		sb2.append(10);
	}
	
	return sb2;
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public StringBuffer processSymblock2(StringBuffer sb)
{
	StringBuffer sb2=new StringBuffer();
	//determino el max
	int max=0;
	int min=100;
	int symant=-1;
	for (int i=0;i<sb.length();i++) {
		int sym=(int)(sb.charAt(i)-48);
		if (sym>max) max=sym;
		if (sym<min) min=sym;
	}
	//System.out.println("max:"+max+" min:"+min);
	//boolean binario=false;
	//for (int i=0;i<max;i++)	sb2.append(1);
	//resto a todos uno
	//sb2.append(min);
	
	int tail=0;
	//medimos la cola
	for (int i=sb.length()-1;i>=0;i--) {
		int sym=(int)(sb.charAt(i)-48);
		if (sym==1) tail++;
		else break;
	}
	if (tail>10)
		{sb2.append(sb.substring(0,sb.length()-tail));
		sb2.append(10);
		}
	else return sb;
	return sb2;
	
	
}
public int getlen(StringBuffer sb)
{
	int len=0;
	for (int i=0;i<sb.length();i++) {
		int sym=(int)(sb.charAt(i)-48);
		len+=sym;
		
	}
	return len;
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public StringBuffer processSymblock3(StringBuffer sb)
{
	StringBuffer sb2=new StringBuffer();
	
	
	
	int[] pos=new int[10];// 9 simbolos. posiciones
	int[] sim=new int[10];// 9 simbolos. posiciones
	
	// pos[sym]=position
	// sim[position]=sym
	
	for (int i=1;i<10;i++)pos[i]=i;//cada simbolo ocupa su posicion
	for (int i=1;i<10;i++)sim[i]=i;//el simbolo que ocupa la pos i es i
	
	for (int i=0;i<sb.length();i++) {
		int sym=(int)(sb.charAt(i)-48);
		
		//System.out.println(sym);
		//avanza el codigo del sym
		sb2.append(pos[sym]);
		//if (symant==sym)
		
		/*
		int victim=1;
		
		if (pos[sym]>0)//si es 1 ya no se puede mejorar
		{
			//int aux=rank[pos[sym]-1];rank[pos[sym]-1]=sym;rank[pos[sym]]=aux;pos[sym]--;pos[aux]++;
			int aux=sim[pos[1]];//aux asi siempre es 1
			//System.out.println("aux:"+aux);
			sim[pos[1]]=sym;//lo colocamos donde este el 1
			//int aux=rank[2];rank[2]=sym;
			sim[pos[sym]]=aux;pos[aux]=pos[sym];pos[sym]=1;//
			//rank[pos[sym]]=1;pos[1]=pos[sym];pos[sym]=1;//
		
		//for (int k=0;k<=9;k++) System.out.println(pos[k]);
		//System.out.println("-----");
		} 
		*/
		int symant=1;
		int u=1;
		if (pos[sym]>1 )//si es 1 ya no se puede mejorar
		{
			//int aux=rank[pos[sym]-1];rank[pos[sym]-1]=sym;rank[pos[sym]]=aux;pos[sym]--;pos[aux]++;
			int posS=pos[sym];
			int sym1er=sim[2];
			
			//int pos1=pos[1];
			
			//pos[1]=posS;
			pos[sym]=2;//pos1;
			pos[sym1er]=posS;
			
			sim[2]=sym;
			sim[posS]=sym1er;//sym1er;
		} 
		
		symant=sym;
		//if (pos[sym]<9)//si es 1 ya no se puede empeorar
		//{int aux=rank[pos[sym]+1];rank[pos[sym]+1]=sym;rank[pos[sym]]=aux;pos[sym]++;pos[aux]--;}
		
		
	}
	
	return sb;
	
	
}
public StringBuffer processSymblock4(StringBuffer sb)
{
	StringBuffer sb2=new StringBuffer();
	
	
	
	
	
	int symant=1;
	int counter=0;
	int u=5;
	for (int i=0;i<sb.length();i++) {
		int sym=(int)(sb.charAt(i)-48);
	
		if (sym!=1 && counter>=u) sb2.append(2);//break
		if (sym!=1) counter=0;
		
		if (counter>=u)
		{
			if (i%2==0)sb2.append(1);
			continue;
		}
		
		 sb2.append(sym);//0
		symant=sym;
		
	}
	
	return sb2;
	
	
}

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public StringBuffer processSymblock5(StringBuffer sb,float PRx,float PRy)
{
	StringBuffer sb2=new StringBuffer();
	for (int i=0;i<sb.length();i++) {
		int sym=(int)(sb.charAt(i)-48);
		
	 if (!(PRx>=0.5 && PRy>=0.5 &&PRx<=1 && PRy<=1)) 
		{
			    bits_len[0]=1;//000000000    
				bits_len[1]=2;//0000001
				bits_len[2]=4;//00001
				bits_len[3]=4;//001
				bits_len[4]=4;//1
				bits_len[5]=6;//01
				bits_len[6]=6;//0001
				bits_len[7]=6;//000001
				bits_len[8]=6;//00000001
				int len_hop=bits_len[sym-1];
				sb2.append(len_hop);//
		}
		
		 else
		 {
			
				int len_hop=sym;
				sb2.append(len_hop);//
				
		 }
	 
    }//for
	 return sb;
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	/*************************************************************
	 * hops to bits
	 * inputs: array of hops and limits
	 * 
	 * 0 1 2 3 4 5 6 7 8
	 *      <--|--->
	 *      
	 * hops are the raw symbols
	 * hops are  transformed into symbols based on spatial redundancy     
	 * symbols are from 0 to 8, but percentages are different from hops.
	 *      
	 * output: 
	 *   bits_counter : returned number of bits taken by this block: this value is not used because
	 *                  later on, the symbols are transformed into bits using Huffman
	 *   txt : (global stringbuffer of  symbols)
	 *   stathops[]: statistics of hops. this value is important. it is used for Huffman table computation and 
	 *               final binary lenght 
	 *   
	 **************************************************************/

	public int hopsToBits_v3(int[] hops, int xini, int yini, int xfin, int yfin, float PRx,float PRy)
	{
		
		StringBuffer symblock=new StringBuffer();//symbols of this block
		int bits_counter=0;//bits taken by the symbols of this block
		int pos=yini*width+xini;//initial position of the block
		int[] discard=new int[9];//hops discarded during symbol asignment		
		int not_null_symbols=0;//number of not null symbols. useful for blocks filled of 1s
		
		int last_redundancy=0;
		//int last_symbol=-1;
		// 0 is none, 1 is H (null hop) 2 is V (up)
		int lasthop=4;
		
		int[] blockstats=new int[10];
		//bucle for scan hops
		//----------------------
		for (int y=yini; y<=yfin;y++)
		{
			for (int x=xini; x<=xfin; x++)
			{
				totalhops++;
				
				int symbol_len=0;//length of this symbol. bits_counter is the counter for the set of symbols of the block
				
				//boolean found=false;
				
				for (int i=0;i<=8;i++) discard[i]=0;//hops not discarded
				pos=y*width+x;//hop position
				//System.out.println("x:"+x+"  y:"+y);
				int hop=hops[pos];
			    
				
				
			    
			    
				//System.out.println(hop+","+pos);
				stathops[hop]++;//statistics of hops
				
				int up=-1;//hop located at up position
				if (y>yini) up=hops[pos-width];
				
				int left=-1;//hop located at left position
				if (x>xini) left=hops[pos-1]; 
				
				blockstats[hop]++;
				
				if (lasthop>=4)
				
				{
					bits_len[0]=9;//000000000    
					bits_len[1]=7;//0000001
					bits_len[2]=5;//00001
					bits_len[3]=3;//001
					bits_len[4]=1;//1
					bits_len[5]=2;//01
					bits_len[6]=4;//0001
					bits_len[7]=6;//000001
					bits_len[8]=8;//00000001
				}
				else 
				{
					bits_len[0]=8;//000000000    
					bits_len[1]=6;//0000001
					bits_len[2]=4;//00001
					bits_len[3]=2;//001
					bits_len[4]=1;//1
					bits_len[5]=3;//01
					bits_len[6]=5;//0001
					bits_len[7]=7;//000001
					bits_len[8]=9;//00000001	
				}
				
				/*
				if (bits_len[lasthop]>3)
				{
					int bits=bits_len[lasthop];
					int v=-1;
					for (int t=0;t<=9;t++) if (bits_len[t]==bits-1) {v=t;break;}
					
					
					bits_len[lasthop]--;
					bits_len[v]++;
				}
				
				lasthop=hop;
				*/
				
				//18/01/2015
				/*{
				int predicted_hop=4;
				if (hop==predicted_hop) {last_redundancy=1;bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
				else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalty
				}
				*/
				// last_redundancy=1;
				
				if (last_redundancy==1)
				{
				int predicted_hop=4;
				if (hop==predicted_hop) {last_redundancy=1;bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
				else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalty
				//last_symbol=0;
				}
				else if (last_redundancy==2 && up!=-1)
				{
					int predicted_hop=up;
					if (hop==predicted_hop) {last_redundancy=2;bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
					else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalty
					//last_symbol=0;
				}
				
				
				
				if (PRx<=PRy) //optimization: first H, then V redundancies
				
					{	
					    //horizontal redundancy
						int predicted_hop=4;
						if (hop==predicted_hop) {last_redundancy=1;bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalty
						//last_symbol=0;
						not_null_symbols++;//symbol is not "1"
						
						//vertical redundancy
						if (up!=-1) predicted_hop=up;
						if (hop==predicted_hop) {last_redundancy=2;bits_counter++;statup++;symbol_len++;symblock.append(symbol_len);continue ;}
						else if ( discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalty

						
						//softing: hops decreasing. This improvement is very light. can be removed
						
						if (left!=-1 && left >5) predicted_hop=left-1;
						else if (left!=-1 && left <3) predicted_hop=left+1;
						if (hop==predicted_hop) {bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalty
						
						if (up!=-1 && up >5) predicted_hop=up-1;
						else if (up!=-1 && up <3) predicted_hop=up+1;
						if (hop==predicted_hop) {bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalizacion
						
						
					}
					else //PRx >PRy
					{
						
						int predicted_hop=4;
						//vertical redundancy
						
						if (up!=-1) predicted_hop=up;
						if (hop==predicted_hop) {last_redundancy=2;bits_counter++;statup++;symbol_len++;symblock.append(symbol_len);continue;}
						else if ( discard[predicted_hop]!=1 ) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalty
						
						not_null_symbols++;
						
						//horizontal redundancy
						predicted_hop=4;
						if (hop==predicted_hop) {last_redundancy=1;bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalizacion
						
						//softing: hops decreasing. This improvement is very light. can be removed
						
						if (up!=-1 && up >5) predicted_hop=up-1;
						else if (up!=-1 && up <3) predicted_hop=up+1;
						if (hop==predicted_hop) {bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalizacion
						
						if (left!=-1 && left >5) predicted_hop=left-1;
						else if (left!=-1 && left <3) predicted_hop=left+1;
						if (hop==predicted_hop) {bits_counter++;symbol_len++;symblock.append(symbol_len);continue ;}
						else if (discard[predicted_hop]!=1) {bits_counter++;symbol_len++;discard[predicted_hop]=1;}//penalizacion
						
					}
				
				//redundancy has failed. Asignment of symbol code taking into account discarded hops
				//----------------------------------------------------------------------------------
				last_redundancy=0;
				
				int len_hop=bits_len[hop];
				
				int discount=0;//discounts per discarded hops
				for (int j=0;j<=8;j++)
				{
					int lenj=bits_len[j];
					if (lenj<len_hop && discard[j]==1) discount++;
				}
					
					symbol_len+=len_hop-discount;//penalties included
					symblock.append(symbol_len);// ESTO ESTA BIEN Y ES LO QUE SE USA PARA EL BITRATE
					
					/*if (len_hop==9) {
						System.out.println(" lenhop9"+ "  discount:"+discount+ " symbol"+symbol_len);
						System.exit(0);
					}
					*/
					//esto esta mal? quizas sea bits counter+=symbol_len
					
					//DA IGUAL ESTO ESTA MAL PERO NO SE USA PARA CALCULAR EL BIT RATE
					bits_counter+=len_hop-discount;//penalties included
					//bits_counter+=symbol_len;//penalties included
			}//x
			//pos+=width;
		}//y
		
		//System.out.println(bits_counter);
		//three cases for return
		//-------------------------
		   
		
		float dato=(float)blockstats[4]/(float)((yfin-yini)*(xfin-xini));
		
		//con esta funcion cambio la tabla de codificacion de simbolos
		//int lenh=compressHuffmanSymblock(symblock);
			
		
		//symblock=processSymblock6(symblock,PRx,PRy, dato);
		        
		    symblock=processSymblock8(symblock,PRx,PRy, dato);
		    
		   
		    
			bits_counter=getlen(symblock);
			txt.append (symblock); 
			
			
			//bits_counter=lenh;
			
			return bits_counter;//this block takes bits_counter bits


		
		
		
	}
	
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public StringBuffer processSymblock6(StringBuffer sb,float PRx,float PRy,float data)
	{
		StringBuffer sb2=new StringBuffer();
		boolean especial=false;
		
		float pravg=(PRx+PRy)/2f;
		
		int[] statblock=new int[10];
		for (int i=0;i<sb.length();i++) 
		{
			int sym=(int)(sb.charAt(i)-48);
			int len_hop=sym;
			statblock[len_hop]++;
		}
		float dato=(float)statblock[1]/(float)sb.length();
		
		//compressHuffmanSymblock(sb);
		
		 //if (PRx>0.25f && PRy>0.25f ) 
		// if (PRx>0.5f && PRy>0.5f )
		//if (PRx+PRy>0.75)
			// if (dato<0.35)//si se calcula con simbolos
		// if (dato<0.35)//si se calcula con simbolos
		if (pravg>=0.5)// 
		//if (PRx>0.3f && PRy>0.3f ) 
		 //if (data<0.33)
			{
			        //if (DEBUG) System.out.println("PRx:"+PRx+"  PRy:"+PRy+"  dato:"+dato+ "      "+pravg+"  ="+(PRx+PRy)/2f);
				    bits_len[0]=2;//000000000    
					bits_len[1]=2;//0000001
					bits_len[2]=3;//00001
					bits_len[3]=3;//001
					bits_len[4]=4;//1
					bits_len[5]=4;//01
					bits_len[6]=4;//0001
					bits_len[7]=5;//000001
					bits_len[8]=5;//00000001
					
					especial=true;
			}
		else return sb;
		
		 //special blocks with non vozaz huffman coding 
		for (int i=0;i<sb.length();i++) 
		{
			//int sym=(int)(sb.charAt(i)-48);//sym es >=1 
			int sym=(int)(sb.charAt(i)-49);//sym es >=0
			//System.out.println("sym:"+sym);
			int len_hop=sym;
			//el if sobra. aqui llega si es especial
			//if (especial) len_hop=bits_len[sym-1];
			if (especial) len_hop=bits_len[sym];
			 sb2.append(len_hop);//
		}//for
		 return sb2;
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	/**
	 * este metodo comprime por huffman los simbolos de un bloque a modo de experimento
	 * @param sb
	 * @return
	 */
	public  int compressHuffmanSymblock(StringBuffer sb)
	{
		System.out.println("entrada en compressHuffmanSymblock()");
		//System.out.println("hello hufman");
		StringBuffer sb2=new StringBuffer();
		int[] statblock=new int[10];
		for (int i=0;i<sb.length();i++) 
		{
			int sym=(int)(sb.charAt(i)-48);
			int len_hop=sym;
			statblock[len_hop]++;
		}
		Huffman huff=new Huffman(9);
	
		int len=huff.getLenTranslateCodes(statblock);
		System.out.println("salida de compressHuffmanSymblock()");
		
		return len;//sb2;
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public StringBuffer processSymblock7(StringBuffer sb,float PRx,float PRy)
	{
		StringBuffer sb2=new StringBuffer();
		boolean especial=false;
		int[] bits_len=new int[10];
		 if (PRx>0.5f && PRy>0.5f ) 
			{
				    bits_len[0]=1;//000000000    
					bits_len[1]=2;//0000001
					bits_len[2]=3;//00001
					bits_len[3]=4;//001
					bits_len[4]=5;//1
					bits_len[5]=6;//01
					bits_len[6]=7;//0001
					bits_len[7]=8;//000001
					bits_len[8]=9;//00000001
					bits_len[9]=9;//00000001
					especial=true;
			}
		 else
		 {
			    bits_len[0]=0;//000000000    
				bits_len[1]=1;//0000001
				bits_len[2]=2;//00001
				bits_len[3]=3;//001
				bits_len[4]=4;//1
				bits_len[5]=5;//01
				bits_len[6]=6;//0001
				bits_len[7]=7;//000001
				bits_len[8]=8;//00000001
				bits_len[9]=8;//00000001
				especial=false;
		 }
		 
		for (int i=0;i<sb.length();i+=2) 
		{
			
			int sym1=(int)(sb.charAt(i)-48);
			int sym2=0;
			if (i+1<sb.length()) sym2=(int)(sb.charAt(i+1)-48);
			
			if (sym1==1 && sym2==1 && especial)  sb2.append(bits_len[0]);
			else {sb2.append(bits_len[sym1]);sb2.append(bits_len[sym2]);}
			
					
			
		 
	    }//for
		 return sb2;
	}


//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public int gridToBits_old2(Grid grid)
{
	if (DEBUG) System.out.println("---------------comprimiendo la grid------------------");
	int[] freq_pr=new int[7];
	bits_len=new int[5];
	
	//la longitud de cada simbolo la vamos a asignar en funcion de la frecuencia de aparicion
	bits_len[0]=5;//000000001    
	bits_len[1]=1;//0000001
	bits_len[2]=2;//00001
	bits_len[3]=3;//001
	bits_len[4]=4;//001
	int[] discard=new int[5];
	
	
	
	//primero transformo en cuantos de 0 a 4
	//--------------------------------------
	int[][] qx=new int[grid.number_of_blocks_V+1][grid.number_of_blocks_H+1];
	int[][] qy=new int[grid.number_of_blocks_V+1][grid.number_of_blocks_H+1];
	for ( int y=0 ; y<grid.number_of_blocks_V+1;y++)
	{
		for ( int x=0 ; x<grid.number_of_blocks_H+1;x++)
		{
			PRblock b=grid.prbl[y][x];
			int c=0;
			if (b.PRx==0.125f) c=1;
			else if(b.PRx==0.25f) c=2;
			else if(b.PRx==0.5f) c=3;
			else if(b.PRx==1f) c=4;
			qx[y][x]=c;
			freq_pr[c]++;
			c=0;
			if (b.PRy==0.125f) c=1;
			else if(b.PRy==0.25f) c=2;
			else if(b.PRy==0.5f) c=3;
			else if(b.PRy==1f) c=4;
			qy[y][x]=c;
			freq_pr[c]++;
		}
	}
	//ya tenemos las frecuencias de cada simbolo
	//------------------------------------------
	if (DEBUG) for (int i=0;i<5;i++) System.out.println("PR["+i+"]="+freq_pr[i]);
	//ahora asociamos longitudes
	//int[] code=new int[5];
	int default_prediction=0;
	int bit_code=1;
	for (int j=0;j<5;j++)
	{
	int maxpr=0;
	int maxi=0;
	for (int i=0;i<5;i++)
	{
		if (freq_pr[i]>=maxpr) {maxpr=freq_pr[i];maxi=i;}
	}
	if (bit_code==1) default_prediction=maxi;//el mas frecuente
	bits_len[maxi]=bit_code;
	freq_pr[maxi]=0;
	if (bit_code<4) 
		bit_code++;
	}
	if (DEBUG) for (int i=0;i<5;i++) System.out.println("bits_len["+i+"]:"+bits_len[i]);
	if (DEBUG) System.out.println("default prediction:"+default_prediction);
	
	
	
	
	//if (DEBUG) 
	if (DEBUG) 	for (int i=0;i<5;i++) System.out.println("PR["+i+"]="+freq_pr[i]);
	freq_pr=new int[7];//se borran todas las frecuencias
	if (DEBUG) System.out.println("");
	//--------
	for ( int y=0 ; y<grid.number_of_blocks_V+1;y++)
	{
		
		//primero hago x y luego y
		for ( int x=0 ; x<grid.number_of_blocks_H+1;x++)
		{
			discard=new int[5];//limpiamos
			
			int upx=-1;
			int upy=-1;
			int leftx=-1;
			int lefty=-1;
			if (x>0) {leftx=qx[y][x-1];lefty=qy[y][x-1];}
			if (y>0) {upx=qy[y-1][x];upy=qy[y-1][x];}
			int c=qx[y][x];
			
			int len=1;
			//int prediccion=1;
			int prediccion=default_prediction;
			if (leftx!=-1) prediccion=leftx;
			else if (upx!=-1) prediccion=upx;
			
			if (c==prediccion) {freq_pr[len]++; continue;} 
			else {discard[prediccion]=1;len++;}//penaliza y descarta
			
			if (upx!=-1 && discard[upx]!=1) prediccion=upx;
			if (c==prediccion) {freq_pr[len]++; continue;} 
			else {discard[prediccion]=1;len++;}//penaliza y descarta
			
			//System.out.println(" aqui no llegamos?");
			int len_pr=bits_len[c];
			int discount=0;
			//for (int i=0;i<c;i++) if (bits_len[i]<len_pr && discard[i]==1) discount++;
			for (int i=0;i<5;i++) if (bits_len[i]<len_pr && discard[i]==1) discount++;
			len_pr=len_pr-discount;
			freq_pr[len_pr]++;
		}
	     //ahora y
		//System.out.println(" haciendo PRy["+y+"]...");
		for ( int x=0 ; x<grid.number_of_blocks_H+1;x++)
		{
			discard=new int[5];//limpiamos
			
			int upx=-1;
			int upy=-1;
			int leftx=-1;
			int lefty=-1;
			if (x>0) {leftx=qx[y][x-1];lefty=qy[y][x-1];}
			if (y>0) {upx=qy[y-1][x];upy=qy[y-1][x];}
			int c=qy[y][x];
			
			int len=1;
			int prediccion=1;
			if (lefty!=-1) prediccion=lefty;
			else if (upy!=-1) prediccion=upy;
			
			if (upy!=-1) prediccion=upy;
			else if (upy!=-1) prediccion=lefty;
			
			if (c==prediccion) {freq_pr[len]++; continue;} 
			else {discard[prediccion]=1;len++;}//penaliza y descarta
			
			//if (upy!=-1 && discard[upy]!=1) prediccion=upy;
			if (lefty!=-1 && discard[lefty]!=1) prediccion=lefty;
			if (c==prediccion) {freq_pr[len]++; continue;} 
			else {discard[prediccion]=1;len++;}//penaliza y descarta
			
			int len_pr=bits_len[c];
			int discount=0;
			//for (int i=0;i<c;i++) if (bits_len[i]<len_pr && discard[i]==1) discount++;
			for (int i=0;i<5;i++) if (bits_len[i]<len_pr && discard[i]==1) discount++;
			len_pr=len_pr-discount;
			freq_pr[len_pr]++;
		}		
			
		
		//System.out.println("suma x="+sumax+ "  %="+(float)sumax/(float)(grid.number_of_blocks_H+1));
		
	}
	
	
	//if (DEBUG) 
	if (DEBUG)		for (int i=0;i<5;i++) System.out.println("freq_pr["+i+"]="+freq_pr[i]);
	int bits=0;
	for (int i=0;i<5;i++) bits+=freq_pr[i]*bits_len[i];
	System.out.println("bits grid:"+bits);
	//System.out.println("bits grid:"+(bits+(33*33*8)));
	return bits;//+(33*33*8);
	//PASO DE HUFFMAN
  //Huffman huff=new Huffman(5);
	//huff.getTranslateCodes(freq_pr);
	
	/*
	//como solo son 5 vamos a asignar de forma voraz
		int bits=0;
		int bits_ini=1;
		for (int k=0; k<5;k++)
		{
			int max=0;
			int j=-1;	
		for (int i=0;i<5;i++)
		{
			if (freq_pr[i]>=max) {max=freq_pr[i];j=i;}
		}
		bits+=freq_pr[j]*bits_ini;
		freq_pr[j]=0;
		if (bits_ini<4)bits_ini++;
		}
		System.out.println("bits grid:"+bits);
		return bits;
		*/
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public StringBuffer processSymblock8(StringBuffer sb,float PRx,float PRy,float data)
{
	
	//if (1<2) return compressHuffmanSymblock(sb);
	
	StringBuffer sb2=new StringBuffer();
	boolean especial=false;
	
	float pravg=(PRx+PRy)/2f;
	
	int[] statblock=new int[10];
	
	
	int saco=0; //UN SOLO SACO 
	//if (pravg<0.5f) saco=0;
	//else if (pravg<0.5f) saco=1;
	//else if (pravg<0.75f) saco=2;
	//else  saco=3;
	
	int len_antes=down_stats[1];
	int lenbloque=0;
	for (int i=0;i<sb.length();i++) 
	{
		int sym=(int)(sb.charAt(i)-48);
		int len_hop=sym;
		if (sym==9) {
			//System.out.println("eeeeee");
			//System.exit(0);
		}
		lenbloque+=len_hop;
		statblock[len_hop]++;
		down_stats[len_hop]++;//ACTUALIZACION DE VARIABLE GLOBAL
		down_stats_saco[saco][len_hop]++;
	}
	if (lenbloque==4) down_stats_saco[saco][1]=down_stats_saco[saco][1]-4;
	//if (down_stats[1]==len_antes+4) down_stats[1]=down_stats[1]-4; 
	
	
	//todo lo que viene  a continuacion no se usa
	
	
	float dato=(float)statblock[1]/(float)sb.length();
	
	//compressHuffmanSymblock(sb);
	
	 //if (PRx>0.25f && PRy>0.25f ) 
	// if (PRx>0.5f && PRy>0.5f )
	//if (PRx+PRy>0.75)
		// if (dato<0.35)//si se calcula con simbolos
	// if (dato<0.35)//si se calcula con simbolos
	//if (pravg<0.25f)// 
	if (pravg>777770.25f)// nunca entra
		
			{
		  bits_len[0]=1;//    
			bits_len[1]=2;//
			bits_len[2]=2;//
			
			bits_len[4]=500000;//
			bits_len[5]=600000;//
			bits_len[6]=700000;//
			bits_len[7]=800000;//
			bits_len[8]=800000;//asi
			especial=true;
			}
	else if (pravg<10.7f)//  SIEMPRE ENTRO AQUI
	//if (PRx>0.3f && PRy>0.3f ) 
	 //if (data<0.33)
		{
		        //if (DEBUG) System.out.println("PRx:"+PRx+"  PRy:"+PRy+"  dato:"+dato+ "      "+pravg+"  ="+(PRx+PRy)/2f);
		
				// la variable bits_len esta mal llamada. Es el nombre del simbolo, y se corresponde
				// con la tabla de simbolos definida en la funcion hopsToBits_v3() la cual
				//aprovecha redundancias espaciales y ordena los simbolos de mas pequeño a mas grande
		
				//si no usase huffman , se corresponderia con los bits usados y se podria 
				//hacer bits_len[8]=8 pero como uso huffman pues esta parte del codigo
				//solo es para darle un nombre a los 9 simbolos , que normalmente
				//van a ser precisamente su orden en numero de apariciones aunque no siempre.
		
			    bits_len[0]=1;//    se corresponde con el hop 0 o con UP
				bits_len[1]=2;//    se corresponde con h+1 o con h-1 segun la tabla que depende del PR del bloque
				bits_len[2]=3;//
				bits_len[3]=4;//
				bits_len[4]=5;//
				bits_len[5]=6;//
				bits_len[6]=7;//
				bits_len[7]=8;//
				
				
				bits_len[8]=9;//asi ponemos 8 aqui, que si no son 9? no. ya viene bien
				
				especial=true;
		}
	/*
	else if (pravg<0.5f)
	{
		    bits_len[0]=1;//000000000    
			bits_len[1]=2;//0000001
			bits_len[2]=3;//00001
			bits_len[3]=5;//001
			bits_len[4]=5;//1
			bits_len[5]=6;//01
			bits_len[6]=6;//0001
			bits_len[7]=6;//000001
			bits_len[8]=6;//00000001
			especial=true;
	}
	*/
	else //if (pravg>=0.5 )// 
		//if (PRx>0.3f && PRy>0.3f ) 
		 //if (data<0.33)
			{
			        //if (DEBUG) System.out.println("PRx:"+PRx+"  PRy:"+PRy+"  dato:"+dato+ "      "+pravg+"  ="+(PRx+PRy)/2f);
		    bits_len[0]=2;//000000000    
			bits_len[1]=2;//0000001
			bits_len[2]=3;//00001
			bits_len[3]=3;//001
			bits_len[4]=4;//1
			bits_len[5]=4;//01
			bits_len[6]=4;//0001
			bits_len[7]=5;//000001
			bits_len[8]=5;//00000001
					
					especial=true;
			}
	/*
	else if (pravg>=0.5 )// 
		//if (PRx>0.3f && PRy>0.3f ) 
		 //if (data<0.33)
			{
		//if (DEBUG) System.out.println("PRx:"+PRx+"  PRy:"+PRy+"  dato:"+dato+ "      "+pravg+"  ="+(PRx+PRy)/2f);
		    bits_len[0]=1;//000000000    
			bits_len[1]=2;//0000001
			bits_len[2]=3;//00001
			bits_len[3]=5;//001
			bits_len[4]=5;//1
			bits_len[5]=6;//01
			bits_len[6]=6;//0001
			bits_len[7]=6;//000001
			bits_len[8]=6;//00000001
					
					especial=true;
			}
		*/
	//else return sb;
	
	 //special blocks with non vozaz huffman coding 
	for (int i=0;i<sb.length();i++) 
	{
		//int sym=(int)(sb.charAt(i)-48);//sym es >=1 
		int sym=(int)(sb.charAt(i)-49);//sym es >=0
		//System.out.println("sym:"+sym);
		int len_hop=sym;
		//el if sobra. aqui llega si es especial
		//if (especial) len_hop=bits_len[sym-1];
		if (especial) len_hop=bits_len[sym];
		 sb2.append(len_hop);//
	}//for
	 return sb2;
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public int gridToBits(Grid grid)
{
	if (DEBUG) System.out.println("---------------comprimiendo la grid------------------");
	int[] freq_pr=new int[7];
	bits_len=new int[5];
	
	//la longitud de cada simbolo la vamos a asignar en funcion de la frecuencia de aparicion
	bits_len[0]=5;//000000001    
	bits_len[1]=1;//0000001
	bits_len[2]=2;//00001
	bits_len[3]=3;//001
	bits_len[4]=4;//001
	int[] discard=new int[5];
	
	
	
	//primero transformo en cuantos de 0 a 4
	//--------------------------------------
	int[][] qx=new int[grid.number_of_blocks_V+1][grid.number_of_blocks_H+1];
	int[][] qy=new int[grid.number_of_blocks_V+1][grid.number_of_blocks_H+1];
	for ( int y=0 ; y<grid.number_of_blocks_V+1;y++)
	{
		for ( int x=0 ; x<grid.number_of_blocks_H+1;x++)
		{
			PRblock b=grid.prbl[y][x];
			int c=0;
			if (b.PRx==0.125f) c=1;
			else if(b.PRx==0.25f) c=2;
			else if(b.PRx==0.5f) c=3;
			else if(b.PRx==1f) c=4;
			qx[y][x]=c;
			freq_pr[c]++;
			c=0;
			if (b.PRy==0.125f) c=1;
			else if(b.PRy==0.25f) c=2;
			else if(b.PRy==0.5f) c=3;
			else if(b.PRy==1f) c=4;
			qy[y][x]=c;
			freq_pr[c]++;
		}
	}
	//ya tenemos las frecuencias de cada simbolo
	//------------------------------------------
	if (DEBUG) for (int i=0;i<5;i++) System.out.println("PR["+i+"]="+freq_pr[i]);
	//ahora asociamos simbolos
	//int[] code=new int[5];
	int default_prediction=0;
	int bit_code=1;
	for (int j=0;j<5;j++)
	{
	int maxpr=0;
	int maxi=0;
	for (int i=0;i<5;i++)
	{
		if (freq_pr[i]>=maxpr) {maxpr=freq_pr[i];maxi=i;}
	}
	if (bit_code==1) default_prediction=maxi;//el mas frecuente
	bits_len[maxi]=bit_code;
	freq_pr[maxi]=0;
	if (bit_code<4) 
		bit_code++;
	}
	if (DEBUG) for (int i=0;i<5;i++) System.out.println("simbolo["+i+"]:"+bits_len[i]);
	if (DEBUG) System.out.println("default prediction:"+default_prediction);
	
	
	
	
	//if (DEBUG) 
	if (DEBUG) 	for (int i=0;i<5;i++) System.out.println("PR["+i+"]="+freq_pr[i]);
	freq_pr=new int[7];//se borran todas las frecuencias
	if (DEBUG) System.out.println("");
	//--------
	for ( int y=0 ; y<grid.number_of_blocks_V+1;y++)
	{
		
		//primero hago x y luego y
		for ( int x=0 ; x<grid.number_of_blocks_H+1;x++)
		{
			discard=new int[5];//limpiamos
			
			int upx=-1;
			int upy=-1;
			int leftx=-1;
			int lefty=-1;
			if (x>0) {leftx=qx[y][x-1];lefty=qy[y][x-1];}
			if (y>0) {upx=qy[y-1][x];upy=qy[y-1][x];}
			int c=qx[y][x];
			
			int len=1;
			//int prediccion=1;
			int prediccion=default_prediction;
			if (leftx!=-1) prediccion=leftx;
			else if (upx!=-1) prediccion=upx;
			
			if (c==prediccion) {freq_pr[len]++; continue;} 
			else {discard[prediccion]=1;len++;}//penaliza y descarta
			
			if (upx!=-1 && discard[upx]!=1) prediccion=upx;
			if (c==prediccion) {freq_pr[len]++; continue;} 
			else {discard[prediccion]=1;len++;}//penaliza y descarta
			
			//System.out.println(" aqui no llegamos?");
			int len_pr=bits_len[c];
			int discount=0;
			//for (int i=0;i<c;i++) if (bits_len[i]<len_pr && discard[i]==1) discount++;
			for (int i=0;i<5;i++) if (bits_len[i]<len_pr && discard[i]==1) discount++;
			len_pr=len_pr-discount;
			freq_pr[len_pr]++;
		}
	     //ahora y
		//System.out.println(" haciendo PRy["+y+"]...");
		for ( int x=0 ; x<grid.number_of_blocks_H+1;x++)
		{
			discard=new int[5];//limpiamos
			
			int upx=-1;
			int upy=-1;
			int leftx=-1;
			int lefty=-1;
			if (x>0) {leftx=qx[y][x-1];lefty=qy[y][x-1];}
			if (y>0) {upx=qy[y-1][x];upy=qy[y-1][x];}
			int c=qy[y][x];
			
			int len=1;
			int prediccion=1;
			if (lefty!=-1) prediccion=lefty;
			else if (upy!=-1) prediccion=upy;
			
			if (upy!=-1) prediccion=upy;
			else if (upy!=-1) prediccion=lefty;
			
			if (c==prediccion) {freq_pr[len]++; continue;} 
			else {discard[prediccion]=1;len++;}//penaliza y descarta
			
			//if (upy!=-1 && discard[upy]!=1) prediccion=upy;
			if (lefty!=-1 && discard[lefty]!=1) prediccion=lefty;
			if (c==prediccion) {freq_pr[len]++; continue;} 
			else {discard[prediccion]=1;len++;}//penaliza y descarta
			
			int len_pr=bits_len[c];
			int discount=0;
			//for (int i=0;i<c;i++) if (bits_len[i]<len_pr && discard[i]==1) discount++;
			for (int i=0;i<5;i++) if (bits_len[i]<len_pr && discard[i]==1) discount++;
			len_pr=len_pr-discount;
			freq_pr[len_pr]++;
		}		
			
		
		//System.out.println("suma x="+sumax+ "  %="+(float)sumax/(float)(grid.number_of_blocks_H+1));
		
	}
	
	
	//if (DEBUG) 
	if (DEBUG)		for (int i=0;i<5;i++) System.out.println("freq_pr["+i+"]="+freq_pr[i]);
	int bits=0;
	
	//NUEVO 6/4/2015
	Huffman huff=new Huffman(5);
	int lengrid=huff.getLenTranslateCodes(freq_pr);
	System.out.println("bits grid:"+lengrid);
	if (1<2)
	return lengrid;
	
	
	for (int i=0;i<5;i++) bits+=freq_pr[i]*bits_len[i];
	System.out.println("bits grid:"+bits);
	//System.out.println("bits grid:"+(bits+(33*33*8)));
	return bits;//+(33*33*8);
	//PASO DE HUFFMAN
//Huffman huff=new Huffman(5);
	//huff.getTranslateCodes(freq_pr);
	
	/*
	//como solo son 5 vamos a asignar de forma voraz
		int bits=0;
		int bits_ini=1;
		for (int k=0; k<5;k++)
		{
			int max=0;
			int j=-1;	
		for (int i=0;i<5;i++)
		{
			if (freq_pr[i]>=max) {max=freq_pr[i];j=i;}
		}
		bits+=freq_pr[j]*bits_ini;
		freq_pr[j]=0;
		if (bits_ini<4)bits_ini++;
		}
		System.out.println("bits grid:"+bits);
		return bits;
		*/
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void saveSymbolsToTxt(String path_file)
{
	try{
		System.out.println("Entrando en saveSymbolsToTxt");
		DataOutputStream d = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path_file)));


		//primero escribo el ancho, alto y primer color:
		//d.writeBytes(width+"\n");
		//d.writeBytes(height+"\n");
		//d.writeBytes(YUV[0][0]+"\n");
		for (int i=0;i<width*height;i++){

			//if ((i%width==0)&& (i>0)) {d.writeBytes("\n");}



			//esto salva los hops normales[0..4..8]
			//d.writeBytes(hops[0][i]+"");
			d.writeBytes(txt.charAt(i)+"");

			
		}

		d.close();
	}catch(Exception e){System.out.println("ERROR writing hops in txt format:"+e);}	


}
//**************************************
}