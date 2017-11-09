package LHE;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;


/**
 * 
 * @author Jose Javier Garcia Aranda 
 * 2014
 */
public class Grid {

public boolean DEBUG=false;
	
	//compression factor: set at creation
	public float compression_factor;

	//magic number is the number of blocks in the larger side
	public float blocks_of_larger_image_side=32;

	//array of blocks
	public Block[][] bl;//block list
	public int number_of_blocks_H;
	public int number_of_blocks_V;

	//array of PRblocks
	public PRblock[][] prbl;//preceptual relevance block list

	//block size of the sides ( floating value). 
	//real blocks are integer (but not necessarily identical)  
	public float sizeh;
	public float sizev;

	//max, min number of blocks in the larger side of the image
	public final static int MAX_BLOCKS=32;////32;//32;//32;//32;//32;//1;//32;//1;//32;//32;//32;//32;//32;//64;//32;//32;//32;//32;//16;//
	//public final static int MIN_BLOCKS=32;
	public final static int MIN_LEN_BLOCK=4;//16;//16;//16;//16;//16; hace que haya menos de 32 bloques si width<512

	
	

	/**
	 * constructor
	 * grid is created based on image dimensions and magic number
	 * magic number is the number of blocks in the larger side 
	 * 
	 * @param image_width
	 * @param image_high
	 */
	public Grid()//float comp_factor)
	{
		/*
		//comp factor is a floating value from 0 to 10
		compression_factor=(float)Math.pow(2,comp_factor)/100f; 



		//computation of  number of blocks
		blocks_of_larger_image_side=MIN_BLOCKS+(int)((1f-comp_factor/10f)*(MAX_BLOCKS-MIN_BLOCKS));	

		System.out.println(" number of blocks at larger image side ="+blocks_of_larger_image_side);
		//magic_number=3;
		// 32 seria  el limite para pppmax=16 (32*16=512) osea 1 pix por bloque. eso no va, al menos hacen falta dos pix

	//	blocks_of_larger_image_side=10;//6;//6;//10;;
		 */


	}
	//*************************************************************************************************
	/**
	 * create an array of blocks. 
	 * blocks are not identical, unless image width and high are divisible by the block length 
	 * vertical size of blocks may be different from horizontal size
	 * the grid must cover the entire image
	 * the magic number is configurable (number of blocks in larger side) , but 32 is default value
	 */
	
	//******************************************************************************
	public int getNumberOfDownsampledPixels()
	{
		int down_counter=0;
		int counter=0;
		for ( int y=0 ; y<number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<number_of_blocks_H;x++)

			{
				Block b=bl[y][x];
				down_counter+=b.lx_sc *b.ly_sc ;
				counter+=b.lx*b.ly;
			}
		}
		float percent=100*(float)down_counter/(float)counter;
		if (DEBUG) System.out.println(" image is downsampled to "+down_counter+" pixels, "+percent+" %");
		return down_counter;
	}
	//*******************************************************************************
	/**
	 * ratio must belong to 1..256 corresponding to 1:1 to 1:256
	 * 
	 * @param ratio
	 */
	public void setPPPHomogeneousDownsamplingRatio(float ratio)
	{
		//ratio=10;
		System.out.println ("Homogeneous downsampling ratio 1:"+ratio);
		
		for ( int y=0 ; y<number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<number_of_blocks_H;x++)

			{
				Block b=bl[y][x];
				for (int coord=0;coord<2;coord++)
					for (int corner=0;corner<4; corner++)
					{
						//b.ppp[coord][corner]=4;
						b.ppp[coord][corner]=(float) Math.sqrt(ratio);

						if (b.ppp[coord][corner]>Block.MAX_PPP) b.ppp[coord][corner]=Block.MAX_PPP;
						else if (b.ppp[coord][corner]<1) b.ppp[coord][corner]=1;
						//System.out.println(" bl["+y+"]["+x+"].ppp["+coord+"]["+corner+"]="+ b.ppp[coord][corner]+"f;");
					}

			}
		}

	}
	//************************************************************************
	public void setBpp()
	{

		fromPRtoPPP(compression_factor);

		//fromPRtoPPP(8);
		if (2>1) return; 

		for ( int y=0 ; y<number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<number_of_blocks_H;x++)

			{
				Block b=bl[y][x];
				for (int coord=0;coord<2;coord++)
					for (int corner=0;corner<4; corner++)
					{
						//b.ppp[coord][corner]=4;
						b.ppp[coord][corner]=1+(float)Math.random()*7f;

						//System.out.println(" bl["+y+"]["+x+"].ppp["+coord+"]["+corner+"]="+ b.ppp[coord][corner]+"f;");

						b.ppp[coord][0]= 1f;
						b.ppp[coord][1]= 8f;;
						b.ppp[coord][2]= 8f;;
						b.ppp[coord][3]= 1f;;

						/*


					b.ppp[0][0]= 2;
					b.ppp[0][1]= 2;
					b.ppp[0][2]= 2;
					b.ppp[0][3]= 2;

					b.ppp[1][0]= 1;
					b.ppp[1][1]= 4;
					b.ppp[1][2]= 4;
					b.ppp[1][3]= 1;
						 */

						/*
					b.ppp[0][0]= 1;
					b.ppp[0][1]= 4;
					b.ppp[0][2]= 1;
					b.ppp[0][3]= 4;

					b.ppp[1][0]= 1;
					b.ppp[1][1]= 1;
					b.ppp[1][2]= 4;
					b.ppp[1][3]= 4;
						 */



					}
				//b.setPreferredDimension();
				//b.pppToRectangleShape();
			}

		}


	}

	public void SetGaussianDsitributionPR(double avg, double stdev)
	{
		
		float totalblocks=(number_of_blocks_V+1)*(number_of_blocks_H+1);
		
	//	float block=0;
		//first we compute metrics at each PRblock
		float numerator=(float)(1f/(stdev*2.5f));
	//	float residuo=0;
		int contablock=0;
		int x=0;
		int y=0;
		float area=0;
		for (float pr=0;pr<=3.0f;pr+=0.01f) //pr never is > threshold
		{
			float dx=0.01f;	
			area+=	dx*numerator*(float)Math.exp(-0.5f*Math.pow((pr-avg)/stdev,2));
		}
		//pr axis

		System.out.println(" area:"+area);// area is always 1
		//area=1.0f;
		float blockvalue=totalblocks/area;//factor per block
		int c=0;
		
		//ahora recorro los bloques a segmentos, mientras recorro pr
		
		for (float pr=0;pr<=3.0;pr+=0.01f)
		{
			//para este valor de pr veamos cuantos elementos debe haber
			float dx=0.01f;
			float darea=dx*numerator*(float)Math.exp(-0.5f*Math.pow((pr-avg)/stdev,2));
			
			
			float n=darea*blockvalue;
			
			float prfinal=0;
			if (pr>0 && pr<=0.5f) prfinal=pr;
			else if (pr>0.5f) prfinal=0.5f;
			
			int nint=(int)(n);//+residuo);
			//residuo=0;//n-nint;
			
			System.out.println(nint);
			//segmento de bloques con el mismo pr
			for (int i=contablock;i<=contablock+nint;i++)
			{
				//System.out.println("x"+x+"  y:"+y+ "max:"+(number_of_blocks_V+1));
				prbl[y][x].PRx=prfinal;
				prbl[y][x].PRy=prfinal;
				
				//System.out.println(prbl[y][x].PRx);
				c++;
				x++;
				if (x==number_of_blocks_H+1){ x=0;y++;}
				if (y==number_of_blocks_V+1){ break;}
			}
			contablock=contablock+nint;
			if (y==number_of_blocks_V+1){ break;}
			
		}
		
		
		
		System.out.println(" bloques ajustados:"+c);
		/*
		
		
		for (int y=0;y<number_of_blocks_V+1;y++)
		{
			for (int x=0;x<number_of_blocks_H+1;x++)
			{
				
				float i=block/totalblocks;
				float num=(float)(1f/(stdev*2.5f));
				//prbl[y][x].PRx=(float)(num)*(float)Math.exp(-0.5f*Math.pow((i-avg)/stdev,2));
				//if (prbl[y][x].PRx>1) prbl[y][x].PRx=1f;
				//if (prbl[y][x].PRx>0.5) prbl[y][x].PRx=0.5f;
				//prbl[y][x].PRy=prbl[y][x].PRx;
				block+=1;
				System.out.println(prbl[y][x].PRx);
			}//x
		}//y
		*/
	}
	
	
	//**************************************************************************************
	/**
	 * this function compute PRmetrics for all PRblocks of the grid
	 * 
	 * first we compute metrics at each PRblock
	 * then we equalize PR histogram
	 * later on, we quantize PR using 4 levels
	 * 
	 */
public void topaPR(float threshold)
{
	for (int y=0;y<number_of_blocks_V+1;y++)
	{
		for (int x=0;x<number_of_blocks_H+1;x++)
		{
			if (prbl[y][x].PRx>threshold) prbl[y][x].PRx=threshold;
			if (prbl[y][x].PRy>threshold) prbl[y][x].PRy=threshold;
		}//x
	}//y
}
//*************************************************************
public void savePRtxt(String path_file)
{
	
		try{
			System.out.println("Entrando en savePRtxt");
			DataOutputStream d = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path_file)));
			d.writeBytes("[\n");
			for (int y=0;y<number_of_blocks_V+1;y++)
			{
				for (int x=0;x<number_of_blocks_H+1;x++)
				{
					
					d.writeBytes("("+prbl[y][x].PRx+","+prbl[y][x].PRy+"),");
					
				}//x
				d.writeBytes(",\n");
			}//y
			d.writeBytes("]\n");
			
			d.close();
			}

			
		catch(Exception e){System.out.println("ERROR writing PR in txt format:"+e);}	


	}
	

//*************************************************************
public void savePRtxtCSV(String path_file)
{
	
		try{
			System.out.println("Entrando en savePRtxt");
			DataOutputStream d = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path_file)));
			d.writeBytes("\n");
			for (int x=0;x<number_of_blocks_H+1;x++)
			{
				d.writeBytes("PRx;PRy;");
			}
			d.writeBytes("\n");
			for (int y=0;y<number_of_blocks_V+1;y++)
			{
				for (int x=0;x<number_of_blocks_H+1;x++)
				{
					
					d.writeBytes(""+prbl[y][x].PRx+";"+prbl[y][x].PRy+";");
					
					//d.writeBytes(""+prbl[y][x].PRx+"\n"+prbl[y][x].PRy);
					//d.writeBytes("\n");	
				}//x
				d.writeBytes("\n");
			}//y
			d.writeBytes("\n");
			
			d.close();
			}

			
		catch(Exception e){System.out.println("ERROR writing PR in txt format:"+e);}	


	}
	

//****************************************************************************
//*************************************************************
public void savePRtxtClanguage(String path_file)
{
	
		try{
			System.out.println("Entrando en savePRtxtClanguaje");
			DataOutputStream d = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path_file)));
			d.writeBytes("\n");
			
			d.writeBytes("\n");
			
			for (int y=0;y<number_of_blocks_V+1;y++)
			{
				for (int x=0;x<number_of_blocks_H+1;x++)
				{
					
					//d.writeBytes(""+prbl[y][x].PRx+";"+prbl[y][x].PRy+";");
					d.writeBytes("perceptual_relevance_x["+y+"]["+x+"] = "+prbl[y][x].PRx+"; \n");
					d.writeBytes("perceptual_relevance_y["+y+"]["+x+"] = "+prbl[y][x].PRy+"; \n");
					
				}//x
				d.writeBytes("\n");
			}//y
			d.writeBytes("\n");
			
			d.close();
			}

			
		catch(Exception e){System.out.println("ERROR writing PR in txt format:"+e);}	


	}
	

//****************************************************************************
public void savePRtxtJavalanguage(String path_file)
{
	
		try{
			System.out.println("Entrando en savePRtxtJavalanguaje");
			DataOutputStream d = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path_file)));
			d.writeBytes("\n");
			
			d.writeBytes("\n");
			
			for (int y=0;y<number_of_blocks_V+1;y++)
			{
				for (int x=0;x<number_of_blocks_H+1;x++)
				{
					
					//d.writeBytes(""+prbl[y][x].PRx+";"+prbl[y][x].PRy+";");
					d.writeBytes("prbl["+y+"]["+x+"].PRx = "+prbl[y][x].PRx+"f; \n");
					d.writeBytes("prbl["+y+"]["+x+"].PRy = "+prbl[y][x].PRy+"f; \n");
					
				}//x
				d.writeBytes("\n");
			}//y
			d.writeBytes("\n");
			
			d.close();
			}

			
		catch(Exception e){System.out.println("ERROR writing PR in txt format:"+e);}	


	}
	

//****************************************************************************


	public void computeMetrics()
	{
		//esta no usa lo de almacenar los valores de PR sin cuantizar, util para el video
		//en video se usa la otra. de ese modo se implementa la remanencia
		
		//first we compute metrics at each PRblock
		for (int y=0;y<number_of_blocks_V+1;y++)
		{
			for (int x=0;x<number_of_blocks_H+1;x++)
			{
				prbl[y][x].computePRmetrics();
				
				//System.out.println("fin primer bloque");
				
				//System.exit(0);
				
				//prbl[y][x].computePRmetrics4SPS(2,2);
				//prbl[y][x].computePRmetrics4SPS(3,3);
				//prbl[y][x].computePRmetrics4SPS(4,4);
				//prbl[y][x].computePRmetricsSPS();
				//prbl[y][x].computePRmetrics_experimental();
				
			}//x
		}//y
		savePRtxtCSV("./output_debug/PR_raw.csv");
		savePRtxtJavalanguage("./output_debug/PR_final_java.txt");
		
        //loadPrecomputedPR_512();
        //loadPrecomputedPR_512avg();
        //loadPrecomputedPR_1k();
		for (int y=0;y<number_of_blocks_V+1;y++)
		{
			for (int x=0;x<number_of_blocks_H+1;x++)
			{
				//esta funciona bastante bien. es como topar a 0.4. se expande mejor
				//if (prbl[y][x].PRx>0.4) prbl[y][x].PRx=0.4f;//prbl[y][x].PRx*0.5f;
				//if (prbl[y][x].PRy>0.4) prbl[y][x].PRy=0.4f;//prbl[y][x].PRy*0.5f;
				

				//if (prbl[y][x].PRx>0.36) prbl[y][x].PRx=0.36f;//prbl[y][x].PRx*0.5f;
				//if (prbl[y][x].PRy>0.36) prbl[y][x].PRy=0.36f;//prbl[y][x].PRy*0.5f;
				
				//esta funciona peor , es como multiplicar por 0.8
				//prbl[y][x].PRx=prbl[y][x].PRx- prbl[y][x].PRx*0.2f;
				//prbl[y][x].PRy=prbl[y][x].PRy- prbl[y][x].PRy*0.2f;
				 
				
			}//x
		}//y
		//SetGaussianDsitributionPR(0.29f,0.0474);
		//SetGaussianDsitributionPR(0.45f,0.069f);
		//topaPR(0.5f);
		//SetGaussianDsitributionPR(0.25f,0.23f);
		//expandHistogramPR();
		
		//analisys on PR and decision about best equalization
		//---------------------------------------------------
		//this computes PR average and standard deviation
		if (DEBUG)
		{
		float[] PRstats=new float[3];
		PRstats=computePRavg();
		float PRdev=computePRdev(PRstats[0]);
		
		
		
		System.out.println("--- PR STATS BEFORE EQUALIZATION ---");
		System.out.println("   >PR average:"+PRstats[0]+"    , max:"+PRstats[1]+"    , min:"+PRstats[2]);
		System.out.println("   >PR std deviation:"+PRdev);
		
		}
				
		//savePRtxt("./output_debug/PR_raw.txt");
		//savePRtxtCSV("./output_debug/PR_raw.csv");

		//then we equalize PR histogram
		//------------------------------
		if (DEBUG) System.out.println(" equalizing...");
		
		//expandHistogramPR_02(PRstats);
		
		//expandHistogramPR_03(PRstats[0],PRdev);
		
		//printHistogramPR();
		
		if (DEBUG) System.out.println(" setting upper limit at PR=0.5");
		
		topaPR(0.5f);//llllllllllllllllllllllllllllllll
		
		if (DEBUG) System.out.println(" expanding histogram...");
		
		
		
		expandHistogramPR_04(0.2f,0.5f);//PRstats[0],PRdev); 
		//expandHistogramPR_04(0.2f,0.6f);//PRstats[0],PRdev); 
		
		
		if (DEBUG)
		{
		float[] PRstats=new float[3];
		PRstats=computePRavg();
		float PRdev=computePRdev(PRstats[0]);
		System.out.println("--- PR STATS  AFTER EQUALIZATION ---");
		System.out.println("   >PR average:"+PRstats[0]+"    , max:"+PRstats[1]+"    , min:"+PRstats[2]);
		System.out.println("   >PR std deviation:"+PRdev);
		}
		//equalizeHistogramPR();

		if (DEBUG) System.out.println(" quantizing...");
		// the last step: quantization of PR
		//-----------------------------------------
		for (int y=0;y<number_of_blocks_V+1;y++)
		{
			for (int x=0;x<number_of_blocks_H+1;x++)
			{
				
				prbl[y][x].quantizeGeometricalPR();
				
			}//x
		}//y

		//savePRtxtCSV("./output_debug/PR_final.csv");
		//savePRtxtClanguage("./output_debug/PR_final_c.txt");
		//savePRtxtJavalanguage("./output_debug/PR_final_java.txt");
		
        //loadPrecomputedPR();
	}
	
public void loadPrecomputedPR_1k()
{



prbl[0][0].PRx = 0.28076923f; 
prbl[0][0].PRy = 0.27727273f; 
prbl[0][1].PRx = 0.29597703f; 
prbl[0][1].PRy = 0.2729885f; 
prbl[0][2].PRx = 0.2771739f; 
prbl[0][2].PRy = 0.29166666f; 
prbl[0][3].PRx = 0.2923387f; 
prbl[0][3].PRy = 0.28181818f; 
prbl[0][4].PRx = 0.27608696f; 
prbl[0][4].PRy = 0.27864584f; 
prbl[0][5].PRx = 0.27932099f; 
prbl[0][5].PRy = 0.28030303f; 
prbl[0][6].PRx = 0.28144655f; 
prbl[0][6].PRy = 0.28125f; 
prbl[0][7].PRx = 0.3026316f; 
prbl[0][7].PRy = 0.29746836f; 
prbl[0][8].PRx = 0.2996454f; 
prbl[0][8].PRy = 0.29787233f; 
prbl[0][9].PRx = 0.38257575f; 
prbl[0][9].PRy = 0.35185185f; 
prbl[0][10].PRx = 0.3015873f; 
prbl[0][10].PRy = 0.3f; 
prbl[0][11].PRx = 0.28f; 
prbl[0][11].PRy = 0.28157896f; 
prbl[0][12].PRx = 0.29248366f; 
prbl[0][12].PRy = 0.28504673f; 
prbl[0][13].PRx = 0.28666666f; 
prbl[0][13].PRy = 0.2740964f; 
prbl[0][14].PRx = 0.28373015f; 
prbl[0][14].PRy = 0.2725225f; 
prbl[0][15].PRx = 0.29567307f; 
prbl[0][15].PRy = 0.27941176f; 
prbl[0][16].PRx = 0.28723404f; 
prbl[0][16].PRy = 0.27747253f; 
prbl[0][17].PRx = 0.29222974f; 
prbl[0][17].PRy = 0.28861788f; 
prbl[0][18].PRx = 0.29605263f; 
prbl[0][18].PRy = 0.2635135f; 
prbl[0][19].PRx = 0.29874215f; 
prbl[0][19].PRy = 0.27f; 
prbl[0][20].PRx = 0.29264706f; 
prbl[0][20].PRy = 0.27227724f; 
prbl[0][21].PRx = 0.2953125f; 
prbl[0][21].PRy = 0.30337077f; 
prbl[0][22].PRx = 0.30191258f; 
prbl[0][22].PRy = 0.2826087f; 
prbl[0][23].PRx = 0.29905063f; 
prbl[0][23].PRy = 0.28448275f; 
prbl[0][24].PRx = 0.3343949f; 
prbl[0][24].PRy = 0.3f; 
prbl[0][25].PRx = 0.29360464f; 
prbl[0][25].PRy = 0.28478262f; 
prbl[0][26].PRx = 0.2904412f; 
prbl[0][26].PRy = 0.28989363f; 
prbl[0][27].PRx = 0.28104576f; 
prbl[0][27].PRy = 0.27197802f; 
prbl[0][28].PRx = 0.2797619f; 
prbl[0][28].PRy = 0.28024194f; 
prbl[0][29].PRx = 0.2783019f; 
prbl[0][29].PRy = 0.28947368f; 
prbl[0][30].PRx = 0.2766854f; 
prbl[0][30].PRy = 0.28373015f; 
prbl[0][31].PRx = 0.296875f; 
prbl[0][31].PRy = 0.28030303f; 
prbl[0][32].PRx = 0.2943038f; 
prbl[0][32].PRy = 0.27272728f; 

prbl[1][0].PRx = 0.27515724f; 
prbl[1][0].PRy = 0.28515625f; 
prbl[1][1].PRx = 0.28099173f; 
prbl[1][1].PRy = 0.28063726f; 
prbl[1][2].PRx = 0.2782258f; 
prbl[1][2].PRy = 0.27108434f; 
prbl[1][3].PRx = 0.2908497f; 
prbl[1][3].PRy = 0.28697184f; 
prbl[1][4].PRx = 0.29454023f; 
prbl[1][4].PRy = 0.28870967f; 
prbl[1][5].PRx = 0.28346458f; 
prbl[1][5].PRy = 0.28188777f; 
prbl[1][6].PRx = 0.2871486f; 
prbl[1][6].PRy = 0.2840909f; 
prbl[1][7].PRx = 0.28207546f; 
prbl[1][7].PRy = 0.29145077f; 
prbl[1][8].PRx = 0.3375f; 
prbl[1][8].PRy = 0.33901516f; 
prbl[1][9].PRx = 0.4100877f; 
prbl[1][9].PRy = 0.40641025f; 
prbl[1][10].PRx = 0.32482395f; 
prbl[1][10].PRy = 0.32331732f; 
prbl[1][11].PRx = 0.32401314f; 
prbl[1][11].PRy = 0.32360405f; 
prbl[1][12].PRx = 0.28673837f; 
prbl[1][12].PRy = 0.29010695f; 
prbl[1][13].PRx = 0.29042554f; 
prbl[1][13].PRy = 0.28703704f; 
prbl[1][14].PRx = 0.29038462f; 
prbl[1][14].PRy = 0.2849099f; 
prbl[1][15].PRx = 0.2874016f; 
prbl[1][15].PRy = 0.28522727f; 
prbl[1][16].PRx = 0.28543308f; 
prbl[1][16].PRy = 0.28952992f; 
prbl[1][17].PRx = 0.2872671f; 
prbl[1][17].PRy = 0.2804878f; 
prbl[1][18].PRx = 0.29154077f; 
prbl[1][18].PRy = 0.28618422f; 
prbl[1][19].PRx = 0.28911042f; 
prbl[1][19].PRy = 0.2821101f; 
prbl[1][20].PRx = 0.2957143f; 
prbl[1][20].PRy = 0.28139013f; 
prbl[1][21].PRx = 0.2875f; 
prbl[1][21].PRy = 0.28229666f; 
prbl[1][22].PRx = 0.29f; 
prbl[1][22].PRy = 0.29223743f; 
prbl[1][23].PRx = 0.30887097f; 
prbl[1][23].PRy = 0.30714285f; 
prbl[1][24].PRx = 0.29984894f; 
prbl[1][24].PRy = 0.28623188f; 
prbl[1][25].PRx = 0.30305466f; 
prbl[1][25].PRy = 0.28282827f; 
prbl[1][26].PRx = 0.2947761f; 
prbl[1][26].PRy = 0.29620853f; 
prbl[1][27].PRx = 0.2875f; 
prbl[1][27].PRy = 0.28915662f; 
prbl[1][28].PRx = 0.27890626f; 
prbl[1][28].PRy = 0.27191234f; 
prbl[1][29].PRx = 0.2777778f; 
prbl[1][29].PRy = 0.26704547f; 
prbl[1][30].PRx = 0.2807808f; 
prbl[1][30].PRy = 0.27923387f; 
prbl[1][31].PRx = 0.29872206f; 
prbl[1][31].PRy = 0.26595744f; 
prbl[1][32].PRx = 0.29666665f; 
prbl[1][32].PRy = 0.27f; 

prbl[2][0].PRx = 0.27450982f; 
prbl[2][0].PRy = 0.27083334f; 
prbl[2][1].PRx = 0.28431374f; 
prbl[2][1].PRy = 0.27766395f; 
prbl[2][2].PRx = 0.28333333f; 
prbl[2][2].PRy = 0.284375f; 
prbl[2][3].PRx = 0.2920082f; 
prbl[2][3].PRy = 0.2850679f; 
prbl[2][4].PRx = 0.28403756f; 
prbl[2][4].PRy = 0.27146465f; 
prbl[2][5].PRx = 0.2982595f; 
prbl[2][5].PRy = 0.2890995f; 
prbl[2][6].PRx = 0.2944664f; 
prbl[2][6].PRy = 0.30851063f; 
prbl[2][7].PRx = 0.28942654f; 
prbl[2][7].PRy = 0.29739776f; 
prbl[2][8].PRx = 0.34364262f; 
prbl[2][8].PRy = 0.35469314f; 
prbl[2][9].PRx = 0.37140575f; 
prbl[2][9].PRy = 0.3786611f; 
prbl[2][10].PRx = 0.3177481f; 
prbl[2][10].PRy = 0.32520324f; 
prbl[2][11].PRx = 0.3290441f; 
prbl[2][11].PRy = 0.33268481f; 
prbl[2][12].PRx = 0.3025292f; 
prbl[2][12].PRy = 0.28846154f; 
prbl[2][13].PRx = 0.28977272f; 
prbl[2][13].PRy = 0.29282868f; 
prbl[2][14].PRx = 0.29657796f; 
prbl[2][14].PRy = 0.3030888f; 
prbl[2][15].PRx = 0.28650442f; 
prbl[2][15].PRy = 0.29032257f; 
prbl[2][16].PRx = 0.28947368f; 
prbl[2][16].PRy = 0.2835498f; 
prbl[2][17].PRx = 0.2980226f; 
prbl[2][17].PRy = 0.29285714f; 
prbl[2][18].PRx = 0.29273504f; 
prbl[2][18].PRy = 0.27748692f; 
prbl[2][19].PRx = 0.29856688f; 
prbl[2][19].PRy = 0.27328432f; 
prbl[2][20].PRx = 0.2846154f; 
prbl[2][20].PRy = 0.27978724f; 
prbl[2][21].PRx = 0.30169493f; 
prbl[2][21].PRy = 0.28403142f; 
prbl[2][22].PRx = 0.3097643f; 
prbl[2][22].PRy = 0.29302326f; 
prbl[2][23].PRx = 0.28601694f; 
prbl[2][23].PRy = 0.2855691f; 
prbl[2][24].PRx = 0.30475205f; 
prbl[2][24].PRy = 0.29777777f; 
prbl[2][25].PRx = 0.29457363f; 
prbl[2][25].PRy = 0.28199053f; 
prbl[2][26].PRx = 0.28600824f; 
prbl[2][26].PRy = 0.2850877f; 
prbl[2][27].PRx = 0.2942387f; 
prbl[2][27].PRy = 0.29404762f; 
prbl[2][28].PRx = 0.28671327f; 
prbl[2][28].PRy = 0.2816594f; 
prbl[2][29].PRx = 0.28054664f; 
prbl[2][29].PRy = 0.28163266f; 
prbl[2][30].PRx = 0.28264332f; 
prbl[2][30].PRy = 0.27578476f; 
prbl[2][31].PRx = 0.30416667f; 
prbl[2][31].PRy = 0.26744187f; 
prbl[2][32].PRx = 0.29655173f; 
prbl[2][32].PRy = 0.29268292f; 

prbl[3][0].PRx = 0.27286586f; 
prbl[3][0].PRy = 0.27272728f; 
prbl[3][1].PRx = 0.28547296f; 
prbl[3][1].PRy = 0.27697095f; 
prbl[3][2].PRx = 0.2798387f; 
prbl[3][2].PRy = 0.27988046f; 
prbl[3][3].PRx = 0.27313432f; 
prbl[3][3].PRy = 0.27621722f; 
prbl[3][4].PRx = 0.28400734f; 
prbl[3][4].PRy = 0.28138074f; 
prbl[3][5].PRx = 0.29455444f; 
prbl[3][5].PRy = 0.2881356f; 
prbl[3][6].PRx = 0.431677f; 
prbl[3][6].PRy = 0.3974359f; 
prbl[3][7].PRx = 0.36758894f; 
prbl[3][7].PRy = 0.35638297f; 
prbl[3][8].PRx = 0.36921707f; 
prbl[3][8].PRy = 0.3697318f; 
prbl[3][9].PRx = 0.3410042f; 
prbl[3][9].PRy = 0.33471075f; 
prbl[3][10].PRx = 0.29584774f; 
prbl[3][10].PRy = 0.3002008f; 
prbl[3][11].PRx = 0.30809128f; 
prbl[3][11].PRy = 0.29204544f; 
prbl[3][12].PRx = 0.29555085f; 
prbl[3][12].PRy = 0.30235043f; 
prbl[3][13].PRx = 0.29785156f; 
prbl[3][13].PRy = 0.28985506f; 
prbl[3][14].PRx = 0.2881818f; 
prbl[3][14].PRy = 0.2867347f; 
prbl[3][15].PRx = 0.29841897f; 
prbl[3][15].PRy = 0.30578512f; 
prbl[3][16].PRx = 0.29219747f; 
prbl[3][16].PRy = 0.29919678f; 
prbl[3][17].PRx = 0.31f; 
prbl[3][17].PRy = 0.29807693f; 
prbl[3][18].PRx = 0.313783f; 
prbl[3][18].PRy = 0.30268595f; 
prbl[3][19].PRx = 0.28846154f; 
prbl[3][19].PRy = 0.2939189f; 
prbl[3][20].PRx = 0.2970297f; 
prbl[3][20].PRy = 0.28664923f; 
prbl[3][21].PRx = 0.29836065f; 
prbl[3][21].PRy = 0.28787878f; 
prbl[3][22].PRx = 0.3001894f; 
prbl[3][22].PRy = 0.28819445f; 
prbl[3][23].PRx = 0.30357143f; 
prbl[3][23].PRy = 0.2972973f; 
prbl[3][24].PRx = 0.29285714f; 
prbl[3][24].PRy = 0.28819445f; 
prbl[3][25].PRx = 0.30510205f; 
prbl[3][25].PRy = 0.30290458f; 
prbl[3][26].PRx = 0.2882353f; 
prbl[3][26].PRy = 0.28125f; 
prbl[3][27].PRx = 0.29482758f; 
prbl[3][27].PRy = 0.2897196f; 
prbl[3][28].PRx = 0.28586066f; 
prbl[3][28].PRy = 0.28618422f; 
prbl[3][29].PRx = 0.28052804f; 
prbl[3][29].PRy = 0.2890995f; 
prbl[3][30].PRx = 0.29035088f; 
prbl[3][30].PRy = 0.2776163f; 
prbl[3][31].PRx = 0.3095588f; 
prbl[3][31].PRy = 0.28143713f; 
prbl[3][32].PRx = 0.3097015f; 
prbl[3][32].PRy = 0.284375f; 

prbl[4][0].PRx = 0.2672414f; 
prbl[4][0].PRy = 0.2653846f; 
prbl[4][1].PRx = 0.27767527f; 
prbl[4][1].PRy = 0.28353658f; 
prbl[4][2].PRx = 0.28387097f; 
prbl[4][2].PRy = 0.27529183f; 
prbl[4][3].PRx = 0.2866242f; 
prbl[4][3].PRy = 0.27047414f; 
prbl[4][4].PRx = 0.27996254f; 
prbl[4][4].PRy = 0.27561477f; 
prbl[4][5].PRx = 0.2766272f; 
prbl[4][5].PRy = 0.28333333f; 
prbl[4][6].PRx = 0.42824075f; 
prbl[4][6].PRy = 0.41190475f; 
prbl[4][7].PRx = 0.41826922f; 
prbl[4][7].PRy = 0.41858238f; 
prbl[4][8].PRx = 0.3970588f; 
prbl[4][8].PRy = 0.41903916f; 
prbl[4][9].PRx = 0.30811402f; 
prbl[4][9].PRy = 0.31220096f; 
prbl[4][10].PRx = 0.31976745f; 
prbl[4][10].PRy = 0.32254463f; 
prbl[4][11].PRx = 0.30844155f; 
prbl[4][11].PRy = 0.30625f; 
prbl[4][12].PRx = 0.29946524f; 
prbl[4][12].PRy = 0.29615384f; 
prbl[4][13].PRx = 0.3f; 
prbl[4][13].PRy = 0.29320988f; 
prbl[4][14].PRx = 0.30941704f; 
prbl[4][14].PRy = 0.29654256f; 
prbl[4][15].PRx = 0.28515625f; 
prbl[4][15].PRy = 0.290625f; 
prbl[4][16].PRx = 0.29503107f; 
prbl[4][16].PRy = 0.29245284f; 
prbl[4][17].PRx = 0.36142322f; 
prbl[4][17].PRy = 0.38324872f; 
prbl[4][18].PRx = 0.31539735f; 
prbl[4][18].PRy = 0.30930233f; 
prbl[4][19].PRx = 0.2912913f; 
prbl[4][19].PRy = 0.2900433f; 
prbl[4][20].PRx = 0.2995427f; 
prbl[4][20].PRy = 0.2872093f; 
prbl[4][21].PRx = 0.29701492f; 
prbl[4][21].PRy = 0.2818396f; 
prbl[4][22].PRx = 0.29702196f; 
prbl[4][22].PRy = 0.29100528f; 
prbl[4][23].PRx = 0.2878007f; 
prbl[4][23].PRy = 0.28372094f; 
prbl[4][24].PRx = 0.28374234f; 
prbl[4][24].PRy = 0.28333333f; 
prbl[4][25].PRx = 0.29692307f; 
prbl[4][25].PRy = 0.29257643f; 
prbl[4][26].PRx = 0.28647417f; 
prbl[4][26].PRy = 0.28277153f; 
prbl[4][27].PRx = 0.30718955f; 
prbl[4][27].PRy = 0.29166666f; 
prbl[4][28].PRx = 0.2920082f; 
prbl[4][28].PRy = 0.28879312f; 
prbl[4][29].PRx = 0.29421222f; 
prbl[4][29].PRy = 0.29792747f; 
prbl[4][30].PRx = 0.2833904f; 
prbl[4][30].PRy = 0.27534562f; 
prbl[4][31].PRx = 0.30081967f; 
prbl[4][31].PRy = 0.29060915f; 
prbl[4][32].PRx = 0.28219697f; 
prbl[4][32].PRy = 0.28431374f; 

prbl[5][0].PRx = 0.2767857f; 
prbl[5][0].PRy = 0.2800926f; 
prbl[5][1].PRx = 0.28977272f; 
prbl[5][1].PRy = 0.28879312f; 
prbl[5][2].PRx = 0.28152174f; 
prbl[5][2].PRy = 0.28019324f; 
prbl[5][3].PRx = 0.28846154f; 
prbl[5][3].PRy = 0.2802198f; 
prbl[5][4].PRx = 0.29262295f; 
prbl[5][4].PRy = 0.2895349f; 
prbl[5][5].PRx = 0.32903227f; 
prbl[5][5].PRy = 0.32426777f; 
prbl[5][6].PRx = 0.3590909f; 
prbl[5][6].PRy = 0.36189517f; 
prbl[5][7].PRx = 0.3899614f; 
prbl[5][7].PRy = 0.43173078f; 
prbl[5][8].PRx = 0.3900966f; 
prbl[5][8].PRy = 0.40778098f; 
prbl[5][9].PRx = 0.4225352f; 
prbl[5][9].PRy = 0.4147541f; 
prbl[5][10].PRx = 0.3503861f; 
prbl[5][10].PRy = 0.35962567f; 
prbl[5][11].PRx = 0.359375f; 
prbl[5][11].PRy = 0.375f; 
prbl[5][12].PRx = 0.30569947f; 
prbl[5][12].PRy = 0.28125f; 
prbl[5][13].PRx = 0.31125f; 
prbl[5][13].PRy = 0.30813953f; 
prbl[5][14].PRx = 0.3172043f; 
prbl[5][14].PRy = 0.3013889f; 
prbl[5][15].PRx = 0.296875f; 
prbl[5][15].PRy = 0.30177516f; 
prbl[5][16].PRx = 0.29147464f; 
prbl[5][16].PRy = 0.2882353f; 
prbl[5][17].PRx = 0.3297491f; 
prbl[5][17].PRy = 0.32843137f; 
prbl[5][18].PRx = 0.31785715f; 
prbl[5][18].PRy = 0.3025701f; 
prbl[5][19].PRx = 0.2984375f; 
prbl[5][19].PRy = 0.28867403f; 
prbl[5][20].PRx = 0.30385852f; 
prbl[5][20].PRy = 0.28877005f; 
prbl[5][21].PRx = 0.2995283f; 
prbl[5][21].PRy = 0.28475937f; 
prbl[5][22].PRx = 0.28535354f; 
prbl[5][22].PRy = 0.2789855f; 
prbl[5][23].PRx = 0.2869318f; 
prbl[5][23].PRy = 0.2863248f; 
prbl[5][24].PRx = 0.3031768f; 
prbl[5][24].PRy = 0.28987068f; 
prbl[5][25].PRx = 0.29646018f; 
prbl[5][25].PRy = 0.29605263f; 
prbl[5][26].PRx = 0.28858024f; 
prbl[5][26].PRy = 0.2804878f; 
prbl[5][27].PRx = 0.2855987f; 
prbl[5][27].PRy = 0.2857143f; 
prbl[5][28].PRx = 0.28151262f; 
prbl[5][28].PRy = 0.28236607f; 
prbl[5][29].PRx = 0.30452675f; 
prbl[5][29].PRy = 0.29424778f; 
prbl[5][30].PRx = 0.2936508f; 
prbl[5][30].PRy = 0.2823834f; 
prbl[5][31].PRx = 0.30377358f; 
prbl[5][31].PRy = 0.27218935f; 
prbl[5][32].PRx = 0.28355706f; 
prbl[5][32].PRy = 0.27884614f; 

prbl[6][0].PRx = 0.27258065f; 
prbl[6][0].PRy = 0.265625f; 
prbl[6][1].PRx = 0.28932583f; 
prbl[6][1].PRy = 0.289548f; 
prbl[6][2].PRx = 0.2861635f; 
prbl[6][2].PRy = 0.27307692f; 
prbl[6][3].PRx = 0.27401745f; 
prbl[6][3].PRy = 0.2735602f; 
prbl[6][4].PRx = 0.28541666f; 
prbl[6][4].PRy = 0.2841797f; 
prbl[6][5].PRx = 0.35474005f; 
prbl[6][5].PRy = 0.31847826f; 
prbl[6][6].PRx = 0.31930694f; 
prbl[6][6].PRy = 0.32994187f; 
prbl[6][7].PRx = 0.3900966f; 
prbl[6][7].PRy = 0.39849624f; 
prbl[6][8].PRx = 0.45588234f; 
prbl[6][8].PRy = 0.45833334f; 
prbl[6][9].PRx = 0.4970588f; 
prbl[6][9].PRy = 0.5242347f; 
prbl[6][10].PRx = 0.3640625f; 
prbl[6][10].PRy = 0.36956522f; 
prbl[6][11].PRx = 0.4351093f; 
prbl[6][11].PRy = 0.46413934f; 
prbl[6][12].PRx = 0.34076434f; 
prbl[6][12].PRy = 0.32429245f; 
prbl[6][13].PRx = 0.30677292f; 
prbl[6][13].PRy = 0.28921568f; 
prbl[6][14].PRx = 0.31044775f; 
prbl[6][14].PRy = 0.30165288f; 
prbl[6][15].PRx = 0.29580745f; 
prbl[6][15].PRy = 0.29215688f; 
prbl[6][16].PRx = 0.29180887f; 
prbl[6][16].PRy = 0.2857143f; 
prbl[6][17].PRx = 0.30125523f; 
prbl[6][17].PRy = 0.286157f; 
prbl[6][18].PRx = 0.31661677f; 
prbl[6][18].PRy = 0.29521278f; 
prbl[6][19].PRx = 0.31034482f; 
prbl[6][19].PRy = 0.29069766f; 
prbl[6][20].PRx = 0.28465346f; 
prbl[6][20].PRy = 0.28318584f; 
prbl[6][21].PRx = 0.30124223f; 
prbl[6][21].PRy = 0.28618422f; 
prbl[6][22].PRx = 0.30338982f; 
prbl[6][22].PRy = 0.28898305f; 
prbl[6][23].PRx = 0.28453454f; 
prbl[6][23].PRy = 0.2858527f; 
prbl[6][24].PRx = 0.30058652f; 
prbl[6][24].PRy = 0.29464287f; 
prbl[6][25].PRx = 0.29792333f; 
prbl[6][25].PRy = 0.2838983f; 
prbl[6][26].PRx = 0.29929578f; 
prbl[6][26].PRy = 0.29209185f; 
prbl[6][27].PRx = 0.28365386f; 
prbl[6][27].PRy = 0.2872549f; 
prbl[6][28].PRx = 0.29651162f; 
prbl[6][28].PRy = 0.2851064f; 
prbl[6][29].PRx = 0.29471543f; 
prbl[6][29].PRy = 0.28940886f; 
prbl[6][30].PRx = 0.28417265f; 
prbl[6][30].PRy = 0.28070176f; 
prbl[6][31].PRx = 0.29616725f; 
prbl[6][31].PRy = 0.28010473f; 
prbl[6][32].PRx = 0.2989865f; 
prbl[6][32].PRy = 0.2647059f; 

prbl[7][0].PRx = 0.26744187f; 
prbl[7][0].PRy = 0.26984128f; 
prbl[7][1].PRx = 0.28144655f; 
prbl[7][1].PRy = 0.28586498f; 
prbl[7][2].PRx = 0.27949852f; 
prbl[7][2].PRy = 0.27480915f; 
prbl[7][3].PRx = 0.2734139f; 
prbl[7][3].PRy = 0.2745283f; 
prbl[7][4].PRx = 0.2830033f; 
prbl[7][4].PRy = 0.2819149f; 
prbl[7][5].PRx = 0.32091346f; 
prbl[7][5].PRy = 0.3140625f; 
prbl[7][6].PRx = 0.37991267f; 
prbl[7][6].PRy = 0.38479263f; 
prbl[7][7].PRx = 0.4256757f; 
prbl[7][7].PRy = 0.44012606f; 
prbl[7][8].PRx = 0.3760246f; 
prbl[7][8].PRy = 0.34892085f; 
prbl[7][9].PRx = 0.41914192f; 
prbl[7][9].PRy = 0.45967743f; 
prbl[7][10].PRx = 0.37695312f; 
prbl[7][10].PRy = 0.39772728f; 
prbl[7][11].PRx = 0.4150641f; 
prbl[7][11].PRy = 0.42447916f; 
prbl[7][12].PRx = 0.3042857f; 
prbl[7][12].PRy = 0.31382978f; 
prbl[7][13].PRx = 0.33112094f; 
prbl[7][13].PRy = 0.31069365f; 
prbl[7][14].PRx = 0.28947368f; 
prbl[7][14].PRy = 0.28414634f; 
prbl[7][15].PRx = 0.28816795f; 
prbl[7][15].PRy = 0.28755364f; 
prbl[7][16].PRx = 0.29617834f; 
prbl[7][16].PRy = 0.28414634f; 
prbl[7][17].PRx = 0.29882812f; 
prbl[7][17].PRy = 0.30569306f; 
prbl[7][18].PRx = 0.28660715f; 
prbl[7][18].PRy = 0.27995393f; 
prbl[7][19].PRx = 0.3f; 
prbl[7][19].PRy = 0.2846821f; 
prbl[7][20].PRx = 0.28030303f; 
prbl[7][20].PRy = 0.2863248f; 
prbl[7][21].PRx = 0.2835821f; 
prbl[7][21].PRy = 0.2820248f; 
prbl[7][22].PRx = 0.29301947f; 
prbl[7][22].PRy = 0.29249012f; 
prbl[7][23].PRx = 0.28790614f; 
prbl[7][23].PRy = 0.28156567f; 
prbl[7][24].PRx = 0.31917807f; 
prbl[7][24].PRy = 0.29483697f; 
prbl[7][25].PRx = 0.29928315f; 
prbl[7][25].PRy = 0.29057592f; 
prbl[7][26].PRx = 0.29243544f; 
prbl[7][26].PRy = 0.29366812f; 
prbl[7][27].PRx = 0.29758307f; 
prbl[7][27].PRy = 0.2956274f; 
prbl[7][28].PRx = 0.3011811f; 
prbl[7][28].PRy = 0.30603448f; 
prbl[7][29].PRx = 0.28339696f; 
prbl[7][29].PRy = 0.28784403f; 
prbl[7][30].PRx = 0.2960725f; 
prbl[7][30].PRy = 0.2849741f; 
prbl[7][31].PRx = 0.30357143f; 
prbl[7][31].PRy = 0.2905028f; 
prbl[7][32].PRx = 0.307947f; 
prbl[7][32].PRy = 0.2798913f; 

prbl[8][0].PRx = 0.27623457f; 
prbl[8][0].PRy = 0.28467155f; 
prbl[8][1].PRx = 0.2776968f; 
prbl[8][1].PRy = 0.27356556f; 
prbl[8][2].PRx = 0.28014705f; 
prbl[8][2].PRy = 0.27743903f; 
prbl[8][3].PRx = 0.29154727f; 
prbl[8][3].PRy = 0.29661018f; 
prbl[8][4].PRx = 0.30050504f; 
prbl[8][4].PRy = 0.31463414f; 
prbl[8][5].PRx = 0.37797618f; 
prbl[8][5].PRy = 0.36622807f; 
prbl[8][6].PRx = 0.395625f; 
prbl[8][6].PRy = 0.3736413f; 
prbl[8][7].PRx = 0.45786518f; 
prbl[8][7].PRy = 0.4343318f; 
prbl[8][8].PRx = 0.41273585f; 
prbl[8][8].PRy = 0.37254903f; 
prbl[8][9].PRx = 0.37461302f; 
prbl[8][9].PRy = 0.34848484f; 
prbl[8][10].PRx = 0.42153284f; 
prbl[8][10].PRy = 0.4205298f; 
prbl[8][11].PRx = 0.40762272f; 
prbl[8][11].PRy = 0.39977476f; 
prbl[8][12].PRx = 0.28587443f; 
prbl[8][12].PRy = 0.27941176f; 
prbl[8][13].PRx = 0.31143343f; 
prbl[8][13].PRy = 0.2994924f; 
prbl[8][14].PRx = 0.3106383f; 
prbl[8][14].PRy = 0.30125523f; 
prbl[8][15].PRx = 0.28680202f; 
prbl[8][15].PRy = 0.29227054f; 
prbl[8][16].PRx = 0.29738563f; 
prbl[8][16].PRy = 0.28f; 
prbl[8][17].PRx = 0.3237288f; 
prbl[8][17].PRy = 0.28188777f; 
prbl[8][18].PRx = 0.29575163f; 
prbl[8][18].PRy = 0.2912844f; 
prbl[8][19].PRx = 0.29166666f; 
prbl[8][19].PRy = 0.28381148f; 
prbl[8][20].PRx = 0.29073033f; 
prbl[8][20].PRy = 0.2817623f; 
prbl[8][21].PRx = 0.2852853f; 
prbl[8][21].PRy = 0.2782258f; 
prbl[8][22].PRx = 0.28189912f; 
prbl[8][22].PRy = 0.27470356f; 
prbl[8][23].PRx = 0.2872629f; 
prbl[8][23].PRy = 0.28441295f; 
prbl[8][24].PRx = 0.30483872f; 
prbl[8][24].PRy = 0.28301886f; 
prbl[8][25].PRx = 0.28690037f; 
prbl[8][25].PRy = 0.2834008f; 
prbl[8][26].PRx = 0.29229322f; 
prbl[8][26].PRy = 0.28588516f; 
prbl[8][27].PRx = 0.32969433f; 
prbl[8][27].PRy = 0.3026316f; 
prbl[8][28].PRx = 0.28597122f; 
prbl[8][28].PRy = 0.27120537f; 
prbl[8][29].PRx = 0.2784091f; 
prbl[8][29].PRy = 0.2718254f; 
prbl[8][30].PRx = 0.29421768f; 
prbl[8][30].PRy = 0.2857143f; 
prbl[8][31].PRx = 0.28725165f; 
prbl[8][31].PRy = 0.2755102f; 
prbl[8][32].PRx = 0.32453415f; 
prbl[8][32].PRy = 0.28521127f; 

prbl[9][0].PRx = 0.2744253f; 
prbl[9][0].PRy = 0.2744755f; 
prbl[9][1].PRx = 0.2787267f; 
prbl[9][1].PRy = 0.28174603f; 
prbl[9][2].PRx = 0.27407932f; 
prbl[9][2].PRy = 0.27818182f; 
prbl[9][3].PRx = 0.291f; 
prbl[9][3].PRy = 0.28985506f; 
prbl[9][4].PRx = 0.31985295f; 
prbl[9][4].PRy = 0.3172589f; 
prbl[9][5].PRx = 0.35970148f; 
prbl[9][5].PRy = 0.34977064f; 
prbl[9][6].PRx = 0.39130434f; 
prbl[9][6].PRy = 0.36625f; 
prbl[9][7].PRx = 0.39864865f; 
prbl[9][7].PRy = 0.35443038f; 
prbl[9][8].PRx = 0.36883804f; 
prbl[9][8].PRy = 0.32563025f; 
prbl[9][9].PRx = 0.37714285f; 
prbl[9][9].PRy = 0.3502825f; 
prbl[9][10].PRx = 0.4486994f; 
prbl[9][10].PRy = 0.44852942f; 
prbl[9][11].PRx = 0.4135101f; 
prbl[9][11].PRy = 0.3755924f; 
prbl[9][12].PRx = 0.29893616f; 
prbl[9][12].PRy = 0.29347825f; 
prbl[9][13].PRx = 0.28846154f; 
prbl[9][13].PRy = 0.28193432f; 
prbl[9][14].PRx = 0.29568106f; 
prbl[9][14].PRy = 0.29471543f; 
prbl[9][15].PRx = 0.29240283f; 
prbl[9][15].PRy = 0.2886266f; 
prbl[9][16].PRx = 0.29591838f; 
prbl[9][16].PRy = 0.29393306f; 
prbl[9][17].PRx = 0.30714285f; 
prbl[9][17].PRy = 0.27914798f; 
prbl[9][18].PRx = 0.28601694f; 
prbl[9][18].PRy = 0.28838584f; 
prbl[9][19].PRx = 0.28652596f; 
prbl[9][19].PRy = 0.2780269f; 
prbl[9][20].PRx = 0.2891374f; 
prbl[9][20].PRy = 0.28019324f; 
prbl[9][21].PRx = 0.28640777f; 
prbl[9][21].PRy = 0.28663793f; 
prbl[9][22].PRx = 0.2822086f; 
prbl[9][22].PRy = 0.2792793f; 
prbl[9][23].PRx = 0.2827744f; 
prbl[9][23].PRy = 0.27755103f; 
prbl[9][24].PRx = 0.3111511f; 
prbl[9][24].PRy = 0.29672897f; 
prbl[9][25].PRx = 0.29590163f; 
prbl[9][25].PRy = 0.28320312f; 
prbl[9][26].PRx = 0.295302f; 
prbl[9][26].PRy = 0.2863436f; 
prbl[9][27].PRx = 0.30983606f; 
prbl[9][27].PRy = 0.30450237f; 
prbl[9][28].PRx = 0.29440156f; 
prbl[9][28].PRy = 0.29495615f; 
prbl[9][29].PRx = 0.27824268f; 
prbl[9][29].PRy = 0.27813852f; 
prbl[9][30].PRx = 0.27962962f; 
prbl[9][30].PRy = 0.28466386f; 
prbl[9][31].PRx = 0.30487806f; 
prbl[9][31].PRy = 0.27651516f; 
prbl[9][32].PRx = 0.2823741f; 
prbl[9][32].PRy = 0.26875f; 

prbl[10][0].PRx = 0.27571428f; 
prbl[10][0].PRy = 0.2710084f; 
prbl[10][1].PRx = 0.2833904f; 
prbl[10][1].PRy = 0.27489176f; 
prbl[10][2].PRx = 0.29198474f; 
prbl[10][2].PRy = 0.29429135f; 
prbl[10][3].PRx = 0.30567685f; 
prbl[10][3].PRy = 0.31311882f; 
prbl[10][4].PRx = 0.3572727f; 
prbl[10][4].PRy = 0.3649635f; 
prbl[10][5].PRx = 0.40425533f; 
prbl[10][5].PRy = 0.3990964f; 
prbl[10][6].PRx = 0.3576779f; 
prbl[10][6].PRy = 0.31375f; 
prbl[10][7].PRx = 0.3504673f; 
prbl[10][7].PRy = 0.3045977f; 
prbl[10][8].PRx = 0.33440515f; 
prbl[10][8].PRy = 0.29874215f; 
prbl[10][9].PRx = 0.3872549f; 
prbl[10][9].PRy = 0.3773148f; 
prbl[10][10].PRx = 0.47716627f; 
prbl[10][10].PRy = 0.5289634f; 
prbl[10][11].PRx = 0.44208038f; 
prbl[10][11].PRy = 0.43390805f; 
prbl[10][12].PRx = 0.34198812f; 
prbl[10][12].PRy = 0.3181818f; 
prbl[10][13].PRx = 0.2882948f; 
prbl[10][13].PRy = 0.28158844f; 
prbl[10][14].PRx = 0.2888889f; 
prbl[10][14].PRy = 0.2956274f; 
prbl[10][15].PRx = 0.28790984f; 
prbl[10][15].PRy = 0.29333332f; 
prbl[10][16].PRx = 0.32976654f; 
prbl[10][16].PRy = 0.3110048f; 
prbl[10][17].PRx = 0.28958333f; 
prbl[10][17].PRy = 0.28723404f; 
prbl[10][18].PRx = 0.28438395f; 
prbl[10][18].PRy = 0.2855691f; 
prbl[10][19].PRx = 0.27294305f; 
prbl[10][19].PRy = 0.27433628f; 
prbl[10][20].PRx = 0.2850877f; 
prbl[10][20].PRy = 0.2803398f; 
prbl[10][21].PRx = 0.28003004f; 
prbl[10][21].PRy = 0.28125f; 
prbl[10][22].PRx = 0.29402515f; 
prbl[10][22].PRy = 0.28537735f; 
prbl[10][23].PRx = 0.29469696f; 
prbl[10][23].PRy = 0.28555554f; 
prbl[10][24].PRx = 0.3340336f; 
prbl[10][24].PRy = 0.32738096f; 
prbl[10][25].PRx = 0.29618475f; 
prbl[10][25].PRy = 0.30635247f; 
prbl[10][26].PRx = 0.29961833f; 
prbl[10][26].PRy = 0.309375f; 
prbl[10][27].PRx = 0.31368822f; 
prbl[10][27].PRy = 0.30405405f; 
prbl[10][28].PRx = 0.28042763f; 
prbl[10][28].PRy = 0.27923387f; 
prbl[10][29].PRx = 0.2875f; 
prbl[10][29].PRy = 0.285f; 
prbl[10][30].PRx = 0.27992278f; 
prbl[10][30].PRy = 0.2850679f; 
prbl[10][31].PRx = 0.2797619f; 
prbl[10][31].PRy = 0.28625953f; 
prbl[10][32].PRx = 0.28623188f; 
prbl[10][32].PRy = 0.2857143f; 

prbl[11][0].PRx = 0.27192983f; 
prbl[11][0].PRy = 0.26666668f; 
prbl[11][1].PRx = 0.28894928f; 
prbl[11][1].PRy = 0.28688523f; 
prbl[11][2].PRx = 0.29646018f; 
prbl[11][2].PRy = 0.29875f; 
prbl[11][3].PRx = 0.38247862f; 
prbl[11][3].PRy = 0.3765528f; 
prbl[11][4].PRx = 0.49678662f; 
prbl[11][4].PRy = 0.47545218f; 
prbl[11][5].PRx = 0.39042208f; 
prbl[11][5].PRy = 0.35159817f; 
prbl[11][6].PRx = 0.3202555f; 
prbl[11][6].PRy = 0.2920792f; 
prbl[11][7].PRx = 0.32331377f; 
prbl[11][7].PRy = 0.31164384f; 
prbl[11][8].PRx = 0.41096866f; 
prbl[11][8].PRy = 0.39251208f; 
prbl[11][9].PRx = 0.4205163f; 
prbl[11][9].PRy = 0.43666667f; 
prbl[11][10].PRx = 0.43935928f; 
prbl[11][10].PRy = 0.4417373f; 
prbl[11][11].PRx = 0.41747573f; 
prbl[11][11].PRy = 0.4448357f; 
prbl[11][12].PRx = 0.38793105f; 
prbl[11][12].PRy = 0.38519314f; 
prbl[11][13].PRx = 0.29871324f; 
prbl[11][13].PRy = 0.2993421f; 
prbl[11][14].PRx = 0.29181185f; 
prbl[11][14].PRy = 0.29866412f; 
prbl[11][15].PRx = 0.29396984f; 
prbl[11][15].PRy = 0.2977387f; 
prbl[11][16].PRx = 0.45625f; 
prbl[11][16].PRy = 0.45572916f; 
prbl[11][17].PRx = 0.38920453f; 
prbl[11][17].PRy = 0.39827585f; 
prbl[11][18].PRx = 0.2855153f; 
prbl[11][18].PRy = 0.2784553f; 
prbl[11][19].PRx = 0.28458214f; 
prbl[11][19].PRy = 0.28656125f; 
prbl[11][20].PRx = 0.2716418f; 
prbl[11][20].PRy = 0.27916667f; 
prbl[11][21].PRx = 0.2877907f; 
prbl[11][21].PRy = 0.2747934f; 
prbl[11][22].PRx = 0.2828125f; 
prbl[11][22].PRy = 0.2907489f; 
prbl[11][23].PRx = 0.2921875f; 
prbl[11][23].PRy = 0.27880183f; 
prbl[11][24].PRx = 0.28967065f; 
prbl[11][24].PRy = 0.27968037f; 
prbl[11][25].PRx = 0.27945402f; 
prbl[11][25].PRy = 0.27745098f; 
prbl[11][26].PRx = 0.29614094f; 
prbl[11][26].PRy = 0.27540106f; 
prbl[11][27].PRx = 0.29866412f; 
prbl[11][27].PRy = 0.2847222f; 
prbl[11][28].PRx = 0.2869775f; 
prbl[11][28].PRy = 0.28458497f; 
prbl[11][29].PRx = 0.28494623f; 
prbl[11][29].PRy = 0.28707626f; 
prbl[11][30].PRx = 0.2842742f; 
prbl[11][30].PRy = 0.28283897f; 
prbl[11][31].PRx = 0.27311644f; 
prbl[11][31].PRy = 0.27834007f; 
prbl[11][32].PRx = 0.28030303f; 
prbl[11][32].PRy = 0.2804348f; 

prbl[12][0].PRx = 0.26179245f; 
prbl[12][0].PRy = 0.28333333f; 
prbl[12][1].PRx = 0.28651685f; 
prbl[12][1].PRy = 0.29273504f; 
prbl[12][2].PRx = 0.33766234f; 
prbl[12][2].PRy = 0.33102766f; 
prbl[12][3].PRx = 0.52905405f; 
prbl[12][3].PRy = 0.52094597f; 
prbl[12][4].PRx = 0.4742647f; 
prbl[12][4].PRy = 0.46398306f; 
prbl[12][5].PRx = 0.35289115f; 
prbl[12][5].PRy = 0.325419f; 
prbl[12][6].PRx = 0.3141264f; 
prbl[12][6].PRy = 0.30092594f; 
prbl[12][7].PRx = 0.33601287f; 
prbl[12][7].PRy = 0.32651246f; 
prbl[12][8].PRx = 0.3341346f; 
prbl[12][8].PRy = 0.3016055f; 
prbl[12][9].PRx = 0.52697366f; 
prbl[12][9].PRy = 0.51898736f; 
prbl[12][10].PRx = 0.35151008f; 
prbl[12][10].PRy = 0.37565446f; 
prbl[12][11].PRx = 0.4456522f; 
prbl[12][11].PRy = 0.44133574f; 
prbl[12][12].PRx = 0.4047619f; 
prbl[12][12].PRy = 0.38084114f; 
prbl[12][13].PRx = 0.39036313f; 
prbl[12][13].PRy = 0.38209608f; 
prbl[12][14].PRx = 0.39440992f; 
prbl[12][14].PRy = 0.39019608f; 
prbl[12][15].PRx = 0.36694914f; 
prbl[12][15].PRy = 0.373444f; 
prbl[12][16].PRx = 0.4523196f; 
prbl[12][16].PRy = 0.48425925f; 
prbl[12][17].PRx = 0.48591548f; 
prbl[12][17].PRy = 0.52246094f; 
prbl[12][18].PRx = 0.37567204f; 
prbl[12][18].PRy = 0.37770563f; 
prbl[12][19].PRx = 0.28757668f; 
prbl[12][19].PRy = 0.28799018f; 
prbl[12][20].PRx = 0.28671876f; 
prbl[12][20].PRy = 0.29712042f; 
prbl[12][21].PRx = 0.2859477f; 
prbl[12][21].PRy = 0.2845622f; 
prbl[12][22].PRx = 0.28246754f; 
prbl[12][22].PRy = 0.2807377f; 
prbl[12][23].PRx = 0.29231974f; 
prbl[12][23].PRy = 0.297561f; 
prbl[12][24].PRx = 0.28451177f; 
prbl[12][24].PRy = 0.28586066f; 
prbl[12][25].PRx = 0.28161764f; 
prbl[12][25].PRy = 0.28467155f; 
prbl[12][26].PRx = 0.30438933f; 
prbl[12][26].PRy = 0.27121213f; 
prbl[12][27].PRx = 0.32669324f; 
prbl[12][27].PRy = 0.30508474f; 
prbl[12][28].PRx = 0.2862069f; 
prbl[12][28].PRy = 0.2767094f; 
prbl[12][29].PRx = 0.28665414f; 
prbl[12][29].PRy = 0.28061223f; 
prbl[12][30].PRx = 0.2745614f; 
prbl[12][30].PRy = 0.27083334f; 
prbl[12][31].PRx = 0.27982455f; 
prbl[12][31].PRy = 0.2801205f; 
prbl[12][32].PRx = 0.2777778f; 
prbl[12][32].PRy = 0.2784553f; 

prbl[13][0].PRx = 0.2807971f; 
prbl[13][0].PRy = 0.28663793f; 
prbl[13][1].PRx = 0.2787162f; 
prbl[13][1].PRy = 0.28150406f; 
prbl[13][2].PRx = 0.46192053f; 
prbl[13][2].PRy = 0.4834802f; 
prbl[13][3].PRx = 0.54318184f; 
prbl[13][3].PRy = 0.52478135f; 
prbl[13][4].PRx = 0.43951613f; 
prbl[13][4].PRy = 0.4248366f; 
prbl[13][5].PRx = 0.32f; 
prbl[13][5].PRy = 0.3050239f; 
prbl[13][6].PRx = 0.34790874f; 
prbl[13][6].PRy = 0.34607437f; 
prbl[13][7].PRx = 0.3298193f; 
prbl[13][7].PRy = 0.32871974f; 
prbl[13][8].PRx = 0.3493976f; 
prbl[13][8].PRy = 0.3323293f; 
prbl[13][9].PRx = 0.45143884f; 
prbl[13][9].PRy = 0.42424244f; 
prbl[13][10].PRx = 0.3864985f; 
prbl[13][10].PRy = 0.40816328f; 
prbl[13][11].PRx = 0.3506192f; 
prbl[13][11].PRy = 0.35326087f; 
prbl[13][12].PRx = 0.43733335f; 
prbl[13][12].PRy = 0.45527524f; 
prbl[13][13].PRx = 0.3895664f; 
prbl[13][13].PRy = 0.37443438f; 
prbl[13][14].PRx = 0.38012618f; 
prbl[13][14].PRy = 0.35308057f; 
prbl[13][15].PRx = 0.38896278f; 
prbl[13][15].PRy = 0.41483516f; 
prbl[13][16].PRx = 0.44195047f; 
prbl[13][16].PRy = 0.43699187f; 
prbl[13][17].PRx = 0.37860084f; 
prbl[13][17].PRy = 0.39615384f; 
prbl[13][18].PRx = 0.4238827f; 
prbl[13][18].PRy = 0.4227273f; 
prbl[13][19].PRx = 0.28279883f; 
prbl[13][19].PRy = 0.2838983f; 
prbl[13][20].PRx = 0.28798342f; 
prbl[13][20].PRy = 0.28451884f; 
prbl[13][21].PRx = 0.2913961f; 
prbl[13][21].PRy = 0.2877907f; 
prbl[13][22].PRx = 0.28972602f; 
prbl[13][22].PRy = 0.2857143f; 
prbl[13][23].PRx = 0.28990227f; 
prbl[13][23].PRy = 0.2857143f; 
prbl[13][24].PRx = 0.27686566f; 
prbl[13][24].PRy = 0.2734657f; 
prbl[13][25].PRx = 0.29100946f; 
prbl[13][25].PRy = 0.29216868f; 
prbl[13][26].PRx = 0.3034483f; 
prbl[13][26].PRy = 0.28125f; 
prbl[13][27].PRx = 0.31465518f; 
prbl[13][27].PRy = 0.28731343f; 
prbl[13][28].PRx = 0.28197673f; 
prbl[13][28].PRy = 0.28600824f; 
prbl[13][29].PRx = 0.28448275f; 
prbl[13][29].PRy = 0.28448275f; 
prbl[13][30].PRx = 0.28707224f; 
prbl[13][30].PRy = 0.28733033f; 
prbl[13][31].PRx = 0.28264332f; 
prbl[13][31].PRy = 0.2751004f; 
prbl[13][32].PRx = 0.2951389f; 
prbl[13][32].PRy = 0.28483605f; 

prbl[14][0].PRx = 0.28804347f; 
prbl[14][0].PRy = 0.268018f; 
prbl[14][1].PRx = 0.31914893f; 
prbl[14][1].PRy = 0.30873495f; 
prbl[14][2].PRx = 0.4074074f; 
prbl[14][2].PRy = 0.41221374f; 
prbl[14][3].PRx = 0.44701493f; 
prbl[14][3].PRy = 0.48228347f; 
prbl[14][4].PRx = 0.4219243f; 
prbl[14][4].PRy = 0.40625f; 
prbl[14][5].PRx = 0.33539096f; 
prbl[14][5].PRy = 0.32253885f; 
prbl[14][6].PRx = 0.33096084f; 
prbl[14][6].PRy = 0.32224333f; 
prbl[14][7].PRx = 0.32242063f; 
prbl[14][7].PRy = 0.32142857f; 
prbl[14][8].PRx = 0.3601804f; 
prbl[14][8].PRy = 0.34375f; 
prbl[14][9].PRx = 0.3901099f; 
prbl[14][9].PRy = 0.3448718f; 
prbl[14][10].PRx = 0.44646925f; 
prbl[14][10].PRy = 0.4312227f; 
prbl[14][11].PRx = 0.39935064f; 
prbl[14][11].PRy = 0.40796703f; 
prbl[14][12].PRx = 0.36466667f; 
prbl[14][12].PRy = 0.3446215f; 
prbl[14][13].PRx = 0.45640326f; 
prbl[14][13].PRy = 0.42142856f; 
prbl[14][14].PRx = 0.4216954f; 
prbl[14][14].PRy = 0.3955224f; 
prbl[14][15].PRx = 0.4564394f; 
prbl[14][15].PRy = 0.46301776f; 
prbl[14][16].PRx = 0.40810812f; 
prbl[14][16].PRy = 0.45833334f; 
prbl[14][17].PRx = 0.28755867f; 
prbl[14][17].PRy = 0.29130435f; 
prbl[14][18].PRx = 0.41184574f; 
prbl[14][18].PRy = 0.4035326f; 
prbl[14][19].PRx = 0.28812316f; 
prbl[14][19].PRy = 0.2857143f; 
prbl[14][20].PRx = 0.29270187f; 
prbl[14][20].PRy = 0.28557312f; 
prbl[14][21].PRx = 0.27887538f; 
prbl[14][21].PRy = 0.2874396f; 
prbl[14][22].PRx = 0.2875f; 
prbl[14][22].PRy = 0.28466386f; 
prbl[14][23].PRx = 0.30936456f; 
prbl[14][23].PRy = 0.29187194f; 
prbl[14][24].PRx = 0.29261363f; 
prbl[14][24].PRy = 0.29132232f; 
prbl[14][25].PRx = 0.28362572f; 
prbl[14][25].PRy = 0.2777778f; 
prbl[14][26].PRx = 0.29166666f; 
prbl[14][26].PRy = 0.27738094f; 
prbl[14][27].PRx = 0.30451128f; 
prbl[14][27].PRy = 0.28787878f; 
prbl[14][28].PRx = 0.29069766f; 
prbl[14][28].PRy = 0.28605768f; 
prbl[14][29].PRx = 0.27731788f; 
prbl[14][29].PRy = 0.27579364f; 
prbl[14][30].PRx = 0.28825623f; 
prbl[14][30].PRy = 0.29017857f; 
prbl[14][31].PRx = 0.289966f; 
prbl[14][31].PRy = 0.28923768f; 
prbl[14][32].PRx = 0.27892563f; 
prbl[14][32].PRy = 0.28125f; 

prbl[15][0].PRx = 0.2806604f; 
prbl[15][0].PRy = 0.26404494f; 
prbl[15][1].PRx = 0.3876953f; 
prbl[15][1].PRy = 0.38601035f; 
prbl[15][2].PRx = 0.3229927f; 
prbl[15][2].PRy = 0.34625f; 
prbl[15][3].PRx = 0.27881357f; 
prbl[15][3].PRy = 0.2849741f; 
prbl[15][4].PRx = 0.3493266f; 
prbl[15][4].PRy = 0.33139536f; 
prbl[15][5].PRx = 0.40833333f; 
prbl[15][5].PRy = 0.4222973f; 
prbl[15][6].PRx = 0.3975f; 
prbl[15][6].PRy = 0.410066f; 
prbl[15][7].PRx = 0.38877118f; 
prbl[15][7].PRy = 0.3934708f; 
prbl[15][8].PRx = 0.533179f; 
prbl[15][8].PRy = 0.56064355f; 
prbl[15][9].PRx = 0.45822942f; 
prbl[15][9].PRy = 0.4588015f; 
prbl[15][10].PRx = 0.44080603f; 
prbl[15][10].PRy = 0.4054307f; 
prbl[15][11].PRx = 0.39791185f; 
prbl[15][11].PRy = 0.38073394f; 
prbl[15][12].PRx = 0.4280397f; 
prbl[15][12].PRy = 0.41783217f; 
prbl[15][13].PRx = 0.41450778f; 
prbl[15][13].PRy = 0.40448114f; 
prbl[15][14].PRx = 0.43935645f; 
prbl[15][14].PRy = 0.44881305f; 
prbl[15][15].PRx = 0.50449103f; 
prbl[15][15].PRy = 0.5067568f; 
prbl[15][16].PRx = 0.43093923f; 
prbl[15][16].PRy = 0.43867925f; 
prbl[15][17].PRx = 0.33072916f; 
prbl[15][17].PRy = 0.31656805f; 
prbl[15][18].PRx = 0.40426996f; 
prbl[15][18].PRy = 0.36432162f; 
prbl[15][19].PRx = 0.2881356f; 
prbl[15][19].PRy = 0.28020832f; 
prbl[15][20].PRx = 0.27844313f; 
prbl[15][20].PRy = 0.27606177f; 
prbl[15][21].PRx = 0.28482974f; 
prbl[15][21].PRy = 0.2845982f; 
prbl[15][22].PRx = 0.29318938f; 
prbl[15][22].PRy = 0.28556034f; 
prbl[15][23].PRx = 0.2927046f; 
prbl[15][23].PRy = 0.2803398f; 
prbl[15][24].PRx = 0.28230336f; 
prbl[15][24].PRy = 0.28477442f; 
prbl[15][25].PRx = 0.28216374f; 
prbl[15][25].PRy = 0.28189301f; 
prbl[15][26].PRx = 0.2819489f; 
prbl[15][26].PRy = 0.27876106f; 
prbl[15][27].PRx = 0.29387417f; 
prbl[15][27].PRy = 0.28444445f; 
prbl[15][28].PRx = 0.2969858f; 
prbl[15][28].PRy = 0.30044842f; 
prbl[15][29].PRx = 0.28892735f; 
prbl[15][29].PRy = 0.2834008f; 
prbl[15][30].PRx = 0.28019324f; 
prbl[15][30].PRy = 0.29235536f; 
prbl[15][31].PRx = 0.2826923f; 
prbl[15][31].PRy = 0.28265765f; 
prbl[15][32].PRx = 0.2804054f; 
prbl[15][32].PRy = 0.28794643f; 

prbl[16][0].PRx = 0.27288732f; 
prbl[16][0].PRy = 0.27118644f; 
prbl[16][1].PRx = 0.37307692f; 
prbl[16][1].PRy = 0.37558138f; 
prbl[16][2].PRx = 0.2676282f; 
prbl[16][2].PRy = 0.25906736f; 
prbl[16][3].PRx = 0.29919356f; 
prbl[16][3].PRy = 0.27747253f; 
prbl[16][4].PRx = 0.36416668f; 
prbl[16][4].PRy = 0.32608697f; 
prbl[16][5].PRx = 0.625f; 
prbl[16][5].PRy = 0.70408165f; 
prbl[16][6].PRx = 0.68465227f; 
prbl[16][6].PRy = 0.687677f; 
prbl[16][7].PRx = 0.537037f; 
prbl[16][7].PRy = 0.5448113f; 
prbl[16][8].PRx = 0.6183674f; 
prbl[16][8].PRy = 0.6926752f; 
prbl[16][9].PRx = 0.44005102f; 
prbl[16][9].PRy = 0.40264422f; 
prbl[16][10].PRx = 0.3841912f; 
prbl[16][10].PRy = 0.3880435f; 
prbl[16][11].PRx = 0.40511727f; 
prbl[16][11].PRy = 0.38378906f; 
prbl[16][12].PRx = 0.3929504f; 
prbl[16][12].PRy = 0.36621094f; 
prbl[16][13].PRx = 0.36778116f; 
prbl[16][13].PRy = 0.34895834f; 
prbl[16][14].PRx = 0.45379147f; 
prbl[16][14].PRy = 0.42687073f; 
prbl[16][15].PRx = 0.5109091f; 
prbl[16][15].PRy = 0.55801105f; 
prbl[16][16].PRx = 0.4313031f; 
prbl[16][16].PRy = 0.44609666f; 
prbl[16][17].PRx = 0.35738832f; 
prbl[16][17].PRy = 0.36235955f; 
prbl[16][18].PRx = 0.41643837f; 
prbl[16][18].PRy = 0.40449437f; 
prbl[16][19].PRx = 0.33086053f; 
prbl[16][19].PRy = 0.33133972f; 
prbl[16][20].PRx = 0.28635016f; 
prbl[16][20].PRy = 0.28333333f; 
prbl[16][21].PRx = 0.28526646f; 
prbl[16][21].PRy = 0.2886266f; 
prbl[16][22].PRx = 0.28583062f; 
prbl[16][22].PRy = 0.27302632f; 
prbl[16][23].PRx = 0.28721684f; 
prbl[16][23].PRy = 0.27109703f; 
prbl[16][24].PRx = 0.28142858f; 
prbl[16][24].PRy = 0.29166666f; 
prbl[16][25].PRx = 0.27850163f; 
prbl[16][25].PRy = 0.2763713f; 
prbl[16][26].PRx = 0.28674123f; 
prbl[16][26].PRy = 0.2713568f; 
prbl[16][27].PRx = 0.28674123f; 
prbl[16][27].PRy = 0.27765486f; 
prbl[16][28].PRx = 0.29479167f; 
prbl[16][28].PRy = 0.29069766f; 
prbl[16][29].PRx = 0.30733082f; 
prbl[16][29].PRy = 0.30472103f; 
prbl[16][30].PRx = 0.30776516f; 
prbl[16][30].PRy = 0.29497355f; 
prbl[16][31].PRx = 0.29722223f; 
prbl[16][31].PRy = 0.28901735f; 
prbl[16][32].PRx = 0.28333333f; 
prbl[16][32].PRy = 0.28440368f; 

prbl[17][0].PRx = 0.28f; 
prbl[17][0].PRy = 0.2777778f; 
prbl[17][1].PRx = 0.3844086f; 
prbl[17][1].PRy = 0.39248705f; 
prbl[17][2].PRx = 0.28816795f; 
prbl[17][2].PRy = 0.28323698f; 
prbl[17][3].PRx = 0.32464027f; 
prbl[17][3].PRy = 0.2997076f; 
prbl[17][4].PRx = 0.34219858f; 
prbl[17][4].PRy = 0.33544305f; 
prbl[17][5].PRx = 0.41351745f; 
prbl[17][5].PRy = 0.44266054f; 
prbl[17][6].PRx = 0.33237547f; 
prbl[17][6].PRy = 0.34567901f; 
prbl[17][7].PRx = 0.33687943f; 
prbl[17][7].PRy = 0.365625f; 
prbl[17][8].PRx = 0.47173914f; 
prbl[17][8].PRy = 0.54049295f; 
prbl[17][9].PRx = 0.41992664f; 
prbl[17][9].PRy = 0.39735773f; 
prbl[17][10].PRx = 0.3677686f; 
prbl[17][10].PRy = 0.3469163f; 
prbl[17][11].PRx = 0.42880794f; 
prbl[17][11].PRy = 0.41753927f; 
prbl[17][12].PRx = 0.41666666f; 
prbl[17][12].PRy = 0.39186507f; 
prbl[17][13].PRx = 0.3659091f; 
prbl[17][13].PRy = 0.3514235f; 
prbl[17][14].PRx = 0.4347826f; 
prbl[17][14].PRy = 0.41758242f; 
prbl[17][15].PRx = 0.43133804f; 
prbl[17][15].PRy = 0.453125f; 
prbl[17][16].PRx = 0.43788344f; 
prbl[17][16].PRy = 0.4475945f; 
prbl[17][17].PRx = 0.3003876f; 
prbl[17][17].PRy = 0.29601228f; 
prbl[17][18].PRx = 0.34848484f; 
prbl[17][18].PRy = 0.32734805f; 
prbl[17][19].PRx = 0.36567163f; 
prbl[17][19].PRy = 0.33222222f; 
prbl[17][20].PRx = 0.2952586f; 
prbl[17][20].PRy = 0.28645834f; 
prbl[17][21].PRx = 0.2863924f; 
prbl[17][21].PRy = 0.28070176f; 
prbl[17][22].PRx = 0.29285714f; 
prbl[17][22].PRy = 0.27838427f; 
prbl[17][23].PRx = 0.30032468f; 
prbl[17][23].PRy = 0.28703704f; 
prbl[17][24].PRx = 0.280597f; 
prbl[17][24].PRy = 0.28303573f; 
prbl[17][25].PRx = 0.2901515f; 
prbl[17][25].PRy = 0.28441295f; 
prbl[17][26].PRx = 0.29401994f; 
prbl[17][26].PRy = 0.29261363f; 
prbl[17][27].PRx = 0.2872024f; 
prbl[17][27].PRy = 0.2710084f; 
prbl[17][28].PRx = 0.27722773f; 
prbl[17][28].PRy = 0.269958f; 
prbl[17][29].PRx = 0.2877193f; 
prbl[17][29].PRy = 0.28899083f; 
prbl[17][30].PRx = 0.3137417f; 
prbl[17][30].PRy = 0.2933526f; 
prbl[17][31].PRx = 0.28359684f; 
prbl[17][31].PRy = 0.28784403f; 
prbl[17][32].PRx = 0.3029661f; 
prbl[17][32].PRy = 0.29395604f; 

prbl[18][0].PRx = 0.28318584f; 
prbl[18][0].PRy = 0.28440368f; 
prbl[18][1].PRx = 0.3380783f; 
prbl[18][1].PRy = 0.30780348f; 
prbl[18][2].PRx = 0.27520162f; 
prbl[18][2].PRy = 0.27707008f; 
prbl[18][3].PRx = 0.3768116f; 
prbl[18][3].PRy = 0.3427673f; 
prbl[18][4].PRx = 0.33243728f; 
prbl[18][4].PRy = 0.32947975f; 
prbl[18][5].PRx = 0.27203065f; 
prbl[18][5].PRy = 0.2672414f; 
prbl[18][6].PRx = 0.33396947f; 
prbl[18][6].PRy = 0.36578947f; 
prbl[18][7].PRx = 0.25326797f; 
prbl[18][7].PRy = 0.25f; 
prbl[18][8].PRx = 0.36607143f; 
prbl[18][8].PRy = 0.3327338f; 
prbl[18][9].PRx = 0.37190083f; 
prbl[18][9].PRy = 0.3603679f; 
prbl[18][10].PRx = 0.38114753f; 
prbl[18][10].PRy = 0.35321102f; 
prbl[18][11].PRx = 0.4484127f; 
prbl[18][11].PRy = 0.4189189f; 
prbl[18][12].PRx = 0.43694195f; 
prbl[18][12].PRy = 0.37978724f; 
prbl[18][13].PRx = 0.36675823f; 
prbl[18][13].PRy = 0.36042404f; 
prbl[18][14].PRx = 0.4955128f; 
prbl[18][14].PRy = 0.50446427f; 
prbl[18][15].PRx = 0.39248252f; 
prbl[18][15].PRy = 0.41463414f; 
prbl[18][16].PRx = 0.3950893f; 
prbl[18][16].PRy = 0.426f; 
prbl[18][17].PRx = 0.2848432f; 
prbl[18][17].PRy = 0.2826087f; 
prbl[18][18].PRx = 0.26556018f; 
prbl[18][18].PRy = 0.25529101f; 
prbl[18][19].PRx = 0.39908257f; 
prbl[18][19].PRy = 0.40186915f; 
prbl[18][20].PRx = 0.2935657f; 
prbl[18][20].PRy = 0.28640777f; 
prbl[18][21].PRx = 0.2897898f; 
prbl[18][21].PRy = 0.27904564f; 
prbl[18][22].PRx = 0.29288027f; 
prbl[18][22].PRy = 0.2792887f; 
prbl[18][23].PRx = 0.2972136f; 
prbl[18][23].PRy = 0.2751479f; 
prbl[18][24].PRx = 0.2816265f; 
prbl[18][24].PRy = 0.28036436f; 
prbl[18][25].PRx = 0.27859476f; 
prbl[18][25].PRy = 0.28097346f; 
prbl[18][26].PRx = 0.29863483f; 
prbl[18][26].PRy = 0.28783783f; 
prbl[18][27].PRx = 0.29123712f; 
prbl[18][27].PRy = 0.27826086f; 
prbl[18][28].PRx = 0.28894928f; 
prbl[18][28].PRy = 0.28301886f; 
prbl[18][29].PRx = 0.28963414f; 
prbl[18][29].PRy = 0.27333334f; 
prbl[18][30].PRx = 0.29516128f; 
prbl[18][30].PRy = 0.27894738f; 
prbl[18][31].PRx = 0.28584906f; 
prbl[18][31].PRy = 0.28588516f; 
prbl[18][32].PRx = 0.2908654f; 
prbl[18][32].PRy = 0.28947368f; 

prbl[19][0].PRx = 0.4076923f; 
prbl[19][0].PRy = 0.3668478f; 
prbl[19][1].PRx = 0.3030303f; 
prbl[19][1].PRy = 0.30675676f; 
prbl[19][2].PRx = 0.25473934f; 
prbl[19][2].PRy = 0.25182483f; 
prbl[19][3].PRx = 0.43677044f; 
prbl[19][3].PRy = 0.43035713f; 
prbl[19][4].PRx = 0.33015266f; 
prbl[19][4].PRy = 0.3220859f; 
prbl[19][5].PRx = 0.25803214f; 
prbl[19][5].PRy = 0.25862068f; 
prbl[19][6].PRx = 0.27593362f; 
prbl[19][6].PRy = 0.2834507f; 
prbl[19][7].PRx = 0.25714287f; 
prbl[19][7].PRy = 0.2596154f; 
prbl[19][8].PRx = 0.39022663f; 
prbl[19][8].PRy = 0.33497536f; 
prbl[19][9].PRx = 0.378866f; 
prbl[19][9].PRy = 0.35487288f; 
prbl[19][10].PRx = 0.40304878f; 
prbl[19][10].PRy = 0.36422414f; 
prbl[19][11].PRx = 0.3875969f; 
prbl[19][11].PRy = 0.34355828f; 
prbl[19][12].PRx = 0.39371258f; 
prbl[19][12].PRy = 0.35953608f; 
prbl[19][13].PRx = 0.39168936f; 
prbl[19][13].PRy = 0.38618678f; 
prbl[19][14].PRx = 0.5254692f; 
prbl[19][14].PRy = 0.5259009f; 
prbl[19][15].PRx = 0.44715446f; 
prbl[19][15].PRy = 0.45692885f; 
prbl[19][16].PRx = 0.3732639f; 
prbl[19][16].PRy = 0.40206185f; 
prbl[19][17].PRx = 0.3201581f; 
prbl[19][17].PRy = 0.31598985f; 
prbl[19][18].PRx = 0.29453442f; 
prbl[19][18].PRy = 0.3018617f; 
prbl[19][19].PRx = 0.3783784f; 
prbl[19][19].PRy = 0.37563452f; 
prbl[19][20].PRx = 0.34556785f; 
prbl[19][20].PRy = 0.34806034f; 
prbl[19][21].PRx = 0.28405574f; 
prbl[19][21].PRy = 0.27753305f; 
prbl[19][22].PRx = 0.29596773f; 
prbl[19][22].PRy = 0.28502417f; 
prbl[19][23].PRx = 0.28853047f; 
prbl[19][23].PRy = 0.2784091f; 
prbl[19][24].PRx = 0.2862776f; 
prbl[19][24].PRy = 0.2804878f; 
prbl[19][25].PRx = 0.28560126f; 
prbl[19][25].PRy = 0.28414097f; 
prbl[19][26].PRx = 0.3140625f; 
prbl[19][26].PRy = 0.28631285f; 
prbl[19][27].PRx = 0.2855987f; 
prbl[19][27].PRy = 0.27422908f; 
prbl[19][28].PRx = 0.27728873f; 
prbl[19][28].PRy = 0.28218883f; 
prbl[19][29].PRx = 0.29109588f; 
prbl[19][29].PRy = 0.27662036f; 
prbl[19][30].PRx = 0.29954955f; 
prbl[19][30].PRy = 0.28605768f; 
prbl[19][31].PRx = 0.28846154f; 
prbl[19][31].PRy = 0.28341585f; 
prbl[19][32].PRx = 0.28719008f; 
prbl[19][32].PRy = 0.2939815f; 

prbl[20][0].PRx = 0.4732143f; 
prbl[20][0].PRy = 0.4707207f; 
prbl[20][1].PRx = 0.28942308f; 
prbl[20][1].PRy = 0.28246754f; 
prbl[20][2].PRx = 0.255137f; 
prbl[20][2].PRy = 0.25f; 
prbl[20][3].PRx = 0.3966431f; 
prbl[20][3].PRy = 0.34509203f; 
prbl[20][4].PRx = 0.35714287f; 
prbl[20][4].PRy = 0.35248446f; 
prbl[20][5].PRx = 0.26494023f; 
prbl[20][5].PRy = 0.25892857f; 
prbl[20][6].PRx = 0.2779188f; 
prbl[20][6].PRy = 0.2867647f; 
prbl[20][7].PRx = 0.2966321f; 
prbl[20][7].PRy = 0.29285714f; 
prbl[20][8].PRx = 0.3907563f; 
prbl[20][8].PRy = 0.34388646f; 
prbl[20][9].PRx = 0.37310606f; 
prbl[20][9].PRy = 0.35588235f; 
prbl[20][10].PRx = 0.35561496f; 
prbl[20][10].PRy = 0.34163347f; 
prbl[20][11].PRx = 0.33173078f; 
prbl[20][11].PRy = 0.31220096f; 
prbl[20][12].PRx = 0.40055248f; 
prbl[20][12].PRy = 0.42329547f; 
prbl[20][13].PRx = 0.35313317f; 
prbl[20][13].PRy = 0.3114525f; 
prbl[20][14].PRx = 0.47610295f; 
prbl[20][14].PRy = 0.45465687f; 
prbl[20][15].PRx = 0.359375f; 
prbl[20][15].PRy = 0.3495066f; 
prbl[20][16].PRx = 0.29950494f; 
prbl[20][16].PRy = 0.31066945f; 
prbl[20][17].PRx = 0.34332192f; 
prbl[20][17].PRy = 0.34543568f; 
prbl[20][18].PRx = 0.3402527f; 
prbl[20][18].PRy = 0.33583692f; 
prbl[20][19].PRx = 0.33577406f; 
prbl[20][19].PRy = 0.34192824f; 
prbl[20][20].PRx = 0.39465874f; 
prbl[20][20].PRy = 0.41017315f; 
prbl[20][21].PRx = 0.2900641f; 
prbl[20][21].PRy = 0.28174603f; 
prbl[20][22].PRx = 0.2851711f; 
prbl[20][22].PRy = 0.27734375f; 
prbl[20][23].PRx = 0.34656653f; 
prbl[20][23].PRy = 0.33659217f; 
prbl[20][24].PRx = 0.28154573f; 
prbl[20][24].PRy = 0.2739726f; 
prbl[20][25].PRx = 0.2893082f; 
prbl[20][25].PRy = 0.27978724f; 
prbl[20][26].PRx = 0.30305466f; 
prbl[20][26].PRy = 0.2855392f; 
prbl[20][27].PRx = 0.29166666f; 
prbl[20][27].PRy = 0.285f; 
prbl[20][28].PRx = 0.279f; 
prbl[20][28].PRy = 0.2761905f; 
prbl[20][29].PRx = 0.29310346f; 
prbl[20][29].PRy = 0.2804878f; 
prbl[20][30].PRx = 0.29765624f; 
prbl[20][30].PRy = 0.27664974f; 
prbl[20][31].PRx = 0.29766536f; 
prbl[20][31].PRy = 0.28928572f; 
prbl[20][32].PRx = 0.28219697f; 
prbl[20][32].PRy = 0.28830644f; 

prbl[21][0].PRx = 0.3779412f; 
prbl[21][0].PRy = 0.32471263f; 
prbl[21][1].PRx = 0.3024911f; 
prbl[21][1].PRy = 0.30555555f; 
prbl[21][2].PRx = 0.28270042f; 
prbl[21][2].PRy = 0.26236263f; 
prbl[21][3].PRx = 0.41666666f; 
prbl[21][3].PRy = 0.4268617f; 
prbl[21][4].PRx = 0.3153527f; 
prbl[21][4].PRy = 0.30612245f; 
prbl[21][5].PRx = 0.25315127f; 
prbl[21][5].PRy = 0.25588235f; 
prbl[21][6].PRx = 0.2784091f; 
prbl[21][6].PRy = 0.2847222f; 
prbl[21][7].PRx = 0.31051588f; 
prbl[21][7].PRy = 0.29696134f; 
prbl[21][8].PRx = 0.31384614f; 
prbl[21][8].PRy = 0.30475205f; 
prbl[21][9].PRx = 0.43024692f; 
prbl[21][9].PRy = 0.39007092f; 
prbl[21][10].PRx = 0.35151935f; 
prbl[21][10].PRy = 0.33906883f; 
prbl[21][11].PRx = 0.3368946f; 
prbl[21][11].PRy = 0.33139536f; 
prbl[21][12].PRx = 0.43489584f; 
prbl[21][12].PRy = 0.42194092f; 
prbl[21][13].PRx = 0.3696884f; 
prbl[21][13].PRy = 0.35064936f; 
prbl[21][14].PRx = 0.2898671f; 
prbl[21][14].PRy = 0.28353658f; 
prbl[21][15].PRx = 0.3771777f; 
prbl[21][15].PRy = 0.359375f; 
prbl[21][16].PRx = 0.35914454f; 
prbl[21][16].PRy = 0.37346938f; 
prbl[21][17].PRx = 0.33737865f; 
prbl[21][17].PRy = 0.33061224f; 
prbl[21][18].PRx = 0.31741574f; 
prbl[21][18].PRy = 0.32427537f; 
prbl[21][19].PRx = 0.31944445f; 
prbl[21][19].PRy = 0.3248175f; 
prbl[21][20].PRx = 0.38402778f; 
prbl[21][20].PRy = 0.39260563f; 
prbl[21][21].PRx = 0.31617647f; 
prbl[21][21].PRy = 0.32879376f; 
prbl[21][22].PRx = 0.32899022f; 
prbl[21][22].PRy = 0.31456044f; 
prbl[21][23].PRx = 0.32522124f; 
prbl[21][23].PRy = 0.32162923f; 
prbl[21][24].PRx = 0.28583062f; 
prbl[21][24].PRy = 0.28243244f; 
prbl[21][25].PRx = 0.2824367f; 
prbl[21][25].PRy = 0.28605768f; 
prbl[21][26].PRx = 0.29333332f; 
prbl[21][26].PRy = 0.28522727f; 
prbl[21][27].PRx = 0.28942952f; 
prbl[21][27].PRy = 0.28024194f; 
prbl[21][28].PRx = 0.29150578f; 
prbl[21][28].PRy = 0.29331684f; 
prbl[21][29].PRx = 0.29496402f; 
prbl[21][29].PRy = 0.28603604f; 
prbl[21][30].PRx = 0.28113878f; 
prbl[21][30].PRy = 0.27380952f; 
prbl[21][31].PRx = 0.29181495f; 
prbl[21][31].PRy = 0.3004292f; 
prbl[21][32].PRx = 0.28968254f; 
prbl[21][32].PRy = 0.2837838f; 

prbl[22][0].PRx = 0.35305342f; 
prbl[22][0].PRy = 0.36666667f; 
prbl[22][1].PRx = 0.33f; 
prbl[22][1].PRy = 0.32594937f; 
prbl[22][2].PRx = 0.28381148f; 
prbl[22][2].PRy = 0.2792208f; 
prbl[22][3].PRx = 0.3753894f; 
prbl[22][3].PRy = 0.34142858f; 
prbl[22][4].PRx = 0.2931416f; 
prbl[22][4].PRy = 0.2996988f; 
prbl[22][5].PRx = 0.30454546f; 
prbl[22][5].PRy = 0.31213018f; 
prbl[22][6].PRx = 0.26687765f; 
prbl[22][6].PRy = 0.27526596f; 
prbl[22][7].PRx = 0.34141275f; 
prbl[22][7].PRy = 0.3372093f; 
prbl[22][8].PRx = 0.316156f; 
prbl[22][8].PRy = 0.31512606f; 
prbl[22][9].PRx = 0.34446254f; 
prbl[22][9].PRy = 0.31620553f; 
prbl[22][10].PRx = 0.33136094f; 
prbl[22][10].PRy = 0.30333334f; 
prbl[22][11].PRx = 0.34246576f; 
prbl[22][11].PRy = 0.3330116f; 
prbl[22][12].PRx = 0.39275363f; 
prbl[22][12].PRy = 0.37449798f; 
prbl[22][13].PRx = 0.3167116f; 
prbl[22][13].PRy = 0.295f; 
prbl[22][14].PRx = 0.28966007f; 
prbl[22][14].PRy = 0.2875458f; 
prbl[22][15].PRx = 0.35701755f; 
prbl[22][15].PRy = 0.3359375f; 
prbl[22][16].PRx = 0.40069687f; 
prbl[22][16].PRy = 0.38510638f; 
prbl[22][17].PRx = 0.34104937f; 
prbl[22][17].PRy = 0.32978722f; 
prbl[22][18].PRx = 0.34160304f; 
prbl[22][18].PRy = 0.33870968f; 
prbl[22][19].PRx = 0.3277592f; 
prbl[22][19].PRy = 0.32462686f; 
prbl[22][20].PRx = 0.32873377f; 
prbl[22][20].PRy = 0.29942966f; 
prbl[22][21].PRx = 0.31502524f; 
prbl[22][21].PRy = 0.30063292f; 
prbl[22][22].PRx = 0.3760388f; 
prbl[22][22].PRy = 0.3632653f; 
prbl[22][23].PRx = 0.295f; 
prbl[22][23].PRy = 0.2936321f; 
prbl[22][24].PRx = 0.27846974f; 
prbl[22][24].PRy = 0.28140098f; 
prbl[22][25].PRx = 0.28605017f; 
prbl[22][25].PRy = 0.28455284f; 
prbl[22][26].PRx = 0.28877887f; 
prbl[22][26].PRy = 0.28669724f; 
prbl[22][27].PRx = 0.29347825f; 
prbl[22][27].PRy = 0.29147464f; 
prbl[22][28].PRx = 0.29009435f; 
prbl[22][28].PRy = 0.2970297f; 
prbl[22][29].PRx = 0.29816514f; 
prbl[22][29].PRy = 0.29812834f; 
prbl[22][30].PRx = 0.2851064f; 
prbl[22][30].PRy = 0.2752809f; 
prbl[22][31].PRx = 0.29927886f; 
prbl[22][31].PRy = 0.2717949f; 
prbl[22][32].PRx = 0.29411766f; 
prbl[22][32].PRy = 0.27445653f; 

prbl[23][0].PRx = 0.2920792f; 
prbl[23][0].PRy = 0.28431374f; 
prbl[23][1].PRx = 0.33333334f; 
prbl[23][1].PRy = 0.29597703f; 
prbl[23][2].PRx = 0.28826532f; 
prbl[23][2].PRy = 0.27044025f; 
prbl[23][3].PRx = 0.45231214f; 
prbl[23][3].PRy = 0.4519774f; 
prbl[23][4].PRx = 0.3403846f; 
prbl[23][4].PRy = 0.33536586f; 
prbl[23][5].PRx = 0.308f; 
prbl[23][5].PRy = 0.32444444f; 
prbl[23][6].PRx = 0.30775318f; 
prbl[23][6].PRy = 0.29508197f; 
prbl[23][7].PRx = 0.30503145f; 
prbl[23][7].PRy = 0.30784315f; 
prbl[23][8].PRx = 0.32446808f; 
prbl[23][8].PRy = 0.32612783f; 
prbl[23][9].PRx = 0.30992508f; 
prbl[23][9].PRy = 0.31688964f; 
prbl[23][10].PRx = 0.3272727f; 
prbl[23][10].PRy = 0.31639004f; 
prbl[23][11].PRx = 0.30138037f; 
prbl[23][11].PRy = 0.30961537f; 
prbl[23][12].PRx = 0.3577044f; 
prbl[23][12].PRy = 0.34958506f; 
prbl[23][13].PRx = 0.29815865f; 
prbl[23][13].PRy = 0.2933213f; 
prbl[23][14].PRx = 0.27955273f; 
prbl[23][14].PRy = 0.29245284f; 
prbl[23][15].PRx = 0.37266356f; 
prbl[23][15].PRy = 0.35555556f; 
prbl[23][16].PRx = 0.37066475f; 
prbl[23][16].PRy = 0.3774272f; 
prbl[23][17].PRx = 0.3879573f; 
prbl[23][17].PRy = 0.33289474f; 
prbl[23][18].PRx = 0.30855018f; 
prbl[23][18].PRy = 0.3135776f; 
prbl[23][19].PRx = 0.30172414f; 
prbl[23][19].PRy = 0.29424778f; 
prbl[23][20].PRx = 0.2980456f; 
prbl[23][20].PRy = 0.2872596f; 
prbl[23][21].PRx = 0.36819485f; 
prbl[23][21].PRy = 0.3169456f; 
prbl[23][22].PRx = 0.3438914f; 
prbl[23][22].PRy = 0.34388646f; 
prbl[23][23].PRx = 0.32142857f; 
prbl[23][23].PRy = 0.312212f; 
prbl[23][24].PRx = 0.28490567f; 
prbl[23][24].PRy = 0.28618422f; 
prbl[23][25].PRx = 0.28947368f; 
prbl[23][25].PRy = 0.28653845f; 
prbl[23][26].PRx = 0.28326997f; 
prbl[23][26].PRy = 0.27729258f; 
prbl[23][27].PRx = 0.28703704f; 
prbl[23][27].PRy = 0.28719008f; 
prbl[23][28].PRx = 0.3033088f; 
prbl[23][28].PRy = 0.28869048f; 
prbl[23][29].PRx = 0.30555555f; 
prbl[23][29].PRy = 0.29895833f; 
prbl[23][30].PRx = 0.28829786f; 
prbl[23][30].PRy = 0.28936172f; 
prbl[23][31].PRx = 0.2966926f; 
prbl[23][31].PRy = 0.291841f; 
prbl[23][32].PRx = 0.29605263f; 
prbl[23][32].PRy = 0.2977273f; 

prbl[24][0].PRx = 0.3647059f; 
prbl[24][0].PRy = 0.3529412f; 
prbl[24][1].PRx = 0.36003235f; 
prbl[24][1].PRy = 0.3115578f; 
prbl[24][2].PRx = 0.3060166f; 
prbl[24][2].PRy = 0.28965518f; 
prbl[24][3].PRx = 0.46260387f; 
prbl[24][3].PRy = 0.42825112f; 
prbl[24][4].PRx = 0.35977563f; 
prbl[24][4].PRy = 0.351476f; 
prbl[24][5].PRx = 0.30802292f; 
prbl[24][5].PRy = 0.28333333f; 
prbl[24][6].PRx = 0.30235988f; 
prbl[24][6].PRy = 0.301f; 
prbl[24][7].PRx = 0.28927204f; 
prbl[24][7].PRy = 0.3020446f; 
prbl[24][8].PRx = 0.2964684f; 
prbl[24][8].PRy = 0.30387205f; 
prbl[24][9].PRx = 0.3254386f; 
prbl[24][9].PRy = 0.3330116f; 
prbl[24][10].PRx = 0.31206897f; 
prbl[24][10].PRy = 0.31363636f; 
prbl[24][11].PRx = 0.29081634f; 
prbl[24][11].PRy = 0.27978724f; 
prbl[24][12].PRx = 0.3017544f; 
prbl[24][12].PRy = 0.30462962f; 
prbl[24][13].PRx = 0.3291536f; 
prbl[24][13].PRy = 0.32875457f; 
prbl[24][14].PRx = 0.373444f; 
prbl[24][14].PRy = 0.39456522f; 
prbl[24][15].PRx = 0.3119195f; 
prbl[24][15].PRy = 0.30444443f; 
prbl[24][16].PRx = 0.39325842f; 
prbl[24][16].PRy = 0.41353384f; 
prbl[24][17].PRx = 0.31135532f; 
prbl[24][17].PRy = 0.30769232f; 
prbl[24][18].PRx = 0.33333334f; 
prbl[24][18].PRy = 0.35714287f; 
prbl[24][19].PRx = 0.39108187f; 
prbl[24][19].PRy = 0.38333333f; 
prbl[24][20].PRx = 0.34007353f; 
prbl[24][20].PRy = 0.30743244f; 
prbl[24][21].PRx = 0.33775812f; 
prbl[24][21].PRy = 0.31914893f; 
prbl[24][22].PRx = 0.30852416f; 
prbl[24][22].PRy = 0.28806585f; 
prbl[24][23].PRx = 0.35766962f; 
prbl[24][23].PRy = 0.31698564f; 
prbl[24][24].PRx = 0.28894928f; 
prbl[24][24].PRy = 0.27659574f; 
prbl[24][25].PRx = 0.28623188f; 
prbl[24][25].PRy = 0.28139013f; 
prbl[24][26].PRx = 0.2960993f; 
prbl[24][26].PRy = 0.29819277f; 
prbl[24][27].PRx = 0.29229322f; 
prbl[24][27].PRy = 0.30510205f; 
prbl[24][28].PRx = 0.30784315f; 
prbl[24][28].PRy = 0.3010204f; 
prbl[24][29].PRx = 0.30585107f; 
prbl[24][29].PRy = 0.30228138f; 
prbl[24][30].PRx = 0.30876866f; 
prbl[24][30].PRy = 0.29820627f; 
prbl[24][31].PRx = 0.30420712f; 
prbl[24][31].PRy = 0.28958333f; 
prbl[24][32].PRx = 0.28716215f; 
prbl[24][32].PRy = 0.30227274f; 

prbl[25][0].PRx = 0.39583334f; 
prbl[25][0].PRy = 0.44354838f; 
prbl[25][1].PRx = 0.35702613f; 
prbl[25][1].PRy = 0.35597825f; 
prbl[25][2].PRx = 0.3112245f; 
prbl[25][2].PRy = 0.3031609f; 
prbl[25][3].PRx = 0.4133803f; 
prbl[25][3].PRy = 0.4090909f; 
prbl[25][4].PRx = 0.3034483f; 
prbl[25][4].PRy = 0.3156146f; 
prbl[25][5].PRx = 0.31748468f; 
prbl[25][5].PRy = 0.3202555f; 
prbl[25][6].PRx = 0.31621003f; 
prbl[25][6].PRy = 0.32130584f; 
prbl[25][7].PRx = 0.31459332f; 
prbl[25][7].PRy = 0.33892617f; 
prbl[25][8].PRx = 0.34375f; 
prbl[25][8].PRy = 0.348659f; 
prbl[25][9].PRx = 0.37704918f; 
prbl[25][9].PRy = 0.38706896f; 
prbl[25][10].PRx = 0.29400748f; 
prbl[25][10].PRy = 0.31501833f; 
prbl[25][11].PRx = 0.32976654f; 
prbl[25][11].PRy = 0.3251029f; 
prbl[25][12].PRx = 0.34569734f; 
prbl[25][12].PRy = 0.3266129f; 
prbl[25][13].PRx = 0.3492823f; 
prbl[25][13].PRy = 0.33236435f; 
prbl[25][14].PRx = 0.3871841f; 
prbl[25][14].PRy = 0.41460395f; 
prbl[25][15].PRx = 0.37174723f; 
prbl[25][15].PRy = 0.39502165f; 
prbl[25][16].PRx = 0.2923077f; 
prbl[25][16].PRy = 0.285f; 
prbl[25][17].PRx = 0.37102473f; 
prbl[25][17].PRy = 0.34787235f; 
prbl[25][18].PRx = 0.3539326f; 
prbl[25][18].PRy = 0.36462092f; 
prbl[25][19].PRx = 0.30892858f; 
prbl[25][19].PRy = 0.30923694f; 
prbl[25][20].PRx = 0.29828662f; 
prbl[25][20].PRy = 0.29302326f; 
prbl[25][21].PRx = 0.31196582f; 
prbl[25][21].PRy = 0.2964135f; 
prbl[25][22].PRx = 0.3024911f; 
prbl[25][22].PRy = 0.299639f; 
prbl[25][23].PRx = 0.3609589f; 
prbl[25][23].PRy = 0.32747933f; 
prbl[25][24].PRx = 0.30904907f; 
prbl[25][24].PRy = 0.31164384f; 
prbl[25][25].PRx = 0.29411766f; 
prbl[25][25].PRy = 0.2965368f; 
prbl[25][26].PRx = 0.3240418f; 
prbl[25][26].PRy = 0.3110465f; 
prbl[25][27].PRx = 0.32f; 
prbl[25][27].PRy = 0.3101852f; 
prbl[25][28].PRx = 0.30228138f; 
prbl[25][28].PRy = 0.29545453f; 
prbl[25][29].PRx = 0.30923694f; 
prbl[25][29].PRy = 0.2985348f; 
prbl[25][30].PRx = 0.30514705f; 
prbl[25][30].PRy = 0.3006073f; 
prbl[25][31].PRx = 0.31782946f; 
prbl[25][31].PRy = 0.30791506f; 
prbl[25][32].PRx = 0.30291972f; 
prbl[25][32].PRy = 0.298f; 

prbl[26][0].PRx = 0.27604166f; 
prbl[26][0].PRy = 0.27884614f; 
prbl[26][1].PRx = 0.31938326f; 
prbl[26][1].PRy = 0.31451613f; 
prbl[26][2].PRx = 0.32560483f; 
prbl[26][2].PRy = 0.3220859f; 
prbl[26][3].PRx = 0.4675926f; 
prbl[26][3].PRy = 0.46934867f; 
prbl[26][4].PRx = 0.3108856f; 
prbl[26][4].PRy = 0.3248503f; 
prbl[26][5].PRx = 0.3140496f; 
prbl[26][5].PRy = 0.34305993f; 
prbl[26][6].PRx = 0.36212122f; 
prbl[26][6].PRy = 0.3918919f; 
prbl[26][7].PRx = 0.38567072f; 
prbl[26][7].PRy = 0.39297125f; 
prbl[26][8].PRx = 0.33586955f; 
prbl[26][8].PRy = 0.36498258f; 
prbl[26][9].PRx = 0.4332061f; 
prbl[26][9].PRy = 0.4394737f; 
prbl[26][10].PRx = 0.34924242f; 
prbl[26][10].PRy = 0.3488593f; 
prbl[26][11].PRx = 0.36283785f; 
prbl[26][11].PRy = 0.32684824f; 
prbl[26][12].PRx = 0.34763682f; 
prbl[26][12].PRy = 0.33031675f; 
prbl[26][13].PRx = 0.38598576f; 
prbl[26][13].PRy = 0.40154868f; 
prbl[26][14].PRx = 0.4314236f; 
prbl[26][14].PRy = 0.44747898f; 
prbl[26][15].PRx = 0.29812834f; 
prbl[26][15].PRy = 0.3021739f; 
prbl[26][16].PRx = 0.278169f; 
prbl[26][16].PRy = 0.28125f; 
prbl[26][17].PRx = 0.41021127f; 
prbl[26][17].PRy = 0.38229573f; 
prbl[26][18].PRx = 0.29616725f; 
prbl[26][18].PRy = 0.29776424f; 
prbl[26][19].PRx = 0.3017711f; 
prbl[26][19].PRy = 0.28353658f; 
prbl[26][20].PRx = 0.30200502f; 
prbl[26][20].PRy = 0.2814136f; 
prbl[26][21].PRx = 0.296875f; 
prbl[26][21].PRy = 0.2918251f; 
prbl[26][22].PRx = 0.29866412f; 
prbl[26][22].PRy = 0.29411766f; 
prbl[26][23].PRx = 0.3482143f; 
prbl[26][23].PRy = 0.31072876f; 
prbl[26][24].PRx = 0.3138138f; 
prbl[26][24].PRy = 0.3007663f; 
prbl[26][25].PRx = 0.31569344f; 
prbl[26][25].PRy = 0.30627707f; 
prbl[26][26].PRx = 0.32201988f; 
prbl[26][26].PRy = 0.30438933f; 
prbl[26][27].PRx = 0.32167235f; 
prbl[26][27].PRy = 0.30882353f; 
prbl[26][28].PRx = 0.3285473f; 
prbl[26][28].PRy = 0.3174905f; 
prbl[26][29].PRx = 0.32822582f; 
prbl[26][29].PRy = 0.30813953f; 
prbl[26][30].PRx = 0.30769232f; 
prbl[26][30].PRy = 0.31111112f; 
prbl[26][31].PRx = 0.3f; 
prbl[26][31].PRy = 0.31296295f; 
prbl[26][32].PRx = 0.31762296f; 
prbl[26][32].PRy = 0.30921054f; 

prbl[27][0].PRx = 0.25984251f; 
prbl[27][0].PRy = 0.265625f; 
prbl[27][1].PRx = 0.26106194f; 
prbl[27][1].PRy = 0.2596685f; 
prbl[27][2].PRx = 0.30904523f; 
prbl[27][2].PRy = 0.31182796f; 
prbl[27][3].PRx = 0.41296297f; 
prbl[27][3].PRy = 0.419145f; 
prbl[27][4].PRx = 0.46470588f; 
prbl[27][4].PRy = 0.44842407f; 
prbl[27][5].PRx = 0.3373162f; 
prbl[27][5].PRy = 0.3570039f; 
prbl[27][6].PRx = 0.30286738f; 
prbl[27][6].PRy = 0.2985348f; 
prbl[27][7].PRx = 0.31539735f; 
prbl[27][7].PRy = 0.32022473f; 
prbl[27][8].PRx = 0.30113637f; 
prbl[27][8].PRy = 0.2983871f; 
prbl[27][9].PRx = 0.30232558f; 
prbl[27][9].PRy = 0.30587122f; 
prbl[27][10].PRx = 0.33275864f; 
prbl[27][10].PRy = 0.34417808f; 
prbl[27][11].PRx = 0.3572335f; 
prbl[27][11].PRy = 0.30504587f; 
prbl[27][12].PRx = 0.31766918f; 
prbl[27][12].PRy = 0.3002232f; 
prbl[27][13].PRx = 0.3286199f; 
prbl[27][13].PRy = 0.3206522f; 
prbl[27][14].PRx = 0.5567633f; 
prbl[27][14].PRy = 0.5731343f; 
prbl[27][15].PRx = 0.41666666f; 
prbl[27][15].PRy = 0.44964027f; 
prbl[27][16].PRx = 0.25403225f; 
prbl[27][16].PRy = 0.2520661f; 
prbl[27][17].PRx = 0.43656716f; 
prbl[27][17].PRy = 0.48241207f; 
prbl[27][18].PRx = 0.35658306f; 
prbl[27][18].PRy = 0.36814347f; 
prbl[27][19].PRx = 0.29947916f; 
prbl[27][19].PRy = 0.2885514f; 
prbl[27][20].PRx = 0.30831265f; 
prbl[27][20].PRy = 0.28605768f; 
prbl[27][21].PRx = 0.28685898f; 
prbl[27][21].PRy = 0.28928572f; 
prbl[27][22].PRx = 0.28947368f; 
prbl[27][22].PRy = 0.30017605f; 
prbl[27][23].PRx = 0.37222221f; 
prbl[27][23].PRy = 0.35110295f; 
prbl[27][24].PRx = 0.29951298f; 
prbl[27][24].PRy = 0.2900844f; 
prbl[27][25].PRx = 0.33380282f; 
prbl[27][25].PRy = 0.31967214f; 
prbl[27][26].PRx = 0.32912457f; 
prbl[27][26].PRy = 0.31734318f; 
prbl[27][27].PRx = 0.31741574f; 
prbl[27][27].PRy = 0.3139313f; 
prbl[27][28].PRx = 0.3154122f; 
prbl[27][28].PRy = 0.3117284f; 
prbl[27][29].PRx = 0.32476637f; 
prbl[27][29].PRy = 0.30533597f; 
prbl[27][30].PRx = 0.3336207f; 
prbl[27][30].PRy = 0.30652174f; 
prbl[27][31].PRx = 0.31300813f; 
prbl[27][31].PRy = 0.30543932f; 
prbl[27][32].PRx = 0.3075f; 
prbl[27][32].PRy = 0.29856116f; 

prbl[28][0].PRx = 0.275f; 
prbl[28][0].PRy = 0.27419356f; 
prbl[28][1].PRx = 0.25790513f; 
prbl[28][1].PRy = 0.2631579f; 
prbl[28][2].PRx = 0.30227274f; 
prbl[28][2].PRy = 0.28544775f; 
prbl[28][3].PRx = 0.2969745f; 
prbl[28][3].PRy = 0.29247105f; 
prbl[28][4].PRx = 0.28913045f; 
prbl[28][4].PRy = 0.2941767f; 
prbl[28][5].PRx = 0.29248366f; 
prbl[28][5].PRy = 0.2881356f; 
prbl[28][6].PRx = 0.28420195f; 
prbl[28][6].PRy = 0.28658536f; 
prbl[28][7].PRx = 0.2986111f; 
prbl[28][7].PRy = 0.2938247f; 
prbl[28][8].PRx = 0.2882948f; 
prbl[28][8].PRy = 0.28681508f; 
prbl[28][9].PRx = 0.28773585f; 
prbl[28][9].PRy = 0.28755364f; 
prbl[28][10].PRx = 0.29918033f; 
prbl[28][10].PRy = 0.29554656f; 
prbl[28][11].PRx = 0.33828124f; 
prbl[28][11].PRy = 0.32156488f; 
prbl[28][12].PRx = 0.33222592f; 
prbl[28][12].PRy = 0.32608697f; 
prbl[28][13].PRx = 0.41996402f; 
prbl[28][13].PRy = 0.45765027f; 
prbl[28][14].PRx = 0.5868167f; 
prbl[28][14].PRy = 0.625f; 
prbl[28][15].PRx = 0.5551771f; 
prbl[28][15].PRy = 0.57410717f; 
prbl[28][16].PRx = 0.2930403f; 
prbl[28][16].PRy = 0.2879147f; 
prbl[28][17].PRx = 0.32885304f; 
prbl[28][17].PRy = 0.34891304f; 
prbl[28][18].PRx = 0.38905326f; 
prbl[28][18].PRy = 0.43061224f; 
prbl[28][19].PRx = 0.3939394f; 
prbl[28][19].PRy = 0.4222441f; 
prbl[28][20].PRx = 0.34140435f; 
prbl[28][20].PRy = 0.33450705f; 
prbl[28][21].PRx = 0.37985864f; 
prbl[28][21].PRy = 0.37220448f; 
prbl[28][22].PRx = 0.3529412f; 
prbl[28][22].PRy = 0.35714287f; 
prbl[28][23].PRx = 0.29959515f; 
prbl[28][23].PRy = 0.30321783f; 
prbl[28][24].PRx = 0.3068536f; 
prbl[28][24].PRy = 0.29247105f; 
prbl[28][25].PRx = 0.30958903f; 
prbl[28][25].PRy = 0.30876067f; 
prbl[28][26].PRx = 0.33540925f; 
prbl[28][26].PRy = 0.32684427f; 
prbl[28][27].PRx = 0.32254902f; 
prbl[28][27].PRy = 0.32636362f; 
prbl[28][28].PRx = 0.33237547f; 
prbl[28][28].PRy = 0.3127962f; 
prbl[28][29].PRx = 0.3157143f; 
prbl[28][29].PRy = 0.2891705f; 
prbl[28][30].PRx = 0.3320783f; 
prbl[28][30].PRy = 0.29864255f; 
prbl[28][31].PRx = 0.30574912f; 
prbl[28][31].PRy = 0.30041152f; 
prbl[28][32].PRx = 0.31118882f; 
prbl[28][32].PRy = 0.31445312f; 

prbl[29][0].PRx = 0.2614679f; 
prbl[29][0].PRy = 0.2651515f; 
prbl[29][1].PRx = 0.30251142f; 
prbl[29][1].PRy = 0.30945945f; 
prbl[29][2].PRx = 0.2825521f; 
prbl[29][2].PRy = 0.29296875f; 
prbl[29][3].PRx = 0.30177993f; 
prbl[29][3].PRy = 0.30982906f; 
prbl[29][4].PRx = 0.30227274f; 
prbl[29][4].PRy = 0.29693878f; 
prbl[29][5].PRx = 0.28165585f; 
prbl[29][5].PRy = 0.28303966f; 
prbl[29][6].PRx = 0.29456192f; 
prbl[29][6].PRy = 0.29959515f; 
prbl[29][7].PRx = 0.30870447f; 
prbl[29][7].PRy = 0.32991803f; 
prbl[29][8].PRx = 0.317029f; 
prbl[29][8].PRy = 0.34f; 
prbl[29][9].PRx = 0.32738096f; 
prbl[29][9].PRy = 0.3532197f; 
prbl[29][10].PRx = 0.34214744f; 
prbl[29][10].PRy = 0.38225257f; 
prbl[29][11].PRx = 0.34338236f; 
prbl[29][11].PRy = 0.33173078f; 
prbl[29][12].PRx = 0.3721374f; 
prbl[29][12].PRy = 0.37962964f; 
prbl[29][13].PRx = 0.3646409f; 
prbl[29][13].PRy = 0.37751678f; 
prbl[29][14].PRx = 0.3512545f; 
prbl[29][14].PRy = 0.36642158f; 
prbl[29][15].PRx = 0.45628834f; 
prbl[29][15].PRy = 0.47586873f; 
prbl[29][16].PRx = 0.31428573f; 
prbl[29][16].PRy = 0.30241936f; 
prbl[29][17].PRx = 0.30904058f; 
prbl[29][17].PRy = 0.29781422f; 
prbl[29][18].PRx = 0.2964684f; 
prbl[29][18].PRy = 0.2969543f; 
prbl[29][19].PRx = 0.3592437f; 
prbl[29][19].PRy = 0.38243243f; 
prbl[29][20].PRx = 0.39257812f; 
prbl[29][20].PRy = 0.41632652f; 
prbl[29][21].PRx = 0.3202847f; 
prbl[29][21].PRy = 0.33510637f; 
prbl[29][22].PRx = 0.305f; 
prbl[29][22].PRy = 0.3125f; 
prbl[29][23].PRx = 0.32291666f; 
prbl[29][23].PRy = 0.311245f; 
prbl[29][24].PRx = 0.33035713f; 
prbl[29][24].PRy = 0.31788793f; 
prbl[29][25].PRx = 0.32933193f; 
prbl[29][25].PRy = 0.33007118f; 
prbl[29][26].PRx = 0.33136094f; 
prbl[29][26].PRy = 0.32101166f; 
prbl[29][27].PRx = 0.38868612f; 
prbl[29][27].PRy = 0.37916666f; 
prbl[29][28].PRx = 0.3353846f; 
prbl[29][28].PRy = 0.32057416f; 
prbl[29][29].PRx = 0.31846154f; 
prbl[29][29].PRy = 0.2982456f; 
prbl[29][30].PRx = 0.34675142f; 
prbl[29][30].PRy = 0.28910613f; 
prbl[29][31].PRx = 0.31958762f; 
prbl[29][31].PRy = 0.3026906f; 
prbl[29][32].PRx = 0.2933071f; 
prbl[29][32].PRy = 0.30932203f; 

prbl[30][0].PRx = 0.38992536f; 
prbl[30][0].PRy = 0.3913793f; 
prbl[30][1].PRx = 0.38360655f; 
prbl[30][1].PRy = 0.38690478f; 
prbl[30][2].PRx = 0.33055556f; 
prbl[30][2].PRy = 0.35964912f; 
prbl[30][3].PRx = 0.31358886f; 
prbl[30][3].PRy = 0.33783785f; 
prbl[30][4].PRx = 0.29480287f; 
prbl[30][4].PRy = 0.29802954f; 
prbl[30][5].PRx = 0.33422938f; 
prbl[30][5].PRy = 0.36936936f; 
prbl[30][6].PRx = 0.34118852f; 
prbl[30][6].PRy = 0.3532864f; 
prbl[30][7].PRx = 0.28875968f; 
prbl[30][7].PRy = 0.294686f; 
prbl[30][8].PRx = 0.28766027f; 
prbl[30][8].PRy = 0.27702704f; 
prbl[30][9].PRx = 0.28793103f; 
prbl[30][9].PRy = 0.28265765f; 
prbl[30][10].PRx = 0.30274263f; 
prbl[30][10].PRy = 0.31658292f; 
prbl[30][11].PRx = 0.43050542f; 
prbl[30][11].PRy = 0.46360153f; 
prbl[30][12].PRx = 0.49833333f; 
prbl[30][12].PRy = 0.51510066f; 
prbl[30][13].PRx = 0.2784553f; 
prbl[30][13].PRy = 0.27960527f; 
prbl[30][14].PRx = 0.29940712f; 
prbl[30][14].PRy = 0.28402367f; 
prbl[30][15].PRx = 0.3003663f; 
prbl[30][15].PRy = 0.30104712f; 
prbl[30][16].PRx = 0.29475984f; 
prbl[30][16].PRy = 0.2988827f; 
prbl[30][17].PRx = 0.30465588f; 
prbl[30][17].PRy = 0.30694443f; 
prbl[30][18].PRx = 0.3065574f; 
prbl[30][18].PRy = 0.28612718f; 
prbl[30][19].PRx = 0.30162242f; 
prbl[30][19].PRy = 0.29694834f; 
prbl[30][20].PRx = 0.29896143f; 
prbl[30][20].PRy = 0.29852322f; 
prbl[30][21].PRx = 0.33079848f; 
prbl[30][21].PRy = 0.33392227f; 
prbl[30][22].PRx = 0.31543624f; 
prbl[30][22].PRy = 0.33608058f; 
prbl[30][23].PRx = 0.3171875f; 
prbl[30][23].PRy = 0.30751175f; 
prbl[30][24].PRx = 0.30492425f; 
prbl[30][24].PRy = 0.2953668f; 
prbl[30][25].PRx = 0.32002458f; 
prbl[30][25].PRy = 0.3117409f; 
prbl[30][26].PRx = 0.31099194f; 
prbl[30][26].PRy = 0.30608365f; 
prbl[30][27].PRx = 0.36420864f; 
prbl[30][27].PRy = 0.33488372f; 
prbl[30][28].PRx = 0.31514657f; 
prbl[30][28].PRy = 0.2875f; 
prbl[30][29].PRx = 0.3221154f; 
prbl[30][29].PRy = 0.30964467f; 
prbl[30][30].PRx = 0.3148148f; 
prbl[30][30].PRy = 0.30529955f; 
prbl[30][31].PRx = 0.32764506f; 
prbl[30][31].PRy = 0.30229592f; 
prbl[30][32].PRx = 0.318f; 
prbl[30][32].PRy = 0.3f; 

prbl[31][0].PRx = 0.29452056f; 
prbl[31][0].PRy = 0.30105633f; 
prbl[31][1].PRx = 0.40782124f; 
prbl[31][1].PRy = 0.3896605f; 
prbl[31][2].PRx = 0.3143116f; 
prbl[31][2].PRy = 0.31666666f; 
prbl[31][3].PRx = 0.32665506f; 
prbl[31][3].PRy = 0.29807693f; 
prbl[31][4].PRx = 0.35380116f; 
prbl[31][4].PRy = 0.4f; 
prbl[31][5].PRx = 0.28104576f; 
prbl[31][5].PRy = 0.28278688f; 
prbl[31][6].PRx = 0.3064236f; 
prbl[31][6].PRy = 0.29490292f; 
prbl[31][7].PRx = 0.28898305f; 
prbl[31][7].PRy = 0.2811159f; 
prbl[31][8].PRx = 0.28296703f; 
prbl[31][8].PRy = 0.28658536f; 
prbl[31][9].PRx = 0.28519857f; 
prbl[31][9].PRy = 0.2792208f; 
prbl[31][10].PRx = 0.30019686f; 
prbl[31][10].PRy = 0.2992611f; 
prbl[31][11].PRx = 0.327551f; 
prbl[31][11].PRy = 0.33443707f; 
prbl[31][12].PRx = 0.29532966f; 
prbl[31][12].PRy = 0.2989865f; 
prbl[31][13].PRx = 0.30793992f; 
prbl[31][13].PRy = 0.3337766f; 
prbl[31][14].PRx = 0.29958677f; 
prbl[31][14].PRy = 0.3195122f; 
prbl[31][15].PRx = 0.30373833f; 
prbl[31][15].PRy = 0.29896906f; 
prbl[31][16].PRx = 0.29042554f; 
prbl[31][16].PRy = 0.2963801f; 
prbl[31][17].PRx = 0.28777778f; 
prbl[31][17].PRy = 0.28370786f; 
prbl[31][18].PRx = 0.30586082f; 
prbl[31][18].PRy = 0.29319373f; 
prbl[31][19].PRx = 0.32843137f; 
prbl[31][19].PRy = 0.30408654f; 
prbl[31][20].PRx = 0.3030888f; 
prbl[31][20].PRy = 0.30464482f; 
prbl[31][21].PRx = 0.30864197f; 
prbl[31][21].PRy = 0.3002232f; 
prbl[31][22].PRx = 0.3285124f; 
prbl[31][22].PRy = 0.31797236f; 
prbl[31][23].PRx = 0.28959626f; 
prbl[31][23].PRy = 0.28658536f; 
prbl[31][24].PRx = 0.294702f; 
prbl[31][24].PRy = 0.29247105f; 
prbl[31][25].PRx = 0.30571428f; 
prbl[31][25].PRy = 0.29858658f; 
prbl[31][26].PRx = 0.3112745f; 
prbl[31][26].PRy = 0.2974806f; 
prbl[31][27].PRx = 0.3272727f; 
prbl[31][27].PRy = 0.29579207f; 
prbl[31][28].PRx = 0.31229508f; 
prbl[31][28].PRy = 0.3025641f; 
prbl[31][29].PRx = 0.3116883f; 
prbl[31][29].PRy = 0.2973301f; 
prbl[31][30].PRx = 0.3171378f; 
prbl[31][30].PRy = 0.31060606f; 
prbl[31][31].PRx = 0.32685512f; 
prbl[31][31].PRy = 0.2995283f; 
prbl[31][32].PRx = 0.35227272f; 
prbl[31][32].PRy = 0.2956989f; 

prbl[32][0].PRx = 0.28716215f; 
prbl[32][0].PRy = 0.28645834f; 
prbl[32][1].PRx = 0.3480769f; 
prbl[32][1].PRy = 0.34545454f; 
prbl[32][2].PRx = 0.2939189f; 
prbl[32][2].PRy = 0.2984496f; 
prbl[32][3].PRx = 0.33392859f; 
prbl[32][3].PRy = 0.3152174f; 
prbl[32][4].PRx = 0.25f; 
prbl[32][4].PRy = 0.25f; 
prbl[32][5].PRx = 0.31333333f; 
prbl[32][5].PRy = 0.3068182f; 
prbl[32][6].PRx = 0.28368795f; 
prbl[32][6].PRy = 0.28645834f; 
prbl[32][7].PRx = 0.28863636f; 
prbl[32][7].PRy = 0.29651162f; 
prbl[32][8].PRx = 0.32456142f; 
prbl[32][8].PRy = 0.34020618f; 
prbl[32][9].PRx = 0.35164836f; 
prbl[32][9].PRy = 0.38443395f; 
prbl[32][10].PRx = 0.34663865f; 
prbl[32][10].PRy = 0.35755813f; 
prbl[32][11].PRx = 0.26666668f; 
prbl[32][11].PRy = 0.2820513f; 
prbl[32][12].PRx = 0.30147058f; 
prbl[32][12].PRy = 0.3f; 
prbl[32][13].PRx = 0.2921875f; 
prbl[32][13].PRy = 0.29069766f; 
prbl[32][14].PRx = 0.28819445f; 
prbl[32][14].PRy = 0.28769842f; 
prbl[32][15].PRx = 0.29503107f; 
prbl[32][15].PRy = 0.30343512f; 
prbl[32][16].PRx = 0.2850877f; 
prbl[32][16].PRy = 0.29856116f; 
prbl[32][17].PRx = 0.29310346f; 
prbl[32][17].PRy = 0.2897196f; 
prbl[32][18].PRx = 0.3364486f; 
prbl[32][18].PRy = 0.28968254f; 
prbl[32][19].PRx = 0.32291666f; 
prbl[32][19].PRy = 0.30833334f; 
prbl[32][20].PRx = 0.29791668f; 
prbl[32][20].PRy = 0.29473683f; 
prbl[32][21].PRx = 0.3018018f; 
prbl[32][21].PRy = 0.29938272f; 
prbl[32][22].PRx = 0.31603774f; 
prbl[32][22].PRy = 0.30387932f; 
prbl[32][23].PRx = 0.2955975f; 
prbl[32][23].PRy = 0.30905512f; 
prbl[32][24].PRx = 0.30729166f; 
prbl[32][24].PRy = 0.29449153f; 
prbl[32][25].PRx = 0.31294963f; 
prbl[32][25].PRy = 0.32522124f; 
prbl[32][26].PRx = 0.31451613f; 
prbl[32][26].PRy = 0.29504505f; 
prbl[32][27].PRx = 0.32692307f; 
prbl[32][27].PRy = 0.3137255f; 
prbl[32][28].PRx = 0.31118882f; 
prbl[32][28].PRy = 0.30670103f; 
prbl[32][29].PRx = 0.31862745f; 
prbl[32][29].PRy = 0.29672897f; 
prbl[32][30].PRx = 0.32961783f; 
prbl[32][30].PRy = 0.33024693f; 
prbl[32][31].PRx = 0.32363012f; 
prbl[32][31].PRy = 0.2804878f; 
prbl[32][32].PRx = 0.30454546f; 
prbl[32][32].PRy = 0.3018868f; 




}	

public void loadPrecomputedPR_512avg()
{


prbl[0][0].PRx = 0.34615386f; 
prbl[0][0].PRy = 0.26666668f; 
prbl[0][1].PRx = 0.2647059f; 
prbl[0][1].PRy = 0.27f; 
prbl[0][2].PRx = 0.30952382f; 
prbl[0][2].PRy = 0.275f; 
prbl[0][3].PRx = 0.30555555f; 
prbl[0][3].PRy = 0.25833333f; 
prbl[0][4].PRx = 0.26190478f; 
prbl[0][4].PRy = 0.27631578f; 
prbl[0][5].PRx = 0.26973686f; 
prbl[0][5].PRy = 0.2826087f; 
prbl[0][6].PRx = 0.28289473f; 
prbl[0][6].PRy = 0.2777778f; 
prbl[0][7].PRx = 0.35416666f; 
prbl[0][7].PRy = 0.37037036f; 
prbl[0][8].PRx = 0.3604651f; 
prbl[0][8].PRy = 0.36607143f; 
prbl[0][9].PRx = 0.48986486f; 
prbl[0][9].PRy = 0.4871795f; 
prbl[0][10].PRx = 0.3618421f; 
prbl[0][10].PRy = 0.3875f; 
prbl[0][11].PRx = 0.32142857f; 
prbl[0][11].PRy = 0.35f; 
prbl[0][12].PRx = 0.30612245f; 
prbl[0][12].PRy = 0.25925925f; 
prbl[0][13].PRx = 0.38636363f; 
prbl[0][13].PRy = 0.27941176f; 
prbl[0][14].PRx = 0.3125f; 
prbl[0][14].PRy = 0.3f; 
prbl[0][15].PRx = 0.3359375f; 
prbl[0][15].PRy = 0.35714287f; 
prbl[0][16].PRx = 0.37068966f; 
prbl[0][16].PRy = 0.3452381f; 
prbl[0][17].PRx = 0.27325583f; 
prbl[0][17].PRy = 0.29054055f; 
prbl[0][18].PRx = 0.31923077f; 
prbl[0][18].PRy = 0.29054055f; 
prbl[0][19].PRx = 0.3690476f; 
prbl[0][19].PRy = 0.3125f; 
prbl[0][20].PRx = 0.3442623f; 
prbl[0][20].PRy = 0.30208334f; 
prbl[0][21].PRx = 0.31465518f; 
prbl[0][21].PRy = 0.2837838f; 
prbl[0][22].PRx = 0.36267605f; 
prbl[0][22].PRy = 0.34285715f; 
prbl[0][23].PRx = 0.3671875f; 
prbl[0][23].PRy = 0.3472222f; 
prbl[0][24].PRx = 0.51865673f; 
prbl[0][24].PRy = 0.53205127f; 
prbl[0][25].PRx = 0.38636363f; 
prbl[0][25].PRy = 0.3809524f; 
prbl[0][26].PRx = 0.32407406f; 
prbl[0][26].PRy = 0.3043478f; 
prbl[0][27].PRx = 0.2857143f; 
prbl[0][27].PRy = 0.25f; 
prbl[0][28].PRx = 0.27142859f; 
prbl[0][28].PRy = 0.25f; 
prbl[0][29].PRx = 0.28225806f; 
prbl[0][29].PRy = 0.25862068f; 
prbl[0][30].PRx = 0.28629032f; 
prbl[0][30].PRy = 0.2642857f; 
prbl[0][31].PRx = 0.36666667f; 
prbl[0][31].PRy = 0.28125f; 
prbl[0][32].PRx = 0.28846154f; 
prbl[0][32].PRy = 0.2857143f; 

prbl[1][0].PRx = 0.27f; 
prbl[1][0].PRy = 0.29545453f; 
prbl[1][1].PRx = 0.305f; 
prbl[1][1].PRy = 0.3090909f; 
prbl[1][2].PRx = 0.33139536f; 
prbl[1][2].PRy = 0.34210527f; 
prbl[1][3].PRx = 0.2625f; 
prbl[1][3].PRy = 0.28225806f; 
prbl[1][4].PRx = 0.31451613f; 
prbl[1][4].PRy = 0.2847222f; 
prbl[1][5].PRx = 0.29090908f; 
prbl[1][5].PRy = 0.296875f; 
prbl[1][6].PRx = 0.3046875f; 
prbl[1][6].PRy = 0.2857143f; 
prbl[1][7].PRx = 0.39285713f; 
prbl[1][7].PRy = 0.34375f; 
prbl[1][8].PRx = 0.44008264f; 
prbl[1][8].PRy = 0.43235293f; 
prbl[1][9].PRx = 0.54744524f; 
prbl[1][9].PRy = 0.58152175f; 
prbl[1][10].PRx = 0.3655914f; 
prbl[1][10].PRy = 0.3859649f; 
prbl[1][11].PRx = 0.3991228f; 
prbl[1][11].PRy = 0.40822786f; 
prbl[1][12].PRx = 0.34210527f; 
prbl[1][12].PRy = 0.3452381f; 
prbl[1][13].PRx = 0.38253012f; 
prbl[1][13].PRy = 0.35f; 
prbl[1][14].PRx = 0.30597016f; 
prbl[1][14].PRy = 0.32017544f; 
prbl[1][15].PRx = 0.30769232f; 
prbl[1][15].PRy = 0.31666666f; 
prbl[1][16].PRx = 0.3125f; 
prbl[1][16].PRy = 0.29545453f; 
prbl[1][17].PRx = 0.30227274f; 
prbl[1][17].PRy = 0.28666666f; 
prbl[1][18].PRx = 0.30582523f; 
prbl[1][18].PRy = 0.32954547f; 
prbl[1][19].PRx = 0.32045454f; 
prbl[1][19].PRy = 0.31048387f; 
prbl[1][20].PRx = 0.31578946f; 
prbl[1][20].PRy = 0.2881356f; 
prbl[1][21].PRx = 0.32524273f; 
prbl[1][21].PRy = 0.30405405f; 
prbl[1][22].PRx = 0.3398058f; 
prbl[1][22].PRy = 0.29081634f; 
prbl[1][23].PRx = 0.42561984f; 
prbl[1][23].PRy = 0.41666666f; 
prbl[1][24].PRx = 0.39347827f; 
prbl[1][24].PRy = 0.3372093f; 
prbl[1][25].PRx = 0.36637932f; 
prbl[1][25].PRy = 0.3409091f; 
prbl[1][26].PRx = 0.32407406f; 
prbl[1][26].PRy = 0.30555555f; 
prbl[1][27].PRx = 0.27419356f; 
prbl[1][27].PRy = 0.25f; 
prbl[1][28].PRx = 0.27857143f; 
prbl[1][28].PRy = 0.27868852f; 
prbl[1][29].PRx = 0.2777778f; 
prbl[1][29].PRy = 0.2596154f; 
prbl[1][30].PRx = 0.28846154f; 
prbl[1][30].PRy = 0.27659574f; 
prbl[1][31].PRx = 0.40848213f; 
prbl[1][31].PRy = 0.3382353f; 
prbl[1][32].PRx = 0.3548387f; 
prbl[1][32].PRy = 0.25f; 

prbl[2][0].PRx = 0.2867647f; 
prbl[2][0].PRy = 0.28030303f; 
prbl[2][1].PRx = 0.25925925f; 
prbl[2][1].PRy = 0.29166666f; 
prbl[2][2].PRx = 0.27651516f; 
prbl[2][2].PRy = 0.27192983f; 
prbl[2][3].PRx = 0.31f; 
prbl[2][3].PRy = 0.3125f; 
prbl[2][4].PRx = 0.30714285f; 
prbl[2][4].PRy = 0.30952382f; 
prbl[2][5].PRx = 0.30379745f; 
prbl[2][5].PRy = 0.315f; 
prbl[2][6].PRx = 0.4125f; 
prbl[2][6].PRy = 0.45918366f; 
prbl[2][7].PRx = 0.31716418f; 
prbl[2][7].PRy = 0.34666666f; 
prbl[2][8].PRx = 0.47566372f; 
prbl[2][8].PRy = 0.4763158f; 
prbl[2][9].PRx = 0.51282054f; 
prbl[2][9].PRy = 0.556701f; 
prbl[2][10].PRx = 0.37349397f; 
prbl[2][10].PRy = 0.38592234f; 
prbl[2][11].PRx = 0.4090909f; 
prbl[2][11].PRy = 0.3909091f; 
prbl[2][12].PRx = 0.3442623f; 
prbl[2][12].PRy = 0.3275862f; 
prbl[2][13].PRx = 0.3181818f; 
prbl[2][13].PRy = 0.3065476f; 
prbl[2][14].PRx = 0.31048387f; 
prbl[2][14].PRy = 0.31976745f; 
prbl[2][15].PRx = 0.33214286f; 
prbl[2][15].PRy = 0.34840426f; 
prbl[2][16].PRx = 0.29945055f; 
prbl[2][16].PRy = 0.31012657f; 
prbl[2][17].PRx = 0.33552632f; 
prbl[2][17].PRy = 0.3097015f; 
prbl[2][18].PRx = 0.34615386f; 
prbl[2][18].PRy = 0.30357143f; 
prbl[2][19].PRx = 0.31930694f; 
prbl[2][19].PRy = 0.26960784f; 
prbl[2][20].PRx = 0.30952382f; 
prbl[2][20].PRy = 0.30208334f; 
prbl[2][21].PRx = 0.354f; 
prbl[2][21].PRy = 0.31730768f; 
prbl[2][22].PRx = 0.38809523f; 
prbl[2][22].PRy = 0.34876543f; 
prbl[2][23].PRx = 0.35820895f; 
prbl[2][23].PRy = 0.3153409f; 
prbl[2][24].PRx = 0.3458904f; 
prbl[2][24].PRy = 0.36764705f; 
prbl[2][25].PRx = 0.30952382f; 
prbl[2][25].PRy = 0.31944445f; 
prbl[2][26].PRx = 0.31333333f; 
prbl[2][26].PRy = 0.31985295f; 
prbl[2][27].PRx = 0.32777777f; 
prbl[2][27].PRy = 0.31603774f; 
prbl[2][28].PRx = 0.35655737f; 
prbl[2][28].PRy = 0.36666667f; 
prbl[2][29].PRx = 0.28846154f; 
prbl[2][29].PRy = 0.2867647f; 
prbl[2][30].PRx = 0.2995283f; 
prbl[2][30].PRy = 0.28289473f; 
prbl[2][31].PRx = 0.38188976f; 
prbl[2][31].PRy = 0.29807693f; 
prbl[2][32].PRx = 0.30357143f; 
prbl[2][32].PRy = 0.27173913f; 

prbl[3][0].PRx = 0.29f; 
prbl[3][0].PRy = 0.28f; 
prbl[3][1].PRx = 0.28636363f; 
prbl[3][1].PRy = 0.28333333f; 
prbl[3][2].PRx = 0.265625f; 
prbl[3][2].PRy = 0.2761194f; 
prbl[3][3].PRx = 0.259375f; 
prbl[3][3].PRy = 0.25724638f; 
prbl[3][4].PRx = 0.28787878f; 
prbl[3][4].PRy = 0.26190478f; 
prbl[3][5].PRx = 0.2949438f; 
prbl[3][5].PRy = 0.28879312f; 
prbl[3][6].PRx = 0.5902256f; 
prbl[3][6].PRy = 0.6283186f; 
prbl[3][7].PRx = 0.51960784f; 
prbl[3][7].PRy = 0.5221239f; 
prbl[3][8].PRx = 0.5141129f; 
prbl[3][8].PRy = 0.53809524f; 
prbl[3][9].PRx = 0.47f; 
prbl[3][9].PRy = 0.4425f; 
prbl[3][10].PRx = 0.303125f; 
prbl[3][10].PRy = 0.30792683f; 
prbl[3][11].PRx = 0.33333334f; 
prbl[3][11].PRy = 0.31944445f; 
prbl[3][12].PRx = 0.32462686f; 
prbl[3][12].PRy = 0.3266129f; 
prbl[3][13].PRx = 0.33238637f; 
prbl[3][13].PRy = 0.31506848f; 
prbl[3][14].PRx = 0.3429487f; 
prbl[3][14].PRy = 0.32831326f; 
prbl[3][15].PRx = 0.32831326f; 
prbl[3][15].PRy = 0.36263737f; 
prbl[3][16].PRx = 0.34f; 
prbl[3][16].PRy = 0.3394737f; 
prbl[3][17].PRx = 0.38785046f; 
prbl[3][17].PRy = 0.3561321f; 
prbl[3][18].PRx = 0.3940678f; 
prbl[3][18].PRy = 0.35472974f; 
prbl[3][19].PRx = 0.29326922f; 
prbl[3][19].PRy = 0.29642856f; 
prbl[3][20].PRx = 0.4034091f; 
prbl[3][20].PRy = 0.3375f; 
prbl[3][21].PRx = 0.3443396f; 
prbl[3][21].PRy = 0.3018868f; 
prbl[3][22].PRx = 0.34438777f; 
prbl[3][22].PRy = 0.29924244f; 
prbl[3][23].PRx = 0.3125f; 
prbl[3][23].PRy = 0.3115942f; 
prbl[3][24].PRx = 0.30384615f; 
prbl[3][24].PRy = 0.30597016f; 
prbl[3][25].PRx = 0.3375f; 
prbl[3][25].PRy = 0.3382353f; 
prbl[3][26].PRx = 0.32908162f; 
prbl[3][26].PRy = 0.3604651f; 
prbl[3][27].PRx = 0.3661616f; 
prbl[3][27].PRy = 0.35074627f; 
prbl[3][28].PRx = 0.3922414f; 
prbl[3][28].PRy = 0.4107143f; 
prbl[3][29].PRx = 0.30989584f; 
prbl[3][29].PRy = 0.3152174f; 
prbl[3][30].PRx = 0.34438777f; 
prbl[3][30].PRy = 0.29761904f; 
prbl[3][31].PRx = 0.37619048f; 
prbl[3][31].PRy = 0.27857143f; 
prbl[3][32].PRx = 0.3292683f; 
prbl[3][32].PRy = 0.2631579f; 

prbl[4][0].PRx = 0.2647059f; 
prbl[4][0].PRy = 0.25833333f; 
prbl[4][1].PRx = 0.27192983f; 
prbl[4][1].PRy = 0.27739727f; 
prbl[4][2].PRx = 0.2753623f; 
prbl[4][2].PRy = 0.26923078f; 
prbl[4][3].PRx = 0.28846154f; 
prbl[4][3].PRy = 0.2962963f; 
prbl[4][4].PRx = 0.2777778f; 
prbl[4][4].PRy = 0.28214285f; 
prbl[4][5].PRx = 0.30379745f; 
prbl[4][5].PRy = 0.29761904f; 
prbl[4][6].PRx = 0.6074074f; 
prbl[4][6].PRy = 0.61764705f; 
prbl[4][7].PRx = 0.5342742f; 
prbl[4][7].PRy = 0.5301724f; 
prbl[4][8].PRx = 0.5711382f; 
prbl[4][8].PRy = 0.59166664f; 
prbl[4][9].PRx = 0.43452382f; 
prbl[4][9].PRy = 0.46474358f; 
prbl[4][10].PRx = 0.41666666f; 
prbl[4][10].PRy = 0.41477272f; 
prbl[4][11].PRx = 0.39527026f; 
prbl[4][11].PRy = 0.39930555f; 
prbl[4][12].PRx = 0.3480392f; 
prbl[4][12].PRy = 0.38020834f; 
prbl[4][13].PRx = 0.31349206f; 
prbl[4][13].PRy = 0.32083333f; 
prbl[4][14].PRx = 0.34558824f; 
prbl[4][14].PRy = 0.34745762f; 
prbl[4][15].PRx = 0.34745762f; 
prbl[4][15].PRy = 0.35784313f; 
prbl[4][16].PRx = 0.3478261f; 
prbl[4][16].PRy = 0.3319672f; 
prbl[4][17].PRx = 0.4971591f; 
prbl[4][17].PRy = 0.5121951f; 
prbl[4][18].PRx = 0.41203704f; 
prbl[4][18].PRy = 0.41f; 
prbl[4][19].PRx = 0.32619047f; 
prbl[4][19].PRy = 0.32098764f; 
prbl[4][20].PRx = 0.37809917f; 
prbl[4][20].PRy = 0.33870968f; 
prbl[4][21].PRx = 0.3409091f; 
prbl[4][21].PRy = 0.3046875f; 
prbl[4][22].PRx = 0.35044643f; 
prbl[4][22].PRy = 0.29577464f; 
prbl[4][23].PRx = 0.28846154f; 
prbl[4][23].PRy = 0.2857143f; 
prbl[4][24].PRx = 0.3025f; 
prbl[4][24].PRy = 0.29220778f; 
prbl[4][25].PRx = 0.3452381f; 
prbl[4][25].PRy = 0.32307693f; 
prbl[4][26].PRx = 0.29032257f; 
prbl[4][26].PRy = 0.28623188f; 
prbl[4][27].PRx = 0.38771185f; 
prbl[4][27].PRy = 0.37662336f; 
prbl[4][28].PRx = 0.375f; 
prbl[4][28].PRy = 0.3809524f; 
prbl[4][29].PRx = 0.36141303f; 
prbl[4][29].PRy = 0.31666666f; 
prbl[4][30].PRx = 0.31593406f; 
prbl[4][30].PRy = 0.29347825f; 
prbl[4][31].PRx = 0.37239584f; 
prbl[4][31].PRy = 0.35106382f; 
prbl[4][32].PRx = 0.29069766f; 
prbl[4][32].PRy = 0.296875f; 

prbl[5][0].PRx = 0.275f; 
prbl[5][0].PRy = 0.2734375f; 
prbl[5][1].PRx = 0.31111112f; 
prbl[5][1].PRy = 0.2789855f; 
prbl[5][2].PRx = 0.2867647f; 
prbl[5][2].PRy = 0.28f; 
prbl[5][3].PRx = 0.2777778f; 
prbl[5][3].PRy = 0.28125f; 
prbl[5][4].PRx = 0.29545453f; 
prbl[5][4].PRy = 0.2962963f; 
prbl[5][5].PRx = 0.48267326f; 
prbl[5][5].PRy = 0.502551f; 
prbl[5][6].PRx = 0.5769231f; 
prbl[5][6].PRy = 0.55932206f; 
prbl[5][7].PRx = 0.57236844f; 
prbl[5][7].PRy = 0.56707317f; 
prbl[5][8].PRx = 0.61082476f; 
prbl[5][8].PRy = 0.58978873f; 
prbl[5][9].PRx = 0.59732825f; 
prbl[5][9].PRy = 0.59489053f; 
prbl[5][10].PRx = 0.40625f; 
prbl[5][10].PRy = 0.42666668f; 
prbl[5][11].PRx = 0.4963768f; 
prbl[5][11].PRy = 0.5037879f; 
prbl[5][12].PRx = 0.3830645f; 
prbl[5][12].PRy = 0.31547618f; 
prbl[5][13].PRx = 0.3888889f; 
prbl[5][13].PRy = 0.44444445f; 
prbl[5][14].PRx = 0.395f; 
prbl[5][14].PRy = 0.35526314f; 
prbl[5][15].PRx = 0.4056604f; 
prbl[5][15].PRy = 0.35833332f; 
prbl[5][16].PRx = 0.3f; 
prbl[5][16].PRy = 0.31896552f; 
prbl[5][17].PRx = 0.42088607f; 
prbl[5][17].PRy = 0.44396552f; 
prbl[5][18].PRx = 0.3822314f; 
prbl[5][18].PRy = 0.37671232f; 
prbl[5][19].PRx = 0.35416666f; 
prbl[5][19].PRy = 0.31862745f; 
prbl[5][20].PRx = 0.3347826f; 
prbl[5][20].PRy = 0.29615384f; 
prbl[5][21].PRx = 0.32539684f; 
prbl[5][21].PRy = 0.30701753f; 
prbl[5][22].PRx = 0.2904762f; 
prbl[5][22].PRy = 0.2983871f; 
prbl[5][23].PRx = 0.2867647f; 
prbl[5][23].PRy = 0.27651516f; 
prbl[5][24].PRx = 0.33958334f; 
prbl[5][24].PRy = 0.31451613f; 
prbl[5][25].PRx = 0.36466166f; 
prbl[5][25].PRy = 0.36764705f; 
prbl[5][26].PRx = 0.28186274f; 
prbl[5][26].PRy = 0.2625f; 
prbl[5][27].PRx = 0.29012346f; 
prbl[5][27].PRy = 0.29591838f; 
prbl[5][28].PRx = 0.27727273f; 
prbl[5][28].PRy = 0.2947761f; 
prbl[5][29].PRx = 0.3508772f; 
prbl[5][29].PRy = 0.32786885f; 
prbl[5][30].PRx = 0.3445946f; 
prbl[5][30].PRy = 0.33898306f; 
prbl[5][31].PRx = 0.37755102f; 
prbl[5][31].PRy = 0.35869566f; 
prbl[5][32].PRx = 0.33695653f; 
prbl[5][32].PRy = 0.28125f; 

prbl[6][0].PRx = 0.30882353f; 
prbl[6][0].PRy = 0.30357143f; 
prbl[6][1].PRx = 0.3f; 
prbl[6][1].PRy = 0.27564102f; 
prbl[6][2].PRx = 0.25862068f; 
prbl[6][2].PRy = 0.2580645f; 
prbl[6][3].PRx = 0.2625f; 
prbl[6][3].PRy = 0.27173913f; 
prbl[6][4].PRx = 0.31578946f; 
prbl[6][4].PRy = 0.30833334f; 
prbl[6][5].PRx = 0.48818898f; 
prbl[6][5].PRy = 0.49411765f; 
prbl[6][6].PRx = 0.55f; 
prbl[6][6].PRy = 0.5555556f; 
prbl[6][7].PRx = 0.640625f; 
prbl[6][7].PRy = 0.6119403f; 
prbl[6][8].PRx = 0.6415441f; 
prbl[6][8].PRy = 0.6559829f; 
prbl[6][9].PRx = 0.71153843f; 
prbl[6][9].PRy = 0.7340425f; 
prbl[6][10].PRx = 0.42372882f; 
prbl[6][10].PRy = 0.44672132f; 
prbl[6][11].PRx = 0.5744681f; 
prbl[6][11].PRy = 0.61926603f; 
prbl[6][12].PRx = 0.38990825f; 
prbl[6][12].PRy = 0.38768116f; 
prbl[6][13].PRx = 0.37857142f; 
prbl[6][13].PRy = 0.42021278f; 
prbl[6][14].PRx = 0.3625f; 
prbl[6][14].PRy = 0.32258064f; 
prbl[6][15].PRx = 0.3278302f; 
prbl[6][15].PRy = 0.334375f; 
prbl[6][16].PRx = 0.355f; 
prbl[6][16].PRy = 0.35384616f; 
prbl[6][17].PRx = 0.36507937f; 
prbl[6][17].PRy = 0.36968085f; 
prbl[6][18].PRx = 0.41447368f; 
prbl[6][18].PRy = 0.33695653f; 
prbl[6][19].PRx = 0.3625f; 
prbl[6][19].PRy = 0.33490565f; 
prbl[6][20].PRx = 0.2929293f; 
prbl[6][20].PRy = 0.29347825f; 
prbl[6][21].PRx = 0.32619047f; 
prbl[6][21].PRy = 0.315625f; 
prbl[6][22].PRx = 0.38520408f; 
prbl[6][22].PRy = 0.38716814f; 
prbl[6][23].PRx = 0.30582523f; 
prbl[6][23].PRy = 0.29577464f; 
prbl[6][24].PRx = 0.3888889f; 
prbl[6][24].PRy = 0.33620688f; 
prbl[6][25].PRx = 0.376f; 
prbl[6][25].PRy = 0.3125f; 
prbl[6][26].PRx = 0.31132075f; 
prbl[6][26].PRy = 0.2977941f; 
prbl[6][27].PRx = 0.28457448f; 
prbl[6][27].PRy = 0.28289473f; 
prbl[6][28].PRx = 0.30625f; 
prbl[6][28].PRy = 0.29411766f; 
prbl[6][29].PRx = 0.30833334f; 
prbl[6][29].PRy = 0.3164557f; 
prbl[6][30].PRx = 0.2875f; 
prbl[6][30].PRy = 0.26984128f; 
prbl[6][31].PRx = 0.328125f; 
prbl[6][31].PRy = 0.29069766f; 
prbl[6][32].PRx = 0.4453125f; 
prbl[6][32].PRy = 0.33333334f; 

prbl[7][0].PRx = 0.25892857f; 
prbl[7][0].PRy = 0.25f; 
prbl[7][1].PRx = 0.2777778f; 
prbl[7][1].PRy = 0.27314815f; 
prbl[7][2].PRx = 0.27058825f; 
prbl[7][2].PRy = 0.25378788f; 
prbl[7][3].PRx = 0.28879312f; 
prbl[7][3].PRy = 0.29104477f; 
prbl[7][4].PRx = 0.2936508f; 
prbl[7][4].PRy = 0.28225806f; 
prbl[7][5].PRx = 0.43333334f; 
prbl[7][5].PRy = 0.41949153f; 
prbl[7][6].PRx = 0.60263157f; 
prbl[7][6].PRy = 0.5896226f; 
prbl[7][7].PRx = 0.6258278f; 
prbl[7][7].PRy = 0.64122134f; 
prbl[7][8].PRx = 0.5462963f; 
prbl[7][8].PRy = 0.5185185f; 
prbl[7][9].PRx = 0.55851066f; 
prbl[7][9].PRy = 0.67771083f; 
prbl[7][10].PRx = 0.544964f; 
prbl[7][10].PRy = 0.61875f; 
prbl[7][11].PRx = 0.50775194f; 
prbl[7][11].PRy = 0.5744048f; 
prbl[7][12].PRx = 0.40454546f; 
prbl[7][12].PRy = 0.4090909f; 
prbl[7][13].PRx = 0.41666666f; 
prbl[7][13].PRy = 0.4311594f; 
prbl[7][14].PRx = 0.3482143f; 
prbl[7][14].PRy = 0.33467743f; 
prbl[7][15].PRx = 0.3090909f; 
prbl[7][15].PRy = 0.31862745f; 
prbl[7][16].PRx = 0.3511236f; 
prbl[7][16].PRy = 0.31557378f; 
prbl[7][17].PRx = 0.3844086f; 
prbl[7][17].PRy = 0.40350878f; 
prbl[7][18].PRx = 0.32828283f; 
prbl[7][18].PRy = 0.3125f; 
prbl[7][19].PRx = 0.3568376f; 
prbl[7][19].PRy = 0.3114035f; 
prbl[7][20].PRx = 0.29444444f; 
prbl[7][20].PRy = 0.27192983f; 
prbl[7][21].PRx = 0.285f; 
prbl[7][21].PRy = 0.28873238f; 
prbl[7][22].PRx = 0.34438777f; 
prbl[7][22].PRy = 0.3482143f; 
prbl[7][23].PRx = 0.34139785f; 
prbl[7][23].PRy = 0.3237705f; 
prbl[7][24].PRx = 0.40753424f; 
prbl[7][24].PRy = 0.3895349f; 
prbl[7][25].PRx = 0.40654206f; 
prbl[7][25].PRy = 0.38492063f; 
prbl[7][26].PRx = 0.31043956f; 
prbl[7][26].PRy = 0.3301282f; 
prbl[7][27].PRx = 0.3630435f; 
prbl[7][27].PRy = 0.38372093f; 
prbl[7][28].PRx = 0.41237113f; 
prbl[7][28].PRy = 0.41265061f; 
prbl[7][29].PRx = 0.29642856f; 
prbl[7][29].PRy = 0.30479452f; 
prbl[7][30].PRx = 0.33878505f; 
prbl[7][30].PRy = 0.2971698f; 
prbl[7][31].PRx = 0.35f; 
prbl[7][31].PRy = 0.30729166f; 
prbl[7][32].PRx = 0.39473686f; 
prbl[7][32].PRy = 0.3690476f; 

prbl[8][0].PRx = 0.26744187f; 
prbl[8][0].PRy = 0.27142859f; 
prbl[8][1].PRx = 0.26623377f; 
prbl[8][1].PRy = 0.2704918f; 
prbl[8][2].PRx = 0.28928572f; 
prbl[8][2].PRy = 0.27403846f; 
prbl[8][3].PRx = 0.29012346f; 
prbl[8][3].PRy = 0.30333334f; 
prbl[8][4].PRx = 0.38380283f; 
prbl[8][4].PRy = 0.44871795f; 
prbl[8][5].PRx = 0.5217391f; 
prbl[8][5].PRy = 0.56f; 
prbl[8][6].PRx = 0.6159639f; 
prbl[8][6].PRy = 0.69554454f; 
prbl[8][7].PRx = 0.6864162f; 
prbl[8][7].PRy = 0.696281f; 
prbl[8][8].PRx = 0.57421875f; 
prbl[8][8].PRy = 0.625f; 
prbl[8][9].PRx = 0.4878472f; 
prbl[8][9].PRy = 0.5595238f; 
prbl[8][10].PRx = 0.5801282f; 
prbl[8][10].PRy = 0.6868132f; 
prbl[8][11].PRx = 0.5328125f; 
prbl[8][11].PRy = 0.59345794f; 
prbl[8][12].PRx = 0.3556338f; 
prbl[8][12].PRy = 0.3642857f; 
prbl[8][13].PRx = 0.32712767f; 
prbl[8][13].PRy = 0.2857143f; 
prbl[8][14].PRx = 0.32857144f; 
prbl[8][14].PRy = 0.3633721f; 
prbl[8][15].PRx = 0.33189654f; 
prbl[8][15].PRy = 0.36805555f; 
prbl[8][16].PRx = 0.35135135f; 
prbl[8][16].PRy = 0.29166666f; 
prbl[8][17].PRx = 0.4140625f; 
prbl[8][17].PRy = 0.31363636f; 
prbl[8][18].PRx = 0.3125f; 
prbl[8][18].PRy = 0.32142857f; 
prbl[8][19].PRx = 0.30952382f; 
prbl[8][19].PRy = 0.31716418f; 
prbl[8][20].PRx = 0.29395604f; 
prbl[8][20].PRy = 0.28125f; 
prbl[8][21].PRx = 0.29081634f; 
prbl[8][21].PRy = 0.26408452f; 
prbl[8][22].PRx = 0.30376345f; 
prbl[8][22].PRy = 0.28f; 
prbl[8][23].PRx = 0.27011493f; 
prbl[8][23].PRy = 0.27868852f; 
prbl[8][24].PRx = 0.38679245f; 
prbl[8][24].PRy = 0.35546875f; 
prbl[8][25].PRx = 0.30147058f; 
prbl[8][25].PRy = 0.31896552f; 
prbl[8][26].PRx = 0.3324468f; 
prbl[8][26].PRy = 0.31465518f; 
prbl[8][27].PRx = 0.48809522f; 
prbl[8][27].PRy = 0.484375f; 
prbl[8][28].PRx = 0.296875f; 
prbl[8][28].PRy = 0.31f; 
prbl[8][29].PRx = 0.28787878f; 
prbl[8][29].PRy = 0.29508197f; 
prbl[8][30].PRx = 0.3529412f; 
prbl[8][30].PRy = 0.31779662f; 
prbl[8][31].PRx = 0.33482143f; 
prbl[8][31].PRy = 0.2947761f; 
prbl[8][32].PRx = 0.43014705f; 
prbl[8][32].PRy = 0.32608697f; 

prbl[9][0].PRx = 0.25609756f; 
prbl[9][0].PRy = 0.25f; 
prbl[9][1].PRx = 0.27884614f; 
prbl[9][1].PRy = 0.26587301f; 
prbl[9][2].PRx = 0.26298702f; 
prbl[9][2].PRy = 0.26492536f; 
prbl[9][3].PRx = 0.32857144f; 
prbl[9][3].PRy = 0.3859649f; 
prbl[9][4].PRx = 0.3880597f; 
prbl[9][4].PRy = 0.44078946f; 
prbl[9][5].PRx = 0.49380165f; 
prbl[9][5].PRy = 0.5f; 
prbl[9][6].PRx = 0.6267123f; 
prbl[9][6].PRy = 0.65654206f; 
prbl[9][7].PRx = 0.6181507f; 
prbl[9][7].PRy = 0.63271606f; 
prbl[9][8].PRx = 0.5840517f; 
prbl[9][8].PRy = 0.67391306f; 
prbl[9][9].PRx = 0.518797f; 
prbl[9][9].PRy = 0.5421687f; 
prbl[9][10].PRx = 0.6405229f; 
prbl[9][10].PRy = 0.76506025f; 
prbl[9][11].PRx = 0.6f; 
prbl[9][11].PRy = 0.6627358f; 
prbl[9][12].PRx = 0.34935898f; 
prbl[9][12].PRy = 0.375f; 
prbl[9][13].PRx = 0.30277777f; 
prbl[9][13].PRy = 0.29225352f; 
prbl[9][14].PRx = 0.30792683f; 
prbl[9][14].PRy = 0.3f; 
prbl[9][15].PRx = 0.3125f; 
prbl[9][15].PRy = 0.2962963f; 
prbl[9][16].PRx = 0.3452381f; 
prbl[9][16].PRy = 0.31465518f; 
prbl[9][17].PRx = 0.36633664f; 
prbl[9][17].PRy = 0.34574467f; 
prbl[9][18].PRx = 0.29455444f; 
prbl[9][18].PRy = 0.2782258f; 
prbl[9][19].PRx = 0.28055555f; 
prbl[9][19].PRy = 0.28731343f; 
prbl[9][20].PRx = 0.30733946f; 
prbl[9][20].PRy = 0.28636363f; 
prbl[9][21].PRx = 0.29301074f; 
prbl[9][21].PRy = 0.29385966f; 
prbl[9][22].PRx = 0.2970297f; 
prbl[9][22].PRy = 0.2962963f; 
prbl[9][23].PRx = 0.29945055f; 
prbl[9][23].PRy = 0.28125f; 
prbl[9][24].PRx = 0.4475f; 
prbl[9][24].PRy = 0.4244186f; 
prbl[9][25].PRx = 0.3f; 
prbl[9][25].PRy = 0.27884614f; 
prbl[9][26].PRx = 0.30384615f; 
prbl[9][26].PRy = 0.30555555f; 
prbl[9][27].PRx = 0.3828125f; 
prbl[9][27].PRy = 0.32142857f; 
prbl[9][28].PRx = 0.32941177f; 
prbl[9][28].PRy = 0.30864197f; 
prbl[9][29].PRx = 0.30448717f; 
prbl[9][29].PRy = 0.3164557f; 
prbl[9][30].PRx = 0.29651162f; 
prbl[9][30].PRy = 0.3219178f; 
prbl[9][31].PRx = 0.36342594f; 
prbl[9][31].PRy = 0.32575756f; 
prbl[9][32].PRx = 0.31875f; 
prbl[9][32].PRy = 0.3125f; 

prbl[10][0].PRx = 0.31034482f; 
prbl[10][0].PRy = 0.2826087f; 
prbl[10][1].PRx = 0.27734375f; 
prbl[10][1].PRy = 0.2688679f; 
prbl[10][2].PRx = 0.3697183f; 
prbl[10][2].PRy = 0.4050633f; 
prbl[10][3].PRx = 0.38055557f; 
prbl[10][3].PRy = 0.40460527f; 
prbl[10][4].PRx = 0.40674603f; 
prbl[10][4].PRy = 0.46875f; 
prbl[10][5].PRx = 0.5685185f; 
prbl[10][5].PRy = 0.5597015f; 
prbl[10][6].PRx = 0.5564516f; 
prbl[10][6].PRy = 0.5533708f; 
prbl[10][7].PRx = 0.5378788f; 
prbl[10][7].PRy = 0.47058824f; 
prbl[10][8].PRx = 0.516f; 
prbl[10][8].PRy = 0.5285714f; 
prbl[10][9].PRx = 0.615942f; 
prbl[10][9].PRy = 0.63834953f; 
prbl[10][10].PRx = 0.5942857f; 
prbl[10][10].PRy = 0.69148934f; 
prbl[10][11].PRx = 0.6460843f; 
prbl[10][11].PRy = 0.6594488f; 
prbl[10][12].PRx = 0.43067226f; 
prbl[10][12].PRy = 0.4403409f; 
prbl[10][13].PRx = 0.30808082f; 
prbl[10][13].PRy = 0.30821916f; 
prbl[10][14].PRx = 0.28703704f; 
prbl[10][14].PRy = 0.29f; 
prbl[10][15].PRx = 0.34868422f; 
prbl[10][15].PRy = 0.3690476f; 
prbl[10][16].PRx = 0.45108697f; 
prbl[10][16].PRy = 0.425f; 
prbl[10][17].PRx = 0.30376345f; 
prbl[10][17].PRy = 0.30136988f; 
prbl[10][18].PRx = 0.3026316f; 
prbl[10][18].PRy = 0.2990196f; 
prbl[10][19].PRx = 0.29166666f; 
prbl[10][19].PRy = 0.2769231f; 
prbl[10][20].PRx = 0.2797619f; 
prbl[10][20].PRy = 0.275f; 
prbl[10][21].PRx = 0.29411766f; 
prbl[10][21].PRy = 0.28225806f; 
prbl[10][22].PRx = 0.30445546f; 
prbl[10][22].PRy = 0.29761904f; 
prbl[10][23].PRx = 0.3106796f; 
prbl[10][23].PRy = 0.30555555f; 
prbl[10][24].PRx = 0.5033784f; 
prbl[10][24].PRy = 0.4698795f; 
prbl[10][25].PRx = 0.28365386f; 
prbl[10][25].PRy = 0.3638889f; 
prbl[10][26].PRx = 0.4178082f; 
prbl[10][26].PRy = 0.4506579f; 
prbl[10][27].PRx = 0.45833334f; 
prbl[10][27].PRy = 0.45f; 
prbl[10][28].PRx = 0.27960527f; 
prbl[10][28].PRy = 0.26865673f; 
prbl[10][29].PRx = 0.31089744f; 
prbl[10][29].PRy = 0.3181818f; 
prbl[10][30].PRx = 0.33898306f; 
prbl[10][30].PRy = 0.33116883f; 
prbl[10][31].PRx = 0.31071427f; 
prbl[10][31].PRy = 0.3243243f; 
prbl[10][32].PRx = 0.29166666f; 
prbl[10][32].PRy = 0.30147058f; 

prbl[11][0].PRx = 0.29032257f; 
prbl[11][0].PRy = 0.2826087f; 
prbl[11][1].PRx = 0.3114754f; 
prbl[11][1].PRy = 0.33561644f; 
prbl[11][2].PRx = 0.3580247f; 
prbl[11][2].PRy = 0.3802817f; 
prbl[11][3].PRx = 0.52105266f; 
prbl[11][3].PRy = 0.502907f; 
prbl[11][4].PRx = 0.6257576f; 
prbl[11][4].PRy = 0.63235295f; 
prbl[11][5].PRx = 0.56414473f; 
prbl[11][5].PRy = 0.5693069f; 
prbl[11][6].PRx = 0.47772276f; 
prbl[11][6].PRy = 0.46474358f; 
prbl[11][7].PRx = 0.4375f; 
prbl[11][7].PRy = 0.3943662f; 
prbl[11][8].PRx = 0.5644654f; 
prbl[11][8].PRy = 0.5580808f; 
prbl[11][9].PRx = 0.62406015f; 
prbl[11][9].PRy = 0.62886596f; 
prbl[11][10].PRx = 0.6019108f; 
prbl[11][10].PRy = 0.6401869f; 
prbl[11][11].PRx = 0.57284766f; 
prbl[11][11].PRy = 0.6311881f; 
prbl[11][12].PRx = 0.5751748f; 
prbl[11][12].PRy = 0.59292036f; 
prbl[11][13].PRx = 0.39583334f; 
prbl[11][13].PRy = 0.3554217f; 
prbl[11][14].PRx = 0.3140244f; 
prbl[11][14].PRy = 0.31117022f; 
prbl[11][15].PRx = 0.305f; 
prbl[11][15].PRy = 0.3043478f; 
prbl[11][16].PRx = 0.62401575f; 
prbl[11][16].PRy = 0.63617885f; 
prbl[11][17].PRx = 0.49264705f; 
prbl[11][17].PRy = 0.53177965f; 
prbl[11][18].PRx = 0.29545453f; 
prbl[11][18].PRy = 0.275f; 
prbl[11][19].PRx = 0.29819277f; 
prbl[11][19].PRy = 0.27586207f; 
prbl[11][20].PRx = 0.28883496f; 
prbl[11][20].PRy = 0.27192983f; 
prbl[11][21].PRx = 0.29427084f; 
prbl[11][21].PRy = 0.2835821f; 
prbl[11][22].PRx = 0.28333333f; 
prbl[11][22].PRy = 0.27307692f; 
prbl[11][23].PRx = 0.35049018f; 
prbl[11][23].PRy = 0.29166666f; 
prbl[11][24].PRx = 0.3218391f; 
prbl[11][24].PRy = 0.28688523f; 
prbl[11][25].PRx = 0.29032257f; 
prbl[11][25].PRy = 0.2789855f; 
prbl[11][26].PRx = 0.39338234f; 
prbl[11][26].PRy = 0.26666668f; 
prbl[11][27].PRx = 0.48305085f; 
prbl[11][27].PRy = 0.45f; 
prbl[11][28].PRx = 0.29761904f; 
prbl[11][28].PRy = 0.2835821f; 
prbl[11][29].PRx = 0.29411766f; 
prbl[11][29].PRy = 0.32608697f; 
prbl[11][30].PRx = 0.28819445f; 
prbl[11][30].PRy = 0.29012346f; 
prbl[11][31].PRx = 0.27666667f; 
prbl[11][31].PRy = 0.27380952f; 
prbl[11][32].PRx = 0.28289473f; 
prbl[11][32].PRy = 0.27702704f; 

prbl[12][0].PRx = 0.35135135f; 
prbl[12][0].PRy = 0.3625f; 
prbl[12][1].PRx = 0.32692307f; 
prbl[12][1].PRy = 0.3298611f; 
prbl[12][2].PRx = 0.453125f; 
prbl[12][2].PRy = 0.44230768f; 
prbl[12][3].PRx = 0.6983871f; 
prbl[12][3].PRy = 0.70178574f; 
prbl[12][4].PRx = 0.5996835f; 
prbl[12][4].PRy = 0.59351146f; 
prbl[12][5].PRx = 0.5378788f; 
prbl[12][5].PRy = 0.54444444f; 
prbl[12][6].PRx = 0.4234694f; 
prbl[12][6].PRy = 0.37337664f; 
prbl[12][7].PRx = 0.46022728f; 
prbl[12][7].PRy = 0.42456895f; 
prbl[12][8].PRx = 0.4888889f; 
prbl[12][8].PRy = 0.4237805f; 
prbl[12][9].PRx = 0.69822484f; 
prbl[12][9].PRy = 0.7083333f; 
prbl[12][10].PRx = 0.45045045f; 
prbl[12][10].PRy = 0.50357145f; 
prbl[12][11].PRx = 0.5636646f; 
prbl[12][11].PRy = 0.603211f; 
prbl[12][12].PRx = 0.56774193f; 
prbl[12][12].PRy = 0.6111111f; 
prbl[12][13].PRx = 0.5413223f; 
prbl[12][13].PRy = 0.53370786f; 
prbl[12][14].PRx = 0.6f; 
prbl[12][14].PRy = 0.6157895f; 
prbl[12][15].PRx = 0.47708333f; 
prbl[12][15].PRy = 0.540625f; 
prbl[12][16].PRx = 0.60314685f; 
prbl[12][16].PRy = 0.6438679f; 
prbl[12][17].PRx = 0.675f; 
prbl[12][17].PRy = 0.7358491f; 
prbl[12][18].PRx = 0.4487705f; 
prbl[12][18].PRy = 0.53985506f; 
prbl[12][19].PRx = 0.29896906f; 
prbl[12][19].PRy = 0.2801724f; 
prbl[12][20].PRx = 0.30376345f; 
prbl[12][20].PRy = 0.27868852f; 
prbl[12][21].PRx = 0.29656863f; 
prbl[12][21].PRy = 0.28181818f; 
prbl[12][22].PRx = 0.2925f; 
prbl[12][22].PRy = 0.28169015f; 
prbl[12][23].PRx = 0.35840708f; 
prbl[12][23].PRy = 0.30241936f; 
prbl[12][24].PRx = 0.28521127f; 
prbl[12][24].PRy = 0.2835821f; 
prbl[12][25].PRx = 0.28494623f; 
prbl[12][25].PRy = 0.29583332f; 
prbl[12][26].PRx = 0.48630136f; 
prbl[12][26].PRy = 0.5192308f; 
prbl[12][27].PRx = 0.4532967f; 
prbl[12][27].PRy = 0.48360655f; 
prbl[12][28].PRx = 0.31666666f; 
prbl[12][28].PRy = 0.2993421f; 
prbl[12][29].PRx = 0.31617647f; 
prbl[12][29].PRy = 0.31730768f; 
prbl[12][30].PRx = 0.29929578f; 
prbl[12][30].PRy = 0.30078125f; 
prbl[12][31].PRx = 0.307971f; 
prbl[12][31].PRy = 0.28985506f; 
prbl[12][32].PRx = 0.26666668f; 
prbl[12][32].PRy = 0.27564102f; 

prbl[13][0].PRx = 0.29761904f; 
prbl[13][0].PRy = 0.32317072f; 
prbl[13][1].PRx = 0.2734375f; 
prbl[13][1].PRy = 0.2840909f; 
prbl[13][2].PRx = 0.64102566f; 
prbl[13][2].PRy = 0.64285713f; 
prbl[13][3].PRx = 0.64244187f; 
prbl[13][3].PRy = 0.6875f; 
prbl[13][4].PRx = 0.55833334f; 
prbl[13][4].PRy = 0.5919811f; 
prbl[13][5].PRx = 0.47606382f; 
prbl[13][5].PRy = 0.5f; 
prbl[13][6].PRx = 0.48181817f; 
prbl[13][6].PRy = 0.46304348f; 
prbl[13][7].PRx = 0.43008474f; 
prbl[13][7].PRy = 0.4175f; 
prbl[13][8].PRx = 0.50535715f; 
prbl[13][8].PRy = 0.4617647f; 
prbl[13][9].PRx = 0.5813253f; 
prbl[13][9].PRy = 0.5764463f; 
prbl[13][10].PRx = 0.5192308f; 
prbl[13][10].PRy = 0.55376345f; 
prbl[13][11].PRx = 0.4595588f; 
prbl[13][11].PRy = 0.48314607f; 
prbl[13][12].PRx = 0.5625f; 
prbl[13][12].PRy = 0.5970874f; 
prbl[13][13].PRx = 0.50535715f; 
prbl[13][13].PRy = 0.49390244f; 
prbl[13][14].PRx = 0.515873f; 
prbl[13][14].PRy = 0.5176471f; 
prbl[13][15].PRx = 0.46402878f; 
prbl[13][15].PRy = 0.49603173f; 
prbl[13][16].PRx = 0.5541667f; 
prbl[13][16].PRy = 0.5851064f; 
prbl[13][17].PRx = 0.510101f; 
prbl[13][17].PRy = 0.6510417f; 
prbl[13][18].PRx = 0.5591603f; 
prbl[13][18].PRy = 0.6262626f; 
prbl[13][19].PRx = 0.29427084f; 
prbl[13][19].PRy = 0.28846154f; 
prbl[13][20].PRx = 0.28787878f; 
prbl[13][20].PRy = 0.3f; 
prbl[13][21].PRx = 0.2951807f; 
prbl[13][21].PRy = 0.295f; 
prbl[13][22].PRx = 0.3f; 
prbl[13][22].PRy = 0.27314815f; 
prbl[13][23].PRx = 0.32828283f; 
prbl[13][23].PRy = 0.29310346f; 
prbl[13][24].PRx = 0.29296875f; 
prbl[13][24].PRy = 0.27916667f; 
prbl[13][25].PRx = 0.2789855f; 
prbl[13][25].PRy = 0.28365386f; 
prbl[13][26].PRx = 0.4224138f; 
prbl[13][26].PRy = 0.38690478f; 
prbl[13][27].PRx = 0.5224719f; 
prbl[13][27].PRy = 0.53846157f; 
prbl[13][28].PRx = 0.2987013f; 
prbl[13][28].PRy = 0.31164384f; 
prbl[13][29].PRx = 0.32462686f; 
prbl[13][29].PRy = 0.29f; 
prbl[13][30].PRx = 0.31410256f; 
prbl[13][30].PRy = 0.34836066f; 
prbl[13][31].PRx = 0.2590909f; 
prbl[13][31].PRy = 0.2801724f; 
prbl[13][32].PRx = 0.3181818f; 
prbl[13][32].PRy = 0.31666666f; 

prbl[14][0].PRx = 0.2777778f; 
prbl[14][0].PRy = 0.25833333f; 
prbl[14][1].PRx = 0.47340426f; 
prbl[14][1].PRy = 0.4180328f; 
prbl[14][2].PRx = 0.6155462f; 
prbl[14][2].PRy = 0.585f; 
prbl[14][3].PRx = 0.60714287f; 
prbl[14][3].PRy = 0.65625f; 
prbl[14][4].PRx = 0.62237763f; 
prbl[14][4].PRy = 0.62096775f; 
prbl[14][5].PRx = 0.4515306f; 
prbl[14][5].PRy = 0.4597701f; 
prbl[14][6].PRx = 0.45238096f; 
prbl[14][6].PRy = 0.45038167f; 
prbl[14][7].PRx = 0.42418033f; 
prbl[14][7].PRy = 0.42954546f; 
prbl[14][8].PRx = 0.4919355f; 
prbl[14][8].PRy = 0.4394737f; 
prbl[14][9].PRx = 0.5491803f; 
prbl[14][9].PRy = 0.5277778f; 
prbl[14][10].PRx = 0.5893939f; 
prbl[14][10].PRy = 0.62857145f; 
prbl[14][11].PRx = 0.57419354f; 
prbl[14][11].PRy = 0.6287879f; 
prbl[14][12].PRx = 0.47265625f; 
prbl[14][12].PRy = 0.4970588f; 
prbl[14][13].PRx = 0.62664473f; 
prbl[14][13].PRy = 0.62954545f; 
prbl[14][14].PRx = 0.622093f; 
prbl[14][14].PRy = 0.6013514f; 
prbl[14][15].PRx = 0.6015625f; 
prbl[14][15].PRy = 0.6335617f; 
prbl[14][16].PRx = 0.44354838f; 
prbl[14][16].PRy = 0.58870965f; 
prbl[14][17].PRx = 0.31785715f; 
prbl[14][17].PRy = 0.36f; 
prbl[14][18].PRx = 0.5661157f; 
prbl[14][18].PRy = 0.61206895f; 
prbl[14][19].PRx = 0.3027523f; 
prbl[14][19].PRy = 0.2925532f; 
prbl[14][20].PRx = 0.2943038f; 
prbl[14][20].PRy = 0.27651516f; 
prbl[14][21].PRx = 0.29619566f; 
prbl[14][21].PRy = 0.27314815f; 
prbl[14][22].PRx = 0.30238095f; 
prbl[14][22].PRy = 0.2771739f; 
prbl[14][23].PRx = 0.3533654f; 
prbl[14][23].PRy = 0.29411766f; 
prbl[14][24].PRx = 0.29411766f; 
prbl[14][24].PRy = 0.30059522f; 
prbl[14][25].PRx = 0.2715054f; 
prbl[14][25].PRy = 0.27083334f; 
prbl[14][26].PRx = 0.3465909f; 
prbl[14][26].PRy = 0.27040815f; 
prbl[14][27].PRx = 0.45f; 
prbl[14][27].PRy = 0.4618644f; 
prbl[14][28].PRx = 0.3474026f; 
prbl[14][28].PRy = 0.31690142f; 
prbl[14][29].PRx = 0.27238807f; 
prbl[14][29].PRy = 0.28448275f; 
prbl[14][30].PRx = 0.32967034f; 
prbl[14][30].PRy = 0.3139535f; 
prbl[14][31].PRx = 0.31460676f; 
prbl[14][31].PRy = 0.30952382f; 
prbl[14][32].PRx = 0.32608697f; 
prbl[14][32].PRy = 0.32857144f; 

prbl[15][0].PRx = 0.265625f; 
prbl[15][0].PRy = 0.29f; 
prbl[15][1].PRx = 0.4817073f; 
prbl[15][1].PRy = 0.484375f; 
prbl[15][2].PRx = 0.4207317f; 
prbl[15][2].PRy = 0.43571427f; 
prbl[15][3].PRx = 0.35277778f; 
prbl[15][3].PRy = 0.3888889f; 
prbl[15][4].PRx = 0.51325756f; 
prbl[15][4].PRy = 0.5297619f; 
prbl[15][5].PRx = 0.5673077f; 
prbl[15][5].PRy = 0.5769231f; 
prbl[15][6].PRx = 0.5611511f; 
prbl[15][6].PRy = 0.5826923f; 
prbl[15][7].PRx = 0.5147059f; 
prbl[15][7].PRy = 0.51521736f; 
prbl[15][8].PRx = 0.7328125f; 
prbl[15][8].PRy = 0.7416667f; 
prbl[15][9].PRx = 0.6493902f; 
prbl[15][9].PRy = 0.6492248f; 
prbl[15][10].PRx = 0.6052632f; 
prbl[15][10].PRy = 0.62603307f; 
prbl[15][11].PRx = 0.5050676f; 
prbl[15][11].PRy = 0.50252527f; 
prbl[15][12].PRx = 0.53409094f; 
prbl[15][12].PRy = 0.5595238f; 
prbl[15][13].PRx = 0.56085527f; 
prbl[15][13].PRy = 0.6117647f; 
prbl[15][14].PRx = 0.60172415f; 
prbl[15][14].PRy = 0.594697f; 
prbl[15][15].PRx = 0.67391306f; 
prbl[15][15].PRy = 0.6902655f; 
prbl[15][16].PRx = 0.53571427f; 
prbl[15][16].PRy = 0.5668317f; 
prbl[15][17].PRx = 0.40931374f; 
prbl[15][17].PRy = 0.49019608f; 
prbl[15][18].PRx = 0.5719697f; 
prbl[15][18].PRy = 0.64244187f; 
prbl[15][19].PRx = 0.32670453f; 
prbl[15][19].PRy = 0.34051725f; 
prbl[15][20].PRx = 0.27884614f; 
prbl[15][20].PRy = 0.2668919f; 
prbl[15][21].PRx = 0.29439253f; 
prbl[15][21].PRy = 0.30737704f; 
prbl[15][22].PRx = 0.28932583f; 
prbl[15][22].PRy = 0.26415095f; 
prbl[15][23].PRx = 0.3276699f; 
prbl[15][23].PRy = 0.28070176f; 
prbl[15][24].PRx = 0.28932583f; 
prbl[15][24].PRy = 0.290625f; 
prbl[15][25].PRx = 0.26948053f; 
prbl[15][25].PRy = 0.2804054f; 
prbl[15][26].PRx = 0.30357143f; 
prbl[15][26].PRy = 0.2777778f; 
prbl[15][27].PRx = 0.35714287f; 
prbl[15][27].PRy = 0.34375f; 
prbl[15][28].PRx = 0.3490991f; 
prbl[15][28].PRy = 0.37376237f; 
prbl[15][29].PRx = 0.33139536f; 
prbl[15][29].PRy = 0.33707866f; 
prbl[15][30].PRx = 0.3382353f; 
prbl[15][30].PRy = 0.34550563f; 
prbl[15][31].PRx = 0.30747128f; 
prbl[15][31].PRy = 0.31686047f; 
prbl[15][32].PRx = 0.2826087f; 
prbl[15][32].PRy = 0.3203125f; 

prbl[16][0].PRx = 0.29054055f; 
prbl[16][0].PRy = 0.33333334f; 
prbl[16][1].PRx = 0.575f; 
prbl[16][1].PRy = 0.5671642f; 
prbl[16][2].PRx = 0.29885057f; 
prbl[16][2].PRy = 0.2797619f; 
prbl[16][3].PRx = 0.45432693f; 
prbl[16][3].PRy = 0.45652175f; 
prbl[16][4].PRx = 0.5606618f; 
prbl[16][4].PRy = 0.55246913f; 
prbl[16][5].PRx = 0.73541665f; 
prbl[16][5].PRy = 0.82336956f; 
prbl[16][6].PRx = 0.7783333f; 
prbl[16][6].PRy = 0.7814815f; 
prbl[16][7].PRx = 0.6759259f; 
prbl[16][7].PRy = 0.7205882f; 
prbl[16][8].PRx = 0.8203704f; 
prbl[16][8].PRy = 0.8389423f; 
prbl[16][9].PRx = 0.63235295f; 
prbl[16][9].PRy = 0.7057292f; 
prbl[16][10].PRx = 0.4965035f; 
prbl[16][10].PRy = 0.52393615f; 
prbl[16][11].PRx = 0.53571427f; 
prbl[16][11].PRy = 0.55588233f; 
prbl[16][12].PRx = 0.50517243f; 
prbl[16][12].PRy = 0.49532712f; 
prbl[16][13].PRx = 0.51834863f; 
prbl[16][13].PRy = 0.48333332f; 
prbl[16][14].PRx = 0.6057692f; 
prbl[16][14].PRy = 0.6085271f; 
prbl[16][15].PRx = 0.7123894f; 
prbl[16][15].PRy = 0.8301282f; 
prbl[16][16].PRx = 0.5360577f; 
prbl[16][16].PRy = 0.56962025f; 
prbl[16][17].PRx = 0.43695652f; 
prbl[16][17].PRy = 0.5086207f; 
prbl[16][18].PRx = 0.5890151f; 
prbl[16][18].PRy = 0.7463768f; 
prbl[16][19].PRx = 0.3690476f; 
prbl[16][19].PRy = 0.39130434f; 
prbl[16][20].PRx = 0.2983871f; 
prbl[16][20].PRy = 0.29583332f; 
prbl[16][21].PRx = 0.30357143f; 
prbl[16][21].PRy = 0.29545453f; 
prbl[16][22].PRx = 0.31842107f; 
prbl[16][22].PRy = 0.29109588f; 
prbl[16][23].PRx = 0.33139536f; 
prbl[16][23].PRy = 0.29081634f; 
prbl[16][24].PRx = 0.29040405f; 
prbl[16][24].PRy = 0.28082192f; 
prbl[16][25].PRx = 0.27747253f; 
prbl[16][25].PRy = 0.275f; 
prbl[16][26].PRx = 0.33585858f; 
prbl[16][26].PRy = 0.29f; 
prbl[16][27].PRx = 0.31609195f; 
prbl[16][27].PRy = 0.30737704f; 
prbl[16][28].PRx = 0.34469697f; 
prbl[16][28].PRy = 0.3277027f; 
prbl[16][29].PRx = 0.4175f; 
prbl[16][29].PRy = 0.39166668f; 
prbl[16][30].PRx = 0.41581634f; 
prbl[16][30].PRy = 0.38380283f; 
prbl[16][31].PRx = 0.32272726f; 
prbl[16][31].PRy = 0.31756756f; 
prbl[16][32].PRx = 0.3043478f; 
prbl[16][32].PRy = 0.35326087f; 

prbl[17][0].PRx = 0.28333333f; 
prbl[17][0].PRy = 0.27884614f; 
prbl[17][1].PRx = 0.5763889f; 
prbl[17][1].PRy = 0.627193f; 
prbl[17][2].PRx = 0.29619566f; 
prbl[17][2].PRy = 0.28448275f; 
prbl[17][3].PRx = 0.509009f; 
prbl[17][3].PRy = 0.50769234f; 
prbl[17][4].PRx = 0.5367647f; 
prbl[17][4].PRy = 0.6227273f; 
prbl[17][5].PRx = 0.5490654f; 
prbl[17][5].PRy = 0.58231705f; 
prbl[17][6].PRx = 0.384375f; 
prbl[17][6].PRy = 0.42857143f; 
prbl[17][7].PRx = 0.35135135f; 
prbl[17][7].PRy = 0.4021739f; 
prbl[17][8].PRx = 0.6666667f; 
prbl[17][8].PRy = 0.7291667f; 
prbl[17][9].PRx = 0.5366667f; 
prbl[17][9].PRy = 0.56790125f; 
prbl[17][10].PRx = 0.5071942f; 
prbl[17][10].PRy = 0.46010637f; 
prbl[17][11].PRx = 0.5458861f; 
prbl[17][11].PRy = 0.57738096f; 
prbl[17][12].PRx = 0.556962f; 
prbl[17][12].PRy = 0.5681818f; 
prbl[17][13].PRx = 0.49780703f; 
prbl[17][13].PRy = 0.49085367f; 
prbl[17][14].PRx = 0.6200658f; 
prbl[17][14].PRy = 0.60263157f; 
prbl[17][15].PRx = 0.58838385f; 
prbl[17][15].PRy = 0.6354167f; 
prbl[17][16].PRx = 0.585f; 
prbl[17][16].PRy = 0.63461536f; 
prbl[17][17].PRx = 0.34198114f; 
prbl[17][17].PRy = 0.41666666f; 
prbl[17][18].PRx = 0.49f; 
prbl[17][18].PRy = 0.4921875f; 
prbl[17][19].PRx = 0.49390244f; 
prbl[17][19].PRy = 0.5123457f; 
prbl[17][20].PRx = 0.29885057f; 
prbl[17][20].PRy = 0.28125f; 
prbl[17][21].PRx = 0.3010204f; 
prbl[17][21].PRy = 0.296875f; 
prbl[17][22].PRx = 0.2881356f; 
prbl[17][22].PRy = 0.29464287f; 
prbl[17][23].PRx = 0.34375f; 
prbl[17][23].PRy = 0.29245284f; 
prbl[17][24].PRx = 0.2987805f; 
prbl[17][24].PRy = 0.29296875f; 
prbl[17][25].PRx = 0.28125f; 
prbl[17][25].PRy = 0.29276314f; 
prbl[17][26].PRx = 0.3539604f; 
prbl[17][26].PRy = 0.29591838f; 
prbl[17][27].PRx = 0.32070708f; 
prbl[17][27].PRy = 0.29918033f; 
prbl[17][28].PRx = 0.30479452f; 
prbl[17][28].PRy = 0.2857143f; 
prbl[17][29].PRx = 0.32267442f; 
prbl[17][29].PRy = 0.30479452f; 
prbl[17][30].PRx = 0.42584747f; 
prbl[17][30].PRy = 0.39285713f; 
prbl[17][31].PRx = 0.31493506f; 
prbl[17][31].PRy = 0.30952382f; 
prbl[17][32].PRx = 0.35326087f; 
prbl[17][32].PRy = 0.3392857f; 

prbl[18][0].PRx = 0.2767857f; 
prbl[18][0].PRy = 0.2962963f; 
prbl[18][1].PRx = 0.47474748f; 
prbl[18][1].PRy = 0.57222223f; 
prbl[18][2].PRx = 0.27011493f; 
prbl[18][2].PRy = 0.27222222f; 
prbl[18][3].PRx = 0.5903846f; 
prbl[18][3].PRy = 0.62666667f; 
prbl[18][4].PRx = 0.47126436f; 
prbl[18][4].PRy = 0.50409836f; 
prbl[18][5].PRx = 0.27960527f; 
prbl[18][5].PRy = 0.27439025f; 
prbl[18][6].PRx = 0.42613637f; 
prbl[18][6].PRy = 0.5f; 
prbl[18][7].PRx = 0.2596154f; 
prbl[18][7].PRy = 0.25f; 
prbl[18][8].PRx = 0.5689655f; 
prbl[18][8].PRy = 0.5625f; 
prbl[18][9].PRx = 0.54133856f; 
prbl[18][9].PRy = 0.5486111f; 
prbl[18][10].PRx = 0.5261194f; 
prbl[18][10].PRy = 0.49157304f; 
prbl[18][11].PRx = 0.5702703f; 
prbl[18][11].PRy = 0.6676136f; 
prbl[18][12].PRx = 0.5786164f; 
prbl[18][12].PRy = 0.5673077f; 
prbl[18][13].PRx = 0.46139705f; 
prbl[18][13].PRy = 0.4826087f; 
prbl[18][14].PRx = 0.6769481f; 
prbl[18][14].PRy = 0.7373737f; 
prbl[18][15].PRx = 0.63366336f; 
prbl[18][15].PRy = 0.65625f; 
prbl[18][16].PRx = 0.54950494f; 
prbl[18][16].PRy = 0.6541096f; 
prbl[18][17].PRx = 0.2971698f; 
prbl[18][17].PRy = 0.28645834f; 
prbl[18][18].PRx = 0.29285714f; 
prbl[18][18].PRy = 0.2840909f; 
prbl[18][19].PRx = 0.56302524f; 
prbl[18][19].PRy = 0.57848835f; 
prbl[18][20].PRx = 0.28826532f; 
prbl[18][20].PRy = 0.2875f; 
prbl[18][21].PRx = 0.2930328f; 
prbl[18][21].PRy = 0.2943038f; 
prbl[18][22].PRx = 0.30147058f; 
prbl[18][22].PRy = 0.29545453f; 
prbl[18][23].PRx = 0.38020834f; 
prbl[18][23].PRy = 0.32142857f; 
prbl[18][24].PRx = 0.29216868f; 
prbl[18][24].PRy = 0.28846154f; 
prbl[18][25].PRx = 0.28883496f; 
prbl[18][25].PRy = 0.29924244f; 
prbl[18][26].PRx = 0.4030612f; 
prbl[18][26].PRy = 0.38114753f; 
prbl[18][27].PRx = 0.3372093f; 
prbl[18][27].PRy = 0.32307693f; 
prbl[18][28].PRx = 0.3409091f; 
prbl[18][28].PRy = 0.30597016f; 
prbl[18][29].PRx = 0.3068182f; 
prbl[18][29].PRy = 0.29385966f; 
prbl[18][30].PRx = 0.35353535f; 
prbl[18][30].PRy = 0.3705357f; 
prbl[18][31].PRx = 0.36333334f; 
prbl[18][31].PRy = 0.32786885f; 
prbl[18][32].PRx = 0.35344827f; 
prbl[18][32].PRy = 0.34868422f; 

prbl[19][0].PRx = 0.67142856f; 
prbl[19][0].PRy = 0.6484375f; 
prbl[19][1].PRx = 0.36875f; 
prbl[19][1].PRy = 0.43589744f; 
prbl[19][2].PRx = 0.25f; 
prbl[19][2].PRy = 0.25f; 
prbl[19][3].PRx = 0.71350366f; 
prbl[19][3].PRy = 0.77150536f; 
prbl[19][4].PRx = 0.5703125f; 
prbl[19][4].PRy = 0.6f; 
prbl[19][5].PRx = 0.26209676f; 
prbl[19][5].PRy = 0.25862068f; 
prbl[19][6].PRx = 0.3125f; 
prbl[19][6].PRy = 0.3223684f; 
prbl[19][7].PRx = 0.25f; 
prbl[19][7].PRy = 0.25f; 
prbl[19][8].PRx = 0.50206614f; 
prbl[19][8].PRy = 0.42910448f; 
prbl[19][9].PRx = 0.4908088f; 
prbl[19][9].PRy = 0.475f; 
prbl[19][10].PRx = 0.5407801f; 
prbl[19][10].PRy = 0.52427185f; 
prbl[19][11].PRx = 0.49177632f; 
prbl[19][11].PRy = 0.46103895f; 
prbl[19][12].PRx = 0.53348213f; 
prbl[19][12].PRy = 0.55128205f; 
prbl[19][13].PRx = 0.48653847f; 
prbl[19][13].PRy = 0.47f; 
prbl[19][14].PRx = 0.70859873f; 
prbl[19][14].PRy = 0.7752525f; 
prbl[19][15].PRx = 0.58206105f; 
prbl[19][15].PRy = 0.5856481f; 
prbl[19][16].PRx = 0.45698926f; 
prbl[19][16].PRy = 0.5735294f; 
prbl[19][17].PRx = 0.3901099f; 
prbl[19][17].PRy = 0.42083332f; 
prbl[19][18].PRx = 0.3515625f; 
prbl[19][18].PRy = 0.39534885f; 
prbl[19][19].PRx = 0.54545456f; 
prbl[19][19].PRy = 0.6123188f; 
prbl[19][20].PRx = 0.4189815f; 
prbl[19][20].PRy = 0.45754716f; 
prbl[19][21].PRx = 0.30851063f; 
prbl[19][21].PRy = 0.29098362f; 
prbl[19][22].PRx = 0.3106796f; 
prbl[19][22].PRy = 0.2801724f; 
prbl[19][23].PRx = 0.30851063f; 
prbl[19][23].PRy = 0.28070176f; 
prbl[19][24].PRx = 0.30844155f; 
prbl[19][24].PRy = 0.2947761f; 
prbl[19][25].PRx = 0.28989363f; 
prbl[19][25].PRy = 0.30136988f; 
prbl[19][26].PRx = 0.35817307f; 
prbl[19][26].PRy = 0.37745097f; 
prbl[19][27].PRx = 0.3262195f; 
prbl[19][27].PRy = 0.28636363f; 
prbl[19][28].PRx = 0.32971016f; 
prbl[19][28].PRy = 0.31428573f; 
prbl[19][29].PRx = 0.31707317f; 
prbl[19][29].PRy = 0.34836066f; 
prbl[19][30].PRx = 0.39485982f; 
prbl[19][30].PRy = 0.33474576f; 
prbl[19][31].PRx = 0.3525641f; 
prbl[19][31].PRy = 0.3375f; 
prbl[19][32].PRx = 0.30882353f; 
prbl[19][32].PRy = 0.2867647f; 

prbl[20][0].PRx = 0.640625f; 
prbl[20][0].PRy = 0.6418919f; 
prbl[20][1].PRx = 0.31962025f; 
prbl[20][1].PRy = 0.325f; 
prbl[20][2].PRx = 0.25f; 
prbl[20][2].PRy = 0.25f; 
prbl[20][3].PRx = 0.6645833f; 
prbl[20][3].PRy = 0.68209875f; 
prbl[20][4].PRx = 0.55625f; 
prbl[20][4].PRy = 0.6296296f; 
prbl[20][5].PRx = 0.2769231f; 
prbl[20][5].PRy = 0.27380952f; 
prbl[20][6].PRx = 0.30851063f; 
prbl[20][6].PRy = 0.31f; 
prbl[20][7].PRx = 0.37288135f; 
prbl[20][7].PRy = 0.41379312f; 
prbl[20][8].PRx = 0.5122378f; 
prbl[20][8].PRy = 0.52472526f; 
prbl[20][9].PRx = 0.459854f; 
prbl[20][9].PRy = 0.4480198f; 
prbl[20][10].PRx = 0.41468254f; 
prbl[20][10].PRy = 0.39285713f; 
prbl[20][11].PRx = 0.37295082f; 
prbl[20][11].PRy = 0.3529412f; 
prbl[20][12].PRx = 0.49347827f; 
prbl[20][12].PRy = 0.545082f; 
prbl[20][13].PRx = 0.48404256f; 
prbl[20][13].PRy = 0.4f; 
prbl[20][14].PRx = 0.6215596f; 
prbl[20][14].PRy = 0.6184211f; 
prbl[20][15].PRx = 0.47173914f; 
prbl[20][15].PRy = 0.45454547f; 
prbl[20][16].PRx = 0.39583334f; 
prbl[20][16].PRy = 0.4537037f; 
prbl[20][17].PRx = 0.5188172f; 
prbl[20][17].PRy = 0.5f; 
prbl[20][18].PRx = 0.50728154f; 
prbl[20][18].PRy = 0.48979592f; 
prbl[20][19].PRx = 0.54605263f; 
prbl[20][19].PRy = 0.5603448f; 
prbl[20][20].PRx = 0.5597015f; 
prbl[20][20].PRy = 0.60539216f; 
prbl[20][21].PRx = 0.28395063f; 
prbl[20][21].PRy = 0.2983871f; 
prbl[20][22].PRx = 0.3125f; 
prbl[20][22].PRy = 0.30232558f; 
prbl[20][23].PRx = 0.4752747f; 
prbl[20][23].PRy = 0.49333334f; 
prbl[20][24].PRx = 0.31914893f; 
prbl[20][24].PRy = 0.3018868f; 
prbl[20][25].PRx = 0.3026316f; 
prbl[20][25].PRy = 0.28623188f; 
prbl[20][26].PRx = 0.36868685f; 
prbl[20][26].PRy = 0.325f; 
prbl[20][27].PRx = 0.35119048f; 
prbl[20][27].PRy = 0.3319672f; 
prbl[20][28].PRx = 0.2982456f; 
prbl[20][28].PRy = 0.3137255f; 
prbl[20][29].PRx = 0.38671875f; 
prbl[20][29].PRy = 0.35365853f; 
prbl[20][30].PRx = 0.36458334f; 
prbl[20][30].PRy = 0.35784313f; 
prbl[20][31].PRx = 0.33333334f; 
prbl[20][31].PRy = 0.3192771f; 
prbl[20][32].PRx = 0.2840909f; 
prbl[20][32].PRy = 0.28846154f; 

prbl[21][0].PRx = 0.5125f; 
prbl[21][0].PRy = 0.625f; 
prbl[21][1].PRx = 0.33168316f; 
prbl[21][1].PRy = 0.3480392f; 
prbl[21][2].PRx = 0.28308824f; 
prbl[21][2].PRy = 0.29166666f; 
prbl[21][3].PRx = 0.6369048f; 
prbl[21][3].PRy = 0.7275641f; 
prbl[21][4].PRx = 0.4177215f; 
prbl[21][4].PRy = 0.46634614f; 
prbl[21][5].PRx = 0.27173913f; 
prbl[21][5].PRy = 0.2890625f; 
prbl[21][6].PRx = 0.30882353f; 
prbl[21][6].PRy = 0.32142857f; 
prbl[21][7].PRx = 0.42647058f; 
prbl[21][7].PRy = 0.43534482f; 
prbl[21][8].PRx = 0.3895349f; 
prbl[21][8].PRy = 0.33791208f; 
prbl[21][9].PRx = 0.55944055f; 
prbl[21][9].PRy = 0.53099173f; 
prbl[21][10].PRx = 0.42028984f; 
prbl[21][10].PRy = 0.425f; 
prbl[21][11].PRx = 0.39529914f; 
prbl[21][11].PRy = 0.4030612f; 
prbl[21][12].PRx = 0.6034483f; 
prbl[21][12].PRy = 0.6020408f; 
prbl[21][13].PRx = 0.49444443f; 
prbl[21][13].PRy = 0.49333334f; 
prbl[21][14].PRx = 0.28928572f; 
prbl[21][14].PRy = 0.2983871f; 
prbl[21][15].PRx = 0.4973404f; 
prbl[21][15].PRy = 0.4327957f; 
prbl[21][16].PRx = 0.41239315f; 
prbl[21][16].PRy = 0.44329897f; 
prbl[21][17].PRx = 0.4830097f; 
prbl[21][17].PRy = 0.44602272f; 
prbl[21][18].PRx = 0.39950982f; 
prbl[21][18].PRy = 0.42117116f; 
prbl[21][19].PRx = 0.43814433f; 
prbl[21][19].PRy = 0.45786518f; 
prbl[21][20].PRx = 0.5153846f; 
prbl[21][20].PRy = 0.5404762f; 
prbl[21][21].PRx = 0.35080644f; 
prbl[21][21].PRy = 0.425f; 
prbl[21][22].PRx = 0.47f; 
prbl[21][22].PRy = 0.4679487f; 
prbl[21][23].PRx = 0.48051947f; 
prbl[21][23].PRy = 0.48134327f; 
prbl[21][24].PRx = 0.36574075f; 
prbl[21][24].PRy = 0.4489796f; 
prbl[21][25].PRx = 0.30585107f; 
prbl[21][25].PRy = 0.2947761f; 
prbl[21][26].PRx = 0.3778409f; 
prbl[21][26].PRy = 0.3531746f; 
prbl[21][27].PRx = 0.30952382f; 
prbl[21][27].PRy = 0.30769232f; 
prbl[21][28].PRx = 0.31944445f; 
prbl[21][28].PRy = 0.30508474f; 
prbl[21][29].PRx = 0.35820895f; 
prbl[21][29].PRy = 0.30092594f; 
prbl[21][30].PRx = 0.36607143f; 
prbl[21][30].PRy = 0.3028846f; 
prbl[21][31].PRx = 0.32051283f; 
prbl[21][31].PRy = 0.31944445f; 
prbl[21][32].PRx = 0.32738096f; 
prbl[21][32].PRy = 0.35714287f; 

prbl[22][0].PRx = 0.4453125f; 
prbl[22][0].PRy = 0.47413793f; 
prbl[22][1].PRx = 0.43965518f; 
prbl[22][1].PRy = 0.4385965f; 
prbl[22][2].PRx = 0.37349397f; 
prbl[22][2].PRy = 0.46212122f; 
prbl[22][3].PRx = 0.5991379f; 
prbl[22][3].PRy = 0.63486844f; 
prbl[22][4].PRx = 0.3614865f; 
prbl[22][4].PRy = 0.41326532f; 
prbl[22][5].PRx = 0.42465752f; 
prbl[22][5].PRy = 0.47413793f; 
prbl[22][6].PRx = 0.3919753f; 
prbl[22][6].PRy = 0.475f; 
prbl[22][7].PRx = 0.41021127f; 
prbl[22][7].PRy = 0.44805196f; 
prbl[22][8].PRx = 0.3784722f; 
prbl[22][8].PRy = 0.37209302f; 
prbl[22][9].PRx = 0.460177f; 
prbl[22][9].PRy = 0.45408162f; 
prbl[22][10].PRx = 0.42456895f; 
prbl[22][10].PRy = 0.3422619f; 
prbl[22][11].PRx = 0.42056075f; 
prbl[22][11].PRy = 0.41590908f; 
prbl[22][12].PRx = 0.5057252f; 
prbl[22][12].PRy = 0.49f; 
prbl[22][13].PRx = 0.375f; 
prbl[22][13].PRy = 0.3577586f; 
prbl[22][14].PRx = 0.30421686f; 
prbl[22][14].PRy = 0.31321838f; 
prbl[22][15].PRx = 0.46116504f; 
prbl[22][15].PRy = 0.4480198f; 
prbl[22][16].PRx = 0.47857141f; 
prbl[22][16].PRy = 0.5f; 
prbl[22][17].PRx = 0.491453f; 
prbl[22][17].PRy = 0.45f; 
prbl[22][18].PRx = 0.4059406f; 
prbl[22][18].PRy = 0.4059633f; 
prbl[22][19].PRx = 0.41810346f; 
prbl[22][19].PRy = 0.4173913f; 
prbl[22][20].PRx = 0.3918919f; 
prbl[22][20].PRy = 0.38988096f; 
prbl[22][21].PRx = 0.3683206f; 
prbl[22][21].PRy = 0.33950618f; 
prbl[22][22].PRx = 0.4448819f; 
prbl[22][22].PRy = 0.46022728f; 
prbl[22][23].PRx = 0.34415585f; 
prbl[22][23].PRy = 0.36824325f; 
prbl[22][24].PRx = 0.33684212f; 
prbl[22][24].PRy = 0.33898306f; 
prbl[22][25].PRx = 0.3125f; 
prbl[22][25].PRy = 0.31048387f; 
prbl[22][26].PRx = 0.3258427f; 
prbl[22][26].PRy = 0.3402778f; 
prbl[22][27].PRx = 0.33984375f; 
prbl[22][27].PRy = 0.34705883f; 
prbl[22][28].PRx = 0.3220339f; 
prbl[22][28].PRy = 0.3735294f; 
prbl[22][29].PRx = 0.32534248f; 
prbl[22][29].PRy = 0.33841464f; 
prbl[22][30].PRx = 0.32083333f; 
prbl[22][30].PRy = 0.3051948f; 
prbl[22][31].PRx = 0.34615386f; 
prbl[22][31].PRy = 0.33881578f; 
prbl[22][32].PRx = 0.36764705f; 
prbl[22][32].PRy = 0.30357143f; 

prbl[23][0].PRx = 0.36206895f; 
prbl[23][0].PRy = 0.46153846f; 
prbl[23][1].PRx = 0.44155845f; 
prbl[23][1].PRy = 0.45348838f; 
prbl[23][2].PRx = 0.32831326f; 
prbl[23][2].PRy = 0.3359375f; 
prbl[23][3].PRx = 0.65822786f; 
prbl[23][3].PRy = 0.7087629f; 
prbl[23][4].PRx = 0.53197676f; 
prbl[23][4].PRy = 0.54518074f; 
prbl[23][5].PRx = 0.45454547f; 
prbl[23][5].PRy = 0.46907216f; 
prbl[23][6].PRx = 0.31363636f; 
prbl[23][6].PRy = 0.2956989f; 
prbl[23][7].PRx = 0.314f; 
prbl[23][7].PRy = 0.30454546f; 
prbl[23][8].PRx = 0.3611111f; 
prbl[23][8].PRy = 0.33894232f; 
prbl[23][9].PRx = 0.3277027f; 
prbl[23][9].PRy = 0.3310185f; 
prbl[23][10].PRx = 0.43693694f; 
prbl[23][10].PRy = 0.43548387f; 
prbl[23][11].PRx = 0.3271028f; 
prbl[23][11].PRy = 0.33235294f; 
prbl[23][12].PRx = 0.5022523f; 
prbl[23][12].PRy = 0.48850575f; 
prbl[23][13].PRx = 0.2890625f; 
prbl[23][13].PRy = 0.2947761f; 
prbl[23][14].PRx = 0.2951389f; 
prbl[23][14].PRy = 0.29411766f; 
prbl[23][15].PRx = 0.5102041f; 
prbl[23][15].PRy = 0.47900763f; 
prbl[23][16].PRx = 0.49795082f; 
prbl[23][16].PRy = 0.49050632f; 
prbl[23][17].PRx = 0.5138889f; 
prbl[23][17].PRy = 0.5029412f; 
prbl[23][18].PRx = 0.3945783f; 
prbl[23][18].PRy = 0.42682928f; 
prbl[23][19].PRx = 0.37890625f; 
prbl[23][19].PRy = 0.37857142f; 
prbl[23][20].PRx = 0.3108108f; 
prbl[23][20].PRy = 0.29464287f; 
prbl[23][21].PRx = 0.484f; 
prbl[23][21].PRy = 0.4207317f; 
prbl[23][22].PRx = 0.40031645f; 
prbl[23][22].PRy = 0.38764045f; 
prbl[23][23].PRx = 0.34574467f; 
prbl[23][23].PRy = 0.35755813f; 
prbl[23][24].PRx = 0.2904412f; 
prbl[23][24].PRy = 0.31640625f; 
prbl[23][25].PRx = 0.3045977f; 
prbl[23][25].PRy = 0.2777778f; 
prbl[23][26].PRx = 0.31506848f; 
prbl[23][26].PRy = 0.3112245f; 
prbl[23][27].PRx = 0.3286517f; 
prbl[23][27].PRy = 0.32228917f; 
prbl[23][28].PRx = 0.33988765f; 
prbl[23][28].PRy = 0.35969388f; 
prbl[23][29].PRx = 0.37048194f; 
prbl[23][29].PRy = 0.35638297f; 
prbl[23][30].PRx = 0.3345588f; 
prbl[23][30].PRy = 0.3690476f; 
prbl[23][31].PRx = 0.35273972f; 
prbl[23][31].PRy = 0.3510101f; 
prbl[23][32].PRx = 0.32291666f; 
prbl[23][32].PRy = 0.35f; 

prbl[24][0].PRx = 0.43421054f; 
prbl[24][0].PRy = 0.525f; 
prbl[24][1].PRx = 0.5298165f; 
prbl[24][1].PRy = 0.5441176f; 
prbl[24][2].PRx = 0.42897728f; 
prbl[24][2].PRy = 0.50961536f; 
prbl[24][3].PRx = 0.6705298f; 
prbl[24][3].PRy = 0.68303573f; 
prbl[24][4].PRx = 0.5f; 
prbl[24][4].PRy = 0.5208333f; 
prbl[24][5].PRx = 0.3265306f; 
prbl[24][5].PRy = 0.31153846f; 
prbl[24][6].PRx = 0.31443298f; 
prbl[24][6].PRy = 0.30333334f; 
prbl[24][7].PRx = 0.328125f; 
prbl[24][7].PRy = 0.32175925f; 
prbl[24][8].PRx = 0.35096154f; 
prbl[24][8].PRy = 0.32391304f; 
prbl[24][9].PRx = 0.40263158f; 
prbl[24][9].PRy = 0.39784947f; 
prbl[24][10].PRx = 0.39285713f; 
prbl[24][10].PRy = 0.38333333f; 
prbl[24][11].PRx = 0.2987805f; 
prbl[24][11].PRy = 0.29850745f; 
prbl[24][12].PRx = 0.3653846f; 
prbl[24][12].PRy = 0.3875f; 
prbl[24][13].PRx = 0.40801886f; 
prbl[24][13].PRy = 0.42287233f; 
prbl[24][14].PRx = 0.5327381f; 
prbl[24][14].PRy = 0.5425532f; 
prbl[24][15].PRx = 0.42819148f; 
prbl[24][15].PRy = 0.4041096f; 
prbl[24][16].PRx = 0.523913f; 
prbl[24][16].PRy = 0.5371287f; 
prbl[24][17].PRx = 0.40151516f; 
prbl[24][17].PRy = 0.375f; 
prbl[24][18].PRx = 0.48706895f; 
prbl[24][18].PRy = 0.49115044f; 
prbl[24][19].PRx = 0.508547f; 
prbl[24][19].PRy = 0.5272727f; 
prbl[24][20].PRx = 0.4056604f; 
prbl[24][20].PRy = 0.41333333f; 
prbl[24][21].PRx = 0.43913043f; 
prbl[24][21].PRy = 0.42592594f; 
prbl[24][22].PRx = 0.34756097f; 
prbl[24][22].PRy = 0.3152174f; 
prbl[24][23].PRx = 0.39423078f; 
prbl[24][23].PRy = 0.37662336f; 
prbl[24][24].PRx = 0.29268292f; 
prbl[24][24].PRy = 0.28365386f; 
prbl[24][25].PRx = 0.32876712f; 
prbl[24][25].PRy = 0.30555555f; 
prbl[24][26].PRx = 0.37365592f; 
prbl[24][26].PRy = 0.40068492f; 
prbl[24][27].PRx = 0.39175257f; 
prbl[24][27].PRy = 0.40833333f; 
prbl[24][28].PRx = 0.37365592f; 
prbl[24][28].PRy = 0.39673913f; 
prbl[24][29].PRx = 0.3459596f; 
prbl[24][29].PRy = 0.3559322f; 
prbl[24][30].PRx = 0.38709676f; 
prbl[24][30].PRy = 0.38031915f; 
prbl[24][31].PRx = 0.35217392f; 
prbl[24][31].PRy = 0.34895834f; 
prbl[24][32].PRx = 0.3443396f; 
prbl[24][32].PRy = 0.32738096f; 

prbl[25][0].PRx = 0.61f; 
prbl[25][0].PRy = 0.734375f; 
prbl[25][1].PRx = 0.5366379f; 
prbl[25][1].PRy = 0.5970149f; 
prbl[25][2].PRx = 0.36263737f; 
prbl[25][2].PRy = 0.36206895f; 
prbl[25][3].PRx = 0.5800781f; 
prbl[25][3].PRy = 0.615566f; 
prbl[25][4].PRx = 0.3422619f; 
prbl[25][4].PRy = 0.36570248f; 
prbl[25][5].PRx = 0.3669355f; 
prbl[25][5].PRy = 0.34920636f; 
prbl[25][6].PRx = 0.3298611f; 
prbl[25][6].PRy = 0.34791666f; 
prbl[25][7].PRx = 0.39285713f; 
prbl[25][7].PRy = 0.42008197f; 
prbl[25][8].PRx = 0.4830097f; 
prbl[25][8].PRy = 0.47727272f; 
prbl[25][9].PRx = 0.53346455f; 
prbl[25][9].PRy = 0.5395833f; 
prbl[25][10].PRx = 0.378125f; 
prbl[25][10].PRy = 0.38988096f; 
prbl[25][11].PRx = 0.42905405f; 
prbl[25][11].PRy = 0.38271606f; 
prbl[25][12].PRx = 0.4375f; 
prbl[25][12].PRy = 0.41666666f; 
prbl[25][13].PRx = 0.43165466f; 
prbl[25][13].PRy = 0.44411764f; 
prbl[25][14].PRx = 0.50277776f; 
prbl[25][14].PRy = 0.63793105f; 
prbl[25][15].PRx = 0.5394737f; 
prbl[25][15].PRy = 0.58076924f; 
prbl[25][16].PRx = 0.34375f; 
prbl[25][16].PRy = 0.38020834f; 
prbl[25][17].PRx = 0.4649123f; 
prbl[25][17].PRy = 0.4672619f; 
prbl[25][18].PRx = 0.43452382f; 
prbl[25][18].PRy = 0.44277108f; 
prbl[25][19].PRx = 0.39285713f; 
prbl[25][19].PRy = 0.3814433f; 
prbl[25][20].PRx = 0.38425925f; 
prbl[25][20].PRy = 0.3531746f; 
prbl[25][21].PRx = 0.36597937f; 
prbl[25][21].PRy = 0.34810126f; 
prbl[25][22].PRx = 0.32107842f; 
prbl[25][22].PRy = 0.3360215f; 
prbl[25][23].PRx = 0.51008064f; 
prbl[25][23].PRy = 0.52747256f; 
prbl[25][24].PRx = 0.43316832f; 
prbl[25][24].PRy = 0.3888889f; 
prbl[25][25].PRx = 0.40159574f; 
prbl[25][25].PRy = 0.3809524f; 
prbl[25][26].PRx = 0.43028846f; 
prbl[25][26].PRy = 0.4066265f; 
prbl[25][27].PRx = 0.37643677f; 
prbl[25][27].PRy = 0.4053398f; 
prbl[25][28].PRx = 0.36407766f; 
prbl[25][28].PRy = 0.37135923f; 
prbl[25][29].PRx = 0.37096775f; 
prbl[25][29].PRy = 0.38659793f; 
prbl[25][30].PRx = 0.40707964f; 
prbl[25][30].PRy = 0.40957448f; 
prbl[25][31].PRx = 0.41513762f; 
prbl[25][31].PRy = 0.4009434f; 
prbl[25][32].PRx = 0.39634147f; 
prbl[25][32].PRy = 0.35714287f; 

prbl[26][0].PRx = 0.4090909f; 
prbl[26][0].PRy = 0.39583334f; 
prbl[26][1].PRx = 0.3821839f; 
prbl[26][1].PRy = 0.36797753f; 
prbl[26][2].PRx = 0.36875f; 
prbl[26][2].PRy = 0.38586956f; 
prbl[26][3].PRx = 0.6003937f; 
prbl[26][3].PRy = 0.60504204f; 
prbl[26][4].PRx = 0.36574075f; 
prbl[26][4].PRy = 0.32786885f; 
prbl[26][5].PRx = 0.44207317f; 
prbl[26][5].PRy = 0.4364754f; 
prbl[26][6].PRx = 0.51428574f; 
prbl[26][6].PRy = 0.5144628f; 
prbl[26][7].PRx = 0.5506329f; 
prbl[26][7].PRy = 0.5f; 
prbl[26][8].PRx = 0.49342105f; 
prbl[26][8].PRy = 0.4976852f; 
prbl[26][9].PRx = 0.6402439f; 
prbl[26][9].PRy = 0.6231884f; 
prbl[26][10].PRx = 0.46975806f; 
prbl[26][10].PRy = 0.49484536f; 
prbl[26][11].PRx = 0.45033112f; 
prbl[26][11].PRy = 0.40934065f; 
prbl[26][12].PRx = 0.40460527f; 
prbl[26][12].PRy = 0.39423078f; 
prbl[26][13].PRx = 0.43607953f; 
prbl[26][13].PRy = 0.4691358f; 
prbl[26][14].PRx = 0.5718391f; 
prbl[26][14].PRy = 0.6102941f; 
prbl[26][15].PRx = 0.37878788f; 
prbl[26][15].PRy = 0.4473684f; 
prbl[26][16].PRx = 0.26086956f; 
prbl[26][16].PRy = 0.25f; 
prbl[26][17].PRx = 0.5f; 
prbl[26][17].PRy = 0.46782178f; 
prbl[26][18].PRx = 0.32258064f; 
prbl[26][18].PRy = 0.3452381f; 
prbl[26][19].PRx = 0.31875f; 
prbl[26][19].PRy = 0.3068182f; 
prbl[26][20].PRx = 0.34375f; 
prbl[26][20].PRy = 0.29310346f; 
prbl[26][21].PRx = 0.3142202f; 
prbl[26][21].PRy = 0.30882353f; 
prbl[26][22].PRx = 0.3164557f; 
prbl[26][22].PRy = 0.31460676f; 
prbl[26][23].PRx = 0.4478261f; 
prbl[26][23].PRy = 0.4775641f; 
prbl[26][24].PRx = 0.34234235f; 
prbl[26][24].PRy = 0.34036145f; 
prbl[26][25].PRx = 0.45217392f; 
prbl[26][25].PRy = 0.43939394f; 
prbl[26][26].PRx = 0.4676724f; 
prbl[26][26].PRy = 0.42201835f; 
prbl[26][27].PRx = 0.44008264f; 
prbl[26][27].PRy = 0.43548387f; 
prbl[26][28].PRx = 0.41101694f; 
prbl[26][28].PRy = 0.390625f; 
prbl[26][29].PRx = 0.42716536f; 
prbl[26][29].PRy = 0.42676768f; 
prbl[26][30].PRx = 0.4127907f; 
prbl[26][30].PRy = 0.454f; 
prbl[26][31].PRx = 0.42f; 
prbl[26][31].PRy = 0.40291262f; 
prbl[26][32].PRx = 0.39361703f; 
prbl[26][32].PRy = 0.39204547f; 

prbl[27][0].PRx = 0.27380952f; 
prbl[27][0].PRy = 0.27941176f; 
prbl[27][1].PRx = 0.25f; 
prbl[27][1].PRy = 0.25657895f; 
prbl[27][2].PRx = 0.4409091f; 
prbl[27][2].PRy = 0.4296875f; 
prbl[27][3].PRx = 0.53296703f; 
prbl[27][3].PRy = 0.5247525f; 
prbl[27][4].PRx = 0.50268817f; 
prbl[27][4].PRy = 0.525f; 
prbl[27][5].PRx = 0.4327957f; 
prbl[27][5].PRy = 0.47256097f; 
prbl[27][6].PRx = 0.35197368f; 
prbl[27][6].PRy = 0.34375f; 
prbl[27][7].PRx = 0.38732395f; 
prbl[27][7].PRy = 0.34797296f; 
prbl[27][8].PRx = 0.2882353f; 
prbl[27][8].PRy = 0.28169015f; 
prbl[27][9].PRx = 0.33630952f; 
prbl[27][9].PRy = 0.3429487f; 
prbl[27][10].PRx = 0.502f; 
prbl[27][10].PRy = 0.4855372f; 
prbl[27][11].PRx = 0.4679054f; 
prbl[27][11].PRy = 0.4f; 
prbl[27][12].PRx = 0.36220473f; 
prbl[27][12].PRy = 0.31730768f; 
prbl[27][13].PRx = 0.4105263f; 
prbl[27][13].PRy = 0.38432837f; 
prbl[27][14].PRx = 0.65460527f; 
prbl[27][14].PRy = 0.6903846f; 
prbl[27][15].PRx = 0.48181817f; 
prbl[27][15].PRy = 0.6f; 
prbl[27][16].PRx = 0.25f; 
prbl[27][16].PRy = 0.25f; 
prbl[27][17].PRx = 0.5882353f; 
prbl[27][17].PRy = 0.6111111f; 
prbl[27][18].PRx = 0.4099099f; 
prbl[27][18].PRy = 0.43506494f; 
prbl[27][19].PRx = 0.3368644f; 
prbl[27][19].PRy = 0.3181818f; 
prbl[27][20].PRx = 0.3413793f; 
prbl[27][20].PRy = 0.31060606f; 
prbl[27][21].PRx = 0.335f; 
prbl[27][21].PRy = 0.3131579f; 
prbl[27][22].PRx = 0.31690142f; 
prbl[27][22].PRy = 0.33495146f; 
prbl[27][23].PRx = 0.46261683f; 
prbl[27][23].PRy = 0.47772276f; 
prbl[27][24].PRx = 0.3030303f; 
prbl[27][24].PRy = 0.2939189f; 
prbl[27][25].PRx = 0.4326087f; 
prbl[27][25].PRy = 0.41417912f; 
prbl[27][26].PRx = 0.47445256f; 
prbl[27][26].PRy = 0.44329897f; 
prbl[27][27].PRx = 0.38858697f; 
prbl[27][27].PRy = 0.405f; 
prbl[27][28].PRx = 0.4330357f; 
prbl[27][28].PRy = 0.4244186f; 
prbl[27][29].PRx = 0.44918698f; 
prbl[27][29].PRy = 0.40463918f; 
prbl[27][30].PRx = 0.44583333f; 
prbl[27][30].PRy = 0.42857143f; 
prbl[27][31].PRx = 0.47252747f; 
prbl[27][31].PRy = 0.4173913f; 
prbl[27][32].PRx = 0.35625f; 
prbl[27][32].PRy = 0.40151516f; 

prbl[28][0].PRx = 0.2857143f; 
prbl[28][0].PRy = 0.26785713f; 
prbl[28][1].PRx = 0.2672414f; 
prbl[28][1].PRy = 0.27040815f; 
prbl[28][2].PRx = 0.4084507f; 
prbl[28][2].PRy = 0.42682928f; 
prbl[28][3].PRx = 0.28846154f; 
prbl[28][3].PRy = 0.28308824f; 
prbl[28][4].PRx = 0.31304348f; 
prbl[28][4].PRy = 0.29285714f; 
prbl[28][5].PRx = 0.28481013f; 
prbl[28][5].PRy = 0.27941176f; 
prbl[28][6].PRx = 0.2890625f; 
prbl[28][6].PRy = 0.2757353f; 
prbl[28][7].PRx = 0.296875f; 
prbl[28][7].PRy = 0.3046875f; 
prbl[28][8].PRx = 0.29829547f; 
prbl[28][8].PRy = 0.29285714f; 
prbl[28][9].PRx = 0.2712766f; 
prbl[28][9].PRy = 0.27459016f; 
prbl[28][10].PRx = 0.375f; 
prbl[28][10].PRy = 0.37096775f; 
prbl[28][11].PRx = 0.45495495f; 
prbl[28][11].PRy = 0.42647058f; 
prbl[28][12].PRx = 0.42525774f; 
prbl[28][12].PRy = 0.42732558f; 
prbl[28][13].PRx = 0.5361111f; 
prbl[28][13].PRy = 0.61153847f; 
prbl[28][14].PRx = 0.73504275f; 
prbl[28][14].PRy = 0.8f; 
prbl[28][15].PRx = 0.6666667f; 
prbl[28][15].PRy = 0.6968085f; 
prbl[28][16].PRx = 0.33802816f; 
prbl[28][16].PRy = 0.34375f; 
prbl[28][17].PRx = 0.39759037f; 
prbl[28][17].PRy = 0.42045453f; 
prbl[28][18].PRx = 0.5169903f; 
prbl[28][18].PRy = 0.53532606f; 
prbl[28][19].PRx = 0.4728261f; 
prbl[28][19].PRy = 0.55f; 
prbl[28][20].PRx = 0.39932886f; 
prbl[28][20].PRy = 0.4280822f; 
prbl[28][21].PRx = 0.48595506f; 
prbl[28][21].PRy = 0.47368422f; 
prbl[28][22].PRx = 0.5f; 
prbl[28][22].PRy = 0.48550725f; 
prbl[28][23].PRx = 0.3298611f; 
prbl[28][23].PRy = 0.34701493f; 
prbl[28][24].PRx = 0.3265306f; 
prbl[28][24].PRy = 0.30792683f; 
prbl[28][25].PRx = 0.3652174f; 
prbl[28][25].PRy = 0.33988765f; 
prbl[28][26].PRx = 0.4661017f; 
prbl[28][26].PRy = 0.4972826f; 
prbl[28][27].PRx = 0.44761905f; 
prbl[28][27].PRy = 0.4470339f; 
prbl[28][28].PRx = 0.4332061f; 
prbl[28][28].PRy = 0.42892158f; 
prbl[28][29].PRx = 0.40625f; 
prbl[28][29].PRy = 0.36206895f; 
prbl[28][30].PRx = 0.43345323f; 
prbl[28][30].PRy = 0.4175532f; 
prbl[28][31].PRx = 0.4279661f; 
prbl[28][31].PRy = 0.41588786f; 
prbl[28][32].PRx = 0.39166668f; 
prbl[28][32].PRy = 0.3940678f; 

prbl[29][0].PRx = 0.3181818f; 
prbl[29][0].PRy = 0.3030303f; 
prbl[29][1].PRx = 0.3359375f; 
prbl[29][1].PRy = 0.33561644f; 
prbl[29][2].PRx = 0.32258064f; 
prbl[29][2].PRy = 0.31875f; 
prbl[29][3].PRx = 0.32777777f; 
prbl[29][3].PRy = 0.3090278f; 
prbl[29][4].PRx = 0.34848484f; 
prbl[29][4].PRy = 0.3309859f; 
prbl[29][5].PRx = 0.2977528f; 
prbl[29][5].PRy = 0.29642856f; 
prbl[29][6].PRx = 0.3152174f; 
prbl[29][6].PRy = 0.3607595f; 
prbl[29][7].PRx = 0.37692308f; 
prbl[29][7].PRy = 0.39506173f; 
prbl[29][8].PRx = 0.42605633f; 
prbl[29][8].PRy = 0.42582417f; 
prbl[29][9].PRx = 0.40822786f; 
prbl[29][9].PRy = 0.45f; 
prbl[29][10].PRx = 0.39322916f; 
prbl[29][10].PRy = 0.4383117f; 
prbl[29][11].PRx = 0.4572072f; 
prbl[29][11].PRy = 0.43390805f; 
prbl[29][12].PRx = 0.44270834f; 
prbl[29][12].PRy = 0.4672619f; 
prbl[29][13].PRx = 0.43253967f; 
prbl[29][13].PRy = 0.5f; 
prbl[29][14].PRx = 0.44444445f; 
prbl[29][14].PRy = 0.44915253f; 
prbl[29][15].PRx = 0.5724299f; 
prbl[29][15].PRy = 0.6084337f; 
prbl[29][16].PRx = 0.34649122f; 
prbl[29][16].PRy = 0.34615386f; 
prbl[29][17].PRx = 0.41233766f; 
prbl[29][17].PRy = 0.3705357f; 
prbl[29][18].PRx = 0.3292683f; 
prbl[29][18].PRy = 0.36f; 
prbl[29][19].PRx = 0.50793654f; 
prbl[29][19].PRy = 0.5635593f; 
prbl[29][20].PRx = 0.5122549f; 
prbl[29][20].PRy = 0.53431374f; 
prbl[29][21].PRx = 0.41455695f; 
prbl[29][21].PRy = 0.41532257f; 
prbl[29][22].PRx = 0.33333334f; 
prbl[29][22].PRy = 0.33571428f; 
prbl[29][23].PRx = 0.4005102f; 
prbl[29][23].PRy = 0.41483516f; 
prbl[29][24].PRx = 0.32862905f; 
prbl[29][24].PRy = 0.3065476f; 
prbl[29][25].PRx = 0.35661766f; 
prbl[29][25].PRy = 0.33578432f; 
prbl[29][26].PRx = 0.4358407f; 
prbl[29][26].PRy = 0.43023255f; 
prbl[29][27].PRx = 0.5282258f; 
prbl[29][27].PRy = 0.48290598f; 
prbl[29][28].PRx = 0.45036766f; 
prbl[29][28].PRy = 0.43678162f; 
prbl[29][29].PRx = 0.4140625f; 
prbl[29][29].PRy = 0.4021739f; 
prbl[29][30].PRx = 0.50510204f; 
prbl[29][30].PRy = 0.4576923f; 
prbl[29][31].PRx = 0.44008264f; 
prbl[29][31].PRy = 0.4207317f; 
prbl[29][32].PRx = 0.39903846f; 
prbl[29][32].PRy = 0.35576922f; 

prbl[30][0].PRx = 0.47115386f; 
prbl[30][0].PRy = 0.4649123f; 
prbl[30][1].PRx = 0.4607438f; 
prbl[30][1].PRy = 0.4722222f; 
prbl[30][2].PRx = 0.45454547f; 
prbl[30][2].PRy = 0.49147728f; 
prbl[30][3].PRx = 0.42447916f; 
prbl[30][3].PRy = 0.44623655f; 
prbl[30][4].PRx = 0.2987013f; 
prbl[30][4].PRy = 0.31465518f; 
prbl[30][5].PRx = 0.43661973f; 
prbl[30][5].PRy = 0.44852942f; 
prbl[30][6].PRx = 0.50403225f; 
prbl[30][6].PRy = 0.49583334f; 
prbl[30][7].PRx = 0.29216868f; 
prbl[30][7].PRy = 0.3f; 
prbl[30][8].PRx = 0.29310346f; 
prbl[30][8].PRy = 0.32142857f; 
prbl[30][9].PRx = 0.2875f; 
prbl[30][9].PRy = 0.30701753f; 
prbl[30][10].PRx = 0.46031746f; 
prbl[30][10].PRy = 0.42307693f; 
prbl[30][11].PRx = 0.5601852f; 
prbl[30][11].PRy = 0.5607477f; 
prbl[30][12].PRx = 0.73636365f; 
prbl[30][12].PRy = 0.7076271f; 
prbl[30][13].PRx = 0.29347825f; 
prbl[30][13].PRy = 0.30172414f; 
prbl[30][14].PRx = 0.2904412f; 
prbl[30][14].PRy = 0.28658536f; 
prbl[30][15].PRx = 0.35443038f; 
prbl[30][15].PRy = 0.3377193f; 
prbl[30][16].PRx = 0.3382353f; 
prbl[30][16].PRy = 0.3265306f; 
prbl[30][17].PRx = 0.3531746f; 
prbl[30][17].PRy = 0.33510637f; 
prbl[30][18].PRx = 0.37096775f; 
prbl[30][18].PRy = 0.34302327f; 
prbl[30][19].PRx = 0.30701753f; 
prbl[30][19].PRy = 0.31862745f; 
prbl[30][20].PRx = 0.29545453f; 
prbl[30][20].PRy = 0.30508474f; 
prbl[30][21].PRx = 0.475f; 
prbl[30][21].PRy = 0.41842106f; 
prbl[30][22].PRx = 0.4368421f; 
prbl[30][22].PRy = 0.45343137f; 
prbl[30][23].PRx = 0.35833332f; 
prbl[30][23].PRy = 0.36290324f; 
prbl[30][24].PRx = 0.31132075f; 
prbl[30][24].PRy = 0.306962f; 
prbl[30][25].PRx = 0.3425f; 
prbl[30][25].PRy = 0.328125f; 
prbl[30][26].PRx = 0.3478261f; 
prbl[30][26].PRy = 0.33055556f; 
prbl[30][27].PRx = 0.51346153f; 
prbl[30][27].PRy = 0.5046729f; 
prbl[30][28].PRx = 0.43110237f; 
prbl[30][28].PRy = 0.42592594f; 
prbl[30][29].PRx = 0.43448275f; 
prbl[30][29].PRy = 0.4051724f; 
prbl[30][30].PRx = 0.42708334f; 
prbl[30][30].PRy = 0.42553192f; 
prbl[30][31].PRx = 0.453125f; 
prbl[30][31].PRy = 0.41764706f; 
prbl[30][32].PRx = 0.43055555f; 
prbl[30][32].PRy = 0.43571427f; 

prbl[31][0].PRx = 0.3783784f; 
prbl[31][0].PRy = 0.36309522f; 
prbl[31][1].PRx = 0.5f; 
prbl[31][1].PRy = 0.51785713f; 
prbl[31][2].PRx = 0.36f; 
prbl[31][2].PRy = 0.38483146f; 
prbl[31][3].PRx = 0.43421054f; 
prbl[31][3].PRy = 0.35714287f; 
prbl[31][4].PRx = 0.5697674f; 
prbl[31][4].PRy = 0.56914896f; 
prbl[31][5].PRx = 0.3773585f; 
prbl[31][5].PRy = 0.41875f; 
prbl[31][6].PRx = 0.38920453f; 
prbl[31][6].PRy = 0.39642859f; 
prbl[31][7].PRx = 0.2948718f; 
prbl[31][7].PRy = 0.2857143f; 
prbl[31][8].PRx = 0.29375f; 
prbl[31][8].PRy = 0.27941176f; 
prbl[31][9].PRx = 0.29710144f; 
prbl[31][9].PRy = 0.3137255f; 
prbl[31][10].PRx = 0.3731884f; 
prbl[31][10].PRy = 0.38679245f; 
prbl[31][11].PRx = 0.40068492f; 
prbl[31][11].PRy = 0.42553192f; 
prbl[31][12].PRx = 0.4675926f; 
prbl[31][12].PRy = 0.4659091f; 
prbl[31][13].PRx = 0.3642857f; 
prbl[31][13].PRy = 0.39814815f; 
prbl[31][14].PRx = 0.33116883f; 
prbl[31][14].PRy = 0.37637362f; 
prbl[31][15].PRx = 0.3046875f; 
prbl[31][15].PRy = 0.3386076f; 
prbl[31][16].PRx = 0.33474576f; 
prbl[31][16].PRy = 0.34848484f; 
prbl[31][17].PRx = 0.33730158f; 
prbl[31][17].PRy = 0.34545454f; 
prbl[31][18].PRx = 0.5032051f; 
prbl[31][18].PRy = 0.48214287f; 
prbl[31][19].PRx = 0.4019608f; 
prbl[31][19].PRy = 0.4087838f; 
prbl[31][20].PRx = 0.2986111f; 
prbl[31][20].PRy = 0.2962963f; 
prbl[31][21].PRx = 0.33962265f; 
prbl[31][21].PRy = 0.33846155f; 
prbl[31][22].PRx = 0.32575756f; 
prbl[31][22].PRy = 0.3478261f; 
prbl[31][23].PRx = 0.278125f; 
prbl[31][23].PRy = 0.28873238f; 
prbl[31][24].PRx = 0.31176472f; 
prbl[31][24].PRy = 0.28985506f; 
prbl[31][25].PRx = 0.33333334f; 
prbl[31][25].PRy = 0.3375f; 
prbl[31][26].PRx = 0.39565217f; 
prbl[31][26].PRy = 0.40048543f; 
prbl[31][27].PRx = 0.47440946f; 
prbl[31][27].PRy = 0.42821783f; 
prbl[31][28].PRx = 0.41727942f; 
prbl[31][28].PRy = 0.3581081f; 
prbl[31][29].PRx = 0.44244605f; 
prbl[31][29].PRy = 0.42901236f; 
prbl[31][30].PRx = 0.47420636f; 
prbl[31][30].PRy = 0.4066265f; 
prbl[31][31].PRx = 0.48290598f; 
prbl[31][31].PRy = 0.43529412f; 
prbl[31][32].PRx = 0.5394737f; 
prbl[31][32].PRy = 0.5119048f; 

prbl[32][0].PRx = 0.30357143f; 
prbl[32][0].PRy = 0.3f; 
prbl[32][1].PRx = 0.37857142f; 
prbl[32][1].PRy = 0.3897059f; 
prbl[32][2].PRx = 0.29054055f; 
prbl[32][2].PRy = 0.31428573f; 
prbl[32][3].PRx = 0.5403226f; 
prbl[32][3].PRy = 0.45454547f; 
prbl[32][4].PRx = 0.25f; 
prbl[32][4].PRy = 0.25f; 
prbl[32][5].PRx = 0.5f; 
prbl[32][5].PRy = 0.4642857f; 
prbl[32][6].PRx = 0.32857144f; 
prbl[32][6].PRy = 0.30172414f; 
prbl[32][7].PRx = 0.35f; 
prbl[32][7].PRy = 0.375f; 
prbl[32][8].PRx = 0.6136364f; 
prbl[32][8].PRy = 0.5441176f; 
prbl[32][9].PRx = 0.5f; 
prbl[32][9].PRy = 0.45930234f; 
prbl[32][10].PRx = 0.52272725f; 
prbl[32][10].PRy = 0.5378788f; 
prbl[32][11].PRx = 0.3625f; 
prbl[32][11].PRy = 0.425f; 
prbl[32][12].PRx = 0.4513889f; 
prbl[32][12].PRy = 0.4032258f; 
prbl[32][13].PRx = 0.29285714f; 
prbl[32][13].PRy = 0.3097826f; 
prbl[32][14].PRx = 0.3109756f; 
prbl[32][14].PRy = 0.30921054f; 
prbl[32][15].PRx = 0.29651162f; 
prbl[32][15].PRy = 0.2987805f; 
prbl[32][16].PRx = 0.29545453f; 
prbl[32][16].PRy = 0.29285714f; 
prbl[32][17].PRx = 0.35869566f; 
prbl[32][17].PRy = 0.37820512f; 
prbl[32][18].PRx = 0.5990566f; 
prbl[32][18].PRy = 0.65f; 
prbl[32][19].PRx = 0.41346154f; 
prbl[32][19].PRy = 0.425f; 
prbl[32][20].PRx = 0.27884614f; 
prbl[32][20].PRy = 0.2638889f; 
prbl[32][21].PRx = 0.3043478f; 
prbl[32][21].PRy = 0.30555555f; 
prbl[32][22].PRx = 0.375f; 
prbl[32][22].PRy = 0.32575756f; 
prbl[32][23].PRx = 0.30147058f; 
prbl[32][23].PRy = 0.3f; 
prbl[32][24].PRx = 0.31976745f; 
prbl[32][24].PRy = 0.30769232f; 
prbl[32][25].PRx = 0.42213115f; 
prbl[32][25].PRy = 0.425f; 
prbl[32][26].PRx = 0.42410713f; 
prbl[32][26].PRy = 0.4642857f; 
prbl[32][27].PRx = 0.5f; 
prbl[32][27].PRy = 0.42553192f; 
prbl[32][28].PRx = 0.44583333f; 
prbl[32][28].PRy = 0.44375f; 
prbl[32][29].PRx = 0.4318182f; 
prbl[32][29].PRy = 0.43589744f; 
prbl[32][30].PRx = 0.43461537f; 
prbl[32][30].PRy = 0.4189189f; 
prbl[32][31].PRx = 0.5083333f; 
prbl[32][31].PRy = 0.48026314f; 
prbl[32][32].PRx = 0.35f; 
prbl[32][32].PRy = 0.375f; 



}


public void loadPrecomputedPR_512()

{




prbl[0][0].PRx = 0.34615386f; 
prbl[0][0].PRy = 0.25f; 
prbl[0][1].PRx = 0.29f; 
prbl[0][1].PRy = 0.25833333f; 
prbl[0][2].PRx = 0.30555555f; 
prbl[0][2].PRy = 0.30172414f; 
prbl[0][3].PRx = 0.32f; 
prbl[0][3].PRy = 0.27586207f; 
prbl[0][4].PRx = 0.275f; 
prbl[0][4].PRy = 0.2638889f; 
prbl[0][5].PRx = 0.26612905f; 
prbl[0][5].PRy = 0.2840909f; 
prbl[0][6].PRx = 0.30487806f; 
prbl[0][6].PRy = 0.29285714f; 
prbl[0][7].PRx = 0.42391303f; 
prbl[0][7].PRy = 0.35869566f; 
prbl[0][8].PRx = 0.3669355f; 
prbl[0][8].PRy = 0.3581081f; 
prbl[0][9].PRx = 0.4280303f; 
prbl[0][9].PRy = 0.375f; 
prbl[0][10].PRx = 0.28658536f; 
prbl[0][10].PRy = 0.2826087f; 
prbl[0][11].PRx = 0.35f; 
prbl[0][11].PRy = 0.3472222f; 
prbl[0][12].PRx = 0.30625f; 
prbl[0][12].PRy = 0.3152174f; 
prbl[0][13].PRx = 0.39f; 
prbl[0][13].PRy = 0.38541666f; 
prbl[0][14].PRx = 0.33064517f; 
prbl[0][14].PRy = 0.3392857f; 
prbl[0][15].PRx = 0.3548387f; 
prbl[0][15].PRy = 0.32142857f; 
prbl[0][16].PRx = 0.32142857f; 
prbl[0][16].PRy = 0.3f; 
prbl[0][17].PRx = 0.31976745f; 
prbl[0][17].PRy = 0.32352942f; 
prbl[0][18].PRx = 0.30603448f; 
prbl[0][18].PRy = 0.3f; 
prbl[0][19].PRx = 0.3359375f; 
prbl[0][19].PRy = 0.3125f; 
prbl[0][20].PRx = 0.2982456f; 
prbl[0][20].PRy = 0.30555555f; 
prbl[0][21].PRx = 0.3301887f; 
prbl[0][21].PRy = 0.2890625f; 
prbl[0][22].PRx = 0.31690142f; 
prbl[0][22].PRy = 0.3108108f; 
prbl[0][23].PRx = 0.35655737f; 
prbl[0][23].PRy = 0.3359375f; 
prbl[0][24].PRx = 0.4642857f; 
prbl[0][24].PRy = 0.46296296f; 
prbl[0][25].PRx = 0.31557378f; 
prbl[0][25].PRy = 0.3392857f; 
prbl[0][26].PRx = 0.28787878f; 
prbl[0][26].PRy = 0.30208334f; 
prbl[0][27].PRx = 0.2890625f; 
prbl[0][27].PRy = 0.27941176f; 
prbl[0][28].PRx = 0.2857143f; 
prbl[0][28].PRy = 0.2720588f; 
prbl[0][29].PRx = 0.27380952f; 
prbl[0][29].PRy = 0.2777778f; 
prbl[0][30].PRx = 0.30882353f; 
prbl[0][30].PRy = 0.32f; 
prbl[0][31].PRx = 0.345f; 
prbl[0][31].PRy = 0.25f; 
prbl[0][32].PRx = 0.28f; 
prbl[0][32].PRy = 0.25f; 

prbl[1][0].PRx = 0.2857143f; 
prbl[1][0].PRy = 0.30555555f; 
prbl[1][1].PRx = 0.29375f; 
prbl[1][1].PRy = 0.29245284f; 
prbl[1][2].PRx = 0.32142857f; 
prbl[1][2].PRy = 0.27083334f; 
prbl[1][3].PRx = 0.28947368f; 
prbl[1][3].PRy = 0.29545453f; 
prbl[1][4].PRx = 0.28289473f; 
prbl[1][4].PRy = 0.27222222f; 
prbl[1][5].PRx = 0.28846154f; 
prbl[1][5].PRy = 0.28448275f; 
prbl[1][6].PRx = 0.2962963f; 
prbl[1][6].PRy = 0.30555555f; 
prbl[1][7].PRx = 0.375f; 
prbl[1][7].PRy = 0.33333334f; 
prbl[1][8].PRx = 0.38636363f; 
prbl[1][8].PRy = 0.3806818f; 
prbl[1][9].PRx = 0.5f; 
prbl[1][9].PRy = 0.47839507f; 
prbl[1][10].PRx = 0.3458904f; 
prbl[1][10].PRy = 0.3962264f; 
prbl[1][11].PRx = 0.35887095f; 
prbl[1][11].PRy = 0.38541666f; 
prbl[1][12].PRx = 0.306962f; 
prbl[1][12].PRy = 0.31707317f; 
prbl[1][13].PRx = 0.35882354f; 
prbl[1][13].PRy = 0.33888888f; 
prbl[1][14].PRx = 0.31907895f; 
prbl[1][14].PRy = 0.3068182f; 
prbl[1][15].PRx = 0.31f; 
prbl[1][15].PRy = 0.31666666f; 
prbl[1][16].PRx = 0.29310346f; 
prbl[1][16].PRy = 0.3125f; 
prbl[1][17].PRx = 0.31367925f; 
prbl[1][17].PRy = 0.30357143f; 
prbl[1][18].PRx = 0.31198347f; 
prbl[1][18].PRy = 0.31410256f; 
prbl[1][19].PRx = 0.31190476f; 
prbl[1][19].PRy = 0.30508474f; 
prbl[1][20].PRx = 0.3221154f; 
prbl[1][20].PRy = 0.30555555f; 
prbl[1][21].PRx = 0.31565657f; 
prbl[1][21].PRy = 0.28629032f; 
prbl[1][22].PRx = 0.33333334f; 
prbl[1][22].PRy = 0.31914893f; 
prbl[1][23].PRx = 0.38861385f; 
prbl[1][23].PRy = 0.34375f; 
prbl[1][24].PRx = 0.35714287f; 
prbl[1][24].PRy = 0.29464287f; 
prbl[1][25].PRx = 0.33333334f; 
prbl[1][25].PRy = 0.32692307f; 
prbl[1][26].PRx = 0.30833334f; 
prbl[1][26].PRy = 0.29411766f; 
prbl[1][27].PRx = 0.29301074f; 
prbl[1][27].PRy = 0.2801724f; 
prbl[1][28].PRx = 0.2837838f; 
prbl[1][28].PRy = 0.28301886f; 
prbl[1][29].PRx = 0.278169f; 
prbl[1][29].PRy = 0.28636363f; 
prbl[1][30].PRx = 0.30585107f; 
prbl[1][30].PRy = 0.2767857f; 
prbl[1][31].PRx = 0.36570248f; 
prbl[1][31].PRy = 0.2767857f; 
prbl[1][32].PRx = 0.31944445f; 
prbl[1][32].PRy = 0.25f; 

prbl[2][0].PRx = 0.2890625f; 
prbl[2][0].PRy = 0.2777778f; 
prbl[2][1].PRx = 0.28125f; 
prbl[2][1].PRy = 0.2990196f; 
prbl[2][2].PRx = 0.2881356f; 
prbl[2][2].PRy = 0.29166666f; 
prbl[2][3].PRx = 0.29032257f; 
prbl[2][3].PRy = 0.29435483f; 
prbl[2][4].PRx = 0.28061223f; 
prbl[2][4].PRy = 0.2625f; 
prbl[2][5].PRx = 0.3219178f; 
prbl[2][5].PRy = 0.32627118f; 
prbl[2][6].PRx = 0.35227272f; 
prbl[2][6].PRy = 0.40104166f; 
prbl[2][7].PRx = 0.33561644f; 
prbl[2][7].PRy = 0.3452381f; 
prbl[2][8].PRx = 0.42525774f; 
prbl[2][8].PRy = 0.41504854f; 
prbl[2][9].PRx = 0.5165094f; 
prbl[2][9].PRy = 0.5143678f; 
prbl[2][10].PRx = 0.37356323f; 
prbl[2][10].PRy = 0.39325842f; 
prbl[2][11].PRx = 0.371875f; 
prbl[2][11].PRy = 0.39247313f; 
prbl[2][12].PRx = 0.35333332f; 
prbl[2][12].PRy = 0.33116883f; 
prbl[2][13].PRx = 0.2939189f; 
prbl[2][13].PRy = 0.31578946f; 
prbl[2][14].PRx = 0.29666665f; 
prbl[2][14].PRy = 0.3125f; 
prbl[2][15].PRx = 0.32142857f; 
prbl[2][15].PRy = 0.3153409f; 
prbl[2][16].PRx = 0.32417583f; 
prbl[2][16].PRy = 0.32042253f; 
prbl[2][17].PRx = 0.32142857f; 
prbl[2][17].PRy = 0.30208334f; 
prbl[2][18].PRx = 0.3297414f; 
prbl[2][18].PRy = 0.285f; 
prbl[2][19].PRx = 0.30963302f; 
prbl[2][19].PRy = 0.27586207f; 
prbl[2][20].PRx = 0.31880733f; 
prbl[2][20].PRy = 0.32291666f; 
prbl[2][21].PRx = 0.33878505f; 
prbl[2][21].PRy = 0.3f; 
prbl[2][22].PRx = 0.35897437f; 
prbl[2][22].PRy = 0.34756097f; 
prbl[2][23].PRx = 0.3181818f; 
prbl[2][23].PRy = 0.31578946f; 
prbl[2][24].PRx = 0.31439394f; 
prbl[2][24].PRy = 0.3380682f; 
prbl[2][25].PRx = 0.32051283f; 
prbl[2][25].PRy = 0.30769232f; 
prbl[2][26].PRx = 0.32371795f; 
prbl[2][26].PRy = 0.30072463f; 
prbl[2][27].PRx = 0.3448276f; 
prbl[2][27].PRy = 0.3f; 
prbl[2][28].PRx = 0.36290324f; 
prbl[2][28].PRy = 0.36979166f; 
prbl[2][29].PRx = 0.3005618f; 
prbl[2][29].PRy = 0.28947368f; 
prbl[2][30].PRx = 0.30670103f; 
prbl[2][30].PRy = 0.2712766f; 
prbl[2][31].PRx = 0.35805085f; 
prbl[2][31].PRy = 0.28125f; 
prbl[2][32].PRx = 0.3043478f; 
prbl[2][32].PRy = 0.30833334f; 

prbl[3][0].PRx = 0.2777778f; 
prbl[3][0].PRy = 0.28125f; 
prbl[3][1].PRx = 0.2782258f; 
prbl[3][1].PRy = 0.28070176f; 
prbl[3][2].PRx = 0.27160493f; 
prbl[3][2].PRy = 0.27083334f; 
prbl[3][3].PRx = 0.32089552f; 
prbl[3][3].PRy = 0.3018868f; 
prbl[3][4].PRx = 0.27884614f; 
prbl[3][4].PRy = 0.275f; 
prbl[3][5].PRx = 0.31470588f; 
prbl[3][5].PRy = 0.29385966f; 
prbl[3][6].PRx = 0.5223577f; 
prbl[3][6].PRy = 0.5563725f; 
prbl[3][7].PRx = 0.5095238f; 
prbl[3][7].PRy = 0.5132743f; 
prbl[3][8].PRx = 0.44078946f; 
prbl[3][8].PRy = 0.46039605f; 
prbl[3][9].PRx = 0.44072166f; 
prbl[3][9].PRy = 0.41509435f; 
prbl[3][10].PRx = 0.34615386f; 
prbl[3][10].PRy = 0.3227848f; 
prbl[3][11].PRx = 0.32267442f; 
prbl[3][11].PRy = 0.31349206f; 
prbl[3][12].PRx = 0.2939189f; 
prbl[3][12].PRy = 0.30333334f; 
prbl[3][13].PRx = 0.328125f; 
prbl[3][13].PRy = 0.307971f; 
prbl[3][14].PRx = 0.32534248f; 
prbl[3][14].PRy = 0.3310811f; 
prbl[3][15].PRx = 0.30792683f; 
prbl[3][15].PRy = 0.33544305f; 
prbl[3][16].PRx = 0.3152174f; 
prbl[3][16].PRy = 0.32317072f; 
prbl[3][17].PRx = 0.36136365f; 
prbl[3][17].PRy = 0.35817307f; 
prbl[3][18].PRx = 0.375f; 
prbl[3][18].PRy = 0.33974358f; 
prbl[3][19].PRx = 0.2912844f; 
prbl[3][19].PRy = 0.2761194f; 
prbl[3][20].PRx = 0.34552845f; 
prbl[3][20].PRy = 0.30555555f; 
prbl[3][21].PRx = 0.3297414f; 
prbl[3][21].PRy = 0.32f; 
prbl[3][22].PRx = 0.34705883f; 
prbl[3][22].PRy = 0.32916668f; 
prbl[3][23].PRx = 0.31985295f; 
prbl[3][23].PRy = 0.3283582f; 
prbl[3][24].PRx = 0.29245284f; 
prbl[3][24].PRy = 0.3f; 
prbl[3][25].PRx = 0.31868133f; 
prbl[3][25].PRy = 0.31666666f; 
prbl[3][26].PRx = 0.35f; 
prbl[3][26].PRy = 0.34274194f; 
prbl[3][27].PRx = 0.3494318f; 
prbl[3][27].PRy = 0.3345588f; 
prbl[3][28].PRx = 0.41145834f; 
prbl[3][28].PRy = 0.38793105f; 
prbl[3][29].PRx = 0.32051283f; 
prbl[3][29].PRy = 0.32051283f; 
prbl[3][30].PRx = 0.32380953f; 
prbl[3][30].PRy = 0.2777778f; 
prbl[3][31].PRx = 0.34684685f; 
prbl[3][31].PRy = 0.2857143f; 
prbl[3][32].PRx = 0.32777777f; 
prbl[3][32].PRy = 0.29545453f; 

prbl[4][0].PRx = 0.3046875f; 
prbl[4][0].PRy = 0.30172414f; 
prbl[4][1].PRx = 0.29109588f; 
prbl[4][1].PRy = 0.2846154f; 
prbl[4][2].PRx = 0.30136988f; 
prbl[4][2].PRy = 0.29746836f; 
prbl[4][3].PRx = 0.29411766f; 
prbl[4][3].PRy = 0.2838983f; 
prbl[4][4].PRx = 0.29081634f; 
prbl[4][4].PRy = 0.29166666f; 
prbl[4][5].PRx = 0.30448717f; 
prbl[4][5].PRy = 0.27542374f; 
prbl[4][6].PRx = 0.5625f; 
prbl[4][6].PRy = 0.6f; 
prbl[4][7].PRx = 0.55088496f; 
prbl[4][7].PRy = 0.5237069f; 
prbl[4][8].PRx = 0.5247934f; 
prbl[4][8].PRy = 0.5471698f; 
prbl[4][9].PRx = 0.36858973f; 
prbl[4][9].PRy = 0.4216418f; 
prbl[4][10].PRx = 0.43214285f; 
prbl[4][10].PRy = 0.4746377f; 
prbl[4][11].PRx = 0.40151516f; 
prbl[4][11].PRy = 0.38214287f; 
prbl[4][12].PRx = 0.29032257f; 
prbl[4][12].PRy = 0.32352942f; 
prbl[4][13].PRx = 0.30821916f; 
prbl[4][13].PRy = 0.32916668f; 
prbl[4][14].PRx = 0.34615386f; 
prbl[4][14].PRy = 0.3442623f; 
prbl[4][15].PRx = 0.3112245f; 
prbl[4][15].PRy = 0.31632653f; 
prbl[4][16].PRx = 0.3443396f; 
prbl[4][16].PRy = 0.35227272f; 
prbl[4][17].PRx = 0.46084338f; 
prbl[4][17].PRy = 0.484375f; 
prbl[4][18].PRx = 0.3630435f; 
prbl[4][18].PRy = 0.38541666f; 
prbl[4][19].PRx = 0.3259804f; 
prbl[4][19].PRy = 0.30113637f; 
prbl[4][20].PRx = 0.36228815f; 
prbl[4][20].PRy = 0.32094595f; 
prbl[4][21].PRx = 0.3472222f; 
prbl[4][21].PRy = 0.3101852f; 
prbl[4][22].PRx = 0.32291666f; 
prbl[4][22].PRy = 0.31132075f; 
prbl[4][23].PRx = 0.3110465f; 
prbl[4][23].PRy = 0.29166666f; 
prbl[4][24].PRx = 0.2769608f; 
prbl[4][24].PRy = 0.28424656f; 
prbl[4][25].PRx = 0.34188035f; 
prbl[4][25].PRy = 0.3310811f; 
prbl[4][26].PRx = 0.295f; 
prbl[4][26].PRy = 0.30743244f; 
prbl[4][27].PRx = 0.3564815f; 
prbl[4][27].PRy = 0.32575756f; 
prbl[4][28].PRx = 0.34615386f; 
prbl[4][28].PRy = 0.32692307f; 
prbl[4][29].PRx = 0.34198114f; 
prbl[4][29].PRy = 0.28365386f; 
prbl[4][30].PRx = 0.3068182f; 
prbl[4][30].PRy = 0.28333333f; 
prbl[4][31].PRx = 0.34539473f; 
prbl[4][31].PRy = 0.2972973f; 
prbl[4][32].PRx = 0.30113637f; 
prbl[4][32].PRy = 0.31944445f; 

prbl[5][0].PRx = 0.28125f; 
prbl[5][0].PRy = 0.2867647f; 
prbl[5][1].PRx = 0.28703704f; 
prbl[5][1].PRy = 0.30384615f; 
prbl[5][2].PRx = 0.30555555f; 
prbl[5][2].PRy = 0.28125f; 
prbl[5][3].PRx = 0.2755102f; 
prbl[5][3].PRy = 0.28061223f; 
prbl[5][4].PRx = 0.30592105f; 
prbl[5][4].PRy = 0.2923077f; 
prbl[5][5].PRx = 0.4296875f; 
prbl[5][5].PRy = 0.41369048f; 
prbl[5][6].PRx = 0.45959595f; 
prbl[5][6].PRy = 0.46398306f; 
prbl[5][7].PRx = 0.5321101f; 
prbl[5][7].PRy = 0.5527523f; 
prbl[5][8].PRx = 0.56395346f; 
prbl[5][8].PRy = 0.5778689f; 
prbl[5][9].PRx = 0.56956524f; 
prbl[5][9].PRy = 0.5645161f; 
prbl[5][10].PRx = 0.43681318f; 
prbl[5][10].PRy = 0.5127119f; 
prbl[5][11].PRx = 0.45833334f; 
prbl[5][11].PRy = 0.4419643f; 
prbl[5][12].PRx = 0.38114753f; 
prbl[5][12].PRy = 0.3445946f; 
prbl[5][13].PRx = 0.40163934f; 
prbl[5][13].PRy = 0.38020834f; 
prbl[5][14].PRx = 0.3529412f; 
prbl[5][14].PRy = 0.37727273f; 
prbl[5][15].PRx = 0.3392857f; 
prbl[5][15].PRy = 0.31048387f; 
prbl[5][16].PRx = 0.3015873f; 
prbl[5][16].PRy = 0.2982456f; 
prbl[5][17].PRx = 0.39939025f; 
prbl[5][17].PRy = 0.40625f; 
prbl[5][18].PRx = 0.3907563f; 
prbl[5][18].PRy = 0.37820512f; 
prbl[5][19].PRx = 0.32798165f; 
prbl[5][19].PRy = 0.29310346f; 
prbl[5][20].PRx = 0.35051546f; 
prbl[5][20].PRy = 0.2990196f; 
prbl[5][21].PRx = 0.31182796f; 
prbl[5][21].PRy = 0.28804347f; 
prbl[5][22].PRx = 0.325f; 
prbl[5][22].PRy = 0.29385966f; 
prbl[5][23].PRx = 0.2984694f; 
prbl[5][23].PRy = 0.29411766f; 
prbl[5][24].PRx = 0.32f; 
prbl[5][24].PRy = 0.3028846f; 
prbl[5][25].PRx = 0.34962407f; 
prbl[5][25].PRy = 0.315f; 
prbl[5][26].PRx = 0.30612245f; 
prbl[5][26].PRy = 0.296875f; 
prbl[5][27].PRx = 0.2912088f; 
prbl[5][27].PRy = 0.275f; 
prbl[5][28].PRx = 0.2890625f; 
prbl[5][28].PRy = 0.296875f; 
prbl[5][29].PRx = 0.3275862f; 
prbl[5][29].PRy = 0.32307693f; 
prbl[5][30].PRx = 0.31034482f; 
prbl[5][30].PRy = 0.3274648f; 
prbl[5][31].PRx = 0.4119318f; 
prbl[5][31].PRy = 0.34183672f; 
prbl[5][32].PRx = 0.31632653f; 
prbl[5][32].PRy = 0.3152174f; 

prbl[6][0].PRx = 0.3030303f; 
prbl[6][0].PRy = 0.2857143f; 
prbl[6][1].PRx = 0.30882353f; 
prbl[6][1].PRy = 0.29f; 
prbl[6][2].PRx = 0.296875f; 
prbl[6][2].PRy = 0.2983871f; 
prbl[6][3].PRx = 0.275f; 
prbl[6][3].PRy = 0.2875f; 
prbl[6][4].PRx = 0.3181818f; 
prbl[6][4].PRy = 0.29113925f; 
prbl[6][5].PRx = 0.438f; 
prbl[6][5].PRy = 0.44210526f; 
prbl[6][6].PRx = 0.471519f; 
prbl[6][6].PRy = 0.5081967f; 
prbl[6][7].PRx = 0.53932583f; 
prbl[6][7].PRy = 0.55508476f; 
prbl[6][8].PRx = 0.644f; 
prbl[6][8].PRy = 0.6811224f; 
prbl[6][9].PRx = 0.6536697f; 
prbl[6][9].PRy = 0.6868421f; 
prbl[6][10].PRx = 0.396f; 
prbl[6][10].PRy = 0.48305085f; 
prbl[6][11].PRx = 0.53417265f; 
prbl[6][11].PRy = 0.55445546f; 
prbl[6][12].PRx = 0.41121495f; 
prbl[6][12].PRy = 0.37171054f; 
prbl[6][13].PRx = 0.3777174f; 
prbl[6][13].PRy = 0.3882979f; 
prbl[6][14].PRx = 0.34475806f; 
prbl[6][14].PRy = 0.32142857f; 
prbl[6][15].PRx = 0.3400901f; 
prbl[6][15].PRy = 0.315625f; 
prbl[6][16].PRx = 0.32908162f; 
prbl[6][16].PRy = 0.30357143f; 
prbl[6][17].PRx = 0.3266129f; 
prbl[6][17].PRy = 0.32471263f; 
prbl[6][18].PRx = 0.38577586f; 
prbl[6][18].PRy = 0.34756097f; 
prbl[6][19].PRx = 0.36853448f; 
prbl[6][19].PRy = 0.3148148f; 
prbl[6][20].PRx = 0.30172414f; 
prbl[6][20].PRy = 0.29710144f; 
prbl[6][21].PRx = 0.3298969f; 
prbl[6][21].PRy = 0.31343284f; 
prbl[6][22].PRx = 0.35779816f; 
prbl[6][22].PRy = 0.36915886f; 
prbl[6][23].PRx = 0.30729166f; 
prbl[6][23].PRy = 0.29642856f; 
prbl[6][24].PRx = 0.356f; 
prbl[6][24].PRy = 0.33035713f; 
prbl[6][25].PRx = 0.37603307f; 
prbl[6][25].PRy = 0.335f; 
prbl[6][26].PRx = 0.3272727f; 
prbl[6][26].PRy = 0.3181818f; 
prbl[6][27].PRx = 0.28092784f; 
prbl[6][27].PRy = 0.27631578f; 
prbl[6][28].PRx = 0.33231708f; 
prbl[6][28].PRy = 0.29347825f; 
prbl[6][29].PRx = 0.30059522f; 
prbl[6][29].PRy = 0.30743244f; 
prbl[6][30].PRx = 0.31325302f; 
prbl[6][30].PRy = 0.30327868f; 
prbl[6][31].PRx = 0.30769232f; 
prbl[6][31].PRy = 0.28061223f; 
prbl[6][32].PRx = 0.44583333f; 
prbl[6][32].PRy = 0.35f; 

prbl[7][0].PRx = 0.28846154f; 
prbl[7][0].PRy = 0.27027026f; 
prbl[7][1].PRx = 0.2820513f; 
prbl[7][1].PRy = 0.2631579f; 
prbl[7][2].PRx = 0.284375f; 
prbl[7][2].PRy = 0.27155173f; 
prbl[7][3].PRx = 0.27333334f; 
prbl[7][3].PRy = 0.2689394f; 
prbl[7][4].PRx = 0.30241936f; 
prbl[7][4].PRy = 0.2801724f; 
prbl[7][5].PRx = 0.38607594f; 
prbl[7][5].PRy = 0.3669355f; 
prbl[7][6].PRx = 0.49117646f; 
prbl[7][6].PRy = 0.55494505f; 
prbl[7][7].PRx = 0.58455884f; 
prbl[7][7].PRy = 0.6196581f; 
prbl[7][8].PRx = 0.5180723f; 
prbl[7][8].PRy = 0.52459013f; 
prbl[7][9].PRx = 0.5074627f; 
prbl[7][9].PRy = 0.5714286f; 
prbl[7][10].PRx = 0.5128676f; 
prbl[7][10].PRy = 0.5604839f; 
prbl[7][11].PRx = 0.4715447f; 
prbl[7][11].PRy = 0.5574324f; 
prbl[7][12].PRx = 0.41015625f; 
prbl[7][12].PRy = 0.42777777f; 
prbl[7][13].PRx = 0.38636363f; 
prbl[7][13].PRy = 0.37916666f; 
prbl[7][14].PRx = 0.34868422f; 
prbl[7][14].PRy = 0.3220339f; 
prbl[7][15].PRx = 0.29285714f; 
prbl[7][15].PRy = 0.30384615f; 
prbl[7][16].PRx = 0.32978722f; 
prbl[7][16].PRy = 0.31746033f; 
prbl[7][17].PRx = 0.35869566f; 
prbl[7][17].PRy = 0.3939394f; 
prbl[7][18].PRx = 0.3225f; 
prbl[7][18].PRy = 0.32352942f; 
prbl[7][19].PRx = 0.3533654f; 
prbl[7][19].PRy = 0.3382353f; 
prbl[7][20].PRx = 0.31510416f; 
prbl[7][20].PRy = 0.2977941f; 
prbl[7][21].PRx = 0.30163044f; 
prbl[7][21].PRy = 0.30660376f; 
prbl[7][22].PRx = 0.32178217f; 
prbl[7][22].PRy = 0.31547618f; 
prbl[7][23].PRx = 0.30867347f; 
prbl[7][23].PRy = 0.30357143f; 
prbl[7][24].PRx = 0.39361703f; 
prbl[7][24].PRy = 0.3794643f; 
prbl[7][25].PRx = 0.38793105f; 
prbl[7][25].PRy = 0.36688313f; 
prbl[7][26].PRx = 0.33139536f; 
prbl[7][26].PRy = 0.34666666f; 
prbl[7][27].PRx = 0.3510101f; 
prbl[7][27].PRy = 0.36643836f; 
prbl[7][28].PRx = 0.41584158f; 
prbl[7][28].PRy = 0.42088607f; 
prbl[7][29].PRx = 0.30384615f; 
prbl[7][29].PRy = 0.28623188f; 
prbl[7][30].PRx = 0.31793478f; 
prbl[7][30].PRy = 0.3112245f; 
prbl[7][31].PRx = 0.3417431f; 
prbl[7][31].PRy = 0.33333334f; 
prbl[7][32].PRx = 0.36206895f; 
prbl[7][32].PRy = 0.3125f; 

prbl[8][0].PRx = 0.28125f; 
prbl[8][0].PRy = 0.27083334f; 
prbl[8][1].PRx = 0.27710843f; 
prbl[8][1].PRy = 0.27142859f; 
prbl[8][2].PRx = 0.2739726f; 
prbl[8][2].PRy = 0.2838983f; 
prbl[8][3].PRx = 0.2973684f; 
prbl[8][3].PRy = 0.28947368f; 
prbl[8][4].PRx = 0.3531746f; 
prbl[8][4].PRy = 0.41558442f; 
prbl[8][5].PRx = 0.4697802f; 
prbl[8][5].PRy = 0.5f; 
prbl[8][6].PRx = 0.52288735f; 
prbl[8][6].PRy = 0.57530123f; 
prbl[8][7].PRx = 0.6455696f; 
prbl[8][7].PRy = 0.65865386f; 
prbl[8][8].PRx = 0.5714286f; 
prbl[8][8].PRy = 0.5390625f; 
prbl[8][9].PRx = 0.4710145f; 
prbl[8][9].PRy = 0.4775641f; 
prbl[8][10].PRx = 0.58766234f; 
prbl[8][10].PRy = 0.67241377f; 
prbl[8][11].PRx = 0.49489796f; 
prbl[8][11].PRy = 0.5368421f; 
prbl[8][12].PRx = 0.31f; 
prbl[8][12].PRy = 0.33653846f; 
prbl[8][13].PRx = 0.34f; 
prbl[8][13].PRy = 0.29f; 
prbl[8][14].PRx = 0.3169643f; 
prbl[8][14].PRy = 0.32692307f; 
prbl[8][15].PRx = 0.34322035f; 
prbl[8][15].PRy = 0.35661766f; 
prbl[8][16].PRx = 0.34191176f; 
prbl[8][16].PRy = 0.30392158f; 
prbl[8][17].PRx = 0.42819148f; 
prbl[8][17].PRy = 0.3301887f; 
prbl[8][18].PRx = 0.30337077f; 
prbl[8][18].PRy = 0.31343284f; 
prbl[8][19].PRx = 0.31060606f; 
prbl[8][19].PRy = 0.29605263f; 
prbl[8][20].PRx = 0.30927834f; 
prbl[8][20].PRy = 0.27702704f; 
prbl[8][21].PRx = 0.29591838f; 
prbl[8][21].PRy = 0.27272728f; 
prbl[8][22].PRx = 0.29117647f; 
prbl[8][22].PRy = 0.27966103f; 
prbl[8][23].PRx = 0.30050504f; 
prbl[8][23].PRy = 0.2971698f; 
prbl[8][24].PRx = 0.38297874f; 
prbl[8][24].PRy = 0.32272726f; 
prbl[8][25].PRx = 0.2939189f; 
prbl[8][25].PRy = 0.3125f; 
prbl[8][26].PRx = 0.31578946f; 
prbl[8][26].PRy = 0.3046875f; 
prbl[8][27].PRx = 0.425f; 
prbl[8][27].PRy = 0.34302327f; 
prbl[8][28].PRx = 0.30128205f; 
prbl[8][28].PRy = 0.3181818f; 
prbl[8][29].PRx = 0.28370786f; 
prbl[8][29].PRy = 0.29411766f; 
prbl[8][30].PRx = 0.31428573f; 
prbl[8][30].PRy = 0.27142859f; 
prbl[8][31].PRx = 0.31914893f; 
prbl[8][31].PRy = 0.29661018f; 
prbl[8][32].PRx = 0.4047619f; 
prbl[8][32].PRy = 0.33333334f; 

prbl[9][0].PRx = 0.28658536f; 
prbl[9][0].PRy = 0.29166666f; 
prbl[9][1].PRx = 0.2897196f; 
prbl[9][1].PRy = 0.30603448f; 
prbl[9][2].PRx = 0.27710843f; 
prbl[9][2].PRy = 0.2838983f; 
prbl[9][3].PRx = 0.3412162f; 
prbl[9][3].PRy = 0.38333333f; 
prbl[9][4].PRx = 0.37692308f; 
prbl[9][4].PRy = 0.40384614f; 
prbl[9][5].PRx = 0.49305555f; 
prbl[9][5].PRy = 0.4375f; 
prbl[9][6].PRx = 0.55517244f; 
prbl[9][6].PRy = 0.5612745f; 
prbl[9][7].PRx = 0.5615672f; 
prbl[9][7].PRy = 0.5f; 
prbl[9][8].PRx = 0.56798244f; 
prbl[9][8].PRy = 0.6272727f; 
prbl[9][9].PRx = 0.4482143f; 
prbl[9][9].PRy = 0.46929824f; 
prbl[9][10].PRx = 0.63709676f; 
prbl[9][10].PRy = 0.7621951f; 
prbl[9][11].PRx = 0.6006711f; 
prbl[9][11].PRy = 0.6212871f; 
prbl[9][12].PRx = 0.36956522f; 
prbl[9][12].PRy = 0.385f; 
prbl[9][13].PRx = 0.29123712f; 
prbl[9][13].PRy = 0.29829547f; 
prbl[9][14].PRx = 0.2948718f; 
prbl[9][14].PRy = 0.3f; 
prbl[9][15].PRx = 0.30246913f; 
prbl[9][15].PRy = 0.2923077f; 
prbl[9][16].PRx = 0.32371795f; 
prbl[9][16].PRy = 0.29296875f; 
prbl[9][17].PRx = 0.34708738f; 
prbl[9][17].PRy = 0.31779662f; 
prbl[9][18].PRx = 0.2890625f; 
prbl[9][18].PRy = 0.28214285f; 
prbl[9][19].PRx = 0.30612245f; 
prbl[9][19].PRy = 0.29017857f; 
prbl[9][20].PRx = 0.2875f; 
prbl[9][20].PRy = 0.2974138f; 
prbl[9][21].PRx = 0.30147058f; 
prbl[9][21].PRy = 0.29166666f; 
prbl[9][22].PRx = 0.28504673f; 
prbl[9][22].PRy = 0.28968254f; 
prbl[9][23].PRx = 0.28f; 
prbl[9][23].PRy = 0.27604166f; 
prbl[9][24].PRx = 0.4389535f; 
prbl[9][24].PRy = 0.38846153f; 
prbl[9][25].PRx = 0.29333332f; 
prbl[9][25].PRy = 0.31785715f; 
prbl[9][26].PRx = 0.3090278f; 
prbl[9][26].PRy = 0.30078125f; 
prbl[9][27].PRx = 0.36309522f; 
prbl[9][27].PRy = 0.33606556f; 
prbl[9][28].PRx = 0.3227848f; 
prbl[9][28].PRy = 0.31333333f; 
prbl[9][29].PRx = 0.29761904f; 
prbl[9][29].PRy = 0.32462686f; 
prbl[9][30].PRx = 0.30592105f; 
prbl[9][30].PRy = 0.3046875f; 
prbl[9][31].PRx = 0.35840708f; 
prbl[9][31].PRy = 0.31944445f; 
prbl[9][32].PRx = 0.31944445f; 
prbl[9][32].PRy = 0.33333334f; 

prbl[10][0].PRx = 0.29545453f; 
prbl[10][0].PRy = 0.28846154f; 
prbl[10][1].PRx = 0.2875f; 
prbl[10][1].PRy = 0.2857143f; 
prbl[10][2].PRx = 0.35273972f; 
prbl[10][2].PRy = 0.3597561f; 
prbl[10][3].PRx = 0.3919753f; 
prbl[10][3].PRy = 0.42647058f; 
prbl[10][4].PRx = 0.41304347f; 
prbl[10][4].PRy = 0.43867925f; 
prbl[10][5].PRx = 0.53968257f; 
prbl[10][5].PRy = 0.5282609f; 
prbl[10][6].PRx = 0.5208333f; 
prbl[10][6].PRy = 0.50769234f; 
prbl[10][7].PRx = 0.50625f; 
prbl[10][7].PRy = 0.38392857f; 
prbl[10][8].PRx = 0.47727272f; 
prbl[10][8].PRy = 0.47807017f; 
prbl[10][9].PRx = 0.5479452f; 
prbl[10][9].PRy = 0.5651042f; 
prbl[10][10].PRx = 0.5746951f; 
prbl[10][10].PRy = 0.6584507f; 
prbl[10][11].PRx = 0.56875f; 
prbl[10][11].PRy = 0.6136364f; 
prbl[10][12].PRx = 0.4221698f; 
prbl[10][12].PRy = 0.45f; 
prbl[10][13].PRx = 0.28186274f; 
prbl[10][13].PRy = 0.30405405f; 
prbl[10][14].PRx = 0.29545453f; 
prbl[10][14].PRy = 0.30194804f; 
prbl[10][15].PRx = 0.35915494f; 
prbl[10][15].PRy = 0.3697183f; 
prbl[10][16].PRx = 0.41666666f; 
prbl[10][16].PRy = 0.375f; 
prbl[10][17].PRx = 0.2927928f; 
prbl[10][17].PRy = 0.2972973f; 
prbl[10][18].PRx = 0.30376345f; 
prbl[10][18].PRy = 0.28448275f; 
prbl[10][19].PRx = 0.30526316f; 
prbl[10][19].PRy = 0.28703704f; 
prbl[10][20].PRx = 0.29357797f; 
prbl[10][20].PRy = 0.28947368f; 
prbl[10][21].PRx = 0.28605768f; 
prbl[10][21].PRy = 0.2757353f; 
prbl[10][22].PRx = 0.29126215f; 
prbl[10][22].PRy = 0.2840909f; 
prbl[10][23].PRx = 0.3263158f; 
prbl[10][23].PRy = 0.3219697f; 
prbl[10][24].PRx = 0.4520548f; 
prbl[10][24].PRy = 0.45833334f; 
prbl[10][25].PRx = 0.2838983f; 
prbl[10][25].PRy = 0.34770116f; 
prbl[10][26].PRx = 0.3576389f; 
prbl[10][26].PRy = 0.4021739f; 
prbl[10][27].PRx = 0.38352272f; 
prbl[10][27].PRy = 0.38059703f; 
prbl[10][28].PRx = 0.2792208f; 
prbl[10][28].PRy = 0.2983871f; 
prbl[10][29].PRx = 0.27710843f; 
prbl[10][29].PRy = 0.30294117f; 
prbl[10][30].PRx = 0.32792208f; 
prbl[10][30].PRy = 0.31089744f; 
prbl[10][31].PRx = 0.32089552f; 
prbl[10][31].PRy = 0.3117284f; 
prbl[10][32].PRx = 0.30921054f; 
prbl[10][32].PRy = 0.32142857f; 

prbl[11][0].PRx = 0.2784091f; 
prbl[11][0].PRy = 0.29032257f; 
prbl[11][1].PRx = 0.32462686f; 
prbl[11][1].PRy = 0.3321918f; 
prbl[11][2].PRx = 0.34415585f; 
prbl[11][2].PRy = 0.3618421f; 
prbl[11][3].PRx = 0.49019608f; 
prbl[11][3].PRy = 0.48423424f; 
prbl[11][4].PRx = 0.6044304f; 
prbl[11][4].PRy = 0.596875f; 
prbl[11][5].PRx = 0.5591603f; 
prbl[11][5].PRy = 0.5344828f; 
prbl[11][6].PRx = 0.4770115f; 
prbl[11][6].PRy = 0.4190141f; 
prbl[11][7].PRx = 0.43055555f; 
prbl[11][7].PRy = 0.36594203f; 
prbl[11][8].PRx = 0.5321429f; 
prbl[11][8].PRy = 0.48626372f; 
prbl[11][9].PRx = 0.59615386f; 
prbl[11][9].PRy = 0.6356383f; 
prbl[11][10].PRx = 0.5942029f; 
prbl[11][10].PRy = 0.6196808f; 
prbl[11][11].PRx = 0.52684563f; 
prbl[11][11].PRy = 0.59770113f; 
prbl[11][12].PRx = 0.5214286f; 
prbl[11][12].PRy = 0.5424528f; 
prbl[11][13].PRx = 0.39144737f; 
prbl[11][13].PRy = 0.34848484f; 
prbl[11][14].PRx = 0.3262195f; 
prbl[11][14].PRy = 0.30120483f; 
prbl[11][15].PRx = 0.3f; 
prbl[11][15].PRy = 0.3382353f; 
prbl[11][16].PRx = 0.5826772f; 
prbl[11][16].PRy = 0.5840336f; 
prbl[11][17].PRx = 0.48214287f; 
prbl[11][17].PRy = 0.5165094f; 
prbl[11][18].PRx = 0.3063063f; 
prbl[11][18].PRy = 0.30327868f; 
prbl[11][19].PRx = 0.3097826f; 
prbl[11][19].PRy = 0.28365386f; 
prbl[11][20].PRx = 0.30048078f; 
prbl[11][20].PRy = 0.27941176f; 
prbl[11][21].PRx = 0.30319148f; 
prbl[11][21].PRy = 0.3207547f; 
prbl[11][22].PRx = 0.30585107f; 
prbl[11][22].PRy = 0.30769232f; 
prbl[11][23].PRx = 0.32795697f; 
prbl[11][23].PRy = 0.30555555f; 
prbl[11][24].PRx = 0.3271605f; 
prbl[11][24].PRy = 0.29545453f; 
prbl[11][25].PRx = 0.28f; 
prbl[11][25].PRy = 0.28164557f; 
prbl[11][26].PRx = 0.33881578f; 
prbl[11][26].PRy = 0.2857143f; 
prbl[11][27].PRx = 0.4054878f; 
prbl[11][27].PRy = 0.39423078f; 
prbl[11][28].PRx = 0.30792683f; 
prbl[11][28].PRy = 0.2977941f; 
prbl[11][29].PRx = 0.2875f; 
prbl[11][29].PRy = 0.28666666f; 
prbl[11][30].PRx = 0.32058823f; 
prbl[11][30].PRy = 0.31338027f; 
prbl[11][31].PRx = 0.28896105f; 
prbl[11][31].PRy = 0.28767124f; 
prbl[11][32].PRx = 0.29f; 
prbl[11][32].PRy = 0.28703704f; 

prbl[12][0].PRx = 0.375f; 
prbl[12][0].PRy = 0.30645162f; 
prbl[12][1].PRx = 0.32142857f; 
prbl[12][1].PRy = 0.3256579f; 
prbl[12][2].PRx = 0.40853658f; 
prbl[12][2].PRy = 0.42605633f; 
prbl[12][3].PRx = 0.6955782f; 
prbl[12][3].PRy = 0.6788194f; 
prbl[12][4].PRx = 0.56953645f; 
prbl[12][4].PRy = 0.5654762f; 
prbl[12][5].PRx = 0.49795082f; 
prbl[12][5].PRy = 0.5072464f; 
prbl[12][6].PRx = 0.41237113f; 
prbl[12][6].PRy = 0.3621795f; 
prbl[12][7].PRx = 0.43346775f; 
prbl[12][7].PRy = 0.40159574f; 
prbl[12][8].PRx = 0.4489051f; 
prbl[12][8].PRy = 0.38f; 
prbl[12][9].PRx = 0.654908f; 
prbl[12][9].PRy = 0.6853448f; 
prbl[12][10].PRx = 0.43041238f; 
prbl[12][10].PRy = 0.4609375f; 
prbl[12][11].PRx = 0.5135135f; 
prbl[12][11].PRy = 0.54672897f; 
prbl[12][12].PRx = 0.48175183f; 
prbl[12][12].PRy = 0.51123595f; 
prbl[12][13].PRx = 0.508547f; 
prbl[12][13].PRy = 0.5411765f; 
prbl[12][14].PRx = 0.56971157f; 
prbl[12][14].PRy = 0.5833333f; 
prbl[12][15].PRx = 0.48913044f; 
prbl[12][15].PRy = 0.48397437f; 
prbl[12][16].PRx = 0.58f; 
prbl[12][16].PRy = 0.6148649f; 
prbl[12][17].PRx = 0.6576087f; 
prbl[12][17].PRy = 0.6782407f; 
prbl[12][18].PRx = 0.44078946f; 
prbl[12][18].PRy = 0.45625f; 
prbl[12][19].PRx = 0.3110465f; 
prbl[12][19].PRy = 0.30597016f; 
prbl[12][20].PRx = 0.2925f; 
prbl[12][20].PRy = 0.2801724f; 
prbl[12][21].PRx = 0.2936893f; 
prbl[12][21].PRy = 0.28968254f; 
prbl[12][22].PRx = 0.29213482f; 
prbl[12][22].PRy = 0.28731343f; 
prbl[12][23].PRx = 0.35714287f; 
prbl[12][23].PRy = 0.28947368f; 
prbl[12][24].PRx = 0.2797619f; 
prbl[12][24].PRy = 0.2879747f; 
prbl[12][25].PRx = 0.2967033f; 
prbl[12][25].PRy = 0.30597016f; 
prbl[12][26].PRx = 0.39939025f; 
prbl[12][26].PRy = 0.3423913f; 
prbl[12][27].PRx = 0.42857143f; 
prbl[12][27].PRy = 0.4090909f; 
prbl[12][28].PRx = 0.30833334f; 
prbl[12][28].PRy = 0.2923077f; 
prbl[12][29].PRx = 0.31854838f; 
prbl[12][29].PRy = 0.32352942f; 
prbl[12][30].PRx = 0.29473683f; 
prbl[12][30].PRy = 0.284375f; 
prbl[12][31].PRx = 0.2857143f; 
prbl[12][31].PRy = 0.2993421f; 
prbl[12][32].PRx = 0.29807693f; 
prbl[12][32].PRy = 0.30645162f; 

prbl[13][0].PRx = 0.3f; 
prbl[13][0].PRy = 0.32894737f; 
prbl[13][1].PRx = 0.28082192f; 
prbl[13][1].PRy = 0.29929578f; 
prbl[13][2].PRx = 0.64646465f; 
prbl[13][2].PRy = 0.65178573f; 
prbl[13][3].PRx = 0.6378788f; 
prbl[13][3].PRy = 0.65909094f; 
prbl[13][4].PRx = 0.56085527f; 
prbl[13][4].PRy = 0.55982906f; 
prbl[13][5].PRx = 0.4512195f; 
prbl[13][5].PRy = 0.45f; 
prbl[13][6].PRx = 0.44660193f; 
prbl[13][6].PRy = 0.43925235f; 
prbl[13][7].PRx = 0.40648854f; 
prbl[13][7].PRy = 0.3831522f; 
prbl[13][8].PRx = 0.45895523f; 
prbl[13][8].PRy = 0.415625f; 
prbl[13][9].PRx = 0.54901963f; 
prbl[13][9].PRy = 0.5427928f; 
prbl[13][10].PRx = 0.503876f; 
prbl[13][10].PRy = 0.5601266f; 
prbl[13][11].PRx = 0.4262295f; 
prbl[13][11].PRy = 0.4390244f; 
prbl[13][12].PRx = 0.5150376f; 
prbl[13][12].PRy = 0.5377907f; 
prbl[13][13].PRx = 0.46124032f; 
prbl[13][13].PRy = 0.45114943f; 
prbl[13][14].PRx = 0.4827586f; 
prbl[13][14].PRy = 0.471831f; 
prbl[13][15].PRx = 0.4906015f; 
prbl[13][15].PRy = 0.53125f; 
prbl[13][16].PRx = 0.5095238f; 
prbl[13][16].PRy = 0.5344828f; 
prbl[13][17].PRx = 0.45959595f; 
prbl[13][17].PRy = 0.5660377f; 
prbl[13][18].PRx = 0.56991524f; 
prbl[13][18].PRy = 0.58707863f; 
prbl[13][19].PRx = 0.28608248f; 
prbl[13][19].PRy = 0.29296875f; 
prbl[13][20].PRx = 0.3015464f; 
prbl[13][20].PRy = 0.28333333f; 
prbl[13][21].PRx = 0.3f; 
prbl[13][21].PRy = 0.2974138f; 
prbl[13][22].PRx = 0.29816514f; 
prbl[13][22].PRy = 0.30737704f; 
prbl[13][23].PRx = 0.33241758f; 
prbl[13][23].PRy = 0.30882353f; 
prbl[13][24].PRx = 0.28899083f; 
prbl[13][24].PRy = 0.28521127f; 
prbl[13][25].PRx = 0.2804878f; 
prbl[13][25].PRy = 0.2857143f; 
prbl[13][26].PRx = 0.36890244f; 
prbl[13][26].PRy = 0.3010204f; 
prbl[13][27].PRx = 0.44662923f; 
prbl[13][27].PRy = 0.42708334f; 
prbl[13][28].PRx = 0.3115942f; 
prbl[13][28].PRy = 0.30357143f; 
prbl[13][29].PRx = 0.30479452f; 
prbl[13][29].PRy = 0.3148148f; 
prbl[13][30].PRx = 0.31609195f; 
prbl[13][30].PRy = 0.31716418f; 
prbl[13][31].PRx = 0.2943038f; 
prbl[13][31].PRy = 0.2923077f; 
prbl[13][32].PRx = 0.29651162f; 
prbl[13][32].PRy = 0.2888889f; 

prbl[14][0].PRx = 0.32142857f; 
prbl[14][0].PRy = 0.29166666f; 
prbl[14][1].PRx = 0.44117647f; 
prbl[14][1].PRy = 0.421875f; 
prbl[14][2].PRx = 0.5725f; 
prbl[14][2].PRy = 0.5494792f; 
prbl[14][3].PRx = 0.599537f; 
prbl[14][3].PRy = 0.6075f; 
prbl[14][4].PRx = 0.5648855f; 
prbl[14][4].PRy = 0.5416667f; 
prbl[14][5].PRx = 0.43956044f; 
prbl[14][5].PRy = 0.43125f; 
prbl[14][6].PRx = 0.4579646f; 
prbl[14][6].PRy = 0.4504717f; 
prbl[14][7].PRx = 0.4263393f; 
prbl[14][7].PRy = 0.41911766f; 
prbl[14][8].PRx = 0.4806338f; 
prbl[14][8].PRy = 0.39784947f; 
prbl[14][9].PRx = 0.5219298f; 
prbl[14][9].PRy = 0.51898736f; 
prbl[14][10].PRx = 0.5596774f; 
prbl[14][10].PRy = 0.57568806f; 
prbl[14][11].PRx = 0.53571427f; 
prbl[14][11].PRy = 0.52960527f; 
prbl[14][12].PRx = 0.44886363f; 
prbl[14][12].PRy = 0.42391303f; 
prbl[14][13].PRx = 0.567029f; 
prbl[14][13].PRy = 0.56626505f; 
prbl[14][14].PRx = 0.61413044f; 
prbl[14][14].PRy = 0.62616825f; 
prbl[14][15].PRx = 0.6064815f; 
prbl[14][15].PRy = 0.6385135f; 
prbl[14][16].PRx = 0.52692306f; 
prbl[14][16].PRy = 0.56f; 
prbl[14][17].PRx = 0.30952382f; 
prbl[14][17].PRy = 0.30851063f; 
prbl[14][18].PRx = 0.5208333f; 
prbl[14][18].PRy = 0.58441556f; 
prbl[14][19].PRx = 0.2990196f; 
prbl[14][19].PRy = 0.28688523f; 
prbl[14][20].PRx = 0.30528846f; 
prbl[14][20].PRy = 0.3068182f; 
prbl[14][21].PRx = 0.26960784f; 
prbl[14][21].PRy = 0.275f; 
prbl[14][22].PRx = 0.308f; 
prbl[14][22].PRy = 0.29225352f; 
prbl[14][23].PRx = 0.345f; 
prbl[14][23].PRy = 0.3382353f; 
prbl[14][24].PRx = 0.29807693f; 
prbl[14][24].PRy = 0.28164557f; 
prbl[14][25].PRx = 0.2879747f; 
prbl[14][25].PRy = 0.2767857f; 
prbl[14][26].PRx = 0.31321838f; 
prbl[14][26].PRy = 0.3221154f; 
prbl[14][27].PRx = 0.4393204f; 
prbl[14][27].PRy = 0.3647541f; 
prbl[14][28].PRx = 0.33529413f; 
prbl[14][28].PRy = 0.32954547f; 
prbl[14][29].PRx = 0.2767857f; 
prbl[14][29].PRy = 0.27027026f; 
prbl[14][30].PRx = 0.34444445f; 
prbl[14][30].PRy = 0.32228917f; 
prbl[14][31].PRx = 0.30792683f; 
prbl[14][31].PRy = 0.30147058f; 
prbl[14][32].PRx = 0.31756756f; 
prbl[14][32].PRy = 0.2986111f; 

prbl[15][0].PRx = 0.3125f; 
prbl[15][0].PRy = 0.32142857f; 
prbl[15][1].PRx = 0.48355263f; 
prbl[15][1].PRy = 0.47083333f; 
prbl[15][2].PRx = 0.4057971f; 
prbl[15][2].PRy = 0.4330357f; 
prbl[15][3].PRx = 0.34615386f; 
prbl[15][3].PRy = 0.36f; 
prbl[15][4].PRx = 0.48518518f; 
prbl[15][4].PRy = 0.47697368f; 
prbl[15][5].PRx = 0.5480769f; 
prbl[15][5].PRy = 0.5391566f; 
prbl[15][6].PRx = 0.54330707f; 
prbl[15][6].PRy = 0.5478261f; 
prbl[15][7].PRx = 0.50242716f; 
prbl[15][7].PRy = 0.5f; 
prbl[15][8].PRx = 0.6824324f; 
prbl[15][8].PRy = 0.6904762f; 
prbl[15][9].PRx = 0.58928573f; 
prbl[15][9].PRy = 0.57438016f; 
prbl[15][10].PRx = 0.55151516f; 
prbl[15][10].PRy = 0.58878505f; 
prbl[15][11].PRx = 0.51964283f; 
prbl[15][11].PRy = 0.49444443f; 
prbl[15][12].PRx = 0.5441176f; 
prbl[15][12].PRy = 0.5165094f; 
prbl[15][13].PRx = 0.5410714f; 
prbl[15][13].PRy = 0.52960527f; 
prbl[15][14].PRx = 0.5586207f; 
prbl[15][14].PRy = 0.5714286f; 
prbl[15][15].PRx = 0.68333334f; 
prbl[15][15].PRy = 0.68041235f; 
prbl[15][16].PRx = 0.51666665f; 
prbl[15][16].PRy = 0.53197676f; 
prbl[15][17].PRx = 0.36797753f; 
prbl[15][17].PRy = 0.41847825f; 
prbl[15][18].PRx = 0.5080645f; 
prbl[15][18].PRy = 0.5491803f; 
prbl[15][19].PRx = 0.30246913f; 
prbl[15][19].PRy = 0.30932203f; 
prbl[15][20].PRx = 0.28712872f; 
prbl[15][20].PRy = 0.3080357f; 
prbl[15][21].PRx = 0.29896906f; 
prbl[15][21].PRy = 0.29411766f; 
prbl[15][22].PRx = 0.3030303f; 
prbl[15][22].PRy = 0.29166666f; 
prbl[15][23].PRx = 0.3206522f; 
prbl[15][23].PRy = 0.28645834f; 
prbl[15][24].PRx = 0.28225806f; 
prbl[15][24].PRy = 0.28164557f; 
prbl[15][25].PRx = 0.28494623f; 
prbl[15][25].PRy = 0.29435483f; 
prbl[15][26].PRx = 0.306962f; 
prbl[15][26].PRy = 0.29761904f; 
prbl[15][27].PRx = 0.34438777f; 
prbl[15][27].PRy = 0.33441558f; 
prbl[15][28].PRx = 0.3443396f; 
prbl[15][28].PRy = 0.35674158f; 
prbl[15][29].PRx = 0.33125f; 
prbl[15][29].PRy = 0.32848838f; 
prbl[15][30].PRx = 0.33904108f; 
prbl[15][30].PRy = 0.33505154f; 
prbl[15][31].PRx = 0.3223684f; 
prbl[15][31].PRy = 0.30405405f; 
prbl[15][32].PRx = 0.3125f; 
prbl[15][32].PRy = 0.2983871f; 

prbl[16][0].PRx = 0.3046875f; 
prbl[16][0].PRy = 0.33536586f; 
prbl[16][1].PRx = 0.4715909f; 
prbl[16][1].PRy = 0.49295774f; 
prbl[16][2].PRx = 0.26612905f; 
prbl[16][2].PRy = 0.2685185f; 
prbl[16][3].PRx = 0.46505377f; 
prbl[16][3].PRy = 0.47457626f; 
prbl[16][4].PRx = 0.5292969f; 
prbl[16][4].PRy = 0.525f; 
prbl[16][5].PRx = 0.74776787f; 
prbl[16][5].PRy = 0.82012194f; 
prbl[16][6].PRx = 0.76541096f; 
prbl[16][6].PRy = 0.7790698f; 
prbl[16][7].PRx = 0.6510417f; 
prbl[16][7].PRy = 0.6938776f; 
prbl[16][8].PRx = 0.75614756f; 
prbl[16][8].PRy = 0.81686044f; 
prbl[16][9].PRx = 0.61744964f; 
prbl[16][9].PRy = 0.64402175f; 
prbl[16][10].PRx = 0.47340426f; 
prbl[16][10].PRy = 0.47142857f; 
prbl[16][11].PRx = 0.4830247f; 
prbl[16][11].PRy = 0.54569894f; 
prbl[16][12].PRx = 0.46969697f; 
prbl[16][12].PRy = 0.4489796f; 
prbl[16][13].PRx = 0.49390244f; 
prbl[16][13].PRy = 0.5192308f; 
prbl[16][14].PRx = 0.5553892f; 
prbl[16][14].PRy = 0.554f; 
prbl[16][15].PRx = 0.7058824f; 
prbl[16][15].PRy = 0.775f; 
prbl[16][16].PRx = 0.540404f; 
prbl[16][16].PRy = 0.5535714f; 
prbl[16][17].PRx = 0.43421054f; 
prbl[16][17].PRy = 0.47435898f; 
prbl[16][18].PRx = 0.5862069f; 
prbl[16][18].PRy = 0.68939394f; 
prbl[16][19].PRx = 0.35789475f; 
prbl[16][19].PRy = 0.33064517f; 
prbl[16][20].PRx = 0.29896906f; 
prbl[16][20].PRy = 0.28278688f; 
prbl[16][21].PRx = 0.30357143f; 
prbl[16][21].PRy = 0.2982456f; 
prbl[16][22].PRx = 0.30487806f; 
prbl[16][22].PRy = 0.30454546f; 
prbl[16][23].PRx = 0.3110465f; 
prbl[16][23].PRy = 0.2925532f; 
prbl[16][24].PRx = 0.27925533f; 
prbl[16][24].PRy = 0.29268292f; 
prbl[16][25].PRx = 0.29651162f; 
prbl[16][25].PRy = 0.27651516f; 
prbl[16][26].PRx = 0.32474226f; 
prbl[16][26].PRy = 0.32272726f; 
prbl[16][27].PRx = 0.2987805f; 
prbl[16][27].PRy = 0.27916667f; 
prbl[16][28].PRx = 0.3125f; 
prbl[16][28].PRy = 0.35074627f; 
prbl[16][29].PRx = 0.3776596f; 
prbl[16][29].PRy = 0.37634408f; 
prbl[16][30].PRx = 0.39361703f; 
prbl[16][30].PRy = 0.3640351f; 
prbl[16][31].PRx = 0.3301887f; 
prbl[16][31].PRy = 0.31640625f; 
prbl[16][32].PRx = 0.35714287f; 
prbl[16][32].PRy = 0.32142857f; 

prbl[17][0].PRx = 0.26190478f; 
prbl[17][0].PRy = 0.25641027f; 
prbl[17][1].PRx = 0.47777778f; 
prbl[17][1].PRy = 0.50352114f; 
prbl[17][2].PRx = 0.30357143f; 
prbl[17][2].PRy = 0.31666666f; 
prbl[17][3].PRx = 0.5315534f; 
prbl[17][3].PRy = 0.5254237f; 
prbl[17][4].PRx = 0.53333336f; 
prbl[17][4].PRy = 0.6044776f; 
prbl[17][5].PRx = 0.53494626f; 
prbl[17][5].PRy = 0.5860656f; 
prbl[17][6].PRx = 0.38666666f; 
prbl[17][6].PRy = 0.4611111f; 
prbl[17][7].PRx = 0.3611111f; 
prbl[17][7].PRy = 0.42857143f; 
prbl[17][8].PRx = 0.6394231f; 
prbl[17][8].PRy = 0.68846154f; 
prbl[17][9].PRx = 0.535461f; 
prbl[17][9].PRy = 0.5208333f; 
prbl[17][10].PRx = 0.49807692f; 
prbl[17][10].PRy = 0.48097825f; 
prbl[17][11].PRx = 0.4845679f; 
prbl[17][11].PRy = 0.4945055f; 
prbl[17][12].PRx = 0.51785713f; 
prbl[17][12].PRy = 0.50980395f; 
prbl[17][13].PRx = 0.454f; 
prbl[17][13].PRy = 0.46764705f; 
prbl[17][14].PRx = 0.56456953f; 
prbl[17][14].PRy = 0.5518293f; 
prbl[17][15].PRx = 0.59195405f; 
prbl[17][15].PRy = 0.7019231f; 
prbl[17][16].PRx = 0.5752427f; 
prbl[17][16].PRy = 0.6098901f; 
prbl[17][17].PRx = 0.32228917f; 
prbl[17][17].PRy = 0.33333334f; 
prbl[17][18].PRx = 0.38366336f; 
prbl[17][18].PRy = 0.34649122f; 
prbl[17][19].PRx = 0.4725f; 
prbl[17][19].PRy = 0.44178084f; 
prbl[17][20].PRx = 0.2983871f; 
prbl[17][20].PRy = 0.3f; 
prbl[17][21].PRx = 0.3043478f; 
prbl[17][21].PRy = 0.30384615f; 
prbl[17][22].PRx = 0.29464287f; 
prbl[17][22].PRy = 0.29651162f; 
prbl[17][23].PRx = 0.33f; 
prbl[17][23].PRy = 0.2982456f; 
prbl[17][24].PRx = 0.29656863f; 
prbl[17][24].PRy = 0.29452056f; 
prbl[17][25].PRx = 0.2977528f; 
prbl[17][25].PRy = 0.30120483f; 
prbl[17][26].PRx = 0.34042552f; 
prbl[17][26].PRy = 0.33536586f; 
prbl[17][27].PRx = 0.29347825f; 
prbl[17][27].PRy = 0.29017857f; 
prbl[17][28].PRx = 0.30059522f; 
prbl[17][28].PRy = 0.28521127f; 
prbl[17][29].PRx = 0.32291666f; 
prbl[17][29].PRy = 0.30737704f; 
prbl[17][30].PRx = 0.38235295f; 
prbl[17][30].PRy = 0.35344827f; 
prbl[17][31].PRx = 0.32089552f; 
prbl[17][31].PRy = 0.3345588f; 
prbl[17][32].PRx = 0.30487806f; 
prbl[17][32].PRy = 0.3125f; 

prbl[18][0].PRx = 0.29f; 
prbl[18][0].PRy = 0.30172414f; 
prbl[18][1].PRx = 0.45789474f; 
prbl[18][1].PRy = 0.5408163f; 
prbl[18][2].PRx = 0.30821916f; 
prbl[18][2].PRy = 0.30487806f; 
prbl[18][3].PRx = 0.5538793f; 
prbl[18][3].PRy = 0.5390625f; 
prbl[18][4].PRx = 0.45180723f; 
prbl[18][4].PRy = 0.45703125f; 
prbl[18][5].PRx = 0.29285714f; 
prbl[18][5].PRy = 0.2971698f; 
prbl[18][6].PRx = 0.39945653f; 
prbl[18][6].PRy = 0.45547944f; 
prbl[18][7].PRx = 0.25757575f; 
prbl[18][7].PRy = 0.2638889f; 
prbl[18][8].PRx = 0.55172414f; 
prbl[18][8].PRy = 0.5283019f; 
prbl[18][9].PRx = 0.4779412f; 
prbl[18][9].PRy = 0.48913044f; 
prbl[18][10].PRx = 0.46544716f; 
prbl[18][10].PRy = 0.4651163f; 
prbl[18][11].PRx = 0.53070176f; 
prbl[18][11].PRy = 0.60670733f; 
prbl[18][12].PRx = 0.5462329f; 
prbl[18][12].PRy = 0.49731183f; 
prbl[18][13].PRx = 0.4208633f; 
prbl[18][13].PRy = 0.42201835f; 
prbl[18][14].PRx = 0.65562916f; 
prbl[18][14].PRy = 0.7331461f; 
prbl[18][15].PRx = 0.58157897f; 
prbl[18][15].PRy = 0.6230159f; 
prbl[18][16].PRx = 0.4976415f; 
prbl[18][16].PRy = 0.57857144f; 
prbl[18][17].PRx = 0.31321838f; 
prbl[18][17].PRy = 0.32222223f; 
prbl[18][18].PRx = 0.29225352f; 
prbl[18][18].PRy = 0.27659574f; 
prbl[18][19].PRx = 0.51834863f; 
prbl[18][19].PRy = 0.5410959f; 
prbl[18][20].PRx = 0.30140188f; 
prbl[18][20].PRy = 0.2936508f; 
prbl[18][21].PRx = 0.295f; 
prbl[18][21].PRy = 0.3125f; 
prbl[18][22].PRx = 0.29591838f; 
prbl[18][22].PRy = 0.29411766f; 
prbl[18][23].PRx = 0.33737865f; 
prbl[18][23].PRy = 0.2923077f; 
prbl[18][24].PRx = 0.29473683f; 
prbl[18][24].PRy = 0.27734375f; 
prbl[18][25].PRx = 0.29210526f; 
prbl[18][25].PRy = 0.3026316f; 
prbl[18][26].PRx = 0.35164836f; 
prbl[18][26].PRy = 0.34042552f; 
prbl[18][27].PRx = 0.31493506f; 
prbl[18][27].PRy = 0.31785715f; 
prbl[18][28].PRx = 0.32142857f; 
prbl[18][28].PRy = 0.31338027f; 
prbl[18][29].PRx = 0.29444444f; 
prbl[18][29].PRy = 0.28333333f; 
prbl[18][30].PRx = 0.3425926f; 
prbl[18][30].PRy = 0.34016395f; 
prbl[18][31].PRx = 0.34f; 
prbl[18][31].PRy = 0.32627118f; 
prbl[18][32].PRx = 0.31896552f; 
prbl[18][32].PRy = 0.32352942f; 

prbl[19][0].PRx = 0.6574074f; 
prbl[19][0].PRy = 0.67391306f; 
prbl[19][1].PRx = 0.3994253f; 
prbl[19][1].PRy = 0.42105263f; 
prbl[19][2].PRx = 0.25438598f; 
prbl[19][2].PRy = 0.2631579f; 
prbl[19][3].PRx = 0.7004132f; 
prbl[19][3].PRy = 0.7592593f; 
prbl[19][4].PRx = 0.55454546f; 
prbl[19][4].PRy = 0.58116883f; 
prbl[19][5].PRx = 0.26818183f; 
prbl[19][5].PRy = 0.2638889f; 
prbl[19][6].PRx = 0.3f; 
prbl[19][6].PRy = 0.30851063f; 
prbl[19][7].PRx = 0.25f; 
prbl[19][7].PRy = 0.25f; 
prbl[19][8].PRx = 0.4651163f; 
prbl[19][8].PRy = 0.390625f; 
prbl[19][9].PRx = 0.4766187f; 
prbl[19][9].PRy = 0.44505495f; 
prbl[19][10].PRx = 0.48397437f; 
prbl[19][10].PRy = 0.45789474f; 
prbl[19][11].PRx = 0.475f; 
prbl[19][11].PRy = 0.47697368f; 
prbl[19][12].PRx = 0.5272727f; 
prbl[19][12].PRy = 0.56439394f; 
prbl[19][13].PRx = 0.47379032f; 
prbl[19][13].PRy = 0.47945204f; 
prbl[19][14].PRx = 0.7328767f; 
prbl[19][14].PRy = 0.8207071f; 
prbl[19][15].PRx = 0.5728347f; 
prbl[19][15].PRy = 0.6005435f; 
prbl[19][16].PRx = 0.484375f; 
prbl[19][16].PRy = 0.5692308f; 
prbl[19][17].PRx = 0.3831522f; 
prbl[19][17].PRy = 0.4f; 
prbl[19][18].PRx = 0.3301887f; 
prbl[19][18].PRy = 0.36734694f; 
prbl[19][19].PRx = 0.52150536f; 
prbl[19][19].PRy = 0.5703125f; 
prbl[19][20].PRx = 0.42035398f; 
prbl[19][20].PRy = 0.43846154f; 
prbl[19][21].PRx = 0.29336736f; 
prbl[19][21].PRy = 0.2769231f; 
prbl[19][22].PRx = 0.30113637f; 
prbl[19][22].PRy = 0.29850745f; 
prbl[19][23].PRx = 0.29545453f; 
prbl[19][23].PRy = 0.27727273f; 
prbl[19][24].PRx = 0.28614458f; 
prbl[19][24].PRy = 0.29098362f; 
prbl[19][25].PRx = 0.3068182f; 
prbl[19][25].PRy = 0.30357143f; 
prbl[19][26].PRx = 0.38483146f; 
prbl[19][26].PRy = 0.30555555f; 
prbl[19][27].PRx = 0.3140244f; 
prbl[19][27].PRy = 0.28846154f; 
prbl[19][28].PRx = 0.30792683f; 
prbl[19][28].PRy = 0.31060606f; 
prbl[19][29].PRx = 0.3068182f; 
prbl[19][29].PRy = 0.32421875f; 
prbl[19][30].PRx = 0.36238533f; 
prbl[19][30].PRy = 0.29591838f; 
prbl[19][31].PRx = 0.34285715f; 
prbl[19][31].PRy = 0.3169643f; 
prbl[19][32].PRx = 0.30147058f; 
prbl[19][32].PRy = 0.3108108f; 

prbl[20][0].PRx = 0.63586956f; 
prbl[20][0].PRy = 0.6319444f; 
prbl[20][1].PRx = 0.31730768f; 
prbl[20][1].PRy = 0.3254717f; 
prbl[20][2].PRx = 0.25f; 
prbl[20][2].PRy = 0.25f; 
prbl[20][3].PRx = 0.6402439f; 
prbl[20][3].PRy = 0.6506024f; 
prbl[20][4].PRx = 0.5221519f; 
prbl[20][4].PRy = 0.5283019f; 
prbl[20][5].PRx = 0.278169f; 
prbl[20][5].PRy = 0.27906978f; 
prbl[20][6].PRx = 0.3472222f; 
prbl[20][6].PRy = 0.3716216f; 
prbl[20][7].PRx = 0.3319672f; 
prbl[20][7].PRy = 0.3f; 
prbl[20][8].PRx = 0.4820144f; 
prbl[20][8].PRy = 0.45783132f; 
prbl[20][9].PRx = 0.45661157f; 
prbl[20][9].PRy = 0.4359756f; 
prbl[20][10].PRx = 0.44767442f; 
prbl[20][10].PRy = 0.41853932f; 
prbl[20][11].PRx = 0.3821138f; 
prbl[20][11].PRy = 0.3581081f; 
prbl[20][12].PRx = 0.47008547f; 
prbl[20][12].PRy = 0.53571427f; 
prbl[20][13].PRx = 0.4642857f; 
prbl[20][13].PRy = 0.41836736f; 
prbl[20][14].PRx = 0.63809526f; 
prbl[20][14].PRy = 0.6280488f; 
prbl[20][15].PRx = 0.45408162f; 
prbl[20][15].PRy = 0.43811882f; 
prbl[20][16].PRx = 0.3716216f; 
prbl[20][16].PRy = 0.43421054f; 
prbl[20][17].PRx = 0.49175823f; 
prbl[20][17].PRy = 0.5185185f; 
prbl[20][18].PRx = 0.4236842f; 
prbl[20][18].PRy = 0.44230768f; 
prbl[20][19].PRx = 0.46753246f; 
prbl[20][19].PRy = 0.51369864f; 
prbl[20][20].PRx = 0.56762296f; 
prbl[20][20].PRy = 0.58516484f; 
prbl[20][21].PRx = 0.29945055f; 
prbl[20][21].PRy = 0.3022388f; 
prbl[20][22].PRx = 0.32142857f; 
prbl[20][22].PRy = 0.28947368f; 
prbl[20][23].PRx = 0.45454547f; 
prbl[20][23].PRy = 0.5141509f; 
prbl[20][24].PRx = 0.3115942f; 
prbl[20][24].PRy = 0.27966103f; 
prbl[20][25].PRx = 0.29261363f; 
prbl[20][25].PRy = 0.2923077f; 
prbl[20][26].PRx = 0.36893204f; 
prbl[20][26].PRy = 0.31410256f; 
prbl[20][27].PRx = 0.3382353f; 
prbl[20][27].PRy = 0.32936507f; 
prbl[20][28].PRx = 0.2804054f; 
prbl[20][28].PRy = 0.30084747f; 
prbl[20][29].PRx = 0.3432836f; 
prbl[20][29].PRy = 0.2881356f; 
prbl[20][30].PRx = 0.35840708f; 
prbl[20][30].PRy = 0.33898306f; 
prbl[20][31].PRx = 0.33421052f; 
prbl[20][31].PRy = 0.33f; 
prbl[20][32].PRx = 0.2685185f; 
prbl[20][32].PRy = 0.29166666f; 

prbl[21][0].PRx = 0.53629035f; 
prbl[21][0].PRy = 0.5561224f; 
prbl[21][1].PRx = 0.3478261f; 
prbl[21][1].PRy = 0.425f; 
prbl[21][2].PRx = 0.375f; 
prbl[21][2].PRy = 0.3625f; 
prbl[21][3].PRx = 0.65652174f; 
prbl[21][3].PRy = 0.70679015f; 
prbl[21][4].PRx = 0.41447368f; 
prbl[21][4].PRy = 0.43867925f; 
prbl[21][5].PRx = 0.26612905f; 
prbl[21][5].PRy = 0.2784091f; 
prbl[21][6].PRx = 0.27027026f; 
prbl[21][6].PRy = 0.27419356f; 
prbl[21][7].PRx = 0.40333334f; 
prbl[21][7].PRy = 0.39919356f; 
prbl[21][8].PRx = 0.35784313f; 
prbl[21][8].PRy = 0.3429487f; 
prbl[21][9].PRx = 0.51296294f; 
prbl[21][9].PRy = 0.52840906f; 
prbl[21][10].PRx = 0.44918698f; 
prbl[21][10].PRy = 0.40591398f; 
prbl[21][11].PRx = 0.39285713f; 
prbl[21][11].PRy = 0.38592234f; 
prbl[21][12].PRx = 0.5905797f; 
prbl[21][12].PRy = 0.60638297f; 
prbl[21][13].PRx = 0.47709924f; 
prbl[21][13].PRy = 0.4722222f; 
prbl[21][14].PRx = 0.3139535f; 
prbl[21][14].PRy = 0.3283582f; 
prbl[21][15].PRx = 0.4175532f; 
prbl[21][15].PRy = 0.3783784f; 
prbl[21][16].PRx = 0.41284403f; 
prbl[21][16].PRy = 0.4087838f; 
prbl[21][17].PRx = 0.455f; 
prbl[21][17].PRy = 0.4520548f; 
prbl[21][18].PRx = 0.41369048f; 
prbl[21][18].PRy = 0.4198113f; 
prbl[21][19].PRx = 0.4032258f; 
prbl[21][19].PRy = 0.43010753f; 
prbl[21][20].PRx = 0.53205127f; 
prbl[21][20].PRy = 0.5375f; 
prbl[21][21].PRx = 0.35049018f; 
prbl[21][21].PRy = 0.42666668f; 
prbl[21][22].PRx = 0.43973213f; 
prbl[21][22].PRy = 0.44871795f; 
prbl[21][23].PRx = 0.47115386f; 
prbl[21][23].PRy = 0.5163934f; 
prbl[21][24].PRx = 0.375f; 
prbl[21][24].PRy = 0.41145834f; 
prbl[21][25].PRx = 0.2827381f; 
prbl[21][25].PRy = 0.29098362f; 
prbl[21][26].PRx = 0.35795453f; 
prbl[21][26].PRy = 0.38586956f; 
prbl[21][27].PRx = 0.31460676f; 
prbl[21][27].PRy = 0.31746033f; 
prbl[21][28].PRx = 0.30063292f; 
prbl[21][28].PRy = 0.30597016f; 
prbl[21][29].PRx = 0.3203125f; 
prbl[21][29].PRy = 0.2936508f; 
prbl[21][30].PRx = 0.3423913f; 
prbl[21][30].PRy = 0.29910713f; 
prbl[21][31].PRx = 0.34333333f; 
prbl[21][31].PRy = 0.33712122f; 
prbl[21][32].PRx = 0.30147058f; 
prbl[21][32].PRy = 0.31617647f; 

prbl[22][0].PRx = 0.5f; 
prbl[22][0].PRy = 0.52678573f; 
prbl[22][1].PRx = 0.41847825f; 
prbl[22][1].PRy = 0.4107143f; 
prbl[22][2].PRx = 0.3538961f; 
prbl[22][2].PRy = 0.3382353f; 
prbl[22][3].PRx = 0.5847826f; 
prbl[22][3].PRy = 0.5792683f; 
prbl[22][4].PRx = 0.39788732f; 
prbl[22][4].PRy = 0.4318182f; 
prbl[22][5].PRx = 0.46825397f; 
prbl[22][5].PRy = 0.48809522f; 
prbl[22][6].PRx = 0.35185185f; 
prbl[22][6].PRy = 0.3951613f; 
prbl[22][7].PRx = 0.4082569f; 
prbl[22][7].PRy = 0.45f; 
prbl[22][8].PRx = 0.37790698f; 
prbl[22][8].PRy = 0.35759494f; 
prbl[22][9].PRx = 0.4415888f; 
prbl[22][9].PRy = 0.4475f; 
prbl[22][10].PRx = 0.40086207f; 
prbl[22][10].PRy = 0.3607143f; 
prbl[22][11].PRx = 0.3907563f; 
prbl[22][11].PRy = 0.39950982f; 
prbl[22][12].PRx = 0.5019231f; 
prbl[22][12].PRy = 0.47311828f; 
prbl[22][13].PRx = 0.34953704f; 
prbl[22][13].PRy = 0.33333334f; 
prbl[22][14].PRx = 0.30487806f; 
prbl[22][14].PRy = 0.32971016f; 
prbl[22][15].PRx = 0.42307693f; 
prbl[22][15].PRy = 0.43670887f; 
prbl[22][16].PRx = 0.4744318f; 
prbl[22][16].PRy = 0.5f; 
prbl[22][17].PRx = 0.45833334f; 
prbl[22][17].PRy = 0.46575344f; 
prbl[22][18].PRx = 0.37894738f; 
prbl[22][18].PRy = 0.3828829f; 
prbl[22][19].PRx = 0.41262135f; 
prbl[22][19].PRy = 0.4125f; 
prbl[22][20].PRx = 0.37623763f; 
prbl[22][20].PRy = 0.32467532f; 
prbl[22][21].PRx = 0.3761062f; 
prbl[22][21].PRy = 0.33333334f; 
prbl[22][22].PRx = 0.47897196f; 
prbl[22][22].PRy = 0.49107143f; 
prbl[22][23].PRx = 0.3277027f; 
prbl[22][23].PRy = 0.32954547f; 
prbl[22][24].PRx = 0.3401163f; 
prbl[22][24].PRy = 0.34545454f; 
prbl[22][25].PRx = 0.30294117f; 
prbl[22][25].PRy = 0.30392158f; 
prbl[22][26].PRx = 0.3223684f; 
prbl[22][26].PRy = 0.33333334f; 
prbl[22][27].PRx = 0.2983871f; 
prbl[22][27].PRy = 0.33333334f; 
prbl[22][28].PRx = 0.34210527f; 
prbl[22][28].PRy = 0.36764705f; 
prbl[22][29].PRx = 0.31785715f; 
prbl[22][29].PRy = 0.30597016f; 
prbl[22][30].PRx = 0.31785715f; 
prbl[22][30].PRy = 0.29508197f; 
prbl[22][31].PRx = 0.35714287f; 
prbl[22][31].PRy = 0.3432836f; 
prbl[22][32].PRx = 0.34210527f; 
prbl[22][32].PRy = 0.32f; 

prbl[23][0].PRx = 0.390625f; 
prbl[23][0].PRy = 0.45588234f; 
prbl[23][1].PRx = 0.43235293f; 
prbl[23][1].PRy = 0.41346154f; 
prbl[23][2].PRx = 0.3109756f; 
prbl[23][2].PRy = 0.29545453f; 
prbl[23][3].PRx = 0.62580645f; 
prbl[23][3].PRy = 0.6781915f; 
prbl[23][4].PRx = 0.46907216f; 
prbl[23][4].PRy = 0.5277778f; 
prbl[23][5].PRx = 0.4307229f; 
prbl[23][5].PRy = 0.4652778f; 
prbl[23][6].PRx = 0.32222223f; 
prbl[23][6].PRy = 0.31557378f; 
prbl[23][7].PRx = 0.31617647f; 
prbl[23][7].PRy = 0.3152174f; 
prbl[23][8].PRx = 0.33333334f; 
prbl[23][8].PRy = 0.31712964f; 
prbl[23][9].PRx = 0.29054055f; 
prbl[23][9].PRy = 0.31443298f; 
prbl[23][10].PRx = 0.40865386f; 
prbl[23][10].PRy = 0.43269232f; 
prbl[23][11].PRx = 0.35459185f; 
prbl[23][11].PRy = 0.34302327f; 
prbl[23][12].PRx = 0.48076922f; 
prbl[23][12].PRy = 0.45454547f; 
prbl[23][13].PRx = 0.3125f; 
prbl[23][13].PRy = 0.29109588f; 
prbl[23][14].PRx = 0.29310346f; 
prbl[23][14].PRy = 0.3114035f; 
prbl[23][15].PRx = 0.48134327f; 
prbl[23][15].PRy = 0.46359223f; 
prbl[23][16].PRx = 0.45794392f; 
prbl[23][16].PRy = 0.47945204f; 
prbl[23][17].PRx = 0.491453f; 
prbl[23][17].PRy = 0.434375f; 
prbl[23][18].PRx = 0.36597937f; 
prbl[23][18].PRy = 0.3653846f; 
prbl[23][19].PRx = 0.35416666f; 
prbl[23][19].PRy = 0.3445946f; 
prbl[23][20].PRx = 0.31976745f; 
prbl[23][20].PRy = 0.3097015f; 
prbl[23][21].PRx = 0.42543858f; 
prbl[23][21].PRy = 0.37974682f; 
prbl[23][22].PRx = 0.35496184f; 
prbl[23][22].PRy = 0.38961038f; 
prbl[23][23].PRx = 0.3317757f; 
prbl[23][23].PRy = 0.33854166f; 
prbl[23][24].PRx = 0.3181818f; 
prbl[23][24].PRy = 0.3f; 
prbl[23][25].PRx = 0.30357143f; 
prbl[23][25].PRy = 0.3f; 
prbl[23][26].PRx = 0.29924244f; 
prbl[23][26].PRy = 0.3f; 
prbl[23][27].PRx = 0.32012194f; 
prbl[23][27].PRy = 0.30714285f; 
prbl[23][28].PRx = 0.32142857f; 
prbl[23][28].PRy = 0.3451087f; 
prbl[23][29].PRx = 0.33561644f; 
prbl[23][29].PRy = 0.36290324f; 
prbl[23][30].PRx = 0.34836066f; 
prbl[23][30].PRy = 0.35755813f; 
prbl[23][31].PRx = 0.3581081f; 
prbl[23][31].PRy = 0.35526314f; 
prbl[23][32].PRx = 0.3043478f; 
prbl[23][32].PRy = 0.34146342f; 

prbl[24][0].PRx = 0.4852941f; 
prbl[24][0].PRy = 0.57954544f; 
prbl[24][1].PRx = 0.5235849f; 
prbl[24][1].PRy = 0.53571427f; 
prbl[24][2].PRx = 0.4016854f; 
prbl[24][2].PRy = 0.42f; 
prbl[24][3].PRx = 0.61774194f; 
prbl[24][3].PRy = 0.6319444f; 
prbl[24][4].PRx = 0.4695122f; 
prbl[24][4].PRy = 0.5056818f; 
prbl[24][5].PRx = 0.32589287f; 
prbl[24][5].PRy = 0.2987013f; 
prbl[24][6].PRx = 0.31132075f; 
prbl[24][6].PRy = 0.31790122f; 
prbl[24][7].PRx = 0.33571428f; 
prbl[24][7].PRy = 0.3181818f; 
prbl[24][8].PRx = 0.32421875f; 
prbl[24][8].PRy = 0.30808082f; 
prbl[24][9].PRx = 0.4034091f; 
prbl[24][9].PRy = 0.38709676f; 
prbl[24][10].PRx = 0.40860215f; 
prbl[24][10].PRy = 0.3983051f; 
prbl[24][11].PRx = 0.28932583f; 
prbl[24][11].PRy = 0.2890625f; 
prbl[24][12].PRx = 0.36594203f; 
prbl[24][12].PRy = 0.38f; 
prbl[24][13].PRx = 0.39375f; 
prbl[24][13].PRy = 0.3866279f; 
prbl[24][14].PRx = 0.5106383f; 
prbl[24][14].PRy = 0.5426136f; 
prbl[24][15].PRx = 0.38172042f; 
prbl[24][15].PRy = 0.37676057f; 
prbl[24][16].PRx = 0.52330506f; 
prbl[24][16].PRy = 0.53532606f; 
prbl[24][17].PRx = 0.40625f; 
prbl[24][17].PRy = 0.35869566f; 
prbl[24][18].PRx = 0.4362745f; 
prbl[24][18].PRy = 0.4631579f; 
prbl[24][19].PRx = 0.52165353f; 
prbl[24][19].PRy = 0.5264423f; 
prbl[24][20].PRx = 0.41847825f; 
prbl[24][20].PRy = 0.41197184f; 
prbl[24][21].PRx = 0.4056604f; 
prbl[24][21].PRy = 0.3844086f; 
prbl[24][22].PRx = 0.37083334f; 
prbl[24][22].PRy = 0.3253012f; 
prbl[24][23].PRx = 0.37121212f; 
prbl[24][23].PRy = 0.31640625f; 
prbl[24][24].PRx = 0.30555555f; 
prbl[24][24].PRy = 0.28278688f; 
prbl[24][25].PRx = 0.31325302f; 
prbl[24][25].PRy = 0.27868852f; 
prbl[24][26].PRx = 0.3425926f; 
prbl[24][26].PRy = 0.35576922f; 
prbl[24][27].PRx = 0.3359375f; 
prbl[24][27].PRy = 0.37209302f; 
prbl[24][28].PRx = 0.37987012f; 
prbl[24][28].PRy = 0.3653846f; 
prbl[24][29].PRx = 0.34438777f; 
prbl[24][29].PRy = 0.35416666f; 
prbl[24][30].PRx = 0.36805555f; 
prbl[24][30].PRy = 0.36144578f; 
prbl[24][31].PRx = 0.36893204f; 
prbl[24][31].PRy = 0.35227272f; 
prbl[24][32].PRx = 0.32738096f; 
prbl[24][32].PRy = 0.3181818f; 

prbl[25][0].PRx = 0.47767857f; 
prbl[25][0].PRy = 0.5243902f; 
prbl[25][1].PRx = 0.475f; 
prbl[25][1].PRy = 0.5432692f; 
prbl[25][2].PRx = 0.33841464f; 
prbl[25][2].PRy = 0.32894737f; 
prbl[25][3].PRx = 0.5711207f; 
prbl[25][3].PRy = 0.5988372f; 
prbl[25][4].PRx = 0.31388888f; 
prbl[25][4].PRy = 0.33547008f; 
prbl[25][5].PRx = 0.354f; 
prbl[25][5].PRy = 0.37371135f; 
prbl[25][6].PRx = 0.36445785f; 
prbl[25][6].PRy = 0.3775f; 
prbl[25][7].PRx = 0.37012988f; 
prbl[25][7].PRy = 0.38157895f; 
prbl[25][8].PRx = 0.4481132f; 
prbl[25][8].PRy = 0.44491526f; 
prbl[25][9].PRx = 0.515625f; 
prbl[25][9].PRy = 0.5280172f; 
prbl[25][10].PRx = 0.37328768f; 
prbl[25][10].PRy = 0.4390244f; 
prbl[25][11].PRx = 0.40460527f; 
prbl[25][11].PRy = 0.37171054f; 
prbl[25][12].PRx = 0.4010989f; 
prbl[25][12].PRy = 0.4f; 
prbl[25][13].PRx = 0.39738807f; 
prbl[25][13].PRy = 0.40294117f; 
prbl[25][14].PRx = 0.48404256f; 
prbl[25][14].PRy = 0.5230263f; 
prbl[25][15].PRx = 0.4704301f; 
prbl[25][15].PRy = 0.5394737f; 
prbl[25][16].PRx = 0.33606556f; 
prbl[25][16].PRy = 0.34f; 
prbl[25][17].PRx = 0.47474748f; 
prbl[25][17].PRy = 0.45052084f; 
prbl[25][18].PRx = 0.42672414f; 
prbl[25][18].PRy = 0.4247312f; 
prbl[25][19].PRx = 0.38580248f; 
prbl[25][19].PRy = 0.36021507f; 
prbl[25][20].PRx = 0.33418366f; 
prbl[25][20].PRy = 0.32575756f; 
prbl[25][21].PRx = 0.35714287f; 
prbl[25][21].PRy = 0.36666667f; 
prbl[25][22].PRx = 0.32894737f; 
prbl[25][22].PRy = 0.359375f; 
prbl[25][23].PRx = 0.4627193f; 
prbl[25][23].PRy = 0.4207317f; 
prbl[25][24].PRx = 0.42119566f; 
prbl[25][24].PRy = 0.37307692f; 
prbl[25][25].PRx = 0.38311687f; 
prbl[25][25].PRy = 0.36065573f; 
prbl[25][26].PRx = 0.425f; 
prbl[25][26].PRy = 0.3881579f; 
prbl[25][27].PRx = 0.36633664f; 
prbl[25][27].PRy = 0.36f; 
prbl[25][28].PRx = 0.3638889f; 
prbl[25][28].PRy = 0.3427835f; 
prbl[25][29].PRx = 0.38081396f; 
prbl[25][29].PRy = 0.3668478f; 
prbl[25][30].PRx = 0.3592233f; 
prbl[25][30].PRy = 0.36697248f; 
prbl[25][31].PRx = 0.37239584f; 
prbl[25][31].PRy = 0.3678161f; 
prbl[25][32].PRx = 0.3690476f; 
prbl[25][32].PRy = 0.3716216f; 

prbl[26][0].PRx = 0.34615386f; 
prbl[26][0].PRy = 0.3482143f; 
prbl[26][1].PRx = 0.32102272f; 
prbl[26][1].PRy = 0.3091398f; 
prbl[26][2].PRx = 0.37790698f; 
prbl[26][2].PRy = 0.40384614f; 
prbl[26][3].PRx = 0.60470086f; 
prbl[26][3].PRy = 0.6084906f; 
prbl[26][4].PRx = 0.37692308f; 
prbl[26][4].PRy = 0.35840708f; 
prbl[26][5].PRx = 0.40853658f; 
prbl[26][5].PRy = 0.40929204f; 
prbl[26][6].PRx = 0.5423729f; 
prbl[26][6].PRy = 0.5173267f; 
prbl[26][7].PRx = 0.53521127f; 
prbl[26][7].PRy = 0.50223213f; 
prbl[26][8].PRx = 0.4382716f; 
prbl[26][8].PRy = 0.45833334f; 
prbl[26][9].PRx = 0.60046726f; 
prbl[26][9].PRy = 0.59791666f; 
prbl[26][10].PRx = 0.43589744f; 
prbl[26][10].PRy = 0.43811882f; 
prbl[26][11].PRx = 0.452f; 
prbl[26][11].PRy = 0.43529412f; 
prbl[26][12].PRx = 0.38867188f; 
prbl[26][12].PRy = 0.3938356f; 
prbl[26][13].PRx = 0.44127518f; 
prbl[26][13].PRy = 0.475f; 
prbl[26][14].PRx = 0.56f; 
prbl[26][14].PRy = 0.5609756f; 
prbl[26][15].PRx = 0.36818182f; 
prbl[26][15].PRy = 0.37195122f; 
prbl[26][16].PRx = 0.28645834f; 
prbl[26][16].PRy = 0.2986111f; 
prbl[26][17].PRx = 0.5053192f; 
prbl[26][17].PRy = 0.47777778f; 
prbl[26][18].PRx = 0.32178217f; 
prbl[26][18].PRy = 0.33783785f; 
prbl[26][19].PRx = 0.33958334f; 
prbl[26][19].PRy = 0.30405405f; 
prbl[26][20].PRx = 0.3468992f; 
prbl[26][20].PRy = 0.31355932f; 
prbl[26][21].PRx = 0.31182796f; 
prbl[26][21].PRy = 0.31626505f; 
prbl[26][22].PRx = 0.3125f; 
prbl[26][22].PRy = 0.3255814f; 
prbl[26][23].PRx = 0.42827868f; 
prbl[26][23].PRy = 0.44642857f; 
prbl[26][24].PRx = 0.3575f; 
prbl[26][24].PRy = 0.33641976f; 
prbl[26][25].PRx = 0.39485982f; 
prbl[26][25].PRy = 0.3814433f; 
prbl[26][26].PRx = 0.45325205f; 
prbl[26][26].PRy = 0.4119318f; 
prbl[26][27].PRx = 0.40570176f; 
prbl[26][27].PRy = 0.41489363f; 
prbl[26][28].PRx = 0.37603307f; 
prbl[26][28].PRy = 0.38317758f; 
prbl[26][29].PRx = 0.40765765f; 
prbl[26][29].PRy = 0.38157895f; 
prbl[26][30].PRx = 0.39759037f; 
prbl[26][30].PRy = 0.3954918f; 
prbl[26][31].PRx = 0.3897059f; 
prbl[26][31].PRy = 0.37135923f; 
prbl[26][32].PRx = 0.36666667f; 
prbl[26][32].PRy = 0.3622449f; 

prbl[27][0].PRx = 0.265625f; 
prbl[27][0].PRy = 0.27272728f; 
prbl[27][1].PRx = 0.25367647f; 
prbl[27][1].PRy = 0.25f; 
prbl[27][2].PRx = 0.41095892f; 
prbl[27][2].PRy = 0.40202704f; 
prbl[27][3].PRx = 0.5054348f; 
prbl[27][3].PRy = 0.50252527f; 
prbl[27][4].PRx = 0.57530123f; 
prbl[27][4].PRy = 0.539823f; 
prbl[27][5].PRx = 0.43956044f; 
prbl[27][5].PRy = 0.44101125f; 
prbl[27][6].PRx = 0.3401163f; 
prbl[27][6].PRy = 0.32916668f; 
prbl[27][7].PRx = 0.3442623f; 
prbl[27][7].PRy = 0.31451613f; 
prbl[27][8].PRx = 0.2857143f; 
prbl[27][8].PRy = 0.29276314f; 
prbl[27][9].PRx = 0.36746988f; 
prbl[27][9].PRy = 0.34444445f; 
prbl[27][10].PRx = 0.45432693f; 
prbl[27][10].PRy = 0.44285715f; 
prbl[27][11].PRx = 0.4261745f; 
prbl[27][11].PRy = 0.39f; 
prbl[27][12].PRx = 0.375f; 
prbl[27][12].PRy = 0.32666665f; 
prbl[27][13].PRx = 0.36728394f; 
prbl[27][13].PRy = 0.36940297f; 
prbl[27][14].PRx = 0.7034483f; 
prbl[27][14].PRy = 0.7019231f; 
prbl[27][15].PRx = 0.5833333f; 
prbl[27][15].PRy = 0.64772725f; 
prbl[27][16].PRx = 0.25f; 
prbl[27][16].PRy = 0.25f; 
prbl[27][17].PRx = 0.5521978f; 
prbl[27][17].PRy = 0.59090906f; 
prbl[27][18].PRx = 0.425f; 
prbl[27][18].PRy = 0.41447368f; 
prbl[27][19].PRx = 0.33969465f; 
prbl[27][19].PRy = 0.33666667f; 
prbl[27][20].PRx = 0.33076924f; 
prbl[27][20].PRy = 0.31338027f; 
prbl[27][21].PRx = 0.3175f; 
prbl[27][21].PRy = 0.31493506f; 
prbl[27][22].PRx = 0.3219178f; 
prbl[27][22].PRy = 0.32954547f; 
prbl[27][23].PRx = 0.47395834f; 
prbl[27][23].PRy = 0.4970588f; 
prbl[27][24].PRx = 0.29807693f; 
prbl[27][24].PRy = 0.2977941f; 
prbl[27][25].PRx = 0.422f; 
prbl[27][25].PRy = 0.3908046f; 
prbl[27][26].PRx = 0.4579646f; 
prbl[27][26].PRy = 0.4117647f; 
prbl[27][27].PRx = 0.39540815f; 
prbl[27][27].PRy = 0.3909091f; 
prbl[27][28].PRx = 0.42584747f; 
prbl[27][28].PRy = 0.37019232f; 
prbl[27][29].PRx = 0.43849206f; 
prbl[27][29].PRy = 0.4034091f; 
prbl[27][30].PRx = 0.41666666f; 
prbl[27][30].PRy = 0.3960177f; 
prbl[27][31].PRx = 0.40656567f; 
prbl[27][31].PRy = 0.3671171f; 
prbl[27][32].PRx = 0.375f; 
prbl[27][32].PRy = 0.3644068f; 

prbl[28][0].PRx = 0.29166666f; 
prbl[28][0].PRy = 0.3f; 
prbl[28][1].PRx = 0.25f; 
prbl[28][1].PRy = 0.25581396f; 
prbl[28][2].PRx = 0.3449367f; 
prbl[28][2].PRy = 0.34302327f; 
prbl[28][3].PRx = 0.2961165f; 
prbl[28][3].PRy = 0.30882353f; 
prbl[28][4].PRx = 0.29807693f; 
prbl[28][4].PRy = 0.28076923f; 
prbl[28][5].PRx = 0.30882353f; 
prbl[28][5].PRy = 0.2777778f; 
prbl[28][6].PRx = 0.2712766f; 
prbl[28][6].PRy = 0.29666665f; 
prbl[28][7].PRx = 0.28735632f; 
prbl[28][7].PRy = 0.2947761f; 
prbl[28][8].PRx = 0.29577464f; 
prbl[28][8].PRy = 0.29924244f; 
prbl[28][9].PRx = 0.28385416f; 
prbl[28][9].PRy = 0.2857143f; 
prbl[28][10].PRx = 0.32857144f; 
prbl[28][10].PRy = 0.33783785f; 
prbl[28][11].PRx = 0.44392523f; 
prbl[28][11].PRy = 0.37962964f; 
prbl[28][12].PRx = 0.41223404f; 
prbl[28][12].PRy = 0.41223404f; 
prbl[28][13].PRx = 0.5192308f; 
prbl[28][13].PRy = 0.62053573f; 
prbl[28][14].PRx = 0.75442475f; 
prbl[28][14].PRy = 0.81875f; 
prbl[28][15].PRx = 0.69158876f; 
prbl[28][15].PRy = 0.73255813f; 
prbl[28][16].PRx = 0.30769232f; 
prbl[28][16].PRy = 0.32978722f; 
prbl[28][17].PRx = 0.35958904f; 
prbl[28][17].PRy = 0.3991228f; 
prbl[28][18].PRx = 0.5202703f; 
prbl[28][18].PRy = 0.5611702f; 
prbl[28][19].PRx = 0.5337838f; 
prbl[28][19].PRy = 0.5591398f; 
prbl[28][20].PRx = 0.392f; 
prbl[28][20].PRy = 0.38271606f; 
prbl[28][21].PRx = 0.435f; 
prbl[28][21].PRy = 0.41885966f; 
prbl[28][22].PRx = 0.44444445f; 
prbl[28][22].PRy = 0.46917808f; 
prbl[28][23].PRx = 0.31690142f; 
prbl[28][23].PRy = 0.32608697f; 
prbl[28][24].PRx = 0.32352942f; 
prbl[28][24].PRy = 0.3283582f; 
prbl[28][25].PRx = 0.33027524f; 
prbl[28][25].PRy = 0.3322785f; 
prbl[28][26].PRx = 0.4266055f; 
prbl[28][26].PRy = 0.4117647f; 
prbl[28][27].PRx = 0.4019608f; 
prbl[28][27].PRy = 0.42436975f; 
prbl[28][28].PRx = 0.4073276f; 
prbl[28][28].PRy = 0.39117646f; 
prbl[28][29].PRx = 0.39925373f; 
prbl[28][29].PRy = 0.35795453f; 
prbl[28][30].PRx = 0.43495935f; 
prbl[28][30].PRy = 0.39473686f; 
prbl[28][31].PRx = 0.3802521f; 
prbl[28][31].PRy = 0.3773585f; 
prbl[28][32].PRx = 0.3577586f; 
prbl[28][32].PRy = 0.37719297f; 

prbl[29][0].PRx = 0.31034482f; 
prbl[29][0].PRy = 0.29411766f; 
prbl[29][1].PRx = 0.29333332f; 
prbl[29][1].PRy = 0.3125f; 
prbl[29][2].PRx = 0.33f; 
prbl[29][2].PRy = 0.3627451f; 
prbl[29][3].PRx = 0.3206522f; 
prbl[29][3].PRy = 0.32352942f; 
prbl[29][4].PRx = 0.34047619f; 
prbl[29][4].PRy = 0.32911393f; 
prbl[29][5].PRx = 0.28787878f; 
prbl[29][5].PRy = 0.28214285f; 
prbl[29][6].PRx = 0.31989247f; 
prbl[29][6].PRy = 0.33450705f; 
prbl[29][7].PRx = 0.3452381f; 
prbl[29][7].PRy = 0.38607594f; 
prbl[29][8].PRx = 0.44615385f; 
prbl[29][8].PRy = 0.46875f; 
prbl[29][9].PRx = 0.42f; 
prbl[29][9].PRy = 0.43589744f; 
prbl[29][10].PRx = 0.34895834f; 
prbl[29][10].PRy = 0.3778409f; 
prbl[29][11].PRx = 0.4f; 
prbl[29][11].PRy = 0.3901099f; 
prbl[29][12].PRx = 0.4226804f; 
prbl[29][12].PRy = 0.45882353f; 
prbl[29][13].PRx = 0.43846154f; 
prbl[29][13].PRy = 0.48979592f; 
prbl[29][14].PRx = 0.4041096f; 
prbl[29][14].PRy = 0.43396226f; 
prbl[29][15].PRx = 0.5420792f; 
prbl[29][15].PRy = 0.5574713f; 
prbl[29][16].PRx = 0.3508772f; 
prbl[29][16].PRy = 0.35625f; 
prbl[29][17].PRx = 0.34615386f; 
prbl[29][17].PRy = 0.35267857f; 
prbl[29][18].PRx = 0.3614865f; 
prbl[29][18].PRy = 0.42021278f; 
prbl[29][19].PRx = 0.45422536f; 
prbl[29][19].PRy = 0.4722222f; 
prbl[29][20].PRx = 0.5151515f; 
prbl[29][20].PRy = 0.5511364f; 
prbl[29][21].PRx = 0.40384614f; 
prbl[29][21].PRy = 0.42537314f; 
prbl[29][22].PRx = 0.31321838f; 
prbl[29][22].PRy = 0.33552632f; 
prbl[29][23].PRx = 0.35789475f; 
prbl[29][23].PRy = 0.39333335f; 
prbl[29][24].PRx = 0.33119658f; 
prbl[29][24].PRy = 0.32552084f; 
prbl[29][25].PRx = 0.3275194f; 
prbl[29][25].PRy = 0.33426967f; 
prbl[29][26].PRx = 0.39182693f; 
prbl[29][26].PRy = 0.3777174f; 
prbl[29][27].PRx = 0.50635594f; 
prbl[29][27].PRy = 0.47704083f; 
prbl[29][28].PRx = 0.43421054f; 
prbl[29][28].PRy = 0.4054878f; 
prbl[29][29].PRx = 0.38655463f; 
prbl[29][29].PRy = 0.3861111f; 
prbl[29][30].PRx = 0.45238096f; 
prbl[29][30].PRy = 0.40808824f; 
prbl[29][31].PRx = 0.44612068f; 
prbl[29][31].PRy = 0.40425533f; 
prbl[29][32].PRx = 0.3888889f; 
prbl[29][32].PRy = 0.37755102f; 

prbl[30][0].PRx = 0.48026314f; 
prbl[30][0].PRy = 0.45f; 
prbl[30][1].PRx = 0.4642857f; 
prbl[30][1].PRy = 0.4715909f; 
prbl[30][2].PRx = 0.37804878f; 
prbl[30][2].PRy = 0.44117647f; 
prbl[30][3].PRx = 0.42168674f; 
prbl[30][3].PRy = 0.45652175f; 
prbl[30][4].PRx = 0.3005618f; 
prbl[30][4].PRy = 0.3026316f; 
prbl[30][5].PRx = 0.41049382f; 
prbl[30][5].PRy = 0.48015872f; 
prbl[30][6].PRx = 0.46875f; 
prbl[30][6].PRy = 0.46376812f; 
prbl[30][7].PRx = 0.32534248f; 
prbl[30][7].PRy = 0.30660376f; 
prbl[30][8].PRx = 0.30448717f; 
prbl[30][8].PRy = 0.3010204f; 
prbl[30][9].PRx = 0.2957317f; 
prbl[30][9].PRy = 0.3359375f; 
prbl[30][10].PRx = 0.44256756f; 
prbl[30][10].PRy = 0.45652175f; 
prbl[30][11].PRx = 0.5416667f; 
prbl[30][11].PRy = 0.55445546f; 
prbl[30][12].PRx = 0.6393443f; 
prbl[30][12].PRy = 0.66810346f; 
prbl[30][13].PRx = 0.25f; 
prbl[30][13].PRy = 0.25f; 
prbl[30][14].PRx = 0.32786885f; 
prbl[30][14].PRy = 0.3148148f; 
prbl[30][15].PRx = 0.3219697f; 
prbl[30][15].PRy = 0.32407406f; 
prbl[30][16].PRx = 0.29452056f; 
prbl[30][16].PRy = 0.2824074f; 
prbl[30][17].PRx = 0.32786885f; 
prbl[30][17].PRy = 0.31034482f; 
prbl[30][18].PRx = 0.36643836f; 
prbl[30][18].PRy = 0.2925532f; 
prbl[30][19].PRx = 0.3108108f; 
prbl[30][19].PRy = 0.3237705f; 
prbl[30][20].PRx = 0.3125f; 
prbl[30][20].PRy = 0.30555555f; 
prbl[30][21].PRx = 0.38690478f; 
prbl[30][21].PRy = 0.36206895f; 
prbl[30][22].PRx = 0.37637362f; 
prbl[30][22].PRy = 0.40841585f; 
prbl[30][23].PRx = 0.33241758f; 
prbl[30][23].PRy = 0.3310811f; 
prbl[30][24].PRx = 0.32112068f; 
prbl[30][24].PRy = 0.31325302f; 
prbl[30][25].PRx = 0.3318584f; 
prbl[30][25].PRy = 0.3382353f; 
prbl[30][26].PRx = 0.33796296f; 
prbl[30][26].PRy = 0.32180852f; 
prbl[30][27].PRx = 0.48228347f; 
prbl[30][27].PRy = 0.4722222f; 
prbl[30][28].PRx = 0.426f; 
prbl[30][28].PRy = 0.41860464f; 
prbl[30][29].PRx = 0.4031008f; 
prbl[30][29].PRy = 0.37345678f; 
prbl[30][30].PRx = 0.40495867f; 
prbl[30][30].PRy = 0.4021739f; 
prbl[30][31].PRx = 0.4410569f; 
prbl[30][31].PRy = 0.39556962f; 
prbl[30][32].PRx = 0.4056604f; 
prbl[30][32].PRy = 0.41025642f; 

prbl[31][0].PRx = 0.30625f; 
prbl[31][0].PRy = 0.32738096f; 
prbl[31][1].PRx = 0.48451328f; 
prbl[31][1].PRy = 0.47619048f; 
prbl[31][2].PRx = 0.30769232f; 
prbl[31][2].PRy = 0.35277778f; 
prbl[31][3].PRx = 0.39945653f; 
prbl[31][3].PRy = 0.35443038f; 
prbl[31][4].PRx = 0.4642857f; 
prbl[31][4].PRy = 0.5f; 
prbl[31][5].PRx = 0.38207546f; 
prbl[31][5].PRy = 0.4375f; 
prbl[31][6].PRx = 0.33529413f; 
prbl[31][6].PRy = 0.35f; 
prbl[31][7].PRx = 0.29885057f; 
prbl[31][7].PRy = 0.31666666f; 
prbl[31][8].PRx = 0.28658536f; 
prbl[31][8].PRy = 0.28787878f; 
prbl[31][9].PRx = 0.28296703f; 
prbl[31][9].PRy = 0.28169015f; 
prbl[31][10].PRx = 0.35855263f; 
prbl[31][10].PRy = 0.3559322f; 
prbl[31][11].PRx = 0.40123457f; 
prbl[31][11].PRy = 0.45f; 
prbl[31][12].PRx = 0.38059703f; 
prbl[31][12].PRy = 0.37295082f; 
prbl[31][13].PRx = 0.30666667f; 
prbl[31][13].PRy = 0.36619717f; 
prbl[31][14].PRx = 0.3321918f; 
prbl[31][14].PRy = 0.39112905f; 
prbl[31][15].PRx = 0.33173078f; 
prbl[31][15].PRy = 0.37121212f; 
prbl[31][16].PRx = 0.32857144f; 
prbl[31][16].PRy = 0.35655737f; 
prbl[31][17].PRx = 0.30645162f; 
prbl[31][17].PRy = 0.3139535f; 
prbl[31][18].PRx = 0.42582417f; 
prbl[31][18].PRy = 0.40789473f; 
prbl[31][19].PRx = 0.3611111f; 
prbl[31][19].PRy = 0.38235295f; 
prbl[31][20].PRx = 0.29285714f; 
prbl[31][20].PRy = 0.29508197f; 
prbl[31][21].PRx = 0.3221154f; 
prbl[31][21].PRy = 0.32954547f; 
prbl[31][22].PRx = 0.33841464f; 
prbl[31][22].PRy = 0.33223686f; 
prbl[31][23].PRx = 0.30833334f; 
prbl[31][23].PRy = 0.30128205f; 
prbl[31][24].PRx = 0.30526316f; 
prbl[31][24].PRy = 0.31666666f; 
prbl[31][25].PRx = 0.30645162f; 
prbl[31][25].PRy = 0.3255814f; 
prbl[31][26].PRx = 0.3939394f; 
prbl[31][26].PRy = 0.39f; 
prbl[31][27].PRx = 0.4625f; 
prbl[31][27].PRy = 0.4077381f; 
prbl[31][28].PRx = 0.44017094f; 
prbl[31][28].PRy = 0.38356164f; 
prbl[31][29].PRx = 0.42477876f; 
prbl[31][29].PRy = 0.4077381f; 
prbl[31][30].PRx = 0.40524194f; 
prbl[31][30].PRy = 0.37179488f; 
prbl[31][31].PRx = 0.434f; 
prbl[31][31].PRy = 0.41954023f; 
prbl[31][32].PRx = 0.5138889f; 
prbl[31][32].PRy = 0.44230768f; 

prbl[32][0].PRx = 0.28125f; 
prbl[32][0].PRy = 0.2647059f; 
prbl[32][1].PRx = 0.37857142f; 
prbl[32][1].PRy = 0.3602941f; 
prbl[32][2].PRx = 0.2948718f; 
prbl[32][2].PRy = 0.3243243f; 
prbl[32][3].PRx = 0.45731708f; 
prbl[32][3].PRy = 0.44f; 
prbl[32][4].PRx = 0.25f; 
prbl[32][4].PRy = 0.25f; 
prbl[32][5].PRx = 0.4375f; 
prbl[32][5].PRy = 0.5f; 
prbl[32][6].PRx = 0.31034482f; 
prbl[32][6].PRy = 0.27f; 
prbl[32][7].PRx = 0.30357143f; 
prbl[32][7].PRy = 0.3203125f; 
prbl[32][8].PRx = 0.5729167f; 
prbl[32][8].PRy = 0.5052083f; 
prbl[32][9].PRx = 0.47413793f; 
prbl[32][9].PRy = 0.5119048f; 
prbl[32][10].PRx = 0.44827586f; 
prbl[32][10].PRy = 0.49074075f; 
prbl[32][11].PRx = 0.375f; 
prbl[32][11].PRy = 0.5f; 
prbl[32][12].PRx = 0.39864865f; 
prbl[32][12].PRy = 0.3243243f; 
prbl[32][13].PRx = 0.30813953f; 
prbl[32][13].PRy = 0.2987805f; 
prbl[32][14].PRx = 0.30729166f; 
prbl[32][14].PRy = 0.3f; 
prbl[32][15].PRx = 0.29891303f; 
prbl[32][15].PRy = 0.30555555f; 
prbl[32][16].PRx = 0.2857143f; 
prbl[32][16].PRy = 0.28787878f; 
prbl[32][17].PRx = 0.3452381f; 
prbl[32][17].PRy = 0.36486486f; 
prbl[32][18].PRx = 0.5744681f; 
prbl[32][18].PRy = 0.5703125f; 
prbl[32][19].PRx = 0.45f; 
prbl[32][19].PRy = 0.38636363f; 
prbl[32][20].PRx = 0.33035713f; 
prbl[32][20].PRy = 0.3181818f; 
prbl[32][21].PRx = 0.3125f; 
prbl[32][21].PRy = 0.3653846f; 
prbl[32][22].PRx = 0.34146342f; 
prbl[32][22].PRy = 0.34848484f; 
prbl[32][23].PRx = 0.28723404f; 
prbl[32][23].PRy = 0.2983871f; 
prbl[32][24].PRx = 0.33333334f; 
prbl[32][24].PRy = 0.32738096f; 
prbl[32][25].PRx = 0.37903225f; 
prbl[32][25].PRy = 0.3882979f; 
prbl[32][26].PRx = 0.38333333f; 
prbl[32][26].PRy = 0.34146342f; 
prbl[32][27].PRx = 0.45833334f; 
prbl[32][27].PRy = 0.40555555f; 
prbl[32][28].PRx = 0.3923077f; 
prbl[32][28].PRy = 0.37804878f; 
prbl[32][29].PRx = 0.42592594f; 
prbl[32][29].PRy = 0.38157895f; 
prbl[32][30].PRx = 0.4212963f; 
prbl[32][30].PRy = 0.3857143f; 
prbl[32][31].PRx = 0.5f; 
prbl[32][31].PRy = 0.37857142f; 
prbl[32][32].PRx = 0.3478261f; 
prbl[32][32].PRy = 0.3181818f; 





		
		
	}
	//*******************************************************************
	public void expandHistogramPR(float k, float avg)
	{
		for (int y=0;y<number_of_blocks_V+1;y++)
		{
			for (int x=0;x<number_of_blocks_H+1;x++)
			{
				prbl[y][x].PRx= k*(prbl[y][x].PRx-avg)+ avg;
				if (prbl[y][x].PRx>1 )prbl[y][x].PRx=1;
				if (prbl[y][x].PRx<0 )prbl[y][x].PRx=0;
				
				prbl[y][x].PRy= k*(prbl[y][x].PRy-avg)+ avg;
				if (prbl[y][x].PRy>1 )prbl[y][x].PRy=1;
				if (prbl[y][x].PRy<0 )prbl[y][x].PRy=0;
			}
		}
	}
	//*******************************************************************
	public void expandHistogramPR_03(float avg,float dev)
	{
		
		float max=avg+2*dev;
		if (max>1) max=1;
		float min=avg-2*dev;
		if (min<0)min =0;
		
		float current_minPR=min;
		float current_maxPR=max;
		
		System.out.println(" maxPRsigma:"+current_maxPR+"   minPRsigma:"+current_minPR+"   avg:"+avg);
		
		
		for (int y=0;y<number_of_blocks_V+1;y++)
		{

			for (int x=0;x<number_of_blocks_H+1;x++)
			{
				//prbl[y][x].PRx=(new_maxPRx-new_minPRx)*(prbl[y][x].PRx-current_minPRx)/(current_maxPRx-current_minPRx)+new_minPRx;
				
				prbl[y][x].PRx=(prbl[y][x].PRx-current_minPR)/(current_maxPR-current_minPR);
				if (prbl[y][x].PRx<0)prbl[y][x].PRx=0;
				if (prbl[y][x].PRx>1)prbl[y][x].PRx=1;
				
				//prbl[y][x].PRy=(new_maxPRy-new_minPRy)*(prbl[y][x].PRy-current_minPRy)/(current_maxPRy-current_minPRy)+new_minPRy;
				prbl[y][x].PRy=(prbl[y][x].PRy-current_minPR)/(current_maxPR-current_minPR);
				
				if (prbl[y][x].PRy<0)prbl[y][x].PRy=0;
				if (prbl[y][x].PRy>1)prbl[y][x].PRy=1;
				
			}//x

		}//y
	}
	//*******************************************************************
	public void printHistogramPR()
	{
		int[] hist=new int[101];
		for (int y=1;y<number_of_blocks_V;y++)
		{

			for (int x=1;x<number_of_blocks_H;x++)
			{
			hist[(int)(100f*prbl[y][x].PRx)]++;
			hist[(int)(100f*prbl[y][x].PRy)]++;
			
			}
		}
		for (int i=0;i<=100;i++)
		{
			System.out.println(hist[i]);
		}
	}

	
	//*******************************************************************
		public void expandHistogramPR_04(float mini, float maxi)
		{
			
			//lets compute max and min
			//DISTRIBUTION is not gaussian -> 2xsigma is not valid
			/*
			int[] counter=new int[100];
			float min=1;
			float max=0;
			//only consider entire blocks ( not first and last rows/columns)
			
			float cosa=0;
			for (int y=1;y<number_of_blocks_V;y++)
			{

				for (int x=1;x<number_of_blocks_H;x++)
				{
					//prbl[y][x].PRx=0.29f;
					//prbl[y][x].PRy=0.29f;
					
					cosa+=prbl[y][x].PRx;//=0.29f;;
					cosa+=prbl[y][x].PRy;//=0.29f;
					int value=(int)(prbl[y][x].PRx*100);
					//value=29;
					for (int i=0;i<value;i++)
					{
					counter[i]++;	
					}//i
					value=(int)(prbl[y][x].PRy*100);
					//value=29;
					for (int i=0;i<value;i++)
					{
					counter[i]++;	
					}//i
				}//x
			}//y
			
			
			//now counter have the computation
			int total_samples=2*(number_of_blocks_V-1)*(number_of_blocks_H-1);
			//System.out.println ("cosa:"+cosa+ "    samples:"+total_samples+ " cada:"+(cosa/(float)total_samples));
			for (int i=0;i<100;i++)
			{
			//System.out.println("counter["+i+"]="+(float)counter[i]/(float)total_samples);
			}//i
			//set limits into 5% and 95%
			//----------------------------
			
			for (int i=0;i<100;i++)
			{
			float por=(float)counter[i]/(float)total_samples;
			if (por<0.95f) {min=(float)i/100f;break;}	
			//if (por<1f) {min=(float)i/100f;break;}
			}//i
			for (int i=99;i>=0;i--)
			{
			float por=(float)counter[i]/(float)total_samples;
			if (por>0.05f) {max=(float)i/100f;break;}	
			//if (por>0.00f) {max=(float)i/100f;break;}
			}//i
			//----------------------------
			
			//MAXIMOS Y MINIMOS DE PR ANTES DE EXPANDIR
			//en video deben ser forzados
			
			//min=0.10f;
			//max=0.5f;
			*/
			float min=0.2f;//0.2f;//0.2f;//125f;
			float max=0.5f;//0.5f;

			min=mini;
			max=maxi;
			
			float current_minPR=min;
			//if (min<0.1f) min=0.1f;
			float current_maxPR=max;
			
			//System.out.println(" maxPR90:"+current_maxPR+"   minPR10:"+current_minPR);
			//System.out.println(" maxPR:"+current_maxPR+"   minPR:"+current_minPR);
			
			for (int y=0;y<number_of_blocks_V+1;y++)
			{

				for (int x=0;x<number_of_blocks_H+1;x++)
				{
					//prbl[y][x].PRx=(new_maxPRx-new_minPRx)*(prbl[y][x].PRx-current_minPRx)/(current_maxPRx-current_minPRx)+new_minPRx;
					
					//prbl[y][x].PRx=0.29f;
					//prbl[y][x].PRy=0.29f;
					
					prbl[y][x].PRx=(prbl[y][x].PRx-current_minPR)/(current_maxPR-current_minPR);
					if (prbl[y][x].PRx<0)prbl[y][x].PRx=0;
					if (prbl[y][x].PRx>1)prbl[y][x].PRx=1;
					
					//prbl[y][x].PRy=(new_maxPRy-new_minPRy)*(prbl[y][x].PRy-current_minPRy)/(current_maxPRy-current_minPRy)+new_minPRy;
					prbl[y][x].PRy=(prbl[y][x].PRy-current_minPR)/(current_maxPR-current_minPR);
					
					if (prbl[y][x].PRy<0)prbl[y][x].PRy=0;
					if (prbl[y][x].PRy>1)prbl[y][x].PRy=1;
					
				}//x

			}//y
		}
		//*******************************************************************
	//*******************************************************************
	public void expandHistogramPR_02(float[] stats)
	{
		float avg=stats[0];
		float max=stats[1];
		float min=stats[2];
		
		//float k3=0;
		//first move avg to 0.5
		//k3=0.5f/avg;
		
		//then expand
		float avg2=0.5f;
		float k1=0;
		if (avg>min) k1=0.5f/(avg-min);// k1<1
		//else k1=0;
		float k2=1;
		if (avg<max) k2=(0.5f)/(max-avg);//k2>1
		
		
		
		
		for (int y=0;y<number_of_blocks_V+1;y++)
		{
			for (int x=0;x<number_of_blocks_H+1;x++)
			{
				//prbl[y][x].PRx= k*(prbl[y][x].PRx-avg)+ avg;
				
				if (prbl[y][x].PRx<=avg)	prbl[y][x].PRx=0.5f - (k1*(avg-prbl[y][x].PRx));
				else prbl[y][x].PRx=k2*(prbl[y][x].PRx-avg)+0.5f;
					
					
				if (prbl[y][x].PRx>1 )prbl[y][x].PRx=1;
				if (prbl[y][x].PRx<0 )prbl[y][x].PRx=0;
				
				//prbl[y][x].PRy= k*(prbl[y][x].PRy-avg)+ avg;
				
				if (prbl[y][x].PRy<=avg)	prbl[y][x].PRy=0.5f - (k1*(avg-prbl[y][x].PRy));
				else prbl[y][x].PRy=k2*(prbl[y][x].PRy-avg)+0.5f;
				
				
				if (prbl[y][x].PRy>1 )prbl[y][x].PRy=1;
				if (prbl[y][x].PRy<0 )prbl[y][x].PRy=0;
			}
		}
	}
	//*******************************************************************
	
	public void expandHistogramPR()
	{
		//this function works fine

		float current_maxPRx=0;
		float current_minPRx=1000;
		float current_maxPRy=0;
		float current_minPRy=1000;
		
		//for (int y=0;y<number_of_blocks_V+1;y++)
	    for (int y=1;y<number_of_blocks_V;y++)
		{
			//for (int x=0;x<number_of_blocks_H+1;x++)
				for (int x=1;x<number_of_blocks_H;x++)
			{
				if (prbl[y][x].PRx>current_maxPRx) current_maxPRx=prbl[y][x].PRx;
				if (prbl[y][x].PRy>current_maxPRy) current_maxPRy=prbl[y][x].PRy;
				
				if (prbl[y][x].PRx<current_minPRx) current_minPRx=prbl[y][x].PRx;
				
				if (prbl[y][x].PRy<current_minPRy) current_minPRy=prbl[y][x].PRy;

				if (prbl[y][x].PRy==0) System.out.println(" 0 en PRy en x="+x+"  y="+y);
			}//x

		}//y

		float new_maxPRx=1;
		float new_minPRx=0;

		float new_maxPRy=1;
		float new_minPRy=0;

System.out.println(" maxPRx:"+current_maxPRx+"   minPRx:"+current_minPRx);
System.out.println(" maxPRy:"+current_maxPRy+"   minPRy:"+current_minPRy);

//ajuste por si acaso hay un bloque con pr=0 que daña la estadistica
if (current_minPRx<0.1f) current_minPRx=0.1f;
if (current_minPRy<0.1f) current_minPRy=0.1f;
//current_minPRy=0.25f;

		for (int y=0;y<number_of_blocks_V+1;y++)
		{

			for (int x=0;x<number_of_blocks_H+1;x++)
			{
				//prbl[y][x].PRx=(new_maxPRx-new_minPRx)*(prbl[y][x].PRx-current_minPRx)/(current_maxPRx-current_minPRx)+new_minPRx;
				
				prbl[y][x].PRx=(prbl[y][x].PRx-current_minPRx)/(current_maxPRx-current_minPRx);
				if (prbl[y][x].PRx<0)prbl[y][x].PRx=0;
				//prbl[y][x].PRy=(new_maxPRy-new_minPRy)*(prbl[y][x].PRy-current_minPRy)/(current_maxPRy-current_minPRy)+new_minPRy;
				prbl[y][x].PRy=(prbl[y][x].PRy-current_minPRy)/(current_maxPRy-current_minPRy);
				
				if (prbl[y][x].PRy<0)prbl[y][x].PRy=0;
				
				
			}//x

		}//y
	}
	//********************************************************************************
	public void printPRstats(String path_file)
	{
		//int prblocks=number_of_blocks_H*number_of_blocks_V;


		System.out.println("-----  PRx stats --------------------");
		System.out.println(" blocks H:"+number_of_blocks_H+1);
		System.out.println(" blocks V:"+number_of_blocks_V+1);
		try{
			//System.out.println("Entrando en salvaTXT");
			DataOutputStream d = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path_file)));

			int counter=0;			
			for (int y=0;y<number_of_blocks_V+1;y++)
			{
				for (int x=0;x<number_of_blocks_H+1;x++)
				{
					//System.out.println(prbl[j][i].PRx+";"+prbl[j][i].PRy);
					d.writeBytes(new String(counter+";"+prbl[y][x].PRx+";"+prbl[y][x].PRy));
					d.writeBytes("\n");
					counter++;
				}
			}
			d.close();
		}catch(Exception e){ System.out.println(" CIELOS!!!!"+e); System.exit(0);}

	}
	//********************************************************************************************
	/**
	 * aqui entramos con el PR ya cuantizado
	 * @param cf
	 */
	//public void fromPRtoPPP(float compression_factor)
	public void fromPRtoPPP(float cf)
	{
		//COMENTAMOS ESTO
		//compression_factor=1f/(cf*cf);//OJO 20150107
		compression_factor=cf;
		
		//compression_factor=0.125f;//maxima calidad
		//compression_factor=9f;
		
		if (DEBUG) System.out.println(" Compression factor is 1/(cf*cf)  = "+compression_factor);
		
		
		
		/** 
		 * this function transforms PR coeficients in the range [0..1] into
		 * "pixels_per_pixel"  which is the number of original pixels that a scaled pixel represents
		 * This is the first step just before scaling 
		 */

		/**
		 * PR is an array of PR coeficients corresponding to an array of PR blocks 
		 * each PR block's center is the common corner of 4 blocks of the grid
		 * 
		 * PR blocks = (number_of_blocks_H +1) * ( number_of_blocks_V +1) 
		 * 
		 * PR has 2 values: PRx and PRy, like PPP
		 * Therefore PRx and PRy takes into account number of fluctuations and size of hops 
		 * 
		 * These are the PR metrics:
		 * 
		 * PR[block][x || y]
		 * 
		 * PRx,y is a couple of floating values. 
		 * 
		 * 
		 * 
		 */


		for (int by=0;by<number_of_blocks_V;by++)
		{

			for (int bx=0;bx<number_of_blocks_H;bx++)
			{
				Block b=bl[by][bx];


				//float PRmin=0f;
/*
				float k=Block.MAX_PPP-1;
				float k2=Block.MAX_PPP;

				b.ppp[0][0]=(k2/ (1f+k*prbl[by][bx].PRx))*compression_factor;
				b.ppp[0][1]=(k2/ (1f+k*prbl[by][bx+1].PRx))*compression_factor;
				b.ppp[0][2]=(k2/ (1f+k*prbl[by+1][bx].PRx))*compression_factor;
				b.ppp[0][3]=(k2/ (1f+k*prbl[by+1][bx+1].PRx))*compression_factor;


				b.ppp[1][0]=(k2/ (1f+k*prbl[by][bx].PRy))*compression_factor;
				b.ppp[1][1]=(k2/ (1f+k*prbl[by][bx+1].PRy))*compression_factor;
				b.ppp[1][2]=(k2/ (1f+k*prbl[by+1][bx].PRy))*compression_factor;
				b.ppp[1][3]=(k2/ (1f+k*prbl[by+1][bx+1].PRy))*compression_factor;
*/

				
				if (Block.MAX_PPP>4) Block.MAX_PPP=4;
				//Block.MAX_PPP=1;
				//this is an optimization to avoid some multiplications
				float k=Block.MAX_PPP-1;
				float k2=Block.MAX_PPP*compression_factor;

				b.ppp[0][0]=(k2/ (1f+k*prbl[by][bx].PRx));
				b.ppp[0][1]=(k2/ (1f+k*prbl[by][bx+1].PRx));
				b.ppp[0][2]=(k2/ (1f+k*prbl[by+1][bx].PRx));
				b.ppp[0][3]=(k2/ (1f+k*prbl[by+1][bx+1].PRx));


				b.ppp[1][0]=(k2/ (1f+k*prbl[by][bx].PRy));
				b.ppp[1][1]=(k2/ (1f+k*prbl[by][bx+1].PRy));
				b.ppp[1][2]=(k2/ (1f+k*prbl[by+1][bx].PRy));
				b.ppp[1][3]=(k2/ (1f+k*prbl[by+1][bx+1].PRy));
				
				
				//nuevo control de informacion 2014 11 25 CONTROL DE CUANTO NULO
				//--------------------------------------------------------------
				
				if (prbl[by][bx].PRx==0)b.ppp[0][0]=Block.MAX_PPP;
				if (prbl[by][bx+1].PRx==0)b.ppp[0][1]=Block.MAX_PPP;
				if (prbl[by+1][bx].PRx==0)b.ppp[0][2]=Block.MAX_PPP;
				if (prbl[by+1][bx+1].PRx==0)b.ppp[0][3]=Block.MAX_PPP;
				
				if (prbl[by][bx].PRy==0)b.ppp[1][0]=Block.MAX_PPP;
				if (prbl[by][bx+1].PRy==0)b.ppp[1][1]=Block.MAX_PPP;
				if (prbl[by+1][bx].PRy==0)b.ppp[1][2]=Block.MAX_PPP;
				if (prbl[by+1][bx+1].PRy==0)b.ppp[1][3]=Block.MAX_PPP;
				
				//nuevo control de informacion 2014 11 25 CONTROL DE CUANTO 0.125
				//--------------------------------------------------------------
				/*
				float maxq1=2;//Block.MAX_PPP/2;
				float q1=0.25f;
				if (prbl[by][bx].PRx<=q1 && b.ppp[0][0]<maxq1) b.ppp[0][0]=maxq1;
				if (prbl[by][bx+1].PRx<=q1 && b.ppp[0][1]<maxq1) b.ppp[0][1]=maxq1;
				if (prbl[by+1][bx].PRx<=q1 && b.ppp[0][2]<maxq1) b.ppp[0][2]=maxq1;
				if (prbl[by+1][bx+1].PRx<=q1 && b.ppp[0][3]<maxq1) b.ppp[0][3]=maxq1;
				
				if (prbl[by][bx].PRy<=q1 && b.ppp[1][0]<maxq1) b.ppp[1][0]=maxq1;
				if (prbl[by][bx+1].PRy<=q1 && b.ppp[1][1]<maxq1) b.ppp[1][1]=maxq1;
				if (prbl[by+1][bx].PRy<=q1 && b.ppp[1][2]<maxq1) b.ppp[1][2]=maxq1;
				if (prbl[by+1][bx+1].PRy<=q1 && b.ppp[1][3]<maxq1) b.ppp[1][3]=maxq1;
				*/
				// CONTROL DE MAXIMA ELASTICIDAD
				//---------------------------------
				
				//System.out.println("b.ppp[1][3]:"+k2+" PR:"+prbl[by+1][bx+1].PRy+"   MAX:"+k);
				//nueva regla 02/10/2014
				float minppp=Block.MAX_PPP;
				
				for (int coord=0;coord<2;coord++)
					for (int corner=0;corner<4;corner++)
					{
						if (b.ppp[coord][corner]>Block.MAX_PPP)b.ppp[coord][corner]=Block.MAX_PPP;
						if (b.ppp[coord][corner]<1)b.ppp[coord][corner]=1;
						
						if (minppp>b.ppp[coord][corner]) minppp=b.ppp[coord][corner];
						
						//System.out.println(" bl["+by+"]["+bx+"].ppp["+coord+"]["+corner+"]="+ b.ppp[coord][corner]+"f;");
						//System.out.println("comp:"+compression_factor);
					}
				
				
				//esto es del 02/10/2014
				//limitacion de la elasticidad a 3 veces
				//------------------------------------------
				float max_elastic=3;//3f;// con un valor elevado estaria desactivado, pej 300
				float maxppp=minppp*max_elastic;
				for (int coord=0;coord<2;coord++)
					for (int corner=0;corner<4;corner++)
					{
						if (b.ppp[coord][corner]>maxppp)b.ppp[coord][corner]=maxppp;
						
					}
				
				//end 02102014

			}//bx
		}//by

	}
	//**************************************************************************************
	public void equalizeHistogramPR()
	{
		float N=(number_of_blocks_H+1)*(number_of_blocks_V+1);
		//cumulated histograms. Levels= 100, from 0.0 to 1.0, steps 0.01
		int[] cumulated_histx=new int[1000+1];
		int[] cumulated_histy=new int[1000+1];
		/*
	System.out.println("********************BEFORE EQUALIZATION*******************************");
	for (int y=0;y<number_of_blocks_V+1;y++)
	{
	for (int x=0;x<number_of_blocks_H+1;x++)
	{

	System.out.println(""+prbl[y][x].PRx);
	}
	}
		 */

		//float umbral=0.5f;//0.55f;
		float umbral=1.0f;//0.55f;// no hace falta umbral. ya esta puesto a 1 en PRblock si supera umbral
		// el 1.0 (el umbral) no debo tenerlo en cuenta
		//basta con no meter en la ecualizacion a 1.0
		float Nx=0;
		float Ny=0;
		//----------------------------------------
		for (int y=0;y<number_of_blocks_V+1;y++)
		{

			for (int x=0;x<number_of_blocks_H+1;x++)
			{
				//for each block
				/*
			for (int j=0;j<=(int)(prbl[y][x].PRx*1000f);j++)
				cumulated_histx[(int)(prbl[y][x].PRx*1000f)]++;

			for (int j=0;j<=(int)(prbl[y][x].PRy*1000f);j++)
				cumulated_histy[(int)(prbl[y][x].PRy*1000f)]++;
				 */
				if (prbl[y][x].PRx<umbral)
				{
					Nx++;//only computes non saturated blocks

					for (int j=(int)(prbl[y][x].PRx*1000f);j<=1000;j++) // <1000 para no meter a 1.0. da igual. con el if ya no lo mete

					{
						//N++;
						cumulated_histx[j]++;
					}
				}


				if (prbl[y][x].PRy<umbral)
				{
					Ny++;

					for (int j=(int)(prbl[y][x].PRy*1000f);j<=1000;j++)
				
					{
						//N++;
						cumulated_histy[j]++;
					}
				}

			}
		}

		//for (int k=0;k<1000;k++) System.out.println(cumulated_histx[k]);

		for (int y=0;y<number_of_blocks_V+1;y++)
		{

			for (int x=0;x<number_of_blocks_H+1;x++)
			{
				if (prbl[y][x].PRx!=1)
					//prbl[y][x].PRx=(1f/Nx)*(float)cumulated_histx[(int)(prbl[y][x].PRx*1000f)];
				    prbl[y][x].PRx=(1f/Nx)*(float)cumulated_histx[(int)(prbl[y][x].PRx*1000f)];

				if (prbl[y][x].PRy!=1)
					//prbl[y][x].PRy=(1f/Ny)*(float)cumulated_histy[(int)(prbl[y][x].PRy*1000f)];
					prbl[y][x].PRy=(1f/Ny)*(float)cumulated_histy[(int)(prbl[y][x].PRy*1000f)];

			}
		}



		//System.out.println("********************AFTER*******************************");
		for (int y=0;y<number_of_blocks_V+1;y++)
		{
			for (int x=0;x<number_of_blocks_H+1;x++)
			{

				//System.out.println(""+(int)(prbl[y][x].PRx*100));
				//System.out.println(""+(int)(prbl[y][x].PRy*100));
			}
		}

		//if (2>1) System.exit(0);


	}
	//************************************************************
	/**
	 * this function saves the grid into a .txt file
	 * for experimental purposes
	 * 
	 * @param filename
	 */
	public void saveGridTXT(String path_file)
	{
		
		try{
			if (DEBUG) System.out.println("Entrando en saveGridTXT");
			DataOutputStream d = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path_file)));
			

			//primero escribo el numero de bloques de la malla
			d.writeBytes((number_of_blocks_H+1)+"\n");
			d.writeBytes((number_of_blocks_V+1)+"\n");
			
			if (DEBUG) System.out.println("");
		for (int y=0;y<number_of_blocks_V+1;y++)
		{

			for (int x=0;x<number_of_blocks_H+1;x++)
			{
				
				float datox= ((float) ((int)(prbl[y][x].PRx*100)))/100f;
				float datoy= ((float) ((int)(prbl[y][x].PRy*100)))/100f;
				
				d.writeBytes(datox+";");
				d.writeBytes(datoy+";");
				//System.out.print(datox+";");System.out.print(datoy+";");
				
			}
			d.writeBytes("\n");	
			//System.out.println("");
			
		}
		d.close();
	}catch (Exception e){System.out.println("failure saving grid.txt");}
	}	
	

	
	//*************************************************************************************************
	/**
	 * 
	 * First, this function compute the maximum PPP value for a given image. MAX_PPP value must be >=8
	 *   this value must allow 32 blocks of a side of 8pix lenght or higher lenght
	 *   if not possible, number of blocks is set below 32 and MAX_PPP is set to 8
	 * 
	 * Then, this function creates an array of PRblocks and another array of Blocks.
	 * 
	 *   center of the PRblocks are ther corners of Blocks
	 *   blocks are not identical, unless image width and high are divisible by the block length 
	 *   vertical size of blocks may be different from horizontal size
	 *   the grid must cover the entire image
	 *   
	 */
	//public void createGrid(int image_width, int image_height, float comp_factor, float MAX_PPP)
	//public void createGrid(int image_width, int image_height,  float MAX_PPP)
	public void createGrid(int image_width, int image_height)
	{

			//compression_factor=1f/(comp_factor*comp_factor);
			//System.out.println(" Compression factor is 1/(cf*cf)  = "+compression_factor);

			//determinacion del lado mayor
			int larger_side=Math.max(image_width, image_height);
			int shorter_side=Math.min(image_width, image_height);
			
			//computation of number of blocks for larger image side
			//minimum block side length is 16 pixels (minimum MAX PPP=8)
			if (DEBUG) System.out.println(" initial blocks_of_larger_image_side="+MAX_BLOCKS);
			blocks_of_larger_image_side=(float)(MAX_BLOCKS);
			float size1=Math.max((float)(larger_side)/blocks_of_larger_image_side, MIN_LEN_BLOCK);
			//float size2=Math.max((float)(shorter_side)/(int) ((float)(shorter_side)/size1 +0.5f), 16);
			
			float size2=Math.max((float)(shorter_side)/(int) ((float)(shorter_side)/size1), MIN_LEN_BLOCK);
			
			if (larger_side==image_width) {sizeh=size1; sizev=size2;}
			else {sizeh=size2;sizev=size1;}
			if (DEBUG) System.out.println(" grid sizeh="+sizeh+" grid sizev="+sizev);
			blocks_of_larger_image_side=(int)(0.5f+larger_side/size1); //from 16 to 32 blocks
			if (DEBUG) System.out.println(" final blocks_of_larger_image_side="+blocks_of_larger_image_side);
			
			//tunning PPP
			float current_ppp=0.5f*larger_side/blocks_of_larger_image_side;
			if (DEBUG) System.out.println(" MAX_PPP="+current_ppp);
			Block.MAX_PPP=current_ppp;
			
			//number_of_blocks_H=(int) (0.5f+(float)image_width/sizeh);// 0.5 to avoid this:  (int)7.999 is 7 
			//number_of_blocks_V=(int) (0.5f+(float)image_height/sizev);
			
			number_of_blocks_H=(int) (0.5f+(float)image_width/sizeh);// 0.5 to avoid this:  (int)7.999 is 7 
			number_of_blocks_V=(int) (0.5f+(float)image_height/sizev);
			
			if (DEBUG) System.out.println(" blocks H:"+number_of_blocks_H);
			if (DEBUG) System.out.println(" blocks V:"+number_of_blocks_V);
	        
			
			//System.out.println(" blocks H:"+number_of_blocks_H+ "  width:"+image_width);
			//System.out.println(" blocks V:"+number_of_blocks_V+"  height:"+image_height);
			//int total_blocks=number_of_blocks_H*number_of_blocks_V;

			// instantiating block list...
			//bl=new ArrayList(total_blocks);
			bl=new Block[number_of_blocks_V][number_of_blocks_H];

			float coordx=0;
			float coordy=0;
			// bucle filling blocks information 


				int n=0;
			for (int by=0;by<number_of_blocks_V;by++)
			{

				for (int bx=0;bx<number_of_blocks_H;bx++)
				{
					Block b=new Block();
					b.xini=(int)(0.5f+coordx);
					b.xfin=(int)(-0.5f+coordx+sizeh);
					if (b.xfin>image_width-1) b.xfin=image_width-1;
					b.yini=(int)(0.5f+coordy);
					b.yfin=(int)(-0.5f+coordy+sizev);
					if (b.yfin>image_height-1) b.yfin=image_height-1;

					b.lx=b.xfin-b.xini+1;
					b.ly=b.yfin-b.yini+1;


					//bl.add(b);
					bl[by][bx]=b;
					//	System.out.println("block "+n+" lx:"+b.lx+ " ly:"+b.ly+" xini:"+b.xini+" xfin:"+b.xfin+" yini:"+b.yini+" yfin:"+b.yfin);
					coordx+=sizeh;	
							n++;
				}//bx
				coordy+=sizev;
				coordx=0;
			}//by


			// bucle filling PRblocks information
			coordx=-sizeh/2;
			coordy=-sizev/2;
			prbl=new PRblock[number_of_blocks_V+1][number_of_blocks_H+1];
			for (int y=0;y<number_of_blocks_V+1;y++)
			{

				for (int x=0;x<number_of_blocks_H+1;x++)
				{
					PRblock prb=new PRblock();
					
					prb.HistnqPRx=new float[10];
					prb.HistnqPRy=new float[10];
					
					
					//System.out.println("PRxq:"+prb.PRxq);
					prb.yini=(int)(coordy+0.5);
					prb.xini=(int)(coordx+0.5);
					
					//prb.yini=(int)(coordy);
					//prb.xini=(int)(coordx);
					
					prb.yfin=(int)(coordy+sizev);
					prb.xfin=(int)(coordx+sizeh);

					if (prb.yini<0)prb.yini=0;
					if (prb.xini<0)prb.xini=0;
					if (prb.yfin>image_height-1) prb.yfin=image_height-1;
					if (prb.xfin>image_width-1)  prb.xfin=image_width-1;

					coordx+=sizeh;
					prbl[y][x]=prb;
					//System.out.println("PRbl["+y+"]["+x+"]:"+ " xini="+prb.xini+ " yini="+prb.yini+"  xfin="+prb.xfin+"   yfin="+prb.yfin);
				}//x
				coordy+=sizev;
				coordx=-sizeh/2;
			}//y

			//TO DO

		}
	//******************************************************************************	
	public void setOptimalKini()
	{
	for (int by=0;by<number_of_blocks_V;by++)
	{

		for (int bx=0;bx<number_of_blocks_H;bx++)
		{
			Block b=bl[by][bx];



			//this is an optimization to avoid some multiplications
			float k=Block.MAX_PPP-1;
			float k2=Block.MAX_PPP*compression_factor;

			float PRavg=0;
			
			PRavg+=prbl[by][bx].PRx;
			PRavg+=prbl[by][bx+1].PRx;
			PRavg+=prbl[by+1][bx].PRx;
			PRavg+=prbl[by+1][bx+1].PRx;
			
			PRavg+=prbl[by][bx].PRy;
			PRavg+=prbl[by][bx+1].PRy;
			PRavg+=prbl[by+1][bx].PRy;
			PRavg+=prbl[by+1][bx+1].PRy;
			
			
			PRavg=PRavg/8f;
			
			

		}//bx
	}//by

}
//**************************************************************************************
	//******************************************************************************	
		public float[] computePRavg()
		{
			float avg=0;
			float max=0;
			float min=1;
			for (int by=0;by<number_of_blocks_V+1;by++)
			{

				for (int bx=0;bx<number_of_blocks_H+1;bx++)
				{
					if (prbl[by][bx].PRx> max && prbl[by][bx].PRx!=10) max=prbl[by][bx].PRx;
					if (prbl[by][bx].PRx< min) min=prbl[by][bx].PRx;
					
					if (prbl[by][bx].PRy> max && prbl[by][bx].PRy!=10) max=prbl[by][bx].PRy;
					if (prbl[by][bx].PRy< min) min=prbl[by][bx].PRy;
					
					//System.out.println(" max:"+max);
			avg+=prbl[by][bx].PRx;
			avg+=prbl[by][bx].PRy;
			
				}
			}
			avg=avg/(2*(number_of_blocks_V+1)*(number_of_blocks_H+1));
		
			float[] result=new float[3];
			//return avg;
			result[0]=avg;
			result[1]=max;
			result[2]=min;
			return result;
		
		}
		public float computePRdev(float avg)
		{
			float dev=0;
			//System.out.println(" avg es "+avg);
			for (int by=0;by<number_of_blocks_V+1;by++)
			{

				for (int bx=0;bx<number_of_blocks_H+1;bx++)
				{
			dev+=(prbl[by][bx].PRx-avg)*(prbl[by][bx].PRx-avg);
			dev+=(prbl[by][bx].PRy-avg)*(prbl[by][bx].PRy-avg);
			
				}
			}
			dev=(float) Math.sqrt(dev / (2*(number_of_blocks_V+1)*(number_of_blocks_H+1)));
		return dev;	
		}	
	//******************************************************************************	
	public void computePRavgPerBlock()
	{
	for (int by=0;by<number_of_blocks_V;by++)
	{

		for (int bx=0;bx<number_of_blocks_H;bx++)
		{
			Block b=bl[by][bx];



		
			float PRavgx=0;
			float PRavgy=0;
			float PRavg=0;
			
			PRavg+=prbl[by][bx].PRx;
			PRavg+=prbl[by][bx+1].PRx;
			PRavg+=prbl[by+1][bx].PRx;
			PRavg+=prbl[by+1][bx+1].PRx;
			
			PRavgx=PRavg;
			
			PRavg+=prbl[by][bx].PRy;
			PRavg+=prbl[by][bx+1].PRy;
			PRavg+=prbl[by+1][bx].PRy;
			PRavg+=prbl[by+1][bx+1].PRy;
			
			PRavgy=PRavg-PRavgx;
			
			b.PRavg=PRavg/8f;
			
			b.PRavgx=PRavgx/4f;
			b.PRavgy=PRavgy/4f;

		}//bx
	}//by

}
	//***************************************************************
	public void sumadown(int[][] y3, int[][] y3b)
	{
		
		//deberia sumar solo los bloques, no toda la imagen
		for (int by=0;by<number_of_blocks_V;by++)
		{

			for (int bx=0;bx<number_of_blocks_H;bx++)
			{
				Block b=bl[by][bx];
				
		for (int component=0;component<3;component++)
		{
			for (int y=b.yini;y<=b.downsampled_yfin;y++)
			{
			for (int x=b.xini;x<b.downsampled_xfin;x++)
			{
				int pos=y*Block.img.width+x;
				Block.img.downsampled_YUV[component][pos]=y3[component][pos]-y3b[component][pos];
			
				if (Block.img.downsampled_YUV[component][pos]>255) Block.img.downsampled_YUV[component][pos]=255;
				if (Block.img.downsampled_YUV[component][pos]<0) Block.img.downsampled_YUV[component][pos]=0;
			}
		    }
		}
	}
		}
	}
	
	
	public void createGridPlusNOSE_USA(int image_width, int image_height,int plus)
	{

			//compression_factor=1f/(comp_factor*comp_factor);
			//System.out.println(" Compression factor is 1/(cf*cf)  = "+compression_factor);

			//determinacion del lado mayor
			int larger_side=Math.max(image_width, image_height);
			int shorter_side=Math.min(image_width, image_height);
			
			//computation of number of blocks for larger image side
			//minimum block side length is 16 pixels (minimum MAX PPP=8)
			if (DEBUG) System.out.println(" initial blocks_of_larger_image_side="+MAX_BLOCKS);
			blocks_of_larger_image_side=MAX_BLOCKS;
			float size1=Math.max((float)(larger_side)/blocks_of_larger_image_side, 16);
			float size2=Math.max((float)(shorter_side)/(int) ((float)(shorter_side)/size1 +0.5f), 16);
			if (larger_side==image_width) {sizeh=size1; sizev=size2;}
			else {sizeh=size2;sizev=size1;}
			if (DEBUG) System.out.println(" grid sizeh="+sizeh+" grid sizev="+sizev);
			blocks_of_larger_image_side=larger_side/size1; //from 16 to 32 blocks
			if (DEBUG) System.out.println(" final blocks_of_larger_image_side="+blocks_of_larger_image_side);
			
			//tunning PPP
			float current_ppp=0.5f*larger_side/blocks_of_larger_image_side;
			if (DEBUG) System.out.println(" MAX_PPP="+current_ppp);
			Block.MAX_PPP=current_ppp;
			
			number_of_blocks_H=(int) (0.5f+(float)image_width/sizeh);// 0.5 to avoid this:  (int)7.999 is 7 
			number_of_blocks_V=(int) (0.5f+(float)image_height/sizev);
			
			
			number_of_blocks_H=number_of_blocks_H-plus;
			number_of_blocks_V=number_of_blocks_V-plus;
			sizeh=(float)image_width/(float)number_of_blocks_H;
			sizev=(float)image_height/(float)number_of_blocks_V;
			Block.MAX_PPP=0.5f*larger_side/number_of_blocks_H;
			
			
			
			
			if (DEBUG) System.out.println(" blocks H:"+number_of_blocks_H);
			if (DEBUG) System.out.println(" blocks V:"+number_of_blocks_V);
	        
			
			System.out.println(" blocks H:"+number_of_blocks_H+ "  width:"+image_width);
			System.out.println(" blocks V:"+number_of_blocks_V+"  height:"+image_height);
			//int total_blocks=number_of_blocks_H*number_of_blocks_V;

			// instantiating block list...
			//bl=new ArrayList(total_blocks);
			bl=new Block[number_of_blocks_V][number_of_blocks_H];

			float coordx=0;
			float coordy=0;
			// bucle filling blocks information 


			//	int n=0;
			for (int by=0;by<number_of_blocks_V;by++)
			{

				for (int bx=0;bx<number_of_blocks_H;bx++)
				{
					Block b=new Block();
					b.xini=(int)(0.5f+coordx);
					b.xfin=(int)(-0.5f+coordx+sizeh);
					if (b.xfin>image_width-1) b.xfin=image_width-1;
					b.yini=(int)(0.5f+coordy);
					b.yfin=(int)(-0.5f+coordy+sizev);
					if (b.yfin>image_height-1) b.yfin=image_height-1;

					b.lx=b.xfin-b.xini+1;
					b.ly=b.yfin-b.yini+1;


					//bl.add(b);
					bl[by][bx]=b;
					//	System.out.println("block "+n+" lx:"+b.lx+" xini:"+b.xini+" xfin:"+b.xfin+" yini:"+b.yini+" yfin:"+b.yfin);
					coordx+=sizeh;	
					//		n++;
				}//bx
				coordy+=sizev;
				coordx=0;
			}//by


			// bucle filling PRblocks information
			coordx=-sizeh/2;
			coordy=-sizev/2;
			prbl=new PRblock[number_of_blocks_V+1][number_of_blocks_H+1];
			for (int y=0;y<number_of_blocks_V+1;y++)
			{

				for (int x=0;x<number_of_blocks_H+1;x++)
				{
					PRblock prb=new PRblock();
					//System.out.println("PRxq:"+prb.PRxq);
					prb.yini=(int)(coordy+0.5);
					prb.xini=(int)(coordx+0.5);
					prb.yfin=(int)(coordy+sizev);
					prb.xfin=(int)(coordx+sizeh);

					if (prb.yini<0)prb.yini=0;
					if (prb.xini<0)prb.xini=0;
					if (prb.yfin>image_height-1) prb.yfin=image_height-1;
					if (prb.xfin>image_width-1)  prb.xfin=image_width-1;

					coordx+=sizeh;
					prbl[y][x]=prb;
					//System.out.println("PRbl["+y+"]["+x+"]:"+ " xini="+prb.xini+ " yini="+prb.yini+"  xfin="+prb.xfin+"   yfin="+prb.yfin);
				}//x
				coordy+=sizev;
				coordx=-sizeh/2;
			}//y

			//TO DO

		}
	//******************************************************************************
	//****************************************************************************
		public void computeMetrics(Grid grid_ant)//no se usa grid_ant
		{
			
			
			//first we compute metrics at each PRblock
			for (int y=0;y<number_of_blocks_V+1;y++)
			{
				for (int x=0;x<number_of_blocks_H+1;x++)
				{
					prbl[y][x].computePRmetrics();
					
				}//x
			}//y
			
			
			//analisys on PR and decision about best equalization
			//---------------------------------------------------
			//this computes PR average and standard deviation
			if (DEBUG)
			{
			
			float[] PRstats=new float[3];
			PRstats=computePRavg();
			float PRdev=computePRdev(PRstats[0]);
			
			System.out.println("--- PR STATS BEFORE EQUALIZATION ---");
			System.out.println("   >PR average:"+PRstats[0]+"    , max:"+PRstats[1]+"    , min:"+PRstats[2]);
			System.out.println("   >PR std deviation:"+PRdev);
			}
			
					
			

			//then we equalize PR histogram
			//------------------------------
			if (DEBUG) System.out.println(" equalizing...");
			
			//expandHistogramPR_02(PRstats);
			
			//expandHistogramPR_03(PRstats[0],PRdev);
			
			//printHistogramPR();
			
			topaPR(0.5f);
			
			expandHistogramPR_04();//PRstats[0],PRdev);
			
			storeNQPR();//grid_ant); no usa grid_ant
			
			if (DEBUG)
			{
			float[] PRstats=new float[3];
			PRstats=computePRavg();
			float PRdev=computePRdev(PRstats[0]);
			System.out.println("--- PR STATS  AFTER EQUALIZATION ---");
			System.out.println("   >PR average:"+PRstats[0]+"    , max:"+PRstats[1]+"    , min:"+PRstats[2]);
			System.out.println("   >PR std deviation:"+PRdev);
			}
			//equalizeHistogramPR();

			if (DEBUG) System.out.println(" quantizing...");
			// the last step: quantization of PR
			//-----------------------------------------
			for (int y=0;y<number_of_blocks_V+1;y++)
			{
				for (int x=0;x<number_of_blocks_H+1;x++)
				{
					prbl[y][x].quantizeGeometricalPR();
					
				}//x
			}//y

			
			
		}
	//*******************************************************************
	public void storeNQPR()//Grid grid_ant)
	{
		//se invoca despues de expandir y antes de cuantizar
		
		for (int y=0;y<number_of_blocks_V+1;y++)
		{
			for (int x=0;x<number_of_blocks_H+1;x++)
			{
				prbl[y][x].nqPRx=prbl[y][x].PRx;
				prbl[y][x].nqPRy=prbl[y][x].PRy;
				//System.out.println("TUNEANDO antes:"+grid_ant.prbl[y][x].nqPRx+"      despues:"+prbl[y][x].PRx);
				
				
				//if (prbl[y][x].PRx>=0) prbl[y][x].PRx=0.75f; 
				//if (prbl[y][x].PRy>=0) prbl[y][x].PRy=0.75f;
				//nueva historica
				//---------------------
				float sumx=0;
				float sumy=0;
				
				for (int i=3;i>0;i--)
				{
					sumx+=prbl[y][x].HistnqPRx[i];
					sumy+=prbl[y][x].HistnqPRy[i];
					
					prbl[y][x].HistnqPRx[i]=prbl[y][x].HistnqPRx[i-1];
					prbl[y][x].HistnqPRy[i]=prbl[y][x].HistnqPRy[i-1];
				}
				//prbl[y][x].HistnqPRx[0]=prbl[y][x].PRx;
				//prbl[y][x].HistnqPRy[0]=prbl[y][x].PRy;
			    //---------------
				//limite a la bajada
				float u=0.25f;
				//if (prbl[y][x].PRx+u<prbl[y][x].HistnqPRx[1]) {prbl[y][x].PRx=prbl[y][x].HistnqPRx[1]-u;}
				//if (prbl[y][x].PRy+u<prbl[y][x].HistnqPRy[1]) {prbl[y][x].PRy=prbl[y][x].HistnqPRy[1]-u;}
				
				//no se permite bajar salvo si
				//if (prbl[y][x].HistnqPRx[1]>0.25 && prbl[y][x].HistnqPRx[1]<0.75 && prbl[y][x].PRx <prbl[y][x].HistnqPRx[1] ) prbl[y][x].PRx=prbl[y][x].HistnqPRx[1];
				//if (prbl[y][x].HistnqPRy[1]>0.25 && prbl[y][x].HistnqPRy[1]<0.75 && prbl[y][x].PRy <prbl[y][x].HistnqPRy[1] ) prbl[y][x].PRx=prbl[y][x].HistnqPRy[1];
				//limite a la bajada
				//if (prbl[y][x].PRx+0.25f<prbl[y][x].HistnqPRx[1]) {prbl[y][x].PRx=prbl[y][x].HistnqPRx[1]-0.25f;}
				//if (prbl[y][x].PRy+0.25f<prbl[y][x].HistnqPRy[1]) {prbl[y][x].PRy=prbl[y][x].HistnqPRy[1]-0.25f;}
				
				//if (sumx==0)prbl[y][x].PRx=0.5f;//regenera cada n frames
				//if (sumy==0)prbl[y][x].PRy=0.5f;
				prbl[y][x].HistnqPRx[0]=prbl[y][x].PRx;
				prbl[y][x].HistnqPRy[0]=prbl[y][x].PRy;
			    
				//if (prbl[y][x].HistnqPRx[0]>0 && prbl[y][x].HistnqPRx[1]==0) prbl[y][x].PRx=0.75f; 
				//if (prbl[y][x].HistnqPRx[0]>0 && prbl[y][x].HistnqPRx[1]==0) prbl[y][x].PRx=0.75f;
				
				
					//modifico PRx pero no la historia, selectivo por cuantos, aguantando si cambia de cuanto
				/*
				if (prbl[y][x].HistnqPRx[0]<prbl[y][x].HistnqPRx[1] ||
						prbl[y][x].HistnqPRy[0]<prbl[y][x].HistnqPRy[1]
						)
				{
					boolean hecho=false;
					float u=0.74f;
					if (!hecho && prbl[y][x].HistnqPRx[1]>=u && prbl[y][x].HistnqPRx[0]<u) {prbl[y][x].PRx=u+1.1f;hecho=true;}//prbl[y][x].HistnqPRx[1];
					if (!hecho && prbl[y][x].HistnqPRy[1]>=u && prbl[y][x].HistnqPRy[0]<u) {prbl[y][x].PRy=u+1.1f;hecho=true;}//prbl[y][x].HistnqPRy[1];
					
					u=0.49f;
					if (!hecho && prbl[y][x].HistnqPRx[1]>=u && prbl[y][x].HistnqPRx[0]<u) {prbl[y][x].PRx=u+1.1f;hecho=true;}//0.75f;//prbl[y][x].HistnqPRx[1];
					if (!hecho && prbl[y][x].HistnqPRy[1]>=u && prbl[y][x].HistnqPRy[0]<u) {prbl[y][x].PRy=u+1.1f;hecho=true;}//0.75f;//prbl[y][x].HistnqPRy[1];
					u=0.24f;
					if (!hecho && prbl[y][x].HistnqPRx[1]>=u && prbl[y][x].HistnqPRx[0]<u) {prbl[y][x].PRx=u+1.1f;hecho=true;}//0.5f;//prbl[y][x].HistnqPRx[1];
					if (!hecho && prbl[y][x].HistnqPRy[1]>=u && prbl[y][x].HistnqPRy[0]<u) {prbl[y][x].PRy=u+1.1f;hecho=true;}//0.5f;//prbl[y][x].HistnqPRy[1];
				}
				
				*/
				
				/*
				// DE MOMENTO ESTO ES LO MEJOR
				if (prbl[y][x].HistnqPRx[0]< prbl[y][x].HistnqPRx[1] && prbl[y][x].HistnqPRx[1]>0.25f && prbl[y][x].HistnqPRx[0]<0.75f) //si pr menor que la ant
				{
				//if ((prbl[y][x].PRx<0.5 && prbl[y][x].HistnqPRx[1] >=0.5) ||
				//		(prbl[y][x].PRx<0.75 && prbl[y][x].HistnqPRx[1] >=7.5) )
				prbl[y][x].PRx=0.75f;//prbl[y][x].HistnqPRx[1]+0.26f;;//igualamos
				//prbl[y][x].PRy=prbl[y][x].HistnqPRy[1];
				//prbl[y][x].HistnqPRx[0]=prbl[y][x].HistnqPRx[1];//igualamos la hist para la proxima
				
				}
				if (prbl[y][x].HistnqPRy[0]<prbl[y][x].HistnqPRy[1]&& prbl[y][x].HistnqPRy[1]>0.25f && prbl[y][x].HistnqPRy[0]<0.75f)
				{
				//	if ((prbl[y][x].PRy<0.5 && prbl[y][x].HistnqPRy[1] >=0.5) ||
					//(prbl[y][x].PRy<0.75 && prbl[y][x].HistnqPRy[1] >=7.5) )
					//prbl[y][x].PRx=prbl[y][x].HistnqPRx[1];
					prbl[y][x].PRy=0.75f;//prbl[y][x].HistnqPRy[1]+0.26f;
					//prbl[y][x].HistnqPRy[0]=prbl[y][x].HistnqPRy[1];
					
				}
				//-----------------END LO MEJOR
				*/
				/*
				float level0=0.25f;//0.5f;//0.25f;
				float level1=0.5f;//1.4f;//0.50f;
				float level2=0.75f;//
				if (prbl[y][x].PRx<level0) prbl[y][x].PRx=0.125f;//level0/2f;
				else if (prbl[y][x].PRx<level1) prbl[y][x].PRx=0.25f;
				else if (prbl[y][x].PRx<level2) prbl[y][x].PRx=0.5f;
				else prbl[y][x].PRx=1f;//(1f+level2)/2f;

				if (prbl[y][x].PRy<level0) prbl[y][x].PRy=0.125f;//level0/2f;
				else if (prbl[y][x].PRy<level1) prbl[y][x].PRy=0.25f;
				else if (prbl[y][x].PRy<level2) prbl[y][x].PRy=0.5f;
				else prbl[y][x].PRy=1f;//(1f+level2)/2f;
				*/
				//cuantizamos
				/*
				if (prbl[y][x].HistnqPRx[0]>0 && prbl[y][x].HistnqPRx[0]<1)
				{
					if (prbl[y][x].HistnqPRx[0]>=0.75) prbl[y][x].HistnqPRx[0]=0.75f;
					else if (prbl[y][x].HistnqPRx[0]>=0.5) prbl[y][x].HistnqPRx[0]=0.5f;
					else if (prbl[y][x].HistnqPRx[0]>=0.25) prbl[y][x].HistnqPRx[0]=0.25f;
					else if (prbl[y][x].HistnqPRx[0]>0) prbl[y][x].HistnqPRx[0]=0.125f;
				}
				if (prbl[y][x].HistnqPRy[0]>0 && prbl[y][x].HistnqPRy[0]<1)
				{
					if (prbl[y][x].HistnqPRy[0]>=0.75) prbl[y][x].HistnqPRy[0]=0.75f;
					else if (prbl[y][x].HistnqPRy[0]>=0.5) prbl[y][x].HistnqPRy[0]=0.5f;
					else if (prbl[y][x].HistnqPRy[0]>=0.25) prbl[y][x].HistnqPRy[0]=0.25f;
					else if (prbl[y][x].HistnqPRy[0]>0) prbl[y][x].HistnqPRy[0]=0.125f;
				}
				*/
				//upgrade comienzo de mov, para evitar manchas
				
				//no permito dos escalones hacia arriba en un solo frame
				/*
				if (prbl[y][x].HistnqPRx[0]>prbl[y][x].HistnqPRx[1])
				  {	
				  if (prbl[y][x].HistnqPRx[0]>prbl[y][x].HistnqPRx[1]+0.25f)
					{
					  prbl[y][x].HistnqPRx[0]=prbl[y][x].HistnqPRx[1]+0.25f;
					  prbl[y][x].PRx=prbl[y][x].HistnqPRx[0];
					}
				  else
				    {
					//  prbl[y][x].HistnqPRx[0]=prbl[y][x].HistnqPRx[1]; //no sube nada?
				    }
				  }
				if (prbl[y][x].HistnqPRy[0]>prbl[y][x].HistnqPRy[1])
				  {  
				  if (prbl[y][x].HistnqPRy[0]>prbl[y][x].HistnqPRy[1]+0.25f)
				    {
					  prbl[y][x].HistnqPRy[0]=prbl[y][x].HistnqPRy[1]+0.25f;
				      prbl[y][x].PRy=prbl[y][x].HistnqPRy[0];
				    }
				  else
				    {
					 // prbl[y][x].HistnqPRy[0]=prbl[y][x].HistnqPRy[1]; //no subir 
				    }
				  }
				  */
			   //aceptamos subidas pero no bajadas a menos que sea quieto (=0)
				//ojo. <0.25 es max compresion pero no es cero y estropea. Comprobamos >0 y no >025 
				//problema: una subida solo queda bien si lo que estaba parado era liso
				
				/*
				if (prbl[y][x].HistnqPRx[0]>0 && prbl[y][x].HistnqPRx[0]<prbl[y][x].HistnqPRx[1])
				  {
				  prbl[y][x].PRx=prbl[y][x].HistnqPRx[1];
				  prbl[y][x].HistnqPRx[0]=prbl[y][x].PRx;
				  
				  }
				if (prbl[y][x].HistnqPRy[0]>0  && prbl[y][x].HistnqPRy[0]<prbl[y][x].HistnqPRy[1]) 
				  {
				  prbl[y][x].PRy=prbl[y][x].HistnqPRy[1];
				  prbl[y][x].HistnqPRy[0]=prbl[y][x].PRy;
				  }
				*/
				
				//justo antes de entrar en parada subimos
				//if (prbl[y][x].HistnqPRx[0]==0 && prbl[y][x].HistnqPRx[1]>0 )prbl[y][x].PRx=0.75f;
				//if (prbl[y][x].HistnqPRy[0]==0 && prbl[y][x].HistnqPRy[1]>0 )prbl[y][x].PRy=0.75f;
				//if (prbl[y][x].HistnqPRx[0]==0 )prbl[y][x].PRx=0.75f;
				//if (prbl[y][x].HistnqPRy[0]==0  )prbl[y][x].PRy=0.75f;
				//if (prbl[y][x].HistnqPRy[0]>=0.25 )prbl[y][x].PRy=0.75f;
				
				//prbl[y][x].PRy=0.5f;
				//else {prbl[y][x].HistnqPR[0]=0;prbl[y][x].PRx=0;}
				//if (prbl[y][x].HistnqPRy[0]<prbl[y][x].HistnqPRy[1]&& prbl[y][x].HistnqPRy[1]>0.25) {prbl[y][x].PRy=1;}
				//else {prbl[y][x].HistnqPRy[0]=0;prbl[y][x].PRy=0;}
				
				/*
				
				if (prbl[y][x].HistnqPRx[0]>prbl[y][x].HistnqPRx[1] &&  prbl[y][x].HistnqPRx[1]<=0.25 )
					{
					
					prbl[y][x].PRx=1;
					prbl[y][x].HistnqPRx[0]=1;
					}
				if (prbl[y][x].HistnqPRy[0]> prbl[y][x].HistnqPRy[1] &&  prbl[y][x].HistnqPRy[1]<=0.25 )
				{
				prbl[y][x].PRy=1;
				prbl[y][x].HistnqPRy[0]=1;
				}
				
				//upgrade fin de mov
				if (prbl[y][x].HistnqPRx[0]<0.125 && prbl[y][x].HistnqPRx[1]>0  && prbl[y][x].HistnqPRx[1]<1)
					{
					
					prbl[y][x].PRx=1;
					prbl[y][x].HistnqPRx[0]=1;
					}
				if (prbl[y][x].HistnqPRy[0]<0.125 && prbl[y][x].HistnqPRy[1]>0  && prbl[y][x].HistnqPRy[1]<1)
				{
				prbl[y][x].PRy=1;
				prbl[y][x].HistnqPRy[0]=1;
				}
			    //upgrade normal
				if (prbl[y][x].HistnqPRx[0]>0.0 && prbl[y][x].HistnqPRx[0]<prbl[y][x].HistnqPRx[1] && prbl[y][x].HistnqPRx[1]<0.75)
				{
					prbl[y][x].PRx=1;
					prbl[y][x].HistnqPRx[0]=1;
					}	
				if (prbl[y][x].HistnqPRy[0]>0.0 && prbl[y][x].HistnqPRy[0]<prbl[y][x].HistnqPRy[1] && prbl[y][x].HistnqPRy[1]<0.75)
				{
					prbl[y][x].PRy=1;
					prbl[y][x].HistnqPRy[0]=1;
					}	
				
				*/
				
				
				
				/*
				//countdown hacia una mejora 
				//si desactivada y estamos debajo de 0.75 activamos
				if (prbl[y][x].HistnqPRx[9]==0 && prbl[y][x].HistnqPRx[0]<0.75 && prbl[y][x].HistnqPRx[0]>0.25) prbl[y][x].HistnqPRx[9]=1;//activa countdown]
				else if (prbl[y][x].HistnqPRx[0]>=0.75) {prbl[y][x].HistnqPRx[9]=0;prbl[y][x].HistnqPRx[8]=0;}//desactiva countdown
				if (prbl[y][x].HistnqPRx[9]==1) prbl[y][x].HistnqPRx[8]+=1;
				
				if (prbl[y][x].HistnqPRy[9]==0 && prbl[y][x].HistnqPRy[0]<0.75 && prbl[y][x].HistnqPRy[0]>0.25) prbl[y][x].HistnqPRy[9]=1;//activa countdown]
				else if (prbl[y][x].HistnqPRy[0]>=0.75)  {prbl[y][x].HistnqPRy[9]=0;prbl[y][x].HistnqPRy[8]=0;}//desactiva countdown
				if (prbl[y][x].HistnqPRy[9]==1)	prbl[y][x].HistnqPRy[8]+=1;
				
				
				//activacion de upgrade
				if (prbl[y][x].HistnqPRx[0]>prbl[y][x].HistnqPRx[1] && prbl[y][x].HistnqPRx[1] <0.75) prbl[y][x].HistnqPRx[4]=-1;
				if (prbl[y][x].HistnqPRy[0]>prbl[y][x].HistnqPRy[1] && prbl[y][x].HistnqPRy[1] <0.75) prbl[y][x].HistnqPRy[4]=-1;
				
				//ajuste de PR para poner limite a la bajada
				if (prbl[y][x].HistnqPRx[0]>0.75f) prbl[y][x].HistnqPRx[0]=0.75f;
				if (prbl[y][x].HistnqPRy[0]>0.75f) prbl[y][x].HistnqPRy[0]=0.75f;
				
				//limite a la bajada
				if (prbl[y][x].HistnqPRx[0]+0.25f<prbl[y][x].HistnqPRx[1]) prbl[y][x].HistnqPRx[0]=prbl[y][x].HistnqPRx[1]-0.25f;
				if (prbl[y][x].HistnqPRy[0]+0.25f<prbl[y][x].HistnqPRy[1]) prbl[y][x].HistnqPRy[0]=prbl[y][x].HistnqPRy[1]-0.25f;
			    //-------de momento esto es lo mejor. TRatamiento x e y separado pero relacionado
				//upgrade fin de mov
				
				if (prbl[y][x].HistnqPRx[0]==0 && prbl[y][x].HistnqPRx[1]>0 && prbl[y][x].HistnqPRx[1]<0.75)
					{
                    prbl[y][x].HistnqPRx[4]=0;//desactivacion
					prbl[y][x].HistnqPRx[9]=0;//counterdown activator
					prbl[y][x].HistnqPRx[8]=0;//counterdown counter
					
					prbl[y][x].PRx=0.75f;
					prbl[y][x].HistnqPRx[0]=0.75f;
					}
				if (prbl[y][x].HistnqPRy[0]==0 && prbl[y][x].HistnqPRy[1]>0 && prbl[y][x].HistnqPRy[1]<0.75)
				{
                    prbl[y][x].HistnqPRy[4]=0;
					prbl[y][x].HistnqPRy[9]=0;//counterdown activator
					prbl[y][x].HistnqPRy[8]=0;//counterdown counter	
					
				prbl[y][x].PRy=0.75f;
				prbl[y][x].HistnqPRy[0]=0.75f;
				}
				
				int countdown=1000000;
				//if (prbl[y][x].HistnqPRx[0]>0.5f) prbl[y][x].PRx=0.75f;else prbl[y][x].PRx=0.0f;
				//if (prbl[y][x].HistnqPRy[0]>0.5f) prbl[y][x].PRy=0.75f;else prbl[y][x].PRy=0.0f;
				
				if ((	prbl[y][x].HistnqPRx[4]==-1 &&//activo
						prbl[y][x].HistnqPRy[4]==-1 &&// no se porque pero ahorra un 2% y queda igual
						
						prbl[y][x].HistnqPRx[0]>=0.25 &&
						(prbl[y][x].HistnqPRx[0]<prbl[y][x].HistnqPRx[1]&& prbl[y][x].HistnqPRx[1]>=0.25 && prbl[y][x].HistnqPRx[1]<0.75)//||
						
						) || (prbl[y][x].HistnqPRx[8]>=countdown &&  prbl[y][x].HistnqPRx[1]<0.75))
				{
					
					//upgradex=true;
					
					
					prbl[y][x].HistnqPRx[4]=0;//desactivacion
					
					
					prbl[y][x].HistnqPRx[9]=0;//counterdown activator
					prbl[y][x].HistnqPRx[8]=0;//counterdown counter
					
					prbl[y][x].PRx=0.75f;//correccion
				    prbl[y][x].HistnqPRx[0]=0.75f;
				
				    
				    
				
				}
				if ((
						prbl[y][x].HistnqPRy[4]==-1 &&
						prbl[y][x].HistnqPRx[4]==-1 && //ahorra 2% y queda igual
						
						prbl[y][x].HistnqPRy[0]>=0.25 &&
						(prbl[y][x].HistnqPRy[0]<prbl[y][x].HistnqPRy[1] && prbl[y][x].HistnqPRy[1]>=0.25 && prbl[y][x].HistnqPRy[1]<0.75)  //||
					
				) || (prbl[y][x].HistnqPRy[8]>=countdown  &&  prbl[y][x].HistnqPRy[1]<0.75))
				{
					//upgradey=true;
					
					prbl[y][x].HistnqPRy[4]=0;
					
					prbl[y][x].HistnqPRy[9]=0;//counterdown activator
					prbl[y][x].HistnqPRy[8]=0;//counterdown counter
					
					prbl[y][x].PRy=0.75f;
					prbl[y][x].HistnqPRy[0]=0.75f;
					
					
					
				}
				 
				*/
				//-----------------
				/*
				if (
						prbl[y][x].HistnqPRx[4]==-1 &&
						prbl[y][x].HistnqPRy[4]==-1 &&
						
						prbl[y][x].HistnqPRx[0]>=0.25 &&
						(prbl[y][x].HistnqPRx[0]<prbl[y][x].HistnqPRx[1]&& prbl[y][x].HistnqPRx[1]>=0.25 && prbl[y][x].HistnqPRx[1]<0.75)//||
								
						&&
						prbl[y][x].HistnqPRy[0]>=0.25 &&
						(prbl[y][x].HistnqPRy[0]<prbl[y][x].HistnqPRy[1] && prbl[y][x].HistnqPRy[1]>=0.25 && prbl[y][x].HistnqPRy[1]<0.75)  //||
						
	                           )
						
				{
				
					prbl[y][x].HistnqPRx[4]=0;
					
					prbl[y][x].PRx=0.75f;//prbl[y][x].HistnqPRx[1]+0.26f;;//igualamos
				    prbl[y][x].HistnqPRx[0]=0.75f;
				
				
				
					prbl[y][x].HistnqPRy[4]=0;
					prbl[y][x].PRy=0.75f;
					prbl[y][x].HistnqPRy[0]=0.75f;
					
				}
				
				*/
				
				
				
				
				
				    //modifico PRx pero no la historia
				    // no selectivo por cuantos. si baja, retenemos,
				    //si sumo 0.25 me voy al cuanto superior. no es corregir 100%, como usar 1, pero se parece
			
				
				
				/*
				boolean hechox=false;
				boolean hechoy=false;
				   for (float u=0.75f;u>0;u-=0.25f)
				   {  
					if (prbl[y][x].HistnqPRx[0]< u && prbl[y][x].HistnqPRx[1]>u &&
							prbl[y][x].HistnqPRx[0]<prbl[y][x].HistnqPRx[1]) //si pr menor que la ant
					{
					prbl[y][x].PRx=0.75f;//prbl[y][x].HistnqPRx[1]+0.26f;;//igualamos
					//prbl[y][x].PRy=prbl[y][x].HistnqPRy[1];
					//prbl[y][x].HistnqPRx[0]=prbl[y][x].HistnqPRx[1];//igualamos la hist para la proxima
					hechox=true;
					}
					if (prbl[y][x].HistnqPRy[0]< u && prbl[y][x].HistnqPRy[1]>u &&
							prbl[y][x].HistnqPRy[0]<prbl[y][x].HistnqPRy[1])
					{
						//prbl[y][x].PRx=prbl[y][x].HistnqPRx[1];
						prbl[y][x].PRy=0.75f;//prbl[y][x].HistnqPRy[1]+0.26f;
						//prbl[y][x].HistnqPRy[0]=prbl[y][x].HistnqPRy[1];
						hechoy=true;
					}
						if (hechox && hechoy) break;
				   }
				*/
				
				/*
				float umbral=0.01f;
				float PRxant=grid_ant.prbl[y][x].nqPRx;
				if (prbl[y][x].PRx<PRxant)
				{
					if (prbl[y][x].PRx<PRxant-umbral) {
						prbl[y][x].PRx=PRxant-umbral;
						prbl[y][x].nqPRx=prbl[y][x].PRx;
					}
				}
				float PRyant=grid_ant.prbl[y][x].nqPRy;
				if (prbl[y][x].PRy<PRyant)
				{
					if (prbl[y][x].PRy<PRyant-umbral) 
						{
						prbl[y][x].PRy=PRyant-umbral;
						prbl[y][x].nqPRy=prbl[y][x].PRy;
						}
				}
				*/
				/*
				//otra forma: que la primera vez que baja no baje
				if (prbl[y][x].PRx<PRxant-0.25)
				{
						prbl[y][x].nqPRx=prbl[y][x].PRx;
					
				}
				*/
				
				
				
				
				
				
				//System.out.println("TUNEADO :"+prbl[y][x].nqPRx);//+"      despues:"+prbl[y][x].PRx);
				
			}//x
		}//y	
	}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public void setMaxPR()
	{
		// bucle filling PRblocks information
			
			for (int y=0;y<number_of_blocks_V+1;y++)
			{

				for (int x=0;x<number_of_blocks_H+1;x++)
				{
					
					prbl[y][x].PRx=0.5f;
					//prbl[y][x].nqPRx=1.0f;
					prbl[y][x].PRy=0.5f;
					//prbl[y][x].nqPRy=1.0f;
					
				}//x
				
			}//y

			//TO DO

		}
	//******************************************************************************
}//end class Grid
