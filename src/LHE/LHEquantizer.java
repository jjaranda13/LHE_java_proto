package LHE;

/*
 * 
 * hops are stored in:
 *   ImgUtil.hops   - initial LHE encoding
 *   ImgUtil.downsampled_hops  - LHE encoding after downsampling
 * 
 * 
 * luminances:
 *   original luminance at: ImgUtil.YUV[0]
 *   resulting luminance must be stored at: ImgUtil.LHE_YUV[0]
 * 
 * precomputed color components
 *   in order to avoid calculations at every hop assignment, a precomputed cache table of all possible luminance values are
 *   stored at "pcc" array. this allows fast assignment of the luminances corresponding with h0...h8
 * 
 */
public class LHEquantizer {

	//img contains the hops, the original luminance & chroma and the final color components
	public ImgUtil img;//same for all instances of this class

	//precomputed color components for all hops and for all h0 lum value, initialized at init() function
	//[ofp][yref][hop number]
	static int[][][] pcc; //precomputed color component

	static int[][][][] pcck; //precomputed color component
	
	static int[][][][] pccr; //precomputed color component

	
	int contafix=0;
	int bits_fix=0;
	
	int LHE2_resta=0;
	
	//parametros RLC
	//===============
	int TAMANO_RLC=4;//numero de bits del codigo RLC
	int TAMANO_condicion=8;//TAMANO_RLC; num hops nulos como condicion de cambio a modo RLC.
	
	//********************************************************************************************
	public LHEquantizer()
	{
		//at LHEquantizer creation, the precomputations are done
		//if (pccr==null) initGeomR();
		
		if (pccr==null)initPreComputations();
	}
	
	//*********************************************************************************************
	public void init()
	{
		//cache to avoid pow functions
		float[][][][] cache_pow; //[hops][+/-][component value][k x 100]


		//h[hop_number][absolute h1 value][luminance of h0 value]
		float[][] h1,h2,h3,h4,h5,h6;
		//S=new int[9][9][256];
		// h = new float[6][9][256];

		h1=new float[9][256];
		h2=new float[9][256];
		h3=new float[9][256];
		h4=new float[9][256];
		h5=new float[9][256];
		h6=new float[9][256];

		//cf3=new int[9][256][9];
		pcc=new int[9][256][9];
		//for (int i=0;i<9;i++) for (int j=0;j<9;j++) for (int k=0;k<256;k++)S[i][j][k]=10;
		//precomputation of powers for fast quantization process
		//if (cache_pow==null) {

		//cache of power ( to avoid Math.pow operation)
		//---------------------------------------------
		cache_pow=new float[10][2][256][1000];
		for (int yref=0;yref<=255;yref++) {
			for (int ki=100;ki<800;ki++)
			{
				double k=((double)ki)/100.0;
				//precomputed values for different k values
				// k =3 generates abrupt distribution (separated)
				// k= 4 generates soft hops ( condensed)
				// k will belong to interval [3..4]--> ki belongs to [300..400]
				// here we calculate more possibilities, ki belongs to [100..800], but only use [300..400] 
				cache_pow[8][0][yref][ki]=(float)Math.pow((255-yref)/k, 1/k);
				cache_pow[8][1][yref][ki]=(float)Math.pow((yref)/k, 1/k);
			}

			
			//HAY QUE HACER UN BUCLE PARA CADA Kini
			/*
			float kini=3.65f;
			
			for (kini=3.0 ;kini<=4.5f;kini+=0.1f)
			{
			
			LUEGO HAY QUE HACER QUE PCC SEA PCC[KINI][ofp][yref][HOP]
			
			*/
			
			//assignment of precomputed hop values, for each ofp value
			//--------------------------------------------------------
			for (int ofp=0;ofp<=8;ofp++)
			{
				float kini=3.65f;
				kini=4f;
				
				//k value for initial LHE quantization. not valid for LHE applied on downsampled image
				double k=kini+(float)(ofp-4)*0.1275;//0.1275f;//0.13

				float potencia_pos=cache_pow[8][0][yref][(int)(k*100)];//Math.pow((255-yref)/k, 1/k);
				float potencia_neg=cache_pow[8][1][yref][(int)(k*100)];//Math.pow((yref)/k, 1/k);

				//possitive hops
				h1[ofp][yref] = ofp*potencia_pos;
				h2[ofp][yref] = h1[ofp][yref]*potencia_pos;
				h3[ofp][yref] = h2[ofp][yref]*potencia_pos;


				//negative hops	                        
				h4[ofp][yref] =ofp*potencia_neg;
				h5[ofp][yref] = h4[ofp][yref]*potencia_neg;
				h6[ofp][yref] = h5[ofp][yref]*potencia_neg;


				//final color component ( luminance or chrominance). depends on hop1
				//from most negative hop (fcc[ofp][yref][0]) to most possitive hop (fcc[ofp][yref][8])
				//--------------------------------------------------------------------------------------
				pcc[ofp][yref][0]= yref  - (int) h6[ofp][yref] ; if (pcc[ofp][yref][0]<=0) { pcc[ofp][yref][0]=1;}
				pcc[ofp][yref][1]= yref  - (int) h5[ofp][yref]; if (pcc[ofp][yref][1]<=0) {pcc[ofp][yref][1]=1;}
				pcc[ofp][yref][2]= yref  - (int) h4[ofp][yref]; if (pcc[ofp][yref][2]<=0) { pcc[ofp][yref][2]=1;}
				pcc[ofp][yref][3]=yref-ofp;if (pcc[ofp][yref][3]<=0) pcc[ofp][yref][3]=1;
				pcc[ofp][yref][4]=yref; if (pcc[ofp][yref][4]<=0) pcc[ofp][yref][4]=1; //null hop
				pcc[ofp][yref][5]= yref+ofp;if (pcc[ofp][yref][5]>255) pcc[ofp][yref][5]=255;
				pcc[ofp][yref][6]= yref  + (int) h1[ofp][yref]; if (pcc[ofp][yref][6]>255) {pcc[ofp][yref][6]=255;}
				pcc[ofp][yref][7]= yref  + (int) h2[ofp][yref]; if (pcc[ofp][yref][7]>255) {pcc[ofp][yref][7]=255;}
				pcc[ofp][yref][8]= yref  + (int) h3[ofp][yref]; if (pcc[ofp][yref][8]>255) {pcc[ofp][yref][8]=255;}
			}

		}
		//}
		/*
System.out.println(" yref=128, hop1=8 k=3.65 hops:"+pcc[8][128][5]+","+pcc[8][128][6]+","+pcc[8][128][7]+","+pcc[8][128][8]);
System.out.println(" yref=128, hop1=4 k=3.65 hops:"+pcc[4][128][5]+","+pcc[4][128][6]+","+pcc[4][128][7]+","+pcc[4][128][8]);
System.out.println(" yref=0, hop1=8 k=3.65 hops  :"+pcc[8][0][5]+","+pcc[8][0][6]+","+pcc[8][0][7]+","+pcc[8][0][8]);
System.out.println(" yref=0, hop1=4 k=3.65 hops  :"+pcc[4][0][5]+","+pcc[4][0][6]+","+pcc[4][0][7]+","+pcc[4][0][8]);
System.exit(0);
*/
	}

	//**************************************************************************************************
	/**
	 * This is a very fast LHE quantization function, used for initial quantization in order to 
	 * perform Perceptual Relevance Metrics.
	 * Later quantization over downwsampled image allows more tunning ( k value) and therefore 
	 * require more complex calculation (but over a reduced image)
	 * 
	 * image luminance array is the input for this function. 
	 *   This luminance array is suposed to be stored at img.YUV[0][pix]; 
	 *   Image luminance array is not modified
	 * 
	 * hops numbering:
	 *   >negative hops: 0,1,2,3
	 *   >null hop: 4
	 *   >positive hops: 5,6,7,8
	 * 
	 * result_YUV is output array. it can not be removed, because these luminance & chroma values are part of
	 *   algorithm to choose next hop
	 * 
	 * 
	 * @param hops
	 * @param result_YUV
	 */
	public void quantizeOneHopPerPixel_OLD(int[] hops,int[] result_YUV)
	{
		//img.width=1920;
		//img.height=1080;

		//img.width=1280;
		//img.height=720;

		//img.width=720;
		//img.height=576;
		//img.width=429;
		//img.height=429;
		//img.width=576;
		//img.height=256;
		int max_hop1=8;// hop1 interval 4..8
		int min_hop1=4;// 
		int hop1=max_hop1;
		int hop0=0; // predicted signal
		int emin;//error of predicted signal
		int hop_number=4;//selected hop // 4 is NULL HOP
		int oc=0;// original color
		int pix=0;//pixel possition, from 0 to image size        
		boolean last_small_hop=false;// indicates if last hop is small

		for (int y=0;y<img.height;y++)  {
			for (int x=0;x<img.width;x++)  {

				oc=img.YUV[0][pix];

				//prediction of signal (hop0) , based on pixel's coordinates 
				//----------------------------------------------------------
				if ((y>0) &&(x>0) && x!=img.width-1){
					hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	

					//	System.out.println(" result_YUV[pix-1]:"+result_YUV[pix-1]+"  result_YUV[pix+1-img.width]: "+result_YUV[pix+1-img.width]);
				}
				else if ((x==0) && (y>0)){
					hop0=result_YUV[pix-img.width];
					last_small_hop=false;
					hop1=max_hop1;
				}
				else if ((x==img.width-1) && (y>0)) {
					hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
				}else if (y==0 && x>0) {
					hop0=result_YUV[x-1];
				}else if (x==0 && y==0) {  
					hop0=oc;//first pixel always is perfectly predicted! :-)  
				}			


				//hops computation. initial values for errors
				emin=256;//current minimum prediction error 
				int e2=0;//computed error for each hop 

				//hop0 is prediction
				//if (hop0>255)hop0=255;
				//else if (hop0<0) hop0=0; 


				//positive hops computation
				//-------------------------
				if (oc-hop0>=0) 
				{
					for (int j=4;j<=8;j++) {
						e2=oc-pcc[hop1][hop0][j];
						if (e2<0) e2=-e2;
						if (e2<emin) {hop_number=j;emin=e2;}
						else break;
					}
				}
				//negative hops computation
				//-------------------------
				else 
				{
					//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
					//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
					for (int j=4;j>=0;j--) {
						e2=pcc[hop1][hop0][j]-oc;
						if (e2<0) e2=-e2;
						if (e2<emin) {hop_number=j;emin=e2;}
						else break;
					}
				}

				//assignment of final color value
				//--------------------------------
				result_YUV[pix]=pcc[hop1][hop0][hop_number];
				//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
				//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
				hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

				//tunning hop1 for the next hop
				//-------------------------------
				boolean small_hop=false;
				//if (hop_number>=6) small_hop=true;
				//if (hop_number<=6 && hop_number>=2) small_hop=true;
				if (hop_number<=5 && hop_number>=3) small_hop=true;
				else small_hop=false;     

				if( (small_hop) && (last_small_hop))  {
					hop1=hop1-1;
					if (hop1<min_hop1) hop1=min_hop1;
				} 
				else {
					hop1=max_hop1;
				}
				//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

				//lets go for the next pixel
				//--------------------------
				last_small_hop=small_hop;
				pix++;            
			}//for x
		}//for y

	}//end function

	//**************************************************************************************************
	/**
	 * 
	 * 	 downsampled image luminance array is the input for this function. 
	 *   This downsampled luminance array is suposed to be stored at img.downsampled_YUV[0][pix]; 
	 *   downsampled Image luminance array is not modified
	 * 
	 *   ahother input for this function is img.boundaries_YUV[0][pix];
	 * 
	 * 
	 * @param b
	 * @param hops        : OUTPUT
	 * @param result_YUV   : OUTPUT
	 * 
	 */
	public void quantizeDownsampledBlock(Block b, int[] hops,int[] result_YUV, int[] src_YUV, int[] boundaries_YUV)
	{

		
		
		//block b contains the coordinates to set the limits of this function
		
		
		//some parts of this code are identical to the function quantizeOneHopPerPixel() 
		
		
		int max_hop1=8;// hop1 interval 4..8
		int min_hop1=4;// 
		int hop1=max_hop1;
		int hop0=0; // predicted signal
		int emin;//error of predicted signal
		int hop_number=4;//selected hop // 4 is NULL HOP
		int oc=0;// original color
		
		int pix=b.yini*img.width+b.xini;//initial pixel possition        
		
		boolean last_small_hop=false;// indicates if last hop is small
		for (int y=b.yini;y<=b.downsampled_yfin;y++)  {
			for (int x=b.xini;x<=b.downsampled_xfin;x++)  {

				pix=y*img.width+x;
				oc=src_YUV[pix];

				//prediction of signal (hop0) , based on pixel's coordinates 
				//----------------------------------------------------------
				
				//inner pixels ( mostly of them. that is the reason for considering the 1st option)
				//---------------------------------------------------------------------------------
				 if ((y>b.yini) &&(x>b.xini) && x!=b.downsampled_xfin){
					hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	

					//	System.out.println(" result_YUV[pix-1]:"+result_YUV[pix-1]+"  result_YUV[pix+1-img.width]: "+result_YUV[pix+1-img.width]);
				}
				//initial pix
				//-----------
				if (x==0 && y==0) {  
					hop0=oc;//first pixel always is perfectly predicted! :-)  
				}	
				//upper side of the image
			    //-----------------------
				else if (y==0 && x>b.xini) {
					hop0=result_YUV[x-1];
				}
				else if (y==0 && x==b.xini) {
					hop0=boundaries_YUV[x-1];
				}
				
				//left side of the image
				//------------------------
				else if ((x==0) && (y>b.yini)){
					
					hop0=result_YUV[pix-img.width];
					//hop0=img.boundaries_YUV[0][pix-img.width];
					last_small_hop=false;
					hop1=max_hop1;
				}
                else if ((x==0) && (y==b.yini)){
					
					//hop0=result_YUV[pix-img.width];
					hop0=img.boundaries_YUV[0][pix-img.width];
					last_small_hop=false;
					hop1=max_hop1;
				}
				//left side of the block
				//------------------------
				else if ((x==b.xini) && (y>b.yini)){
					//System.out.print("zulu");
						hop0=(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7;
						//hop0=img.boundaries_YUV[0][pix-1];
						//System.out.println(" el boundary v tiene color:"+img.boundaries_YUV[0][pix-1]+"  at  x:"+(x-1)+" ,y:"+y);
				}
				//up-left corner of block
				//--------------
				else if ((x==b.xini) && (y==b.yini)){
					//System.out.print("zulu");
					hop0=(4*img.boundaries_YUV[0][pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;	
				}
				
				
				
				//right side of block (and right side of image. is the same case)
				//-----------------------------------------------------------------
				else if ((x==b.downsampled_xfin) && (y>b.yini)) {
					hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;	
					
					
					
				}
				
					
				//upper side of block
				//---------------------
			    else if (y==b.yini && x>0 && x!=b.downsampled_xfin) {
			    	hop0=(4*result_YUV[pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;	
			    	//hop0=(4*result_YUV[pix-1]+1*img.boundaries_YUV[0][pix+1-img.width])/5;
			    	//hop0=result_YUV[pix-1];
			    }
			    else if ((x==b.downsampled_xfin) && (y==b.yini)) {
					//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;
					hop0=(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix-img.width])/6;
					//if (result_YUV[pix-img.width]==0) System.out.println(" ey");
				}
			    	
				
				
				
				//hops computation. initial values for errors
				emin=256;//current minimum prediction error 
				int e2=0;//computed error for each hop 

				//hop0 is prediction
				//if (hop0>255)hop0=255;
				//else if (hop0<0) hop0=0; 


				// el array PCC habria que escogerlo en funcion de kini
				
				
				//positive hops computation
				//-------------------------
				if (oc-hop0>=0) 
				{
					for (int j=4;j<=8;j++) {
						try{
						e2=oc-pcc[hop1][hop0][j];
						}catch (Exception e){
							System.out.println("j:"+j+" hop1:"+hop1+"  hop0:"+hop0+"  x:"+x+"  y:"+y+"  b.xini:"+b.xini+" b.yini:"+b.yini+" b.downsampled_xfin:"+b.downsampled_xfin+"  up:"+result_YUV[pix-img.width]+" izq:"+result_YUV[pix-1]+ " debe ser:"+(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7+ "boundaries es:"+img.boundaries_YUV[0][pix-1]);
							System.exit(0);
						}
						if (e2<0) e2=-e2;
						if (e2<emin) {hop_number=j;emin=e2;}
						else break;
					}
				}
				//negative hops computation
				//-------------------------
				else 
				{
					//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
					//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
					for (int j=4;j>=0;j--) {
						e2=pcc[hop1][hop0][j]-oc;
						if (e2<0) e2=-e2;
						if (e2<emin) {hop_number=j;emin=e2;}
						else break;
					}
				}

				//assignment of final color value
				//--------------------------------
				result_YUV[pix]=pcc[hop1][hop0][hop_number];
				//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
				//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
				hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

				//tunning hop1 for the next hop
				//-------------------------------
				boolean small_hop=false;
				//if (hop_number>=6) small_hop=true;
				//if (hop_number<=6 && hop_number>=2) small_hop=true;
				if (hop_number<=5 && hop_number>=3) small_hop=true;
				else small_hop=false;     

				if( (small_hop) && (last_small_hop))  {
					hop1=hop1-1;
					if (hop1<min_hop1) hop1=min_hop1;
				} 
				else {
					hop1=max_hop1;
				}
				//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

				//lets go for the next pixel
				//--------------------------
				last_small_hop=small_hop;
				
				
				
				//pix++;            
			}//for x
			//esto es necesario al funcionar con bloques
			//pix+=img.width-b.lx_sc+1;
		}//for y

	}//end function
	//**************************************************************************************
	//*********************************************************************************************
		public void initK()
		{
			//cache to avoid pow functions
			float[][][][] cache_pow; //[hops][+/-][component value][k x 100]


			//h[hop_number][absolute h1 value][luminance of h0 value]
			float[][] h1,h2,h3,h4,h5,h6;
			//S=new int[9][9][256];
			// h = new float[6][9][256];

			h1=new float[9][256];
			h2=new float[9][256];
			h3=new float[9][256];
			h4=new float[9][256];
			h5=new float[9][256];
			h6=new float[9][256];

			//cf3=new int[9][256][9];
			pcck=new int[50][9][256][9];
			//for (int i=0;i<9;i++) for (int j=0;j<9;j++) for (int k=0;k<256;k++)S[i][j][k]=10;
			//precomputation of powers for fast quantization process
			//if (cache_pow==null) {

			//cache of power ( to avoid Math.pow operation)
			//---------------------------------------------
			cache_pow=new float[10][2][256][1000];
			for (int yref=0;yref<=255;yref++) {
				for (int ki=100;ki<800;ki++)
				{
					double k=((double)ki)/100.0;
					//precomputed values for different k values
					// k =3 generates abrupt distribution (separated)
					// k= 4 generates soft hops ( condensed)
					// k will belong to interval [3..4]--> ki belongs to [300..400]
					// here we calculate more possibilities, ki belongs to [100..800], but only use [300..400] 
					cache_pow[8][0][yref][ki]=(float)Math.pow((255-yref)/k, 1/k);
					cache_pow[8][1][yref][ki]=(float)Math.pow((yref)/k, 1/k);
				}

				
				//HAY QUE HACER UN BUCLE PARA CADA Kini
				
				float kini=3.65f;
				
				for (int kinint=30 ;kinint<50f;kinint+=1)
				{
				kini=(float)kinint/10f;
				//LUEGO HAY QUE HACER QUE PCC SEA PCC[KINI][ofp][yref][HOP]
				
				
				
				//assignment of precomputed hop values, for each ofp value
				//--------------------------------------------------------
				for (int ofp=0;ofp<=8;ofp++)
				{
				//	float kini=3.65f;
				//	kini=3.5f;
					//k value for initial LHE quantization. not valid for LHE applied on downsampled image
					double k=kini+(float)(ofp-4)*0.1275;//0.1275f;//0.13

					float potencia_pos=cache_pow[8][0][yref][(int)(k*100)];//Math.pow((255-yref)/k, 1/k);
					float potencia_neg=cache_pow[8][1][yref][(int)(k*100)];//Math.pow((yref)/k, 1/k);

					//possitive hops
					h1[ofp][yref] = ofp*potencia_pos;
					h2[ofp][yref] = h1[ofp][yref]*potencia_pos;
					h3[ofp][yref] = h2[ofp][yref]*potencia_pos;


					//negative hops	                        
					h4[ofp][yref] =ofp*potencia_neg;
					h5[ofp][yref] = h4[ofp][yref]*potencia_neg;
					h6[ofp][yref] = h5[ofp][yref]*potencia_neg;


					//final color component ( luminance or chrominance). depends on hop1
					//from most negative hop (fcc[ofp][yref][0]) to most possitive hop (fcc[ofp][yref][8])
					//--------------------------------------------------------------------------------------
					pcck[kinint][ofp][yref][0]= yref  - (int) h6[ofp][yref] ; if (pcck[kinint][ofp][yref][0]<=0) { pcck[kinint][ofp][yref][0]=1;}
					pcck[kinint][ofp][yref][1]= yref  - (int) h5[ofp][yref]; if (pcck[kinint][ofp][yref][1]<=0) {pcck[kinint][ofp][yref][1]=1;}
					pcck[kinint][ofp][yref][2]= yref  - (int) h4[ofp][yref]; if (pcck[kinint][ofp][yref][2]<=0) { pcck[kinint][ofp][yref][2]=1;}
					pcck[kinint][ofp][yref][3]=yref-ofp;if (pcck[kinint][ofp][yref][3]<=0) pcck[kinint][ofp][yref][3]=1;
					pcck[kinint][ofp][yref][4]=yref; if (pcck[kinint][ofp][yref][4]<=0) pcck[kinint][ofp][yref][4]=1; //null hop
					pcck[kinint][ofp][yref][5]= yref+ofp;if (pcck[kinint][ofp][yref][5]>255) pcck[kinint][ofp][yref][5]=255;
					pcck[kinint][ofp][yref][6]= yref  + (int) h1[ofp][yref]; if (pcck[kinint][ofp][yref][6]>255) {pcck[kinint][ofp][yref][6]=255;}
					pcck[kinint][ofp][yref][7]= yref  + (int) h2[ofp][yref]; if (pcck[kinint][ofp][yref][7]>255) {pcck[kinint][ofp][yref][7]=255;}
					pcck[kinint][ofp][yref][8]= yref  + (int) h3[ofp][yref]; if (pcck[kinint][ofp][yref][8]>255) {pcck[kinint][ofp][yref][8]=255;}
				}

			}
			}
			//}
			/*
	System.out.println(" yref=128, hop1=8 k=3.65 hops:"+pcc[8][128][5]+","+pcc[8][128][6]+","+pcc[8][128][7]+","+pcc[8][128][8]);
	System.out.println(" yref=128, hop1=4 k=3.65 hops:"+pcc[4][128][5]+","+pcc[4][128][6]+","+pcc[4][128][7]+","+pcc[4][128][8]);
	System.out.println(" yref=0, hop1=8 k=3.65 hops  :"+pcc[8][0][5]+","+pcc[8][0][6]+","+pcc[8][0][7]+","+pcc[8][0][8]);
	System.out.println(" yref=0, hop1=4 k=3.65 hops  :"+pcc[4][0][5]+","+pcc[4][0][6]+","+pcc[4][0][7]+","+pcc[4][0][8]);
	System.exit(0);
	*/
		}
	
		public void quantizeDownsampledBlock_k(Block b, int[] hops,int[] result_YUV, int[] src_YUV, int[] boundaries_YUV)
		{

			int kinint =b.kinint;
			
			//block b contains the coordinates to set the limits of this function
			
			
			//some parts of this code are identical to the function quantizeOneHopPerPixel() 
			
			
			int max_hop1=8;// hop1 interval 4..8
			int min_hop1=4;// 
			int hop1=max_hop1;
			int hop0=0; // predicted signal
			int emin;//error of predicted signal
			int hop_number=4;//selected hop // 4 is NULL HOP
			int oc=0;// original color
			
			int pix=b.yini*img.width+b.xini;//initial pixel possition        
			
			boolean last_small_hop=false;// indicates if last hop is small
			for (int y=b.yini;y<=b.downsampled_yfin;y++)  {
				for (int x=b.xini;x<=b.downsampled_xfin;x++)  {

					pix=y*img.width+x;
					oc=src_YUV[pix];

					//prediction of signal (hop0) , based on pixel's coordinates 
					//----------------------------------------------------------
					
					//inner pixels ( mostly of them. that is the reason for considering the 1st option)
					//---------------------------------------------------------------------------------
					 if ((y>b.yini) &&(x>b.xini) && x!=b.downsampled_xfin){
						hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	

						//	System.out.println(" result_YUV[pix-1]:"+result_YUV[pix-1]+"  result_YUV[pix+1-img.width]: "+result_YUV[pix+1-img.width]);
					}
					//initial pix
					//-----------
					if (x==0 && y==0) {  
						hop0=oc;//first pixel always is perfectly predicted! :-)  
					}	
					//upper side of the image
				    //-----------------------
					else if (y==0 && x>b.xini) {
						hop0=result_YUV[x-1];
					}
					else if (y==0 && x==b.xini) {
						hop0=boundaries_YUV[x-1];
					}
					
					//left side of the image
					//------------------------
					else if ((x==0) && (y>b.yini)){
						
						hop0=result_YUV[pix-img.width];
						//hop0=img.boundaries_YUV[0][pix-img.width];
						last_small_hop=false;
						hop1=max_hop1;
					}
	                else if ((x==0) && (y==b.yini)){
						
						//hop0=result_YUV[pix-img.width];
						hop0=img.boundaries_YUV[0][pix-img.width];
						last_small_hop=false;
						hop1=max_hop1;
					}
					//left side of the block
					//------------------------
					else if ((x==b.xini) && (y>b.yini)){
						//System.out.print("zulu");
							hop0=(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7;
							//hop0=img.boundaries_YUV[0][pix-1];
							//System.out.println(" el boundary v tiene color:"+img.boundaries_YUV[0][pix-1]+"  at  x:"+(x-1)+" ,y:"+y);
					}
					//up-left corner of block
					//--------------
					else if ((x==b.xini) && (y==b.yini)){
						//System.out.print("zulu");
						hop0=(4*img.boundaries_YUV[0][pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;	
					}
					
					
					
					//right side of block (and right side of image. is the same case)
					//-----------------------------------------------------------------
					else if ((x==b.downsampled_xfin) && (y>b.yini)) {
						hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;	
						
						
						
					}
					
						
					//upper side of block
					//---------------------
				    else if (y==b.yini && x>0 && x!=b.downsampled_xfin) {
				    	hop0=(4*result_YUV[pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;	
				    	//hop0=(4*result_YUV[pix-1]+1*img.boundaries_YUV[0][pix+1-img.width])/5;
				    	//hop0=result_YUV[pix-1];
				    }
				    else if ((x==b.downsampled_xfin) && (y==b.yini)) {
						//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;
						hop0=(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix-img.width])/6;
						//if (result_YUV[pix-img.width]==0) System.out.println(" ey");
					}
				    	
					
					
					
					//hops computation. initial values for errors
					emin=256;//current minimum prediction error 
					int e2=0;//computed error for each hop 

					//hop0 is prediction
					//if (hop0>255)hop0=255;
					//else if (hop0<0) hop0=0; 


					// el array PCC habria que escogerlo en funcion de kini
					
					
					//positive hops computation
					//-------------------------
					if (oc-hop0>=0) 
					{
						for (int j=4;j<=8;j++) {
							try{
							e2=oc-pcck[kinint][hop1][hop0][j];
							}catch (Exception e){
								System.out.println("j:"+j+" hop1:"+hop1+"  hop0:"+hop0+"  x:"+x+"  y:"+y+"  b.xini:"+b.xini+" b.yini:"+b.yini+" b.downsampled_xfin:"+b.downsampled_xfin+"  up:"+result_YUV[pix-img.width]+" izq:"+result_YUV[pix-1]+ " debe ser:"+(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7+ "boundaries es:"+img.boundaries_YUV[0][pix-1]);
								System.exit(0);
							}
							if (e2<0) e2=-e2;
							if (e2<emin) {hop_number=j;emin=e2;}
							else break;
						}
					}
					//negative hops computation
					//-------------------------
					else 
					{
						//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
						//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
						for (int j=4;j>=0;j--) {
							e2=pcc[hop1][hop0][j]-oc;
							if (e2<0) e2=-e2;
							if (e2<emin) {hop_number=j;emin=e2;}
							else break;
						}
					}

					//assignment of final color value
					//--------------------------------
					result_YUV[pix]=pcck[kinint][hop1][hop0][hop_number];
					//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
					//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
					hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

					//tunning hop1 for the next hop
					//-------------------------------
					boolean small_hop=false;
					//if (hop_number>=6) small_hop=true;
					//if (hop_number<=6 && hop_number>=2) small_hop=true;
					if (hop_number<=5 && hop_number>=3) small_hop=true;
					else small_hop=false;     

					if( (small_hop) && (last_small_hop))  {
						hop1=hop1-1;
						if (hop1<min_hop1) hop1=min_hop1;
					} 
					else {
						hop1=max_hop1;
					}
					//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

					//lets go for the next pixel
					//--------------------------
					last_small_hop=small_hop;
					
					
					
					//pix++;            
				}//for x
				//esto es necesario al funcionar con bloques
				//pix+=img.width-b.lx_sc+1;
			}//for y

		}//end function
		//**************************************************************************************
		public void initGeom()
		{
			//old function, not valid
			
			//cache to avoid pow functions
			float[][][] cache_ratio; //[+/-][h1][yref]

			//hn[absolute h1 value][luminance of h0 value]
			float[][] h1,h2,h3,h4,h5,h6;
			
			h1=new float[9][256];
			h2=new float[9][256];
			h3=new float[9][256];
			h4=new float[9][256];
			h5=new float[9][256];
			h6=new float[9][256];

			pcc=new int[9][256][9];
			
			//cache of geometrical ratios ( to avoid Math.pow operation)
			//---------------------------------------------
			cache_ratio=new float[2][9][256];
			
			for (int yref=0;yref<=255;yref++) {
				for (int hop1=4;hop1<=8;hop1++)
				{
					float percent_range=0.8f;// 80%
					cache_ratio[0][(int)(hop1)][yref]=(float)Math.pow(percent_range*(255-yref)/(hop1), 1f/3f);
					cache_ratio[1][(int)(hop1)][yref]=(float)Math.pow(percent_range*(yref)/(hop1), 1f/3f);
					float max=2.5f;
					if (cache_ratio[0][(int)(hop1)][yref]>max)cache_ratio[0][hop1][yref]=max;
					if (cache_ratio[1][(int)(hop1)][yref]>max)cache_ratio[1][hop1][yref]=max;
					
				}
				//System.out.println("yref:"+yref);
				//assignment of precomputed hop values, for each ofp value
				//--------------------------------------------------------
				for (int hop1=4;hop1<=8;hop1++)
				{
				//	float kini=3.65f;
				//	kini=3.9f;
					//k value for initial LHE quantization. not valid for LHE applied on downsampled image
				//	double k=kini+(float)(ofp-4)*0.1275;//0.1275f;//0.13
//System.out.println("hop1:"+hop1+" yref"+yref);
					float ratio_pos=cache_ratio[0][hop1][yref];//cache_pow[8][0][yref][(int)(k*100)];//Math.pow((255-yref)/k, 1/k);
					float ratio_neg=cache_ratio[1][hop1][yref];//cache_pow[8][1][yref][(int)(k*100)];//Math.pow((yref)/k, 1/k);

					//possitive hops
					h1[hop1][yref] = hop1*ratio_pos;
					h2[hop1][yref] = h1[hop1][yref]*ratio_pos;
					h3[hop1][yref] = h2[hop1][yref]*ratio_pos;


					//negative hops	                        
					h4[hop1][yref] =hop1*ratio_neg;
					h5[hop1][yref] = h4[hop1][yref]*ratio_neg;
					h6[hop1][yref] = h5[hop1][yref]*ratio_neg;


					//final color component ( luminance or chrominance). depends on hop1
					//from most negative hop (fcc[ofp][yref][0]) to most possitive hop (fcc[ofp][yref][8])
					//--------------------------------------------------------------------------------------
					pcc[hop1][yref][0]= yref  - (int) h6[hop1][yref] ; if (pcc[hop1][yref][0]<=0) { pcc[hop1][yref][0]=1;}
					pcc[hop1][yref][1]= yref  - (int) h5[hop1][yref]; if (pcc[hop1][yref][1]<=0) {pcc[hop1][yref][1]=1;}
					pcc[hop1][yref][2]= yref  - (int) h4[hop1][yref]; if (pcc[hop1][yref][2]<=0) { pcc[hop1][yref][2]=1;}
					pcc[hop1][yref][3]=yref-hop1;if (pcc[hop1][yref][3]<=0) pcc[hop1][yref][3]=1;
					pcc[hop1][yref][4]=yref; if (pcc[hop1][yref][4]<=0) pcc[hop1][yref][4]=1; //null hop
					if (pcc[hop1][yref][4]>255) pcc[hop1][yref][4]=255;
					pcc[hop1][yref][5]= yref+hop1;if (pcc[hop1][yref][5]>255) pcc[hop1][yref][5]=255;
					pcc[hop1][yref][6]= yref  + (int) h1[hop1][yref]; if (pcc[hop1][yref][6]>255) {pcc[hop1][yref][6]=255;}
					pcc[hop1][yref][7]= yref  + (int) h2[hop1][yref]; if (pcc[hop1][yref][7]>255) {pcc[hop1][yref][7]=255;}
					pcc[hop1][yref][8]= yref  + (int) h3[hop1][yref]; if (pcc[hop1][yref][8]>255) {pcc[hop1][yref][8]=255;}
				}

			}
			//}
			/*
	System.out.println(" yref=128, hop1=8 k=3.65 hops:"+pcc[8][128][5]+","+pcc[8][128][6]+","+pcc[8][128][7]+","+pcc[8][128][8]);
	System.out.println(" yref=128, hop1=4 k=3.65 hops:"+pcc[4][128][5]+","+pcc[4][128][6]+","+pcc[4][128][7]+","+pcc[4][128][8]);
	System.out.println(" yref=0, hop1=8 k=3.65 hops  :"+pcc[8][0][5]+","+pcc[8][0][6]+","+pcc[8][0][7]+","+pcc[8][0][8]);
	System.out.println(" yref=0, hop1=4 k=3.65 hops  :"+pcc[4][0][5]+","+pcc[4][0][6]+","+pcc[4][0][7]+","+pcc[4][0][8]);
	System.exit(0);
	*/
		}

	//**********************************************************************************************************
				public void initGeom(float  rmax)
				{
					//old function. not valid
					
					
					//cache to avoid pow functions
					float[][][] cache_ratio; //[+/-][h1][yref]

					//hn[absolute h1 value][luminance of h0 value]
					float[][] h1,h2,h3,h4,h5,h6;
					
					h1=new float[9][256];
					h2=new float[9][256];
					h3=new float[9][256];
					h4=new float[9][256];
					h5=new float[9][256];
					h6=new float[9][256];

					pcc=new int[9][256][9];
					
					//cache of geometrical ratios ( to avoid Math.pow operation)
					//---------------------------------------------
					cache_ratio=new float[2][9][256];
					
					for (int yref=0;yref<=255;yref++) {
						for (int hop1=4;hop1<=8;hop1++)
						{
							float percent_range=0.8f;// 80%
							cache_ratio[0][(int)(hop1)][yref]=(float)Math.pow(percent_range*(255-yref)/(hop1), 1f/3f);
							cache_ratio[1][(int)(hop1)][yref]=(float)Math.pow(percent_range*(yref)/(hop1), 1f/3f);
							float max=rmax;//2.5f;
							if (cache_ratio[0][(int)(hop1)][yref]>max)cache_ratio[0][hop1][yref]=max;
							if (cache_ratio[1][(int)(hop1)][yref]>max)cache_ratio[1][hop1][yref]=max;
							
						}
						//System.out.println("yref:"+yref);
						//assignment of precomputed hop values, for each ofp value
						//--------------------------------------------------------
						for (int hop1=4;hop1<=8;hop1++)
						{
						//	float kini=3.65f;
						//	kini=3.9f;
							//k value for initial LHE quantization. not valid for LHE applied on downsampled image
						//	double k=kini+(float)(ofp-4)*0.1275;//0.1275f;//0.13
		//System.out.println("hop1:"+hop1+" yref"+yref);
							float ratio_pos=cache_ratio[0][hop1][yref];//cache_pow[8][0][yref][(int)(k*100)];//Math.pow((255-yref)/k, 1/k);
							float ratio_neg=cache_ratio[1][hop1][yref];//cache_pow[8][1][yref][(int)(k*100)];//Math.pow((yref)/k, 1/k);

							//possitive hops
							h1[hop1][yref] = hop1*ratio_pos;
							h2[hop1][yref] = h1[hop1][yref]*ratio_pos;
							h3[hop1][yref] = h2[hop1][yref]*ratio_pos;


							//negative hops	                        
							h4[hop1][yref] =hop1*ratio_neg;
							h5[hop1][yref] = h4[hop1][yref]*ratio_neg;
							h6[hop1][yref] = h5[hop1][yref]*ratio_neg;


							//final color component ( luminance or chrominance). depends on hop1
							//from most negative hop (fcc[ofp][yref][0]) to most possitive hop (fcc[ofp][yref][8])
							//--------------------------------------------------------------------------------------
							pcc[hop1][yref][0]= yref  - (int) h6[hop1][yref] ; if (pcc[hop1][yref][0]<=0) { pcc[hop1][yref][0]=1;}
							pcc[hop1][yref][1]= yref  - (int) h5[hop1][yref]; if (pcc[hop1][yref][1]<=0) {pcc[hop1][yref][1]=1;}
							pcc[hop1][yref][2]= yref  - (int) h4[hop1][yref]; if (pcc[hop1][yref][2]<=0) { pcc[hop1][yref][2]=1;}
							pcc[hop1][yref][3]=yref-hop1;if (pcc[hop1][yref][3]<=0) pcc[hop1][yref][3]=1;
							pcc[hop1][yref][4]=yref; if (pcc[hop1][yref][4]<=0) pcc[hop1][yref][4]=1; //null hop
							if (pcc[hop1][yref][4]>255) pcc[hop1][yref][4]=255;
							pcc[hop1][yref][5]= yref+hop1;if (pcc[hop1][yref][5]>255) pcc[hop1][yref][5]=255;
							pcc[hop1][yref][6]= yref  + (int) h1[hop1][yref]; if (pcc[hop1][yref][6]>255) {pcc[hop1][yref][6]=255;}
							pcc[hop1][yref][7]= yref  + (int) h2[hop1][yref]; if (pcc[hop1][yref][7]>255) {pcc[hop1][yref][7]=255;}
							pcc[hop1][yref][8]= yref  + (int) h3[hop1][yref]; if (pcc[hop1][yref][8]>255) {pcc[hop1][yref][8]=255;}
						}

					}
					//}
					/*
			System.out.println(" yref=128, hop1=8 k=3.65 hops:"+pcc[8][128][5]+","+pcc[8][128][6]+","+pcc[8][128][7]+","+pcc[8][128][8]);
			System.out.println(" yref=128, hop1=4 k=3.65 hops:"+pcc[4][128][5]+","+pcc[4][128][6]+","+pcc[4][128][7]+","+pcc[4][128][8]);
			System.out.println(" yref=0, hop1=8 k=3.65 hops  :"+pcc[8][0][5]+","+pcc[8][0][6]+","+pcc[8][0][7]+","+pcc[8][0][8]);
			System.out.println(" yref=0, hop1=4 k=3.65 hops  :"+pcc[4][0][5]+","+pcc[4][0][6]+","+pcc[4][0][7]+","+pcc[4][0][8]);
			System.exit(0);
			*/
				}
//****************************************************************************				
				public void initGeomR()
				{
					//This is the function to initialize hop values
					
					System.out.println(" INICIALIZANDO CUANTIZADOR LHE");
					//cache to avoid pow functions
					float[][][][] cache_ratio; //[+/-][h1][h0][rmax]

					//hn[absolute h1 value][luminance of h0 value]
					float[][] h1,h2,h3,h4,h5,h6;
					int h1range=20;
					
					h1=new float[h1range][256];
					h2=new float[h1range][256];
					h3=new float[h1range][256];
					h4=new float[h1range][256];
					h5=new float[h1range][256];
					h6=new float[h1range][256];

					pccr=new int[h1range][256][50][9];
					
					//cache of geometrical ratios ( to avoid Math.pow operation)
					//---------------------------------------------
					cache_ratio=new float[2][h1range][256][50];
					
					for (int hop0=0;hop0<=255;hop0++) {
						for (int hop1=1;hop1<h1range;hop1++)
						{
							float percent_range=0.8f;//0.9f;//0.8f;// 80%
							
							for (int rmax=20;rmax<=40;rmax++)
							{
							cache_ratio[0][(int)(hop1)][hop0][rmax]=(float)Math.pow(percent_range*(255-hop0)/(hop1), 1f/3f);
							cache_ratio[1][(int)(hop1)][hop0][rmax]=(float)Math.pow(percent_range*(hop0)/(hop1), 1f/3f);
							
							//cache_ratio[0][(int)(hop1)][hop0][rmax]=(float)Math.pow(percent_range*(255-hop0)/(hop1), 1f/2f);
							//cache_ratio[1][(int)(hop1)][hop0][rmax]=(float)Math.pow(percent_range*(hop0)/(hop1), 1f/2f);
							
							
							float max=(float)rmax/10f;//2.5f;
							if (cache_ratio[0][(int)(hop1)][hop0][rmax]>max)cache_ratio[0][hop1][hop0][rmax]=max;
							if (cache_ratio[1][(int)(hop1)][hop0][rmax]>max)cache_ratio[1][hop1][hop0][rmax]=max;
							
							
							
							
							float min=1.0f;//esto sobra
							if (cache_ratio[0][(int)(hop1)][hop0][rmax]<min)cache_ratio[0][hop1][hop0][rmax]=min;
							if (cache_ratio[1][(int)(hop1)][hop0][rmax]<min)cache_ratio[1][hop1][hop0][rmax]=min;
							
							
							
							}
						}
						//System.out.println("hop0:"+hop0);
						//assignment of precomputed hop values, for each ofp value
						//--------------------------------------------------------
						for (int hop1=1;hop1<h1range;hop1++)
						{
							for (int rmax=20;rmax<=40;rmax++)
							{
						//	float kini=3.65f;
						//	kini=3.9f;
							//k value for initial LHE quantization. not valid for LHE applied on downsampled image
						//	double k=kini+(float)(ofp-4)*0.1275;//0.1275f;//0.13
		//System.out.println("hop1:"+hop1+" hop0"+hop0);
							float ratio_pos=cache_ratio[0][hop1][hop0][rmax];//cache_pow[8][0][hop0][(int)(k*100)];//Math.pow((255-hop0)/k, 1/k);
							float ratio_neg=cache_ratio[1][hop1][hop0][rmax];//cache_pow[8][1][hop0][(int)(k*100)];//Math.pow((hop0)/k, 1/k);

							//System.out.println("h0="+hop0+"      r+ ="+ratio_pos+"   r- ="+ratio_neg);
							//possitive hops
							h1[hop1][hop0] = hop1*ratio_pos;
							h2[hop1][hop0] = h1[hop1][hop0]*ratio_pos;
							h3[hop1][hop0] = h2[hop1][hop0]*ratio_pos;


							//negative hops	                        
							h4[hop1][hop0] =hop1*ratio_neg;
							h5[hop1][hop0] = h4[hop1][hop0]*ratio_neg;
							h6[hop1][hop0] = h5[hop1][hop0]*ratio_neg;

							//System.out.println("h0="+hop0+"  h1+="+hop1+" h2+="+h1[hop1][hop0]+" h3+="+h2[hop1][hop0]+" h4+="+h3[hop1][hop0]+"      r+ ="+ratio_pos+"   r- ="+ratio_neg);
							//System.out.println("h0="+hop0+"  h1-="+hop1+" h2+="+h4[hop1][hop0]+" h3-="+h5[hop1][hop0]+" h4-="+h6[hop1][hop0]+"      r+ ="+ratio_pos+"   r- ="+ratio_neg);
							//System.out.println("");
							//final color component ( luminance or chrominance). depends on hop1
							//from most negative hop (fcc[ofp][hop0][0]) to most possitive hop (fcc[ofp][hop0][8])
							//--------------------------------------------------------------------------------------
							
							
							
							pccr[hop1][hop0][rmax][0]= hop0  - (int) h6[hop1][hop0] ; if (pccr[hop1][hop0][rmax][0]<=0) { pccr[hop1][hop0][rmax][0]=1;}
							pccr[hop1][hop0][rmax][1]= hop0  - (int) h5[hop1][hop0]; if (pccr[hop1][hop0][rmax][1]<=0) {pccr[hop1][hop0][rmax][1]=1;}
							pccr[hop1][hop0][rmax][2]= hop0  - (int) h4[hop1][hop0]; if (pccr[hop1][hop0][rmax][2]<=0) { pccr[hop1][hop0][rmax][2]=1;}
							pccr[hop1][hop0][rmax][3]=hop0-hop1;if (pccr[hop1][hop0][rmax][3]<=0) pccr[hop1][hop0][rmax][3]=1;
							pccr[hop1][hop0][rmax][4]=hop0; 
							if (pccr[hop1][hop0][rmax][4]<=0) pccr[hop1][hop0][rmax][4]=1; //null hop
							if (pccr[hop1][hop0][rmax][4]>255) pccr[hop1][hop0][rmax][4]=255;//null hop
							pccr[hop1][hop0][rmax][5]= hop0+hop1;if (pccr[hop1][hop0][rmax][5]>255) pccr[hop1][hop0][rmax][5]=255;
							pccr[hop1][hop0][rmax][6]= hop0  + (int) h1[hop1][hop0]; if (pccr[hop1][hop0][rmax][6]>255) {pccr[hop1][hop0][rmax][6]=255;}
							pccr[hop1][hop0][rmax][7]= hop0  + (int) h2[hop1][hop0]; if (pccr[hop1][hop0][rmax][7]>255) {pccr[hop1][hop0][rmax][7]=255;}
							pccr[hop1][hop0][rmax][8]= hop0  + (int) h3[hop1][hop0]; if (pccr[hop1][hop0][rmax][8]>255) {pccr[hop1][hop0][rmax][8]=255;}
							
							
							/*
							pccr[hop1][hop0][rmax][0]= (int)(0.5f+(float)hop0  -  h6[hop1][hop0]) ; if (pccr[hop1][hop0][rmax][0]<=0) { pccr[hop1][hop0][rmax][0]=1;}
							pccr[hop1][hop0][rmax][1]= (int)(0.5f+(float)hop0  -  h5[hop1][hop0]); if (pccr[hop1][hop0][rmax][1]<=0) {pccr[hop1][hop0][rmax][1]=1;}
							pccr[hop1][hop0][rmax][2]= (int)(0.5f+(float)hop0  -  h4[hop1][hop0]); if (pccr[hop1][hop0][rmax][2]<=0) { pccr[hop1][hop0][rmax][2]=1;}
							pccr[hop1][hop0][rmax][3]=hop0-hop1;if (pccr[hop1][hop0][rmax][3]<=0) pccr[hop1][hop0][rmax][3]=1;
							pccr[hop1][hop0][rmax][4]=hop0; 
							if (pccr[hop1][hop0][rmax][4]<=0) pccr[hop1][hop0][rmax][4]=1; //null hop
							if (pccr[hop1][hop0][rmax][4]>255) pccr[hop1][hop0][rmax][4]=255;//null hop
							pccr[hop1][hop0][rmax][5]= hop0+hop1;if (pccr[hop1][hop0][rmax][5]>255) pccr[hop1][hop0][rmax][5]=255;
							pccr[hop1][hop0][rmax][6]= (int)(0.5f+(float)hop0  + h1[hop1][hop0]); if (pccr[hop1][hop0][rmax][6]>255) {pccr[hop1][hop0][rmax][6]=255;}
							pccr[hop1][hop0][rmax][7]= (int)(0.5f+(float)hop0  + h2[hop1][hop0]); if (pccr[hop1][hop0][rmax][7]>255) {pccr[hop1][hop0][rmax][7]=255;}
							pccr[hop1][hop0][rmax][8]= (int)(0.5f+(float)hop0  +  h3[hop1][hop0]); if (pccr[hop1][hop0][rmax][8]>255) {pccr[hop1][hop0][rmax][8]=255;}
							*/
							
							
							}//rmax
							}//hop1

					}//hop0
					//}
					/*
			System.out.println(" hop0=128, hop1=8 k=3.65 hops:"+pcc[8][128][5]+","+pcc[8][128][6]+","+pcc[8][128][7]+","+pcc[8][128][8]);
			System.out.println(" hop0=128, hop1=4 k=3.65 hops:"+pcc[4][128][5]+","+pcc[4][128][6]+","+pcc[4][128][7]+","+pcc[4][128][8]);
			System.out.println(" hop0=0, hop1=8 k=3.65 hops  :"+pcc[8][0][5]+","+pcc[8][0][6]+","+pcc[8][0][7]+","+pcc[8][0][8]);
			System.out.println(" hop0=0, hop1=4 k=3.65 hops  :"+pcc[4][0][5]+","+pcc[4][0][6]+","+pcc[4][0][7]+","+pcc[4][0][8]);
			System.exit(0);
			*/
				}
				//**************************************************************************************************
				/**
				 * 
				 * 	 downsampled image luminance array is the input for this function. 
				 *   This downsampled luminance array is suposed to be stored at img.downsampled_YUV[0][pix]; 
				 *   downsampled Image luminance array is not modified
				 * 
				 *   ahother input for this function is img.boundaries_YUV[0][pix];
				 * 
				 * 
				 * @param b
				 * @param hops        : OUTPUT
				 * @param result_YUV   : OUTPUT
				 * 
				 */
				public void quantizeDownsampledBlock_R_NORMAL(Block b, int[] hops,int[] result_YUV, int[] src_YUV, int[] boundaries_YUV)
				{

					
					
					//block b contains the coordinates to set the limits of this function
					
					
					//some parts of this code are identical to the function quantizeOneHopPerPixel() 
					
					
					int max_hop1=8;// hop1 interval 4..8
					int min_hop1=4;// 
					int hop1=max_hop1;
					int hop0=0; // predicted signal
					int emin;//error of predicted signal
					int hop_number=4;//selected hop // 4 is NULL HOP
					int oc=0;// original color
					
					
					int rmax=20;
					if (b.PRavg==1.0) rmax=30;//bordes abruptos, iconos, ruido 
					//else if (b.PRavg>=0.625) rmax=25;//bordes 1,1,1,0.5
					else if (b.PRavg>=0.75) rmax=25;//bordes 1,1,1,0.5
					//else if (b.PRavg>=0.625) rmax=20;//bordes 1,1,1,0.5
					//rmax=20;
					
					
					int pix=b.yini*img.width+b.xini;//initial pixel possition        
					
					boolean last_small_hop=false;// indicates if last hop is small
					for (int y=b.yini;y<=b.downsampled_yfin;y++)  {
						
						//ponemos a hop minimo al comienzo de scan line de bloque
						//esto es bueno sobre todo en bloques lisos
						//hop1=min_hop1;//NUEVO 10/09/2014
						
						for (int x=b.xini;x<=b.downsampled_xfin;x++)  {

							pix=y*img.width+x;
							oc=src_YUV[pix];

							//prediction of signal (hop0) , based on pixel's coordinates 
							//----------------------------------------------------------
							
							//inner pixels ( mostly of them. that is the reason for considering the 1st option)
							//---------------------------------------------------------------------------------
							 if ((y>b.yini) &&(x>b.xini) && x!=b.downsampled_xfin){
								hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	

								//	System.out.println(" result_YUV[pix-1]:"+result_YUV[pix-1]+"  result_YUV[pix+1-img.width]: "+result_YUV[pix+1-img.width]);
							}
							//initial pix
							//-----------
							if (x==0 && y==0) {  
								hop0=oc;//first pixel always is perfectly predicted! :-)  
							}	
							//upper side of the image
						    //-----------------------
							else if (y==0 && x>b.xini) {
								hop0=result_YUV[x-1];
							}
							else if (y==0 && x==b.xini) {
								hop0=boundaries_YUV[x-1];
							}
							
							//left side of the image
							//------------------------
							else if ((x==0) && (y>b.yini)){
								
								hop0=result_YUV[pix-img.width];
								//hop0=img.boundaries_YUV[0][pix-img.width];
								last_small_hop=false;
								
								//comento esto 12/09/2014
								hop1=max_hop1;
							}
			                else if ((x==0) && (y==b.yini)){
								
								//hop0=result_YUV[pix-img.width];
								hop0=img.boundaries_YUV[0][pix-img.width];
								last_small_hop=false;
								
								//comento esto 12/09/2014
								hop1=max_hop1;
								
							}
							//left side of the block
							//------------------------
							else if ((x==b.xini) && (y>b.yini)){
								//System.out.print("zulu");
									hop0=(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7;
									//hop0=img.boundaries_YUV[0][pix-1];
									//hop0=(4*img.boundaries_YUV[0][pix-1]+4*result_YUV[pix+1-img.width])/8;
									//hop0=result_YUV[pix+1-img.width];//oc;//img.boundaries_YUV[0][pix-1];
									//System.out.println(" el boundary v tiene color:"+img.boundaries_YUV[0][pix-1]+"  at  x:"+(x-1)+" ,y:"+y);
									//hop0=oc;
							}
							//up-left corner of block
							//--------------
							else if ((x==b.xini) && (y==b.yini)){
								//System.out.print("zulu");
								hop0=(4*img.boundaries_YUV[0][pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;	
							}
							
							
							
							//right side of block (and right side of image. is the same case)
							//-----------------------------------------------------------------
							else if ((x==b.downsampled_xfin) && (y>b.yini)) {
								hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;	
								
								
								//hop0=(4*result_YUV[pix-1]+0*result_YUV[pix-img.width])/4;
							}
							
								
							//upper side of block
							//---------------------
						    else if (y==b.yini && x>0 && x!=b.downsampled_xfin) {
						    	hop0=(4*result_YUV[pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;	
						    	//hop0=oc;//(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix+1-img.width])/6;
						    	//hop0=(4*result_YUV[pix-1]+1*img.boundaries_YUV[0][pix+1-img.width])/5;
						    	//hop0=result_YUV[pix-1];
						    	
						    	//hop0=oc;
						    }
						    else if ((x==b.downsampled_xfin) && (y==b.yini)) {
								//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;
								hop0=(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix-img.width])/6;
								//if (result_YUV[pix-img.width]==0) System.out.println(" ey");
							}
						    	
							//hop0=oc;
							
							
							//hops computation. initial values for errors
							emin=256;//current minimum prediction error 
							int e2=0;//computed error for each hop 

							//hop0 is prediction
							//if (hop0>255)hop0=255;
							//else if (hop0<0) hop0=0; 


							// el array PCC habria que escogerlo en funcion de kini
							
							
							//positive hops computation
							//-------------------------
							if (oc-hop0>=0) 
							{
								for (int j=4;j<=8;j++) {
									try{
									e2=oc-pccr[hop1][hop0][rmax][j];
									}catch (Exception e){
										System.out.println("j:"+j+" hop1:"+hop1+"  hop0:"+hop0+"  x:"+x+"  y:"+y+"  b.xini:"+b.xini+" b.yini:"+b.yini+" b.downsampled_xfin:"+b.downsampled_xfin+"  up:"+result_YUV[pix-img.width]+" izq:"+result_YUV[pix-1]+ " debe ser:"+(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7+ "boundaries es:"+img.boundaries_YUV[0][pix-1]);
										System.exit(0);
									}
									if (e2<0) e2=-e2;
									if (e2<emin) {hop_number=j;emin=e2;}
									else break;
								}
							}
							//negative hops computation
							//-------------------------
							else 
							{
								//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
								//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
								for (int j=4;j>=0;j--) {
									e2=pccr[hop1][hop0][rmax][j]-oc;
									if (e2<0) e2=-e2;
									if (e2<emin) {hop_number=j;emin=e2;}
									else break;
								}
							}

							//assignment of final color value
							//--------------------------------
							result_YUV[pix]=pccr[hop1][hop0][rmax][hop_number];
							//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
							//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
							hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

							//tunning hop1 for the next hop
							//-------------------------------
							boolean small_hop=false;
							//if (hop_number>=6) small_hop=true;
							//if (hop_number<=6 && hop_number>=2) small_hop=true;
							if (hop_number<=5 && hop_number>=3) small_hop=true;
							else small_hop=false;     

							if( (small_hop) && (last_small_hop))  {
								hop1=hop1-1;
								if (hop1<min_hop1) hop1=min_hop1;
							} 
							else {
								hop1=max_hop1;
							}
							//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

							//lets go for the next pixel
							//--------------------------
							last_small_hop=small_hop;
							
							
							
							//pix++;            
						}//for x
						//esto es necesario al funcionar con bloques
						//pix+=img.width-b.lx_sc+1;
					}//for y

				}//end function
				//**************************************************************************************
				//**************************************************************************************************
				/**
				 * This is a very fast LHE quantization function, used for initial quantization in order to 
				 * perform Perceptual Relevance Metrics.
				 * Later quantization over downwsampled image allows more tunning ( k value) and therefore 
				 * require more complex calculation (but over a reduced image)
				 * 
				 * image luminance array is the input for this function. 
				 *   This luminance array is suposed to be stored at img.YUV[0][pix]; 
				 *   Image luminance array is not modified
				 * 
				 * hops numbering:
				 *   >negative hops: 0,1,2,3
				 *   >null hop: 4
				 *   >positive hops: 5,6,7,8
				 * 
				 * result_YUV is output array. it can not be removed, because these luminance & chroma values are part of
				 *   algorithm to choose next hop
				 * 
				 * 
				 * @param hops
				 * @param result_YUV
				 */
				public void quantizeOneHopPerPixel_initial(int[] hops,int[] result_YUV)
				{
					System.out.println("quantizying...");
					/*
					int iterations=1000;
					long start_time = System.currentTimeMillis();
					for (int xy=0;xy<iterations;xy++){
						*/
					//ESTA ES LA FUNCION BUENA
					//img.width=1920;
					//img.height=1080;

					//img.width=1280;
					//img.height=720;

					//img.width=720;
					//img.height=576;
					//img.width=429;
					//img.height=429;
					//img.width=576;
					//img.height=256;
					int max_hop1=8;//10;//10;//8;//8;//16;//8;// hop1 interval 4..8
					int min_hop1=4;//4;//4;// 
					int start_hop1=(max_hop1+min_hop1)/2;
					int rmax=25;//40;
					//rmax=27;//27;
					//hop1=8;
					
					
					int hop1=start_hop1;//max_hop1;
					int hop0=0; // predicted signal
					int emin;//error of predicted signal
					int hop_number=4;//selected hop // 4 is NULL HOP
					int oc=0;// original color
					int pix=0;//pixel possition, from 0 to image size        
					boolean last_small_hop=false;// indicates if last hop is small

					
					
					float error_center=0;
					float error_avg=0;
					
					for (int y=0;y<img.height;y++)  {
						for (int x=0;x<img.width;x++)  {

							oc=img.YUV[0][pix];

							//prediction of signal (hop0) , based on pixel's coordinates 
							//----------------------------------------------------------
							if ((y>0) &&(x>0) && x!=img.width-1){
								hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	
								//hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2;
								//hop0=result_YUV[pix+1-img.width];
								//	System.out.println(" result_YUV[pix-1]:"+result_YUV[pix-1]+"  result_YUV[pix+1-img.width]: "+result_YUV[pix+1-img.width]);
							}
							else if ((x==0) && (y>0)){
								hop0=result_YUV[pix-img.width];
								
								
								last_small_hop=false;
								
								
								//hop1=max_hop1;
								hop1=start_hop1;
							}
							else if ((x==img.width-1) && (y>0)) {
								hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
							}else if (y==0 && x>0) {
								hop0=result_YUV[x-1];
							}else if (x==0 && y==0) {  
								hop0=oc;//first pixel always is perfectly predicted! :-)  
							}			

							
							//paeth
							/*
							int A=0;
							int B=0;
							int C=0;
							if (x>0) A=result_YUV[pix-1];
							if (y>0) B=result_YUV[pix-img.width];
							if (x>0 && y>0) C=result_YUV[pix-img.width-1];
							if (x==0) {A=B;C=B;}
							if (y==0) {B=A;C=A;}
							if (x==0 && y==0)hop0=oc;
							else
							{
								int pred=A+B-C;
								int a1=Math.abs(A-pred);
								int b1=Math.abs(B-pred);
								int c1=Math.abs(C-pred);
								if (a1<=b1 && a1<=c1) hop0=A;
								else if (b1<=a1 && b1<=c1) hop0=B;
								else hop0=C;
								//System.out.println("paeth");
							}
							*/
							
							

							//hops computation. initial values for errors
							emin=256;//current minimum prediction error 
							int e2=0;//computed error for each hop 

							//hop0 is prediction
							//if (hop0>255)hop0=255;
							//else if (hop0<0) hop0=0; 

							//max_hop1=16;//(int)((float)hop0 *0.02f +0.5f);
							//System.out.println("hop1max:"+max_hop1);
							//positive hops computation
							//-------------------------
							
							
							//min_hop1=(int)(0.5f+(float)hop0*0.04f);//no puede ser cero
							//if (min_hop1<4) min_hop1=4;
							//if (hop1<min_hop1) hop1=min_hop1;
							
							if (oc-hop0>=0) 
							{
								for (int j=4;j<=8;j++) {
								//for (int j=4;j<=5;j++) {
									//if (j==4) rmax=20;
									//if (j==5) rmax=20;
									//if (j==6) rmax=25;
									//if (j==7) rmax=28;
									//if (j==8) rmax=30;
									
									e2=oc-pccr[hop1][hop0][rmax][j];
									if (e2<0) e2=-e2;
									if (e2<emin) {hop_number=j;emin=e2;
									              //if (e2<min_hop1) break;
												//	if (e2<4) break;
									              }
									else break;
								}
							}
							//negative hops computation
							//-------------------------
							else 
							{
								//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
								//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
								for (int j=4;j>=0;j--) {
								//	for (int j=4;j>=3;j--) {
									//if (j==4) rmax=20;
								    //if (j==3) rmax=20;
							        //if (j==2) rmax=25;
									//if (j==1) rmax=28;
									//if (j==0) rmax=30;
									
									e2=pccr[hop1][hop0][rmax][j]-oc;
									if (e2<0) e2=-e2;
									if (e2<emin) {hop_number=j;emin=e2;
									            //if (e2<min_hop1) break;
												//if (e2<4) break;
									            }
									else break;
								}
							}
//System.out.println("hola");
							//29/12/2014
							
							
							//colin
							
							//rmax=25;
							//rmax=24;
							int hop0i=pccr[hop1][hop0][rmax][4];
							int[] colin= new int[9];
							colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
							colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
							colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
							colin[3]=pccr[hop1][hop0i][rmax][3];
							colin[5]=pccr[hop1][hop0i][rmax][5];
						
							int startcolin=6;
							int endcolin=3;
							
							for (int j=startcolin; j<8;j++)
								{colin[j]=(int)(+1f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
								}
								
							for (int j=1; j<endcolin;j++)
							{	colin[j]=(int)(-0.5f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
						    }
							
							
							
							
							
							
						
							
							//assignment of final color value
							//--------------------------------
							//result_YUV[pix]=pccr[hop1][hop0][25][hop_number];
							
							result_YUV[pix]=pccr[hop1][hop0][rmax][hop_number];
							//result_YUV[pix]=colin[hop_number];//pccr[hop1][hop0][25][hop_number];
							
							//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
							//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
							hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

							
							//calculo de errores medios
							//---------------------------
							error_center+=(oc-result_YUV[pix]);
							error_avg+=Math.abs((oc-result_YUV[pix]));
							
							//tunning hop1 for the next hop
							//-------------------------------
							boolean small_hop=false;
							//if (hop_number>=6) small_hop=true;
							//if (hop_number<=6 && hop_number>=2) small_hop=true;
							if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
							else small_hop=false;     

							if( (small_hop) && (last_small_hop))  {
								hop1=hop1-1;
								if (hop1<min_hop1) hop1=min_hop1;
							} 
							else {
								hop1=max_hop1;
							}
							
							//hop1=6;
							
							//hop1=8;
							//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

							//lets go for the next pixel
							//--------------------------
							last_small_hop=small_hop;
							pix++;            
						}//for x
					}//for y
					
					/*
					}//iterations
					
					long end_time = System.currentTimeMillis();
					double total_time=end_time-start_time;
					double tpp=total_time/(img.width*img.height*iterations);
					double tpi=total_time/(iterations);
					System.out.println("tiempo_total:"+total_time+"  tpp:"+tpp+" ms"+ " tpi:"+tpi +" ms");
					*/
					System.out.println("quantization done");
					
					System.out.println("center of  error:"+error_center/(img.width*img.height));
					System.out.println("average of  error:"+error_avg/(img.width*img.height));
					System.out.println("----------------------------------------------------------");
					
					
					
				}//end function
				
				
				//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
				public void quantizeDownsampledBlock_R(Block b, int[] hops,int[] result_YUV, int[] src_YUV,int[] boundaries_YUV)
				{

					
					
					//block b contains the coordinates to set the limits of this function
					
					
					//some parts of this code are identical to the function quantizeOneHopPerPixel() 
					
					
					int max_hop1=8;// hop1 interval 4..8
					int min_hop1=4;//4;//
					
					//queda mejor 6 que el punto medio (en peppers)
					int start_hop1=(max_hop1+min_hop1)/2;// hop1 at begining (up-left corner) of each block
					
					int hop1=start_hop1;
					
					int hop0=0; // predicted signal
					int emin;//error of predicted signal
					int hop_number=4;//selected hop // 4 is NULL HOP
					int oc=0;// original color
					
					
					int rmax=20;
					//se escoge el juego de hops segun el PR cuantizado
					
					if (b.PRavg==1.0) rmax=30;//bordes abruptos, iconos, ruido 
					//else if (b.PRavg>=0.625) rmax=25;//bordes 1,1,1,0.5
					else if (b.PRavg>=0.75) rmax=25;//bordes 1,1,1,0.5
					else if (b.PRavg>=0.5) rmax=25;//bordes 1,1,1,0.5 NUEVO 2/01/2015
					//else if (b.PRavg>=0.625) rmax=20;//bordes 1,1,1,0.5
					
					//MEJORA 28/12/2014. alteramos h1max segun la PR y ademas el menor valor es 10 y no 8
					if (b.PRavg==1.0) {max_hop1=16;}//16;
					else if (b.PRavg>=0.75) {max_hop1=14;}//12;
					else if (b.PRavg>=0.5) {max_hop1=12;}
					else if (b.PRavg>=0.25) max_hop1=10;
					//
					//else if (b.PRavg>=0.125) max_hop1=10;
					else max_hop1=8;// suave
					
					
					//min_hop1=4;
					
					//PARA HACER PRUEBAS CON LHE BASICO SE PUEDE:
					//PONER GRID A UN SOLO BLOQUE.
					//HACER ESTAS IGUALDADES:
					//max_hop1=8;
					//rmax=25; //si pongo 20, lena gana 43 db
					
					
					
					//System.out.println("PRavg:"+b.PRavg);
					//rmax=20;//valor original
					//max_hop1=19;
					//start_hop1=6;
					
					//System.out.println(" dato:"+img.width);
					int pix=b.yini*img.width+b.xini;//initial pixel possition        
					
					
					
					boolean last_small_hop=false;// indicates if last hop is small
					for (int y=b.yini;y<=b.downsampled_yfin;y++)  {
						
						//ponemos a hop minimo al comienzo de scan line de bloque
						//esto es bueno sobre todo en bloques lisos
						
						//debo de usar un array mejor
						//hop1=min_hop1;//NUEVO 10/09/2014
						
						for (int x=b.xini;x<=b.downsampled_xfin;x++)  {

							pix=y*img.width+x;
							oc=src_YUV[pix];

							//prediction of signal (hop0) , based on pixel's coordinates 
							//----------------------------------------------------------
							
							//inner pixels ( mostly of them. that is the reason for considering the 1st option)
							//---------------------------------------------------------------------------------
							 if ((y>b.yini) &&(x>b.xini) && x!=b.downsampled_xfin){
								hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	
								
								//hop0=(4*result_YUV[pix-1]+4*result_YUV[pix-img.width])/8;	
								
								//hop0=(241*result_YUV[pix-1]+170*result_YUV[pix+1-img.width])/411;	
								//	System.out.println(" result_YUV[pix-1]:"+result_YUV[pix-1]+"  result_YUV[pix+1-img.width]: "+result_YUV[pix+1-img.width]);
							}
							//initial pix
							//-----------
							 else if (x==0 && y==0) {  
								hop0=oc;//first pixel always is perfectly predicted! :-)  
							}	
							//upper side of the image. not include corner
						    //--------------------------------------------
							else if (y==0 && x>b.xini) {
								hop0=result_YUV[pix-1];
								
							}
							
							//corners at upper side of image
							//-------------------------------
							else if (y==0 && x==b.xini) {
								hop0=boundaries_YUV[pix-1];
								
								
								last_small_hop=false;
								hop1=start_hop1;
								
								
							}
							
							//left side of the image. not include corner
							//-----------------------------------------
							else if ((x==0) && (y>b.yini)){
								
								hop0=result_YUV[pix-img.width];
								//hop0=img.boundaries_YUV[0][pix-img.width];
							
								//esta variable puede variar es mejor no ponerla a false
								last_small_hop=false;//31/12/2014 se descomenta
								//last_small_hop=true;//porque no
								//comento esto 12/09/2014
								hop1=start_hop1;//31/12/2014 se descomenta
							}
							//corners of left side of the image.
							//----------------------------------------- 
			                else if ((x==0) && (y==b.yini)){
								
			                	//System.out.println("NUNCA ENTRA");
								//hop0=result_YUV[pix-img.width];
								hop0=img.boundaries_YUV[0][pix-img.width];
								
								//hop0=(3*img.boundaries_YUV[0][pix-img.width]+4*img.boundaries_YUV[0][pix-img.width+1])/7;
								
								
								last_small_hop=false;
								//last_small_hop=true;//porque no
								
								//comento esto 12/09/2014
								hop1=start_hop1;
								
								
								//hop0=oc;
								
							}
							//left side of the block. not include corner
							//--------------------------------------
							else if ((x==b.xini) && (y>b.yini)){
								//System.out.print("zulu");
									hop0=(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7;
									//hop0=img.boundaries_YUV[0][pix-1];
									//hop0=(4*img.boundaries_YUV[0][pix-1]+4*result_YUV[pix+1-img.width])/8;
									//hop0=result_YUV[pix+1-img.width];//oc;//img.boundaries_YUV[0][pix-1];
									//System.out.println(" el boundary v tiene color:"+img.boundaries_YUV[0][pix-1]+"  at  x:"+(x-1)+" ,y:"+y);
									//hop0=oc;
									
									//esto es nuevo 12/09/2014
									last_small_hop=false;//31/12/2014 se descomenta
									//last_small_hop=true;//porque no
									
									//hop1=max_hop1;
									hop1=start_hop1;//31/12/2014 se descomenta
									//hop0=oc;
									
									
							}
							//up-left corner of block
							//--------------
							else if ((x==b.xini) && (y==b.yini)){
								//System.out.print("zulu");
								hop0=(4*img.boundaries_YUV[0][pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;
								
								//esto es nuevo 12/09/2014
								last_small_hop=false;
								//last_small_hop=true;//porque no
								
								hop1=start_hop1;
								//hop1=8;//start_hop1;
								//hop0=255;//oc;
								//System.out.println("hop0 es "+hop0);
								
								//hop0=oc;
							}
							
							
							
							//right side of block (and right side of image. is the same case). Not includes corner
							//-----------------------------------------------------------------------------------
							else if ((x==b.downsampled_xfin) && (y>b.yini)) {
								//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;	
								
								//nueva formula 03/12/2015
								hop0=(result_YUV[pix-1]+result_YUV[pix-img.width])/2;	
								
								
								//hop0=(4*result_YUV[pix-1]+0*result_YUV[pix-img.width])/4;
							}
							
								
							//upper side of block. not includes right corner
							//-----------------------------------------
						    else if (y==b.yini && x>0 && x!=b.downsampled_xfin) {
						    	hop0=(4*result_YUV[pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;	
						    	//hop0=(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix+1-img.width])/6;
						    	//hop0=(4*result_YUV[pix-1]+1*img.boundaries_YUV[0][pix+1-img.width])/5;
						    	//hop0=result_YUV[pix-1];
						    	
						    	//hop0=oc;
						    }
							//up-right CORNER of block
							//-----------------------------------------------------
						    else if ((x==b.downsampled_xfin) && (y==b.yini)) {
						    	
						    	
						    	
								//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;
								hop0=(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix-img.width])/6;
								
								//aqui no se pueden usar los boundaries porque no estan escalados para este bloque
								//hop0=(4*result_YUV[pix-1]+3*img.boundaries_YUV[0][pix-img.width+1])/7;
								//if (result_YUV[pix-img.width]==0) System.out.println(" ey");
							}
						    	
							//hop0=oc;
							 /*
							 if ((x==b.xini) && (y==b.yini)){
								// hop0=oc;	 
							 }
							 */
							
							//hops computation. initial values for errors
							emin=256;//current minimum prediction error 
							int e2=0;//computed error for each hop 

							//hop0 is prediction
							//if (hop0>255)hop0=255;
							//else if (hop0<0) hop0=0; 


							// el array PCC habria que escogerlo en funcion de kini
							
							//if (hop0>128) hop1=(int)Math.max(hop1,(float)hop0*0.03f);
							//if (hop0>200 && hop1<5) hop1=5;//(int)Math.max(hop1,(float)hop0*0.03f);
							//System.out.println("hola");
							//positive hops computation
							//-------------------------
							//hop1=8;
							//rmax=40;
							/*max_hop1=10;//4*rmax;
							if (max_hop1>19) max_hop1=19;
							if (max_hop1<6) max_hop1=6;
							*/
							
							//29/12/2014
							//min_hop1=(int)(0.5f+(float)hop0*0.04f);//no puede ser cero
							//if (min_hop1<=2) min_hop1=2;
							//if (hop1<min_hop1) hop1=min_hop1;
							//max_hop1=(int)((float)min_hop1*2.5f);
							//if (max_hop1>=20) max_hop1=19;
							//if (hop1>max_hop1) hop1=max_hop1;
							
							//min_hop1=(int)(0.5f+(float)hop0*0.08f);//no puede ser cero
							//if (min_hop1<6) min_hop1=6;
							//if (hop1<min_hop1) hop1=min_hop1;
							//max_hop1=12;
							
							if (oc-hop0>=0) 
							{
								for (int j=4;j<=8;j++) {
								//for (int j=4;j<=7;j++) {
									try{
									e2=oc-pccr[hop1][hop0][rmax][j];
									}catch (Exception e){
										System.out.println("j:"+j+" hop1:"+hop1+"  hop0:"+hop0+"  x:"+x+"  y:"+y+"  b.xini:"+b.xini+" b.yini:"+b.yini+" b.downsampled_xfin:"+b.downsampled_xfin+"  up:"+result_YUV[pix-img.width]+" izq:"+result_YUV[pix-1]+ " debe ser:"+(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7+ "boundaries es:"+img.boundaries_YUV[0][pix-1]);
										System.exit(0);
									}
									if (e2<0) e2=-e2;
									if (e2<emin) {hop_number=j;emin=e2;}
									else break;
								}
							}
							//negative hops computation
							//-------------------------
							else 
							{
								//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
								//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
								//
								
								//OPTIMIZACION
								//creo que puedo evitar el j=4 NO, no se puede, pues el valor puede estar 
								//mas cerca del hop nulo que del hop -1
								for (int j=4;j>=0;j--) {
									//for (int j=4;j>=1;j--) {
									e2=pccr[hop1][hop0][rmax][j]-oc;
									if (e2<0) e2=-e2;
									if (e2<emin) {hop_number=j;emin=e2;}
									else break;
								}
							}

							//prueba
							if ((x==b.xini) && (y==b.yini))
							{
								//System.out.println(" se escoge hop ="+hop_number+ "  oc="+oc+"  hop1 es:"+hop1+ "   asigna "+pccr[hop1][hop0][rmax][hop_number]+ "rmax:"+rmax);
								//hop0=oc;
							}
							//assignment of final color value
							//--------------------------------
							result_YUV[pix]=pccr[hop1][hop0][rmax][hop_number];
							//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
							//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
							hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

							
							
							
							//tunning hop1 for the next hop
							//-------------------------------
							boolean small_hop=false;
							//if (hop_number>=6) small_hop=true;
							//if (hop_number<=6 && hop_number>=2) small_hop=true;
							if (hop_number<=5 && hop_number>=3) small_hop=true;
							else small_hop=false;     

							if( (small_hop) && (last_small_hop))  {
								hop1=hop1-1;
								if (hop1<min_hop1) hop1=min_hop1;
							} 
							else {
								hop1=max_hop1;
							}
							
							
							//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

							//lets go for the next pixel
							//--------------------------
							last_small_hop=small_hop;
							
							
							
							//pix++;            
						}//for x
						//esto es necesario al funcionar con bloques
						//pix+=img.width-b.lx_sc+1;
					}//for y

				}//end function

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeDownsampledBlock_R_experimento(Block b, int[] hops,int[] result_YUV, int[] src_YUV,int[] boundaries_YUV)
{

	
	
	//block b contains the coordinates to set the limits of this function
	
	
	//some parts of this code are identical to the function quantizeOneHopPerPixel() 
	
	
	int max_hop1=8;// hop1 interval 4..8
	int min_hop1=4;//4;//
	
	int start_hop1=6;// hop1 at begining (up-left corner) of each block
	
	int hop1=start_hop1;
	
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	
	
	int rmax=20;
	//se escoge el juego de hops segun el PR cuantizado
	
	if (b.PRavg==1.0) rmax=30;//bordes abruptos, iconos, ruido 
	//else if (b.PRavg>=0.625) rmax=25;//bordes 1,1,1,0.5
	else if (b.PRavg>=0.75) rmax=25;//bordes 1,1,1,0.5
	//else if (b.PRavg>=0.625) rmax=20;//bordes 1,1,1,0.5
	
	
	rmax=20;//valor original. suave
	
	//System.out.println(" dato:"+img.width);
	int pix=b.yini*img.width+b.xini;//initial pixel possition        
	
	
	
	boolean last_small_hop=false;// indicates if last hop is small
	for (int y=b.yini;y<=b.downsampled_yfin;y++)  {
		
		//ponemos a hop minimo al comienzo de scan line de bloque
		//esto es bueno sobre todo en bloques lisos
		
		//debo de usar un array mejor
		//hop1=min_hop1;//NUEVO 10/09/2014
		
		for (int x=b.xini;x<=b.downsampled_xfin;x++)  {

			
			
			pix=y*img.width+x;
			oc=src_YUV[pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			
			//inner pixels ( mostly of them. that is the reason for considering the 1st option)
			//---------------------------------------------------------------------------------
			 if ((y>b.yini) &&(x>b.xini) && x!=b.downsampled_xfin){
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	

				//	System.out.println(" result_YUV[pix-1]:"+result_YUV[pix-1]+"  result_YUV[pix+1-img.width]: "+result_YUV[pix+1-img.width]);
			}
			//initial pix
			//-----------
			 else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}	
			//upper side of the image
		    //-----------------------
			else if (y==0 && x>b.xini) {
				hop0=result_YUV[x-1];
				
			}
			
			else if (y==0 && x==b.xini) {
				hop0=boundaries_YUV[x-1];
				
				
				last_small_hop=false;
				hop1=start_hop1;
				//hop0=oc;
				
			}
			
			//left side of the image 2 cases
			//------------------------
			else if ((x==0) && (y>b.yini)){
				
				hop0=result_YUV[pix-img.width];
				//hop0=img.boundaries_YUV[0][pix-img.width];
			
				//esta variable puede variar es mejor no ponerla a false
				//last_small_hop=false;
				
				//comento esto 12/09/2014
				//hop1=start_hop1;
			}
            else if ((x==0) && (y==b.yini)){
				
            	//System.out.println("NUNCA ENTRA");
				//hop0=result_YUV[pix-img.width];
				hop0=img.boundaries_YUV[0][pix-img.width];
				
				last_small_hop=false;
				
				//comento esto 12/09/2014
				hop1=start_hop1;
				//hop0=oc;
				
			}
			//left side of the block
			//------------------------
			else if ((x==b.xini) && (y>b.yini)){
				//System.out.print("zulu");
					hop0=(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7;
					//hop0=img.boundaries_YUV[0][pix-1];
					//hop0=(4*img.boundaries_YUV[0][pix-1]+4*result_YUV[pix+1-img.width])/8;
					//hop0=result_YUV[pix+1-img.width];//oc;//img.boundaries_YUV[0][pix-1];
					//System.out.println(" el boundary v tiene color:"+img.boundaries_YUV[0][pix-1]+"  at  x:"+(x-1)+" ,y:"+y);
					//hop0=oc;
					
					//esto es nuevo 12/09/2014
					//last_small_hop=false;
					//hop1=max_hop1;
					//hop1=start_hop1;
					//hop0=oc;
					
					
			}
			//up-left corner of block
			//--------------
			else if ((x==b.xini) && (y==b.yini)){
				//System.out.print("zulu");
				hop0=(4*img.boundaries_YUV[0][pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;
				
				//esto es nuevo 12/09/2014
				last_small_hop=false;
				
				
				hop1=start_hop1;
				//hop1=8;//start_hop1;
				//hop0=255;//oc;
				//System.out.println("hop0 es "+hop0);
				
				//hop0=oc;
			}
			
			
			
			//right side of block (and right side of image. is the same case)
			//-----------------------------------------------------------------
			else if ((x==b.downsampled_xfin) && (y>b.yini)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;	
				
				
				//hop0=(4*result_YUV[pix-1]+0*result_YUV[pix-img.width])/4;
			}
			
				
			//upper side of block
			//---------------------
		    else if (y==b.yini && x>0 && x!=b.downsampled_xfin) {
		    	hop0=(4*result_YUV[pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;	
		    	//hop0=oc;//(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix+1-img.width])/6;
		    	//hop0=(4*result_YUV[pix-1]+1*img.boundaries_YUV[0][pix+1-img.width])/5;
		    	//hop0=result_YUV[pix-1];
		    	
		    	//hop0=oc;
		    }
		    else if ((x==b.downsampled_xfin) && (y==b.yini)) {
				//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;
				hop0=(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix-img.width])/6;
				//if (result_YUV[pix-img.width]==0) System.out.println(" ey");
			}
		    	
			//hop0=oc;
			 if ((x==b.xini) && (y==b.yini)){
				// hop0=oc;	 
			 }
			
			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//hop0 is prediction
			//if (hop0>255)hop0=255;
			//else if (hop0<0) hop0=0; 


			// el array PCC habria que escogerlo en funcion de kini
			
			//if (hop0>128) hop1=(int)Math.max(hop1,(float)hop0*0.03f);
			//positive hops computation
			//-------------------------
			hop1=4;
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
					try{
					e2=oc-pccr[hop1][hop0][rmax][j];
					}catch (Exception e){
						System.out.println("j:"+j+" hop1:"+hop1+"  hop0:"+hop0+"  x:"+x+"  y:"+y+"  b.xini:"+b.xini+" b.yini:"+b.yini+" b.downsampled_xfin:"+b.downsampled_xfin+"  up:"+result_YUV[pix-img.width]+" izq:"+result_YUV[pix-1]+ " debe ser:"+(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7+ "boundaries es:"+img.boundaries_YUV[0][pix-1]);
						System.exit(0);
					}
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
				//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
				//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
				//
				
				//OPTIMIZACION
				//creo que puedo evitar el j=4 NO, no se puede, pues el valor puede estar 
				//mas cerca del hop nulo que del hop -1
				for (int j=4;j>=0;j--) {
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}

			//prueba
			if ((x==b.xini) && (y==b.yini))
			{
				//System.out.println(" se escoge hop ="+hop_number+ "  oc="+oc+"  hop1 es:"+hop1+ "   asigna "+pccr[hop1][hop0][rmax][hop_number]+ "rmax:"+rmax);
				//hop0=oc;
			}
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][rmax][hop_number];
			//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
			//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
			hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

			
			
			
			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			//if (hop_number>=6) small_hop=true;
			//if (hop_number<=6 && hop_number>=2) small_hop=true;
			if (hop_number<=5 && hop_number>=3) small_hop=true;
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
				
				rmax=rmax-2;
				if (rmax<20) rmax=20;
				
			} 
			else {
				hop1=max_hop1;
				rmax=25;//rmax+1;
				if (rmax>30) rmax=30;
			}
			
			//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			
			
			
			//pix++;            
		}//for x
		//esto es necesario al funcionar con bloques
		//pix+=img.width-b.lx_sc+1;
	}//for y

}//end function
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeDownsampledBlock_SinBoundaries(Block b, int[] hops,int[] result_YUV, int[] src_YUV,int[] boundaries_YUV)
{

	
	
	//block b contains the coordinates to set the limits of this function
	
	
	//some parts of this code are identical to the function quantizeOneHopPerPixel() 
	
	
	int max_hop1=8;// hop1 interval 4..8
	int min_hop1=4;//4;//
	
	//queda mejor 6 que el punto medio (en peppers)
	int start_hop1=(max_hop1+min_hop1)/2;// hop1 at begining (up-left corner) of each block
	
	int hop1=start_hop1;
	
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	
	
	int rmax=20;
	//se escoge el juego de hops segun el PR cuantizado
	
	if (b.PRavg==1.0) rmax=30;//bordes abruptos, iconos, ruido 
	//else if (b.PRavg>=0.625) rmax=25;//bordes 1,1,1,0.5
	else if (b.PRavg>=0.75) rmax=25;//bordes 1,1,1,0.5
	else if (b.PRavg>=0.5) rmax=25;//bordes 1,1,1,0.5 NUEVO 2/01/2015
	//else if (b.PRavg>=0.625) rmax=20;//bordes 1,1,1,0.5
	
	//MEJORA 28/12/2014. alteramos h1max segun la PR y ademas el menor valor es 10 y no 8
	if (b.PRavg==1.0) {max_hop1=16;}//16;
	else if (b.PRavg>=0.75) {max_hop1=14;}//12;
	else if (b.PRavg>=0.5) {max_hop1=12;}
	else if (b.PRavg>=0.25) max_hop1=10;
	//
	//else if (b.PRavg>=0.125) max_hop1=10;
	else max_hop1=8;// suave
	
	
	//min_hop1=4;
	
	//PARA HACER PRUEBAS CON LHE BASICO SE PUEDE:
	//PONER GRID A UN SOLO BLOQUE.
	//HACER ESTAS IGUALDADES:
	//max_hop1=8;
	//rmax=25; //si pongo 20, lena gana 43 db
	
	
	
	//System.out.println("PRavg:"+b.PRavg);
	//rmax=20;//valor original
	//max_hop1=19;
	//start_hop1=6;
	
	//System.out.println(" dato:"+img.width);
	int pix=b.yini*img.width+b.xini;//initial pixel possition        
	
	
	
	boolean last_small_hop=false;// indicates if last hop is small
	for (int y=b.yini;y<=b.downsampled_yfin;y++)  {
		
		//ponemos a hop minimo al comienzo de scan line de bloque
		//esto es bueno sobre todo en bloques lisos
		
		//debo de usar un array mejor
		//hop1=min_hop1;//NUEVO 10/09/2014
		
		for (int x=b.xini;x<=b.downsampled_xfin;x++)  {

			pix=y*img.width+x;
			oc=src_YUV[pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			
			//inner pixels ( mostly of them. that is the reason for considering the 1st option)
			//---------------------------------------------------------------------------------
			 if ((y>b.yini) &&(x>b.xini) && x!=b.downsampled_xfin){
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	
				
				//hop0=(4*result_YUV[pix-1]+4*result_YUV[pix-img.width])/8;	
				
				//hop0=(241*result_YUV[pix-1]+170*result_YUV[pix+1-img.width])/411;	
				//	System.out.println(" result_YUV[pix-1]:"+result_YUV[pix-1]+"  result_YUV[pix+1-img.width]: "+result_YUV[pix+1-img.width]);
			}
			//initial pix
			//-----------
			 //else if (x==0 && y==0) {  
			 else if (x==b.xini && y==b.yini) {
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}	
			//upper side of the image. not include corner
		    //--------------------------------------------
			 else if (y==b.yini && x>b.xini) {
			//else if (y==0 && x>b.xini) {
				hop0=result_YUV[pix-1];
				//hop0=oc;
			}
			
			//corners at upper side of image
			//-------------------------------
			/*
			 else if (y==0 && x==b.xini) {
				
				System.out.println ("NO DEBE PASAR");
				hop0=boundaries_YUV[x-1];
				
				
				last_small_hop=false;
				hop1=start_hop1;
				
				
			}
			*/
			//left side of the image. not include corner
			//-----------------------------------------
			else if ((x==b.xini) && (y>b.yini)){
			//else if ((x==0) && (y>b.yini)){
				
				hop0=result_YUV[pix-img.width];
				//hop0=img.boundaries_YUV[0][pix-img.width];
			
				//esta variable puede variar es mejor no ponerla a false
				last_small_hop=false;//31/12/2014 se descomenta
				//last_small_hop=true;//porque no
				//comento esto 12/09/2014
				hop1=start_hop1;//31/12/2014 se descomenta
			}
			//corners of left side of the image.
			//----------------------------------------- 
            else if ((x==0) && (y==b.yini)){
            	System.out.println ("NO DEBE PASAR");
            	//System.out.println("NUNCA ENTRA");
				//hop0=result_YUV[pix-img.width];
				hop0=img.boundaries_YUV[0][pix-img.width];
				
				//hop0=(3*img.boundaries_YUV[0][pix-img.width]+4*img.boundaries_YUV[0][pix-img.width+1])/7;
				
				
				last_small_hop=false;
				//last_small_hop=true;//porque no
				
				//comento esto 12/09/2014
				hop1=start_hop1;
				
				
				//hop0=oc;
				
			}
			//left side of the block. not include corner
			//--------------------------------------
			else if ((x==b.xini) && (y>b.yini)){
				System.out.println ("NO DEBE PASAR");
				//System.out.print("zulu");
					hop0=(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7;
					//hop0=img.boundaries_YUV[0][pix-1];
					//hop0=(4*img.boundaries_YUV[0][pix-1]+4*result_YUV[pix+1-img.width])/8;
					//hop0=result_YUV[pix+1-img.width];//oc;//img.boundaries_YUV[0][pix-1];
					//System.out.println(" el boundary v tiene color:"+img.boundaries_YUV[0][pix-1]+"  at  x:"+(x-1)+" ,y:"+y);
					//hop0=oc;
					
					//esto es nuevo 12/09/2014
					last_small_hop=false;//31/12/2014 se descomenta
					//last_small_hop=true;//porque no
					
					//hop1=max_hop1;
					hop1=start_hop1;//31/12/2014 se descomenta
					//hop0=oc;
					
					
			}
			//up-left corner of block
			//--------------
			else if ((x==b.xini) && (y==b.yini)){
				//System.out.print("zulu");
				System.out.println ("NO DEBE PASAR");
				hop0=(4*img.boundaries_YUV[0][pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;
				
				//esto es nuevo 12/09/2014
				last_small_hop=false;
				//last_small_hop=true;//porque no
				
				hop1=start_hop1;
				//hop1=8;//start_hop1;
				//hop0=255;//oc;
				//System.out.println("hop0 es "+hop0);
				
				//hop0=oc;
			}
			
			
			
			//right side of block (and right side of image. is the same case). Not includes corner
			//-----------------------------------------------------------------------------------
			else if ((x==b.downsampled_xfin) && (y>b.yini)) {
				//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;	
				
				//nueva formula 03/12/2015
				hop0=(result_YUV[pix-1]+result_YUV[pix-img.width])/2;	
				
				
				//hop0=(4*result_YUV[pix-1]+0*result_YUV[pix-img.width])/4;
			}
			
				
			//upper side of block. not includes right corner
			//-----------------------------------------
		    else if (y==b.yini && x>0 && x!=b.downsampled_xfin) {
		    	System.out.println ("NO DEBE PASAR");
		    	hop0=(4*result_YUV[pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;	
		    	//hop0=(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix+1-img.width])/6;
		    	//hop0=(4*result_YUV[pix-1]+1*img.boundaries_YUV[0][pix+1-img.width])/5;
		    	//hop0=result_YUV[pix-1];
		    	
		    	//hop0=oc;
		    }
			//up-right CORNER of block
			//-----------------------------------------------------
		    else if ((x==b.downsampled_xfin) && (y==b.yini)) {
		    	
		    	System.out.println ("NO DEBE PASAR");
		    	
				//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;
				hop0=(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix-img.width])/6;
				
				//aqui no se pueden usar los boundaries porque no estan escalados para este bloque
				//hop0=(4*result_YUV[pix-1]+3*img.boundaries_YUV[0][pix-img.width+1])/7;
				//if (result_YUV[pix-img.width]==0) System.out.println(" ey");
			}
		    	
			//hop0=oc;
			 /*
			 if ((x==b.xini) && (y==b.yini)){
				// hop0=oc;	 
			 }
			 */
			 
			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//hop0 is prediction
			//if (hop0>255)hop0=255;
			//else if (hop0<0) hop0=0; 


			// el array PCC habria que escogerlo en funcion de kini
			
			//if (hop0>128) hop1=(int)Math.max(hop1,(float)hop0*0.03f);
			//if (hop0>200 && hop1<5) hop1=5;//(int)Math.max(hop1,(float)hop0*0.03f);
			//System.out.println("hola");
			//positive hops computation
			//-------------------------
			//hop1=8;
			//rmax=40;
			/*max_hop1=10;//4*rmax;
			if (max_hop1>19) max_hop1=19;
			if (max_hop1<6) max_hop1=6;
			*/
			
			//29/12/2014
			//min_hop1=(int)(0.5f+(float)hop0*0.04f);//no puede ser cero
			//if (min_hop1<=2) min_hop1=2;
			//if (hop1<min_hop1) hop1=min_hop1;
			//max_hop1=(int)((float)min_hop1*2.5f);
			//if (max_hop1>=20) max_hop1=19;
			//if (hop1>max_hop1) hop1=max_hop1;
			
			//min_hop1=(int)(0.5f+(float)hop0*0.08f);//no puede ser cero
			//if (min_hop1<6) min_hop1=6;
			//if (hop1<min_hop1) hop1=min_hop1;
			//max_hop1=12;
			
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
				//for (int j=4;j<=7;j++) {
					try{
					e2=oc-pccr[hop1][hop0][rmax][j];
					}catch (Exception e){
						System.out.println("j:"+j+" hop1:"+hop1+"  hop0:"+hop0+"  x:"+x+"  y:"+y+"  b.xini:"+b.xini+" b.yini:"+b.yini+" b.downsampled_xfin:"+b.downsampled_xfin+"  up:"+result_YUV[pix-img.width]+" izq:"+result_YUV[pix-1]+ " debe ser:"+(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7+ "boundaries es:"+img.boundaries_YUV[0][pix-1]);
						System.exit(0);
					}
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
				//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
				//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
				//
				
				//OPTIMIZACION
				//creo que puedo evitar el j=4 NO, no se puede, pues el valor puede estar 
				//mas cerca del hop nulo que del hop -1
				for (int j=4;j>=0;j--) {
					//for (int j=4;j>=1;j--) {
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}

			//prueba
			if ((x==b.xini) && (y==b.yini))
			{
				//System.out.println(" se escoge hop ="+hop_number+ "  oc="+oc+"  hop1 es:"+hop1+ "   asigna "+pccr[hop1][hop0][rmax][hop_number]+ "rmax:"+rmax);
				//hop0=oc;
			}
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][rmax][hop_number];
			//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
			//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
			hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

			
			
			
			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			//if (hop_number>=6) small_hop=true;
			//if (hop_number<=6 && hop_number>=2) small_hop=true;
			if (hop_number<=5 && hop_number>=3) small_hop=true;
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			
			
			//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			
			
			
			//pix++;            
		}//for x
		//esto es necesario al funcionar con bloques
		//pix+=img.width-b.lx_sc+1;
	}//for y

}//end function

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeDownsampledBlock_R2(Block b, int[] hops,int[] result_YUV, int[] src_YUV,int[] boundaries_YUV)
{

	
	
	//block b contains the coordinates to set the limits of this function
	
	
	//some parts of this code are identical to the function quantizeOneHopPerPixel() 
	
	
	int max_hop1=8;// hop1 interval 4..8
	int min_hop1=4;//4;//
	
	//queda mejor 6 que el punto medio (en peppers)
	int start_hop1=(max_hop1+min_hop1)/2;// hop1 at begining (up-left corner) of each block
	
	int hop1=start_hop1;
	
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	
	
	int rmax=20;
	//se escoge el juego de hops segun el PR cuantizado
	
	if (b.PRavg==1.0) rmax=30;//bordes abruptos, iconos, ruido 
	//else if (b.PRavg>=0.625) rmax=25;//bordes 1,1,1,0.5
	else if (b.PRavg>=0.75) rmax=25;//bordes 1,1,1,0.5
	else if (b.PRavg>=0.5) rmax=25;//bordes 1,1,1,0.5 NUEVO 2/01/2015
	//else if (b.PRavg>=0.625) rmax=20;//bordes 1,1,1,0.5
	
	//MEJORA 28/12/2014. alteramos h1max segun la PR y ademas el menor valor es 10 y no 8
	if (b.PRavg==1.0) {max_hop1=16;}//16;
	else if (b.PRavg>=0.75) {max_hop1=14;}//12;
	else if (b.PRavg>=0.5) {max_hop1=12;}
	else if (b.PRavg>=0.25) max_hop1=10;
	//
	//else if (b.PRavg>=0.125) max_hop1=10;
	else max_hop1=8;// suave
	
	
	//min_hop1=4;
	
	//PARA HACER PRUEBAS CON LHE BASICO SE PUEDE:
	//PONER GRID A UN SOLO BLOQUE.
	//HACER ESTAS IGUALDADES:
	//max_hop1=8;
	//rmax=25; //si pongo 20, lena gana 43 db
	
	
	
	//System.out.println("PRavg:"+b.PRavg);
	//rmax=20;//valor original
	//max_hop1=19;
	//start_hop1=6;
	
	//System.out.println(" dato:"+img.width);
	int pix=b.yini*img.width+b.xini;//initial pixel possition        
	
	
	
	boolean last_small_hop=false;// indicates if last hop is small
	for (int y=b.yini;y<=b.downsampled_yfin;y++)  {
		
		//ponemos a hop minimo al comienzo de scan line de bloque
		//esto es bueno sobre todo en bloques lisos
		
		//debo de usar un array mejor
		//hop1=min_hop1;//NUEVO 10/09/2014
		
		for (int x=b.xini;x<=b.downsampled_xfin;x++)  {

			pix=y*img.width+x;
			oc=src_YUV[pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			
			//inner pixels ( mostly of them. that is the reason for considering the 1st option)
			//---------------------------------------------------------------------------------
			 if ((y>b.yini) &&(x>b.xini) && x!=b.downsampled_xfin){
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	
				
				//hop0=(4*result_YUV[pix-1]+4*result_YUV[pix-img.width])/8;	
				
				//hop0=(241*result_YUV[pix-1]+170*result_YUV[pix+1-img.width])/411;	
				//	System.out.println(" result_YUV[pix-1]:"+result_YUV[pix-1]+"  result_YUV[pix+1-img.width]: "+result_YUV[pix+1-img.width]);
			}
			//initial pix
			//-----------
			 else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}	
			//upper side of the image. not include corner
		    //--------------------------------------------
			else if (y==0 && x>b.xini) {
				hop0=result_YUV[pix-1];
				
			}
			
			//corners at upper side of image
			//-------------------------------
			else if (y==0 && x==b.xini) {
				hop0=boundaries_YUV[pix-1];
				
				
				last_small_hop=false;
				hop1=start_hop1;
				
				
			}
			
			//left side of the image. not include corner
			//-----------------------------------------
			else if ((x==0) && (y>b.yini)){
				
				hop0=result_YUV[pix-img.width];
				//hop0=img.boundaries_YUV[0][pix-img.width];
			
				//esta variable puede variar es mejor no ponerla a false
				last_small_hop=false;//31/12/2014 se descomenta
				//last_small_hop=true;//porque no
				//comento esto 12/09/2014
				hop1=start_hop1;//31/12/2014 se descomenta
			}
			//corners of left side of the image.
			//----------------------------------------- 
            else if ((x==0) && (y==b.yini)){
				
            	//System.out.println("NUNCA ENTRA");
				//hop0=result_YUV[pix-img.width];
				hop0=img.boundaries_YUV[0][pix-img.width];
				
				//hop0=(3*img.boundaries_YUV[0][pix-img.width]+4*img.boundaries_YUV[0][pix-img.width+1])/7;
				
				
				last_small_hop=false;
				//last_small_hop=true;//porque no
				
				//comento esto 12/09/2014
				hop1=start_hop1;
				
				
				//hop0=oc;
				
			}
			//left side of the block. not include corner
			//--------------------------------------
			else if ((x==b.xini) && (y>b.yini)){
				//System.out.print("zulu");
					hop0=(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7;
					//hop0=img.boundaries_YUV[0][pix-1];
					//hop0=(4*img.boundaries_YUV[0][pix-1]+4*result_YUV[pix+1-img.width])/8;
					//hop0=result_YUV[pix+1-img.width];//oc;//img.boundaries_YUV[0][pix-1];
					//System.out.println(" el boundary v tiene color:"+img.boundaries_YUV[0][pix-1]+"  at  x:"+(x-1)+" ,y:"+y);
					//hop0=oc;
					
					//esto es nuevo 12/09/2014
					last_small_hop=false;//31/12/2014 se descomenta
					//last_small_hop=true;//porque no
					
					//hop1=max_hop1;
					hop1=start_hop1;//31/12/2014 se descomenta
					//hop0=oc;
					
					
			}
			//up-left corner of block
			//--------------
			else if ((x==b.xini) && (y==b.yini)){
				//System.out.print("zulu");
				hop0=(4*img.boundaries_YUV[0][pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;
				
				//esto es nuevo 12/09/2014
				last_small_hop=false;
				//last_small_hop=true;//porque no
				
				hop1=start_hop1;
				//hop1=8;//start_hop1;
				//hop0=255;//oc;
				//System.out.println("hop0 es "+hop0);
				
				//hop0=oc;
			}
			
			
			
			//right side of block (and right side of image. is the same case). Not includes corner
			//-----------------------------------------------------------------------------------
			else if ((x==b.downsampled_xfin) && (y>b.yini)) {
				//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;	
				
				//nueva formula 03/12/2015
				hop0=(result_YUV[pix-1]+result_YUV[pix-img.width])/2;	
				
				
				//hop0=(4*result_YUV[pix-1]+0*result_YUV[pix-img.width])/4;
			}
			
				
			//upper side of block. not includes right corner
			//-----------------------------------------
		    else if (y==b.yini && x>0 && x!=b.downsampled_xfin) {
		    	hop0=(4*result_YUV[pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;	
		    	//hop0=(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix+1-img.width])/6;
		    	//hop0=(4*result_YUV[pix-1]+1*img.boundaries_YUV[0][pix+1-img.width])/5;
		    	//hop0=result_YUV[pix-1];
		    	
		    	//hop0=oc;
		    }
			//up-right CORNER of block
			//-----------------------------------------------------
		    else if ((x==b.downsampled_xfin) && (y==b.yini)) {
		    	
		    	
		    	
				//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;
				hop0=(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix-img.width])/6;
				
				//aqui no se pueden usar los boundaries porque no estan escalados para este bloque
				//hop0=(4*result_YUV[pix-1]+3*img.boundaries_YUV[0][pix-img.width+1])/7;
				//if (result_YUV[pix-img.width]==0) System.out.println(" ey");
			}
		    	
			//hop0=oc;
			 /*
			 if ((x==b.xini) && (y==b.yini)){
				// hop0=oc;	 
			 }
			 */
			
			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//hop0 is prediction
			//if (hop0>255)hop0=255;
			//else if (hop0<0) hop0=0; 


			// el array PCC habria que escogerlo en funcion de kini
			
			//if (hop0>128) hop1=(int)Math.max(hop1,(float)hop0*0.03f);
			//if (hop0>200 && hop1<5) hop1=5;//(int)Math.max(hop1,(float)hop0*0.03f);
			//System.out.println("hola");
			//positive hops computation
			//-------------------------
			//hop1=8;
			//rmax=40;
			/*max_hop1=10;//4*rmax;
			if (max_hop1>19) max_hop1=19;
			if (max_hop1<6) max_hop1=6;
			*/
			
			//29/12/2014
			//min_hop1=(int)(0.5f+(float)hop0*0.04f);//no puede ser cero
			//if (min_hop1<=2) min_hop1=2;
			//if (hop1<min_hop1) hop1=min_hop1;
			//max_hop1=(int)((float)min_hop1*2.5f);
			//if (max_hop1>=20) max_hop1=19;
			//if (hop1>max_hop1) hop1=max_hop1;
			
			//min_hop1=(int)(0.5f+(float)hop0*0.08f);//no puede ser cero
			//if (min_hop1<6) min_hop1=6;
			//if (hop1<min_hop1) hop1=min_hop1;
			//max_hop1=12;
			
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
				//for (int j=4;j<=7;j++) {
					try{
					e2=oc-pccr[hop1][hop0][rmax][j];
					}catch (Exception e){
						System.out.println("j:"+j+" hop1:"+hop1+"  hop0:"+hop0+"  x:"+x+"  y:"+y+"  b.xini:"+b.xini+" b.yini:"+b.yini+" b.downsampled_xfin:"+b.downsampled_xfin+"  up:"+result_YUV[pix-img.width]+" izq:"+result_YUV[pix-1]+ " debe ser:"+(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7+ "boundaries es:"+img.boundaries_YUV[0][pix-1]);
						System.exit(0);
					}
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
				//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
				//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
				//
				
				//OPTIMIZACION
				//creo que puedo evitar el j=4 NO, no se puede, pues el valor puede estar 
				//mas cerca del hop nulo que del hop -1
				for (int j=4;j>=0;j--) {
					//for (int j=4;j>=1;j--) {
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}

			
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][rmax][hop_number];
			hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0
			
			
			//NUEVA ESTRATEGIA DE CORRECCION

			//ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
			if (hop_number>=8 || hop_number<=0)
			{
				int hop1b=hop1;
				int hop_number_aux=hop_number;
				hop0=result_YUV[pix];// nuevo color de fondo
				emin=256;//current minimum prediction error 
				e2=0;//computed error for each hop 
				if (oc-hop0>=0) 
				{
					for (int j=5;j<=6;j++) {
					//for (int j=4;j<=7;j++) {
						try{
						e2=oc-pccr[hop1b][hop0][rmax][j];
						}catch (Exception e){
							System.out.println("j:"+j+" hop1:"+hop1+"  hop0:"+hop0+"  x:"+x+"  y:"+y+"  b.xini:"+b.xini+" b.yini:"+b.yini+" b.downsampled_xfin:"+b.downsampled_xfin+"  up:"+result_YUV[pix-img.width]+" izq:"+result_YUV[pix-1]+ " debe ser:"+(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7+ "boundaries es:"+img.boundaries_YUV[0][pix-1]);
							System.exit(0);
						}
						if (e2<0) e2=-e2;
						if (e2<emin) {hop_number=j;emin=e2;}
						else break;
					}
				}
				//negative hops computation
				//-------------------------
				else 
				{
					//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
					//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
					//
					
					//OPTIMIZACION
					//creo que puedo evitar el j=4 NO, no se puede, pues el valor puede estar 
					//mas cerca del hop nulo que del hop -1
					for (int j=3;j>=2;j--) {
						//for (int j=4;j>=1;j--) {
						e2=pccr[hop1b][hop0][rmax][j]-oc;
						if (e2<0) e2=-e2;
						if (e2<emin) {hop_number=j;emin=e2;}
						else break;
					}
				}
				contafix++;
				int cosa=0;
				if (oc-result_YUV[pix]>5) {cosa=10;bits_fix+=2;}
				else if (oc-result_YUV[pix]<-5) {cosa=-10;bits_fix+=2;}
				else bits_fix+=1;
				result_YUV[pix]+=cosa;
				if (result_YUV[pix]>255) result_YUV[pix]=255;
				if (result_YUV[pix]<1) result_YUV[pix]=1;
				
				
			    //result_YUV[pix]=pccr[hop1][hop0][rmax][hop_number];
				//System.out.println ("antes:"+hop0+" ahora:"+result_YUV[pix]+" hop:"+hop_number);
				
				
				//if (hop_number==4) bits_fix+=1;
				//else  bits_fix+=2;
				//lo dejamos como estaba
				hop_number=hop_number_aux;	
				
			}
			
			
			//END NUEVA ESTRATEGIA
			//ooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo
			
			
			
			//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
			//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
			

			
			
			
			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			//if (hop_number>=6) small_hop=true;
			//if (hop_number<=6 && hop_number>=2) small_hop=true;
			if (hop_number<=5 && hop_number>=3) small_hop=true;
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			
			
			//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			
			
			
			//pix++;            
		}//for x
		//esto es necesario al funcionar con bloques
		//pix+=img.width-b.lx_sc+1;
	}//for y

}//end function
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeDownsampledBlock_R3(Block b, int[] hops,int[] result_YUV, int[] src_YUV,int[] boundaries_YUV)
{

	//System.out.println("colorin");
	
	//block b contains the coordinates to set the limits of this function
	
	
	//some parts of this code are identical to the function quantizeOneHopPerPixel() 
	
	//ESTOS DOS VALORES LUEGO SE AJUSTAN
	int max_hop1=8;// hop1 interval 4..8
	int min_hop1=4;//4;//
	
	//queda mejor 6 que el punto medio (en peppers)
	int start_hop1=(max_hop1+min_hop1)/2;// hop1 at begining (up-left corner) of each block
	
	int hop1=start_hop1;
	
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	
	
	int rmax=20;
	//se escoge el juego de hops segun el PR cuantizado
	
	if (b.PRavg==1.0) rmax=30;//bordes abruptos, iconos, ruido 
	//else if (b.PRavg>=0.625) rmax=25;//bordes 1,1,1,0.5
	else if (b.PRavg>=0.75) rmax=30;//bordes 1,1,1,0.5 LO HE CAMBIADO DESDE 25 . hoy es 5/3/2015 30
	else if (b.PRavg>=0.5) rmax=25;//bordes 1,1,1,0.5 NUEVO 2/01/2015
	else if (b.PRavg>=0.25) rmax=25;//bordes 1,1,1,0.5 NUEVO 5/3/2015 25<--- con 22 es mejor q 25 para lena
	
	//MEJORA 28/12/2014. alteramos h1max segun la PR y ademas el menor valor es 10 y no 8
	if (b.PRavg==1.0) {max_hop1=16;}//16;
	else if (b.PRavg>=0.75) {max_hop1=14;}//12;
	else if (b.PRavg>=0.5) {max_hop1=12;} // de 12 a 10?
	else if (b.PRavg>=0.25) max_hop1=10; 
	//
	//else if (b.PRavg>=0.125) max_hop1=10;
	else max_hop1=8;// suave
	
	
	//min_hop1=4;
	
	//PARA HACER PRUEBAS CON LHE BASICO SE PUEDE:
	//PONER GRID A UN SOLO BLOQUE.
	//HACER ESTAS IGUALDADES:
	//max_hop1=8;
	//rmax=25; //si pongo 20, lena gana 43 db
	
	
	
	//System.out.println("PRavg:"+b.PRavg);
	//rmax=20;//valor original
	//max_hop1=19;
	//start_hop1=6;
	
	//System.out.println(" dato:"+img.width);
	int pix=b.yini*img.width+b.xini;//initial pixel possition        
	
	
	
	boolean last_small_hop=false;// indicates if last hop is small
	for (int y=b.yini;y<=b.downsampled_yfin;y++)  {
		
		//ponemos a hop minimo al comienzo de scan line de bloque
		//esto es bueno sobre todo en bloques lisos
		
		//debo de usar un array mejor
		//hop1=min_hop1;//NUEVO 10/09/2014
		
		for (int x=b.xini;x<=b.downsampled_xfin;x++)  {

			pix=y*img.width+x;
			oc=src_YUV[pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			
			//inner pixels ( mostly of them. that is the reason for considering the 1st option)
			//---------------------------------------------------------------------------------
			 if ((y>b.yini) &&(x>b.xini) && x!=b.downsampled_xfin){
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	
				
				//hop0=(4*result_YUV[pix-1]+4*result_YUV[pix-img.width])/8;	
				
				//hop0=(241*result_YUV[pix-1]+170*result_YUV[pix+1-img.width])/411;	
				//	System.out.println(" result_YUV[pix-1]:"+result_YUV[pix-1]+"  result_YUV[pix+1-img.width]: "+result_YUV[pix+1-img.width]);
			}
			//initial pix
			//-----------
			 else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}	
			//upper side of the image. not include corner
		    //--------------------------------------------
			else if (y==0 && x>b.xini) {
				hop0=result_YUV[pix-1];
				
			}
			
			//corners at upper side of image
			//-------------------------------
			else if (y==0 && x==b.xini) {
				hop0=boundaries_YUV[pix-1];
				
				
				last_small_hop=false;
				hop1=start_hop1;
				
				
			}
			
			//left side of the image. not include corner
			//-----------------------------------------
			else if ((x==0) && (y>b.yini)){
				
				hop0=result_YUV[pix-img.width];
				//hop0=img.boundaries_YUV[0][pix-img.width];
			
				//esta variable puede variar es mejor no ponerla a false
				last_small_hop=false;//31/12/2014 se descomenta
				//last_small_hop=true;//porque no
				//comento esto 12/09/2014
				hop1=start_hop1;//31/12/2014 se descomenta
			}
			//corners of left side of the image.
			//----------------------------------------- 
            else if ((x==0) && (y==b.yini)){
				
            	//System.out.println("NUNCA ENTRA");
				//hop0=result_YUV[pix-img.width];
				hop0=img.boundaries_YUV[0][pix-img.width];
				
				//hop0=(3*img.boundaries_YUV[0][pix-img.width]+4*img.boundaries_YUV[0][pix-img.width+1])/7;
				
				
				last_small_hop=false;
				//last_small_hop=true;//porque no
				
				//comento esto 12/09/2014
				hop1=start_hop1;
				
				
				//hop0=oc;
				
			}
			//left side of the block. not include corner
			//--------------------------------------
			else if ((x==b.xini) && (y>b.yini)){
				//System.out.print("zulu");
					hop0=(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7;
					//hop0=img.boundaries_YUV[0][pix-1];
					//hop0=(4*img.boundaries_YUV[0][pix-1]+4*result_YUV[pix+1-img.width])/8;
					//hop0=result_YUV[pix+1-img.width];//oc;//img.boundaries_YUV[0][pix-1];
					//System.out.println(" el boundary v tiene color:"+img.boundaries_YUV[0][pix-1]+"  at  x:"+(x-1)+" ,y:"+y);
					//hop0=oc;
					
					//esto es nuevo 12/09/2014
					last_small_hop=false;//31/12/2014 se descomenta
					//last_small_hop=true;//porque no
					
					//hop1=max_hop1;
					hop1=start_hop1;//31/12/2014 se descomenta
					//hop0=oc;
					
					
			}
			//up-left corner of block
			//--------------
			else if ((x==b.xini) && (y==b.yini)){
				//System.out.print("zulu");
				hop0=(4*img.boundaries_YUV[0][pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;
				
				//esto es nuevo 12/09/2014
				last_small_hop=false;
				//last_small_hop=true;//porque no
				
				hop1=start_hop1;
				//hop1=8;//start_hop1;
				//hop0=255;//oc;
				//System.out.println("hop0 es "+hop0);
				
				//hop0=oc;
			}
			
			
			
			//right side of block (and right side of image. is the same case). Not includes corner
			//-----------------------------------------------------------------------------------
			else if ((x==b.downsampled_xfin) && (y>b.yini)) {
				//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;	
				
				//nueva formula 03/12/2015
				hop0=(result_YUV[pix-1]+result_YUV[pix-img.width])/2;	
				
				
				//hop0=(4*result_YUV[pix-1]+0*result_YUV[pix-img.width])/4;
			}
			
				
			//upper side of block. not includes right corner
			//-----------------------------------------
		    else if (y==b.yini && x>0 && x!=b.downsampled_xfin) {
		    	hop0=(4*result_YUV[pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;	
		    	//hop0=(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix+1-img.width])/6;
		    	//hop0=(4*result_YUV[pix-1]+1*img.boundaries_YUV[0][pix+1-img.width])/5;
		    	//hop0=result_YUV[pix-1];
		    	
		    	//hop0=oc;
		    }
			//up-right CORNER of block
			//-----------------------------------------------------
		    else if ((x==b.downsampled_xfin) && (y==b.yini)) {
		    	
		    	
		    	
				//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;
				hop0=(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix-img.width])/6;
				
				//aqui no se pueden usar los boundaries porque no estan escalados para este bloque
				//hop0=(4*result_YUV[pix-1]+3*img.boundaries_YUV[0][pix-img.width+1])/7;
				//if (result_YUV[pix-img.width]==0) System.out.println(" ey");
			}
		    	
			//hop0=oc;
			 /*
			 if ((x==b.xini) && (y==b.yini)){
				// hop0=oc;	 
			 }
			 */
			
			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//hop0 is prediction
			//if (hop0>255)hop0=255;
			//else if (hop0<0) hop0=0; 


			// el array PCC habria que escogerlo en funcion de kini
			
			//if (hop0>128) hop1=(int)Math.max(hop1,(float)hop0*0.03f);
			//if (hop0>200 && hop1<5) hop1=5;//(int)Math.max(hop1,(float)hop0*0.03f);
			//System.out.println("hola");
			//positive hops computation
			//-------------------------
			//hop1=8;
			//rmax=40;
			/*max_hop1=10;//4*rmax;
			if (max_hop1>19) max_hop1=19;
			if (max_hop1<6) max_hop1=6;
			*/
			
			//29/12/2014
			//min_hop1=(int)(0.5f+(float)hop0*0.04f);//no puede ser cero
			//if (min_hop1<=2) min_hop1=2;
			//if (hop1<min_hop1) hop1=min_hop1;
			//max_hop1=(int)((float)min_hop1*2.5f);
			//if (max_hop1>=20) max_hop1=19;
			//if (hop1>max_hop1) hop1=max_hop1;
			
			//min_hop1=(int)(0.5f+(float)hop0*0.08f);//no puede ser cero
			//if (min_hop1<6) min_hop1=6;
			//if (hop1<min_hop1) hop1=min_hop1;
			//max_hop1=12;
			
			int inf=pccr[hop1][hop0][rmax][4];;
			int sup=pccr[hop1][hop0][rmax][4];;
			int[] colin= new int[9];
			colin[4]=pccr[hop1][hop0][rmax][4];
			colin[8]=pccr[hop1][hop0][rmax][8];
			colin[0]=pccr[hop1][hop0][rmax][0];
			for (int j=4; j<8;j++)
			   colin[j]=(int)(0+(((float)pccr[hop1][hop0][rmax][j-1]+(float)pccr[hop1][hop0][rmax][j])/2f+((float)pccr[hop1][hop0][rmax][j]+(float)pccr[hop1][hop0][rmax][j+1])/2f)/2f+0.5f);
			for (int j=1; j<4;j++)
			   colin[j]=(int)(0+(((float)pccr[hop1][hop0][rmax][j-1]+(float)pccr[hop1][hop0][rmax][j])/2f+((float)pccr[hop1][hop0][rmax][j]+(float)pccr[hop1][hop0][rmax][j+1])/2f)/2f+0.5f);
			
			//System.out.println( "h4:"+pccr[hop1][hop0][rmax][1]+"    colin4:"+colin[1]);
			
			int colorin=-1;
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
				//for (int j=4;j<=7;j++) {
					try{
					//e2=oc-colin[j];
					e2=oc-pccr[hop1][hop0][rmax][j];
					}catch (Exception e){
						System.out.println("j:"+j+" hop1:"+hop1+"  hop0:"+hop0+"  x:"+x+"  y:"+y+"  b.xini:"+b.xini+" b.yini:"+b.yini+" b.downsampled_xfin:"+b.downsampled_xfin+"  up:"+result_YUV[pix-img.width]+" izq:"+result_YUV[pix-1]+ " debe ser:"+(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7+ "boundaries es:"+img.boundaries_YUV[0][pix-1]);
						System.exit(0);
					}
					if (e2<0) {
						e2=-e2;
						
					}
					if (e2<emin) {hop_number=j;emin=e2;}
					else 
						{
						//ya no hay que seguir
						
						break;
						
						}
				}
				
			if (hop_number==8) sup=(255+pccr[hop1][hop0][rmax][hop_number])/2;//255;//pccr[hop1][hop0][rmax][hop_number];
			else sup=pccr[hop1][hop0][rmax][hop_number+1];
			int sup2=(sup+pccr[hop1][hop0][rmax][hop_number])/2;
			inf=pccr[hop1][hop0][rmax][hop_number-1];
			int inf2=(inf+pccr[hop1][hop0][rmax][hop_number])/2;
			colorin=(sup2+inf2)/2;//adyacente+pccr[hop1][hop0][rmax][hop_number])/2;
			if (hop_number==8) colorin=pccr[hop1][hop0][rmax][hop_number];
			
			colorin=colin[hop_number]+1;	
			}
			
			//negative hops computation
			//-------------------------
			else 
			{
				//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
				//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
				//
				
				//OPTIMIZACION
				//creo que puedo evitar el j=4 NO, no se puede, pues el valor puede estar 
				//mas cerca del hop nulo que del hop -1
				for (int j=4;j>=0;j--) {
					//for (int j=4;j>=1;j--) {
					e2=pccr[hop1][hop0][rmax][j]-oc;
					//e2=colin[j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else 
						{
						//adyacente=pccr[hop1][hop0][rmax][j+1];
						break;
						
						}
				}
			//if (!encontrado) adyacente=0;
			//else 
		//		adyacente=pccr[hop1][hop0][rmax][hop_number];
			//colorin= pccr[hop1][hop0][rmax][hop_number];;//(adyacente+pccr[hop1][hop0][rmax][hop_number])/2;
			
			
			if (hop_number==0)	 inf=pccr[hop1][hop0][rmax][hop_number]/2;//0;//pccr[hop1][hop0][rmax][hop_number];
			else inf=pccr[hop1][hop0][rmax][hop_number-1];
			int inf2=(inf+pccr[hop1][hop0][rmax][hop_number])/2;
			sup=pccr[hop1][hop0][rmax][hop_number+1];
			int sup2=(sup+pccr[hop1][hop0][rmax][hop_number])/2;
			
			//colorin=pccr[hop1][hop0][rmax][hop_number];//(sup2+inf2)/2;
			
			//if (hop_number==8) inf2=pccr[hop1][hop0][rmax][hop_number]- (sup2-pccr[hop1][hop0][rmax][hop_number]);
			//if (inf2<1) inf2=255;
			
			colorin=(sup2+inf2)/2;
			if (hop_number==0) colorin=pccr[hop1][hop0][rmax][hop_number];
			//colorin=pccr[hop1][hop0][rmax][hop_number];
			colorin=colin[hop_number]-1;	
			}

			//prueba
			if ((x==b.xini) && (y==b.yini))
			{
				//System.out.println(" se escoge hop ="+hop_number+ "  oc="+oc+"  hop1 es:"+hop1+ "   asigna "+pccr[hop1][hop0][rmax][hop_number]+ "rmax:"+rmax);
				//hop0=oc;
			}
			//assignment of final color value
			//--------------------------------
			//if (colorin>pccr[hop1][hop0][rmax][4]) colorin=(int)((float)colorin*1.1f+0.5f);
			//if (colorin<pccr[hop1][hop0][rmax][4]) colorin=(int)((float)colorin*1.0f+0.5f);
			
			if (colorin<=1)colorin=1;
			if (colorin>=255)colorin=255;
			
			result_YUV[pix]=colorin;//pccr[hop1][hop0][rmax][hop_number];
			//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
			//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
			hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

			
			
			
			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			//if (hop_number>=6) small_hop=true;
			//if (hop_number<=6 && hop_number>=2) small_hop=true;
			if (hop_number<=5 && hop_number>=3) small_hop=true;
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			
			
			//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			
			
			
			//pix++;            
		}//for x
		//esto es necesario al funcionar con bloques
		//pix+=img.width-b.lx_sc+1;
	}//for y

}//end function

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeDownsampledBlock_R4(Block b, int[] hops,int[] result_YUV, int[] src_YUV,int[] boundaries_YUV)
{

	//System.out.println("colorin");
	
	//block b contains the coordinates to set the limits of this function
	
	
	//some parts of this code are identical to the function quantizeOneHopPerPixel() 
	
	//ESTOS DOS VALORES LUEGO SE AJUSTAN
	int max_hop1=8;// hop1 interval 4..8
	int min_hop1=4;//8;//4;//
	
	//queda mejor 6 que el punto medio (en peppers)
	//int start_hop1=(max_hop1+min_hop1)/2;// hop1 at begining (up-left corner) of each block
	
	//int hop1=start_hop1;
	
	float hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	
	
	int rmax=20;
	//se escoge el juego de hops segun el PR cuantizado
	
	if (b.PRavg==1.0) rmax=30;//bordes abruptos, iconos, ruido 
	//else if (b.PRavg>=0.625) rmax=25;//bordes 1,1,1,0.5
	else if (b.PRavg>=0.75) rmax=30;//bordes 1,1,1,0.5 LO HE CAMBIADO DESDE 25 . hoy es 5/3/2015 30
	else if (b.PRavg>=0.5) rmax=25;//bordes 1,1,1,0.5 NUEVO 2/01/2015
	else if (b.PRavg>=0.25) rmax=22;//25 CAMBIADO el 21/03/2015 asi es mejor
	//else if (b.PRavg>=0.25) rmax=25;//3;//bordes 1,1,1,0.5 NUEVO 5/3/2015 25<--- con 22 es mejor q 25 para lena
	//else if (b.PRavg>=0.3) rmax=25;
	
	//int rmaxini=rmax;
	
	//MEJORA 28/12/2014. alteramos h1max segun la PR y ademas el menor valor es 10 y no 8
	if (b.PRavg==1.0) {max_hop1=16;}//16;
	else if (b.PRavg>=0.75) {max_hop1=14;}//12;
	else if (b.PRavg>=0.5) {max_hop1=12;} // de 12 a 10?
	else if (b.PRavg>=0.25) {max_hop1=10;} 
	//else if (b.PRavg>=0.125) max_hop1=8; 
	//
	//else if (b.PRavg>=0.125) max_hop1=10;
	else {max_hop1=8;}// suave
	
	
    int start_hop1=(max_hop1+min_hop1)/2;// hop1 at begining (up-left corner) of each block
    
	int hop1=start_hop1;
	boolean colin_activo=true;
	//boolean small_colin=false;;
	//boolean colin_activo=false;
	//if (b.PRavg>0.25f) colin_activo=true; 
	
	
	/*
	float pppavg=0;
	for (int i=0;i<2;i++)
		for (int j=0;j<4;j++)
			if (b.ppp[i][j]<=2f) colin_activo=false;;
		*/	
	//pppavg=pppavg/4f; //max es 8  1 min es 1/8=0.125
	//boolean colinoff=false;
	
	/*
	float pppavg=0;
	for (int i=0;i<2;i++)
		for (int j=0;j<4;j++)
			pppavg+=b.ppp[i][j];
	pppavg=pppavg/8f; //max es 8  1 min es 1/8=0.125
	boolean colinoff=false;
	*/
	//if (pppavg==1.0f) colinoff=true;//NUEVO
	//rmax=(int)(1000*b.PRavg/pppavg);
	//if (rmax>30) rmax=30;
	//if (rmax<20) rmax=20;
	//System.out.println("rmax"+rmax+ "     pppavg="+pppavg+"     pr:"+b.PRavg);
	
	//int hop1b=hop1;
	//min_hop1=4;
	
	//PARA HACER PRUEBAS CON LHE BASICO SE PUEDE:
	//PONER GRID A UN SOLO BLOQUE.
	//HACER ESTAS IGUALDADES:
	//max_hop1=8;//10;
	//rmax=20;//25; //si pongo 20, lena gana 43 db
	
	
	
	//System.out.println("PRavg:"+b.PRavg);
	//rmax=20;//valor original
	//max_hop1=19;
	//start_hop1=6;
	
	//System.out.println(" dato:"+img.width);
	int pix=b.yini*img.width+b.xini;//initial pixel possition        
	
	
	
	boolean last_small_hop=false;// indicates if last hop is small
	
	for (int y=b.yini;y<=b.downsampled_yfin;y++)  {
		
		//ponemos a hop minimo al comienzo de scan line de bloque
		//esto es bueno sobre todo en bloques lisos
		
		//debo de usar un array mejor
		//hop1=min_hop1;//NUEVO 10/09/2014
	//	int last_hop=4;
		for (int x=b.xini;x<=b.downsampled_xfin;x++)  {

			pix=y*img.width+x;
			oc=src_YUV[pix];
			//System.out.println(" A last_small_hop:"+last_small_hop);
			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			
			//inner pixels ( mostly of them. that is the reason for considering the 1st option)
			//---------------------------------------------------------------------------------
			 if ((y>b.yini) &&(x>b.xini) && x!=b.downsampled_xfin){
				hop0=(4f*result_YUV[pix-1]+3f*result_YUV[pix+1-img.width])/7f;	
				
				//System.out.println("a");
				//hop0=(4*result_YUV[pix-1]+4*result_YUV[pix-img.width])/8;	
				
				//hop0=(241*result_YUV[pix-1]+170*result_YUV[pix+1-img.width])/411;	
				//	System.out.println(" result_YUV[pix-1]:"+result_YUV[pix-1]+"  result_YUV[pix+1-img.width]: "+result_YUV[pix+1-img.width]);
			}
			//initial pix
			//-----------
			 else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
				//System.out.println("b");
			}	
			//upper side of the image. not include corner
		    //--------------------------------------------
			else if (y==0 && x>b.xini) {
				hop0=result_YUV[pix-1];
				//System.out.println("c");
			}
			
			//corners at upper side of image
			//-------------------------------
			else if (y==0 && x==b.xini) {
				hop0=boundaries_YUV[pix-1];
				
				//CAMBIO HOY
				last_small_hop=false;
				hop1=start_hop1;
				
				
				//System.out.println("d");
				
			}
			
			//left side of the image. not include corner
			//-----------------------------------------
			else if ((x==0) && (y>b.yini)){
			//	System.out.println("e");
				hop0=result_YUV[pix-img.width];
				//hop0=img.boundaries_YUV[0][pix-img.width];
			
				//esta variable puede variar es mejor no ponerla a false
				last_small_hop=false;//31/12/2014 se descomenta
				//last_small_hop=true;//porque no
				//comento esto 12/09/2014
				hop1=start_hop1;//31/12/2014 se descomenta
			}
			//corners of left side of the image.
			//----------------------------------------- 
          else if ((x==0) && (y==b.yini)){
        	//  System.out.println("f");
          	//System.out.println("NUNCA ENTRA");
				//hop0=result_YUV[pix-img.width];
				hop0=img.boundaries_YUV[0][pix-img.width];
				
				//hop0=(3*img.boundaries_YUV[0][pix-img.width]+4*img.boundaries_YUV[0][pix-img.width+1])/7;
				
				
				last_small_hop=false;
				//last_small_hop=true;//porque no
				
				//comento esto 12/09/2014
				hop1=start_hop1;
				
				
				//hop0=oc;
				
			}
			//left side of the block. not include corner
			//--------------------------------------
			else if ((x==b.xini) && (y>b.yini)){
				//System.out.println("g");
				//System.out.print("zulu");
					hop0=(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7;
					//hop0=img.boundaries_YUV[0][pix-1];
					//hop0=(4*img.boundaries_YUV[0][pix-1]+4*result_YUV[pix+1-img.width])/8;
					//hop0=result_YUV[pix+1-img.width];//oc;//img.boundaries_YUV[0][pix-1];
					//System.out.println(" el boundary v tiene color:"+img.boundaries_YUV[0][pix-1]+"  at  x:"+(x-1)+" ,y:"+y);
					//hop0=oc;
					
					//esto es nuevo 12/09/2014
					last_small_hop=false;//31/12/2014 se descomenta
					//last_small_hop=true;//porque no
					
					//hop1=max_hop1;
					hop1=start_hop1;//31/12/2014 se descomenta
					//hop0=oc;
					
					
			}
			//up-left corner of block
			//--------------
			else if ((x==b.xini) && (y==b.yini)){
			//	System.out.println("h");
				//System.out.print("zulu");
				hop0=(4*img.boundaries_YUV[0][pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;
				
				//esto es nuevo 12/09/2014
				last_small_hop=false;
				//last_small_hop=true;//porque no
				
				hop1=start_hop1;
				//hop1=8;//start_hop1;
				//hop0=255;//oc;
				//System.out.println("hop0 es "+hop0);
				
				//hop0=oc;
			}
			
			
			
			//right side of block (and right side of image. is the same case). Not includes corner
			//-----------------------------------------------------------------------------------
			else if ((x==b.downsampled_xfin) && (y>b.yini)) {
				//System.out.println("i");
				//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;	
				
				//nueva formula 03/12/2015
				hop0=(result_YUV[pix-1]+result_YUV[pix-img.width])/2;	
				
				
				//hop0=(4*result_YUV[pix-1]+0*result_YUV[pix-img.width])/4;
			}
			
				
			//upper side of block. not includes right corner
			//-----------------------------------------
		    else if (y==b.yini && x>0 && x!=b.downsampled_xfin) {
		    //	System.out.println("j");
		    	hop0=(4*result_YUV[pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;	
		    	//hop0=(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix+1-img.width])/6;
		    	//hop0=(4*result_YUV[pix-1]+1*img.boundaries_YUV[0][pix+1-img.width])/5;
		    	//hop0=result_YUV[pix-1];
		    	
		    	//hop0=oc;
		    }
			//up-right CORNER of block
			//-----------------------------------------------------
		    else if ((x==b.downsampled_xfin) && (y==b.yini)) {
		    	//System.out.println("k");
		    	
		    	
				//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;
				hop0=(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix-img.width])/6;
				
				//aqui no se pueden usar los boundaries porque no estan escalados para este bloque
				//hop0=(4*result_YUV[pix-1]+3*img.boundaries_YUV[0][pix-img.width+1])/7;
				//if (result_YUV[pix-img.width]==0) System.out.println(" ey");
			}
		    	
			//hop0=oc;
			 /*
			 if ((x==b.xini) && (y==b.yini)){
				// hop0=oc;	 
			 }
			 */
			// System.out.println(" B last_small_hop:"+last_small_hop);
			 
			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//hop0 is prediction
			//if (hop0>255)hop0=255;
			//else if (hop0<0) hop0=0; 


			// el array PCC habria que escogerlo en funcion de kini
			
			//if (hop0>128) hop1=(int)Math.max(hop1,(float)hop0*0.03f);
			//if (hop0>200 && hop1<5) hop1=5;//(int)Math.max(hop1,(float)hop0*0.03f);
			//System.out.println("hola");
			//positive hops computation
			//-------------------------
			//hop1=8;
			//rmax=40;
			/*max_hop1=10;//4*rmax;
			if (max_hop1>19) max_hop1=19;
			if (max_hop1<6) max_hop1=6;
			*/
			
			//29/12/2014
			//min_hop1=(int)(0.5f+(float)hop0*0.04f);//no puede ser cero
			//if (min_hop1<=2) min_hop1=2;
			//if (hop1<min_hop1) hop1=min_hop1;
			//max_hop1=(int)((float)min_hop1*2.5f);
			//if (max_hop1>=20) max_hop1=19;
			//if (hop1>max_hop1) hop1=max_hop1;
			
			//min_hop1=(int)(0.5f+(float)hop0*0.08f);//no puede ser cero
			//if (min_hop1<6) min_hop1=6;
			//if (hop1<min_hop1) hop1=min_hop1;
			//max_hop1=12;
			
			int hop0i=(int)( hop0+0.5f);
			
			//int inf=pccr[hop1][hop0i][rmax][4];;
			//int sup=pccr[hop1][hop0i][rmax][4];;
			//===================================================================================================
			//OJO LA TECNICA DEL "COLIN" CONSISTE EN ASIGNAR EL PUNTO MEDIO DEL INTERVALO EN LUGAR DEL HOP
			//DA MEJOR RESULTADO PERO LIMITA EL MAYOR PSNR ALCANZABLE
			//ADEMAS HAGO UN PEQUEÑO AJUSTE SUMANDO O RESTANDO 1 A LOS POSITIVOS Y NEGATIVOS RESPECTIVAMENTE
			//POR ULTIMO EL HOP NULO (el 4) SE QUEDA COMO ESTA. PARA ELLO PRIMERO RESTO 1 YA QUE LUEGO SE SUMA 1
			//===================================================================================================
			//
			 
			//colin_activo=false;
			//if (colinoff) colin_activo=false;
			//if (b.PRavg>0.4)colin_activo=false;
			int[] colin= new int[9];
			colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
			colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
			colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
			colin[3]=pccr[hop1][hop0i][rmax][3];
			colin[5]=pccr[hop1][hop0i][rmax][5];
		//	colin[8]=pccr[hop1][hop0i][rmax][8]+(pccr[hop1][hop0i][rmax][8]-pccr[hop1][hop0i][rmax][7])/2;
			//colin[0]=pccr[hop1][hop0i][rmax][0]-(pccr[hop1][hop0i][rmax][1]-pccr[hop1][hop0i][rmax][0])/2;
			//esto imprime el ultimo hop number
			//System.out.println("hop1:"+hop1+ "   hop0:"+hop0+" oc:"+oc+ "   hop_number:"+hop_number);
			
			int startcolin=6;
			int endcolin=3;
			//if (!small_colin) {startcolin=6;endcolin=3;}
			
			for (int j=startcolin; j<8;j++)
			   //colin[j]=(int)(1f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
				{colin[j]=(int)(+1f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
			//colin[j]=(colin[j]+pccr[hop1][hop0i][rmax][j])/2;
				}
				
				//colin[j]=(int)((((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
			for (int j=1; j<endcolin;j++)
			   //colin[j]=(int)(-0.5f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
			{	colin[j]=(int)(-0.5f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
		       //colin[j]=(int)((((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
			//colin[j]=(colin[j]+pccr[hop1][hop0i][rmax][j])/2;
			}
			//colin[7]=pccr[hop1][hop0i][rmax][7];
			//colin[1]=pccr[hop1][hop0i][rmax][1];
			//System.out.println( "h4:"+pccr[hop1][hop0][rmax][1]+"    colin4:"+colin[1]);
			
			int colorin=-1;
			
			if (oc-hop0>=0) //hop0 es el flotante. 
			{
				for (int j=4;j<=8;j++) {
					e2=oc-pccr[hop1][hop0i][rmax][j];
					//e2=oc-colin[j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}
			
			//negative hops computation
			//-------------------------
			else 
			{
				//OPTIMIZACION
				//creo que puedo evitar el j=4 NO, no se puede, pues el valor puede estar 
				//mas cerca del hop nulo que del hop -1
				for (int j=4;j>=0;j--) {
					e2=pccr[hop1][hop0i][rmax][j]-oc;
					//e2=oc-colin[j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}
			
			//if (b.PRavg>=0.5) colin_activo=false;
				//colin_activo=false;
			if (colin_activo )//&& hop1==max_hop1)
			//if (colin_activo && hop1==max_hop1)
				{
				/*
				if (hop_number>=5 )//&& hop1==max_hop1)
				{
				if (Math.abs(oc-colin[hop_number])>= Math.abs(oc-colin[hop_number-1]))	hop_number--;	
				//System.out.println("holaaaaaaa");
				}
				else if (hop_number<=3 )//&& hop1==max_hop1)
				{
				if (Math.abs(oc-colin[hop_number])>= Math.abs(oc-colin[hop_number+1]))	hop_number++;	
				
				}
				*/
				
				colorin=colin[hop_number];
			}
			
			else colorin=pccr[hop1][hop0i][rmax][hop_number];
			//System.out.println("hop1:"+hop1+ "   hop0:"+hop0+" oc:"+oc+ "hop_number:"+hop_number+ "color:"+colorin);
			//if (hop_number==0) System.exit(0);
			
			if (colorin<1)colorin=1;
			if (colorin>255)colorin=255;
			
			result_YUV[pix]=colorin;//pccr[hop1][hop0][rmax][hop_number];
			//result_YUV[pix]=pccr[hop1][hop0i][rmax][hop_number];
			//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
			//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
			hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

			//System.out.println("hola");
			
			
			//tunning hop1 for the next hop
			//-------------------------------
			//System.out.println("minhop1:"+min_hop1+ "    hop_number:"+hop_number);//+ "   hop0:"+hop0+" oc:"+oc+ "hop_number:"+hop_number);
			boolean small_hop=false;
		//	boolean medium_hop=false;
			//if (hop_number>=6) small_hop=true;
			//if (hop_number<=6 && hop_number>=2) small_hop=true;
			//if (hop_number<=5 && hop_number>=3) small_hop=true;
			if (hop_number<=4 && hop_number>=4) small_hop=true; //HE CAMBIADO ESTO!!!! ahora es solo 4 antes era 345
			//else if (hop_number<=5 && hop_number>=3) medium_hop=true;
			//if (hop_number<=6 && hop_number>=2) medium_hop=true;   

		
			
			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				//colin_activo=true;
				//small_colin=true;
				// colin_activo=true;
				//rmax=20;
				//if (rmax<20) rmax=20;
				//hop1=min_hop1;
				//hop1b=hop1b-1;
				if (hop1<min_hop1) 
					{hop1=min_hop1;
					
					}
				//hop1=max_hop1;
				//if (hop1b<1 ) hop1b=1;
			}
			
			
			else 
				
			
			{ //colin_activo=false;
				//small_colin=false;
				hop1=max_hop1;//(min_hop1+max_hop1)/2;
				//rmax=rmaxini;
				//hop1b=hop1;
			}
			
			//System.out.println(" lastsmall:"+last_small_hop+"    small:"+small_hop);
			
			//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			
			//last_hop=hop_number;
			
			//pix++;            
		}//for x
		//esto es necesario al funcionar con bloques
		//pix+=img.width-b.lx_sc+1;
	}//for y

}//end function

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void initPreComputations()
{
	//This is the function to initialize pre-computed hop values
	System.out.println(" initializing LHE pre-computed hops");
	
	// This is a cache of ratio ("r") to avoid pow functions
	float[][][][] cache_ratio; //meaning : [+/-][h1][h0][rmax]
	// we will compute cache ratio for different rmax values, although we will use 
	// finally only rmax=25 (which means 2.5f). This function is generic and experimental
	// and this is the cause to compute more things than needed.

	
	// h1 value belongs to [4..10]
	// Given a certain h1 value, and certaing h0 luminance, the "luminance hop" of hop "i" is stored here:
	// hn[absolute h1 value][luminance of h0 value]
	// for example,  h4 (null hop) is always 0, h1 is always hop1 (from 4 to 10), h2 is hop1*r,
	// however this is only the hop. the final luminance of h2 is luminace=(luminance of h0)+hop1*r    
	// hn is, therefore, the array of "hops" in terms of luminance but not the final luminances.
	float[][] h0,h1,h2,h6,h7,h8;// h0,h1,h2 are negative hops. h6,h7,h8 are possitive hops
	
	int h1range=20;//in fact h1range is only from 4 to 10. However i am going to fill more possible values in the pre-computed hops
	h1range=65;
	
	h0=new float[h1range][256];
	h1=new float[h1range][256];
	h2=new float[h1range][256];
	// in the center is located h3=h4-hop1, and h5=h4+hop1, but I dont need array for them
	h6=new float[h1range][256];
	h7=new float[h1range][256];
	h8=new float[h1range][256];
	
	
	// pccr is the value of the REAL cache. This is the cache to be used in the LHE quantizer
	// sorry...i dont remenber why this cache is named "pccr" :) instead of "cache"
	// this array takes into account the ratio
	// meaning: pccr [h1][luminance][ratio][hop_index]
	pccr=new int[h1range][256][50][9];
	
	//cache of ratios ( to avoid Math.pow operation)
	//---------------------------------------------
	cache_ratio=new float[2][h1range][256][50];
	
	
	for (int hop0=0;hop0<=255;hop0++) {
		for (int hop1=1;hop1<h1range;hop1++)
		{
			float percent_range=0.8f;//0.8f;//0.8f;//0.8 is the  80%
			
			//this bucle allows computations for different values of rmax from 20 to 40. 
			//however, finally only one value (25) is used in LHE
			for (int rmax=20;rmax<=40;rmax++)
			{
			// r values for possitive hops	
			cache_ratio[0][(int)(hop1)][hop0][rmax]=(float)Math.pow(percent_range*(255-hop0)/(hop1), 1f/3f);
			
			// r' values for negative hops
			cache_ratio[1][(int)(hop1)][hop0][rmax]=(float)Math.pow(percent_range*(hop0)/(hop1), 1f/3f);
			
			
			
			
			
			
			// control of limits
			float max=(float)rmax/10f;// if rmax is 25 then max is 2.5f;
			if (cache_ratio[0][(int)(hop1)][hop0][rmax]>max)cache_ratio[0][hop1][hop0][rmax]=max;
			if (cache_ratio[1][(int)(hop1)][hop0][rmax]>max)cache_ratio[1][hop1][hop0][rmax]=max;
			
			/*
			float min=1.0f;//esto sobra
			if (cache_ratio[0][(int)(hop1)][hop0][rmax]<min)cache_ratio[0][hop1][hop0][rmax]=min;
			if (cache_ratio[1][(int)(hop1)][hop0][rmax]<min)cache_ratio[1][hop1][hop0][rmax]=min;
			*/
		
			}
		}
		
		//assignment of precomputed hop values, for each ofp value
		//--------------------------------------------------------
		for (int hop1=1;hop1<h1range;hop1++)
		{
			//finally we will only use one value of rmax rmax=30, 
			//however we compute from r=2.0f (rmax=20) to r=4.0f (rmax=40)
			for (int rmax=20;rmax<=40;rmax++)
			{
		        //get r value for possitive hops from cache_ratio	
			float ratio_pos=cache_ratio[0][hop1][hop0][rmax];
			
			//get r' value for negative hops from cache_ratio
			float ratio_neg=cache_ratio[1][hop1][hop0][rmax];

			// COMPUTATION OF LUMINANCES:
			// luminance of possitive hops
			h6[hop1][hop0] = hop1*ratio_pos;
			h7[hop1][hop0] = h6[hop1][hop0]*ratio_pos;
			h8[hop1][hop0] = h7[hop1][hop0]*ratio_pos;

			//luminance of negative hops	                        
			h2[hop1][hop0] =hop1*ratio_neg;
			h1[hop1][hop0] = h2[hop1][hop0]*ratio_neg;
			h0[hop1][hop0] = h1[hop1][hop0]*ratio_neg;

			
			//final color component ( luminance or chrominance). depends on hop1
			//from most negative hop (pccr[hop1][hop0][0]) to most possitive hop (pccr[hop1][hop0][8])
			//--------------------------------------------------------------------------------------
			pccr[hop1][hop0][rmax][0]= hop0  - (int) h0[hop1][hop0] ; if (pccr[hop1][hop0][rmax][0]<=0) { pccr[hop1][hop0][rmax][0]=1;}
			pccr[hop1][hop0][rmax][1]= hop0  - (int) h1[hop1][hop0]; if (pccr[hop1][hop0][rmax][1]<=0) {pccr[hop1][hop0][rmax][1]=1;}
			pccr[hop1][hop0][rmax][2]= hop0  - (int) h2[hop1][hop0]; if (pccr[hop1][hop0][rmax][2]<=0) { pccr[hop1][hop0][rmax][2]=1;}
			pccr[hop1][hop0][rmax][3]=hop0-hop1;if (pccr[hop1][hop0][rmax][3]<=0) pccr[hop1][hop0][rmax][3]=1;
			pccr[hop1][hop0][rmax][4]=hop0; //null hop
			
			  //check of null hop value. This control is used in "LHE advanced", where value of zero is forbidden
			  //in basic LHE there is no need for this control
			  if (pccr[hop1][hop0][rmax][4]<=0) pccr[hop1][hop0][rmax][4]=1; //null hop
			  if (pccr[hop1][hop0][rmax][4]>255) pccr[hop1][hop0][rmax][4]=255;//null hop
			
			pccr[hop1][hop0][rmax][5]= hop0+hop1;if (pccr[hop1][hop0][rmax][5]>255) pccr[hop1][hop0][rmax][5]=255;
			pccr[hop1][hop0][rmax][6]= hop0  + (int) h6[hop1][hop0]; if (pccr[hop1][hop0][rmax][6]>255) {pccr[hop1][hop0][rmax][6]=255;}
			pccr[hop1][hop0][rmax][7]= hop0  + (int) h7[hop1][hop0]; if (pccr[hop1][hop0][rmax][7]>255) {pccr[hop1][hop0][rmax][7]=255;}
			pccr[hop1][hop0][rmax][8]= hop0  + (int) h8[hop1][hop0]; if (pccr[hop1][hop0][rmax][8]>255) {pccr[hop1][hop0][rmax][8]=255;}
			
			}//rmax
		}//hop1

	}//hop0
	
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//**************************************************************************************************
/**
* This is a very fast LHE quantization function, 
* 
* image luminance array is the input for this function. 
*   This luminance array is suposed to be stored at img.YUV[0][pix]; 
*   Image luminance array is not modified
* 
* hops numbering:
*   >negative hops: 0,1,2,3
*   >null hop: 4
*   >positive hops: 5,6,7,8
* 
* result_YUV is output array. 
* 
* 
* @param hops : this array will be filled by this function with resulting hops
* @param result_YUV: this array will be filled by this funcion with resulting luminance values
*/
public void quantizeOneHopPerPixel(int[] hops,int[] result_YUV)
{
	
	
	int max_hop1=10; //hop1 interval 4..10
	int min_hop1=4;// minimum value of hop1 is 4 
	int start_hop1=(max_hop1+min_hop1)/2;
	
	
	int hop1=start_hop1;
	int hop0=0; // predicted luminance signal
	int emin;//error of predicted signal
	int hop_number=4;//pre-selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small. used for h1 adaptation mechanism
	int rmax=25;
	
	//rmax=20;//experimento
	//min_hop1=1;
	
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;x++)  {

			//original image luminances are in the array img.YUV[0]
			// chrominance signals are stored in img.YUV[1] and img.YUV[2] but they are not
			// used in this function, designed for learning LHE basics
			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				last_small_hop=false;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			


			//---------------------COLIN
			/*
			rmax=25;
			if (hop0>255) hop0=255;
			//System.out.println("hop0:"+hop0);
			int hop0i=pccr[hop1][hop0][rmax][4];
			int[] colin= new int[9];
		    for (int t=0;t<9;t++) colin[t]=pccr[hop1][hop0i][rmax][t];
			
		    int startcolin=6;
			int endcolin=3;
			
			for (int j=startcolin; j<8;j++)
				{colin[j]=(int)(+1f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
				}
				
			for (int j=1; j<endcolin;j++)
			{	colin[j]=(int)(-0.5f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
		    }
			
			//----------------------END COLIN
			*/
			
			
			//hops computation. 
			//error initial values 
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//positive hops computation
			//-------------------------
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
					e2=oc-pccr[hop1][hop0][rmax][j];
					//e2=oc-colin[j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
				for (int j=4;j>=0;j--) {
					e2=pccr[hop1][hop0][rmax][j]-oc;
					//e2=colin[j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}

			
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][25][hop_number];//final luminance
			//result_YUV[pix]=colin[hop_number];
			hops[pix]=hop_number; //final hop value

			//tunning hop1 for the next hop ( "h1 adaptation")
			//------------------------------------------------
			boolean small_hop=false;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			
			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			pix++;            
		}//for x
	}//for y
	
	
	
}//end function


//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//**************************************************************************************************
/**
* This is a very fast LHE quantization function, 
* 
* image luminance array is the input for this function. 
*   This luminance array is suposed to be stored at img.YUV[0][pix]; 
*   Image luminance array is not modified
* 
* hops numbering:
*   >negative hops: 0,1,2,3
*   >null hop: 4
*   >positive hops: 5,6,7,8
* 
* result_YUV is output array. 
* 
* 
* @param hops : this array will be filled by this function with resulting hops
* @param result_YUV: this array will be filled by this funcion with resulting luminance values
*/
public void quantizeOneHopPerPixel_prueba(int[] hops,int[] result_YUV)
{
	
	
	int max_hop1=10; //hop1 interval 4..10
	int min_hop1=4;// minimum value of hop1 is 4 
	int start_hop1=(max_hop1+min_hop1)/2;
	
	
	int hop1=start_hop1;
	int hop0=0; // predicted luminance signal
	int emin;//error of predicted signal
	int hop_number=4;//pre-selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small. used for h1 adaptation mechanism
	int rmax=25;
	
	for (int y=0;y<img.height;y++)  {
		
		float delta=0;
		for (int x=0;x<img.width;x++)  {

			//original image luminances are in the array img.YUV[0]
			// chrominance signals are stored in img.YUV[1] and img.YUV[2] but they are not
			// used in this function, designed for learning LHE basics
			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;
				//hop0=result_YUV[pix-1];//img.width];
				
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				last_small_hop=false;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;		
				
				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			
			
			hop0=hop0+(int)delta;
			if (hop0<1) hop0=1;
			if (hop0>255) hop0=255;
			delta= hop0-oc;
			//---------------------COLIN
			/*
			rmax=25;
			if (hop0>255) hop0=255;
			//System.out.println("hop0:"+hop0);
			int hop0i=pccr[hop1][hop0][rmax][4];
			int[] colin= new int[9];
		    for (int t=0;t<9;t++) colin[t]=pccr[hop1][hop0i][rmax][t];
			
		    int startcolin=6;
			int endcolin=3;
			
			for (int j=startcolin; j<8;j++)
				{colin[j]=(int)(+1f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
				}
				
			for (int j=1; j<endcolin;j++)
			{	colin[j]=(int)(-0.5f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
		    }
			
			//----------------------END COLIN
			*/
			
			
			//hops computation. 
			//error initial values 
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//positive hops computation
			//-------------------------
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
					e2=oc-pccr[hop1][hop0][rmax][j];
					//e2=oc-colin[j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
				for (int j=4;j>=0;j--) {
					e2=pccr[hop1][hop0][rmax][j]-oc;
					//e2=colin[j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}

			
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][25][hop_number];//final luminance
			//result_YUV[pix]=colin[hop_number];
			hops[pix]=hop_number; //final hop value

			//tunning hop1 for the next hop ( "h1 adaptation")
			//------------------------------------------------
			boolean small_hop=false;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			
			
			//prueba
			
			//if (hop_number>4) delta=4;
			//else if (hop_number<4) delta=-4;
			//else delta=0;
			
			///fin prueba
			
			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			pix++;            
		}//for x
	}//for y
	
	
	
}//end function
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeOneHopPerPixelBin(int[] hops,int[] result_YUV)
{
	
	
	int max_hop1=16;//6; //hop1 interval 4..10
	int min_hop1=4;//4;// minimum value of hop1 is 4 
	int start_hop1=(max_hop1+min_hop1)/2;
	
	
	int hop1=start_hop1;
	int hop0=0; // predicted luminance signal
	int emin;//error of predicted signal
	int hop_number=4;//pre-selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small. used for h1 adaptation mechanism
	//int rmax=25;
	
	
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;x++)  {

			//original image luminances are in the array img.YUV[0]
			// chrominance signals are stored in img.YUV[1] and img.YUV[2] but they are not
			// used in this function, designed for learning LHE basics
			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				last_small_hop=false;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			//hops computation. 
			//error initial values 
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 
			
			//positive hops computation
			//-------------------------
			int h=0;
			int hfinal=h;
			hop_number=4;
			
			if (oc-hop0>hop1/2) // esta division es una rotacion binaria
			{
				
				if (hop0+hop1>255) {h=255-hop0;} else h=hop1;
				hop_number=5;
				emin=oc-(hop0+h);if (emin<0) emin=-emin;
				for (int j=6;j<=8;j++) {
					int salto=h*2; if (hop0+salto>255) {salto=255-hop0;
					//;System.out.println("hola caracola!!!");System.exit(0);
					}
					//if (hop0+salto>255) break;
					e2=oc-(hop0+salto);//pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2>=emin) break;
					
					//if (e2<4) break; //new
					
					emin=e2;
					hop_number=j;
					h=salto;
				}
				
				hfinal=h;
				
			}
			//negative hops computation
			//-------------------------
			else if (hop0-oc>hop1/2) 
			{
				
				
				if (hop0-hop1<0) h=hop0; else h=hop1;
				hop_number=3;
				emin=hop0-h-oc;if (emin<0) emin=-emin;

				for (int j=2;j>=0;j--) {
					int salto= h*2;if (hop0-salto<0) salto=hop0;
					//if (hop0-salto<0) break;
					e2=hop0-salto-oc;//pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2>=emin) break;
					
					//if (e2<4) break; //new
					
					emin=e2;
					hop_number=j;
					h=salto;	
				}
				
				hfinal=-h;
			
			}

			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=hop0+hfinal;
					
			hops[pix]=hop_number; //final hop value

			//tunning hop1 for the next hop ( "h1 adaptation")
			//------------------------------------------------
			boolean small_hop=false;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			
			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			pix++;            
		}//for x
	}//for y
	
}
//*******************************************************************************


//*******************************************************************************
public void quantizeOneHopPerPixelBin_old(int[] hops,int[] result_YUV)
{
	
	
	int max_hop1=16;//6; //hop1 interval 4..10
	int min_hop1=4;//4;// minimum value of hop1 is 4 
	int start_hop1=(max_hop1+min_hop1)/2;
	
	
	int hop1=start_hop1;
	int hop0=0; // predicted luminance signal
	int emin;//error of predicted signal
	int hop_number=4;//pre-selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small. used for h1 adaptation mechanism
	//int rmax=25;
	
	
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;x++)  {

			//original image luminances are in the array img.YUV[0]
			// chrominance signals are stored in img.YUV[1] and img.YUV[2] but they are not
			// used in this function, designed for learning LHE basics
			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				last_small_hop=false;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			//hops computation. 
			//error initial values 
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 
			
			//positive hops computation
			//-------------------------
			int h=0;
			int hfinal=h;
			hop_number=4;
			
			if (oc-hop0>hop1/2) // esta division es una rotacion binaria
			{
				
				if (hop0+hop1>255) {h=255-hop0;} else h=hop1;
				hop_number=5;
				emin=oc-(hop0+h);if (emin<0) emin=-emin;
				for (int j=6;j<=8;j++) {
					int salto=h*2; if (hop0+salto>255) {salto=255-hop0;
					//;System.out.println("hola caracola!!!");System.exit(0);
					}
					//if (hop0+salto>255) break;
					e2=oc-(hop0+salto);//pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2>=emin) break;
					
					emin=e2;
					hop_number=j;
					h=salto;
				}
				
				hfinal=h;
				
			}
			//negative hops computation
			//-------------------------
			else if (hop0-oc>hop1/2) 
			{
				
				
				if (hop0-hop1<0) h=hop0; else h=hop1;
				hop_number=3;
				emin=hop0-h-oc;if (emin<0) emin=-emin;

				for (int j=2;j>=0;j--) {
					int salto= h*2;if (hop0-salto<0) salto=hop0;
					//if (hop0-salto<0) break;
					e2=hop0-salto-oc;//pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2>=emin) break;
					
					emin=e2;
					hop_number=j;
					h=salto;	
				}
				
				hfinal=-h;
			
			}

			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=hop0+hfinal;
					
			hops[pix]=hop_number; //final hop value

			//tunning hop1 for the next hop ( "h1 adaptation")
			//------------------------------------------------
			boolean small_hop=false;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			
			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			pix++;            
		}//for x
	}//for y
	
}
//*******************************************************************************
public void quantizeOneHopPerPixelBin_nok(int[] hops,int[] result_YUV)
{
	
	
	int max_hop1=16;//6; //hop1 interval 4..10
	int min_hop1=4;//4;// minimum value of hop1 is 4 
	int start_hop1=(max_hop1+min_hop1)/2;
	
	
	int hop1=start_hop1;
	int hop0=0; // predicted luminance signal
	int emin;//error of predicted signal
	int hop_number=4;//pre-selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small. used for h1 adaptation mechanism
	//int rmax=25;
	
	
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;x++)  {

			//original image luminances are in the array img.YUV[0]
			// chrominance signals are stored in img.YUV[1] and img.YUV[2] but they are not
			// used in this function, designed for learning LHE basics
			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				last_small_hop=false;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			//hops computation. 
			//error initial values 
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 
			
			//positive hops computation
			//-------------------------
			int h=0;
			int hfinal=h;
			hop_number=4;
			
			
			int diff=oc-hop0;
			int cosa=hop1;
			int i=0;
			
			hop1=8;
			diff=diff/2;
			diff=diff/2;
			diff=diff/2;
			while (diff>0)//solo entra si diff es positivo
			{
				i++;
				diff=diff/2;
				hop1=hop1*2;
			}
			if (i>0) {hop_number=4+i;hfinal=hop1;}
			hop1=8;
			if (oc-hop0>hop1/2) // esta division es una rotacion binaria
			{
			
			}
			
		
			//negative hops computation
			//-------------------------
			else if (hop0-oc>hop1/2) 
			{
				
				
				if (hop0-hop1<0) h=hop0; else h=hop1;
				hop_number=3;
				emin=hop0-h-oc;if (emin<0) emin=-emin;

				for (int j=2;j>=0;j--) {
					int salto= h*2;if (hop0-salto<0) salto=hop0;
					//if (hop0-salto<0) break;
					e2=hop0-salto-oc;//pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2>=emin) break;
					
					emin=e2;
					hop_number=j;
					h=salto;	
				}
				
				hfinal=-h;
			
			}

			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=hop0+hfinal;
					
			hops[pix]=hop_number; //final hop value

			//tunning hop1 for the next hop ( "h1 adaptation")
			//------------------------------------------------
			boolean small_hop=false;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			
			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			pix++;            
		}//for x
	}//for y
	
}

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeOneHopPerPixel_R_LHE2_ok1(int[] hops,int[] result_YUV)
{
	System.out.println("quantizying...");
	/*
	int iterations=1000;
	long start_time = System.currentTimeMillis();
	for (int xy=0;xy<iterations;xy++){
		*/
	
	int max_hop1=10;//8;//8;//16;//8;// hop1 interval 4..8
	int min_hop1=4;//4;// 
	int start_hop1=(max_hop1+min_hop1)/2;
	
	
	int hop1=start_hop1;//max_hop1;
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small

	int totales=0;
	
	
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;){
				//x++)  {

			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	

				//	System.out.println(" result_YUV[pix-1]:"+result_YUV[pix-1]+"  result_YUV[pix+1-img.width]: "+result_YUV[pix+1-img.width]);
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				
				
				last_small_hop=false;
				
				
			
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			
			
			
			

			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//positive hops computation
			//-------------------------
			int rmax=25;//40;
	
			
			
			
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
			
					e2=oc-pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
			
				for (int j=4;j>=0;j--) {
				
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}

			//29/12/2014
			
			
			//colin
			
			rmax=25;
			int hop0i=pccr[hop1][hop0][rmax][4];
			int[] colin= new int[9];
			colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
			colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
			colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
			colin[3]=pccr[hop1][hop0i][rmax][3];
			colin[5]=pccr[hop1][hop0i][rmax][5];
		
		
			
			
			
			
		
			
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][25][hop_number];
			
			hops[pix]=hop_number; 

			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			
			
			//segun h1 saltamos
			int step=hop1/min_hop1;
			int pixant=pix;
			//float alfa=(result_YUV[pix]-result_YUV[pixant])/(step-1);
					
	//		System.out.println(" b step"+step+"  x:"+x);
			if (x+step> img.width-1) step= img.width -x-1;
			//for (int i2=pixant+1; i2<pix;i2++)
			for (int i2=pix+1; i2<=pix+step;i2++)
				{
				//result_YUV[i2]=(int)(result_YUV[i2-1]+alfa);
				result_YUV[i2]=result_YUV[i2-1];
				hops[i2]=hops[i2-1];//habria que eliminarlo
				//totales++;
				}
			x+=step+1;
			totales++;
//System.out.println("step"+step+"  x:"+x);
			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			//pix++;
			pix+=step+1;
			
		}//for x
	}//for y
	
	/*
	}//iterations
	
	long end_time = System.currentTimeMillis();
	double total_time=end_time-start_time;
	double tpp=total_time/(img.width*img.height*iterations);
	double tpi=total_time/(iterations);
	System.out.println("tiempo_total:"+total_time+"  tpp:"+tpp+" ms"+ " tpi:"+tpi +" ms");
	*/
	System.out.println("quantization done");
	System.out.println("totales="+totales);
	
}//end function

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeOneHopPerPixel_R_LHE2_old(int[] hops,int[] result_YUV)
{
	System.out.println("quantizying...");
	/*
	int iterations=1000;
	long start_time = System.currentTimeMillis();
	for (int xy=0;xy<iterations;xy++){
		*/
	
	int max_hop1=10;//8;//8;//16;//8;// hop1 interval 4..8
	int min_hop1=4;//4;// 
	int start_hop1=(max_hop1+min_hop1)/2;
	
	
	int hop1=start_hop1;//max_hop1;
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small

	int totales=0;
	int pixant=0;
	int stepant=1;
	int step=1;
	
	boolean rectificando=false;
	
	int counter_resta=0;
	
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;){
				//x++)  {

			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			
			
			
			if ((y>0) &&(x>0) && x!=img.width-1){
				if (y%2==0) hop0= result_YUV[pix-1];//esto es mejorable
				else hop0= result_YUV[pix-img.width];
				//hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	

				//hop0=result_YUV[pix-1];
				//	System.out.println(" result_YUV[pix-1]:"+result_YUV[pix-1]+"  result_YUV[pix+1-img.width]: "+result_YUV[pix+1-img.width]);
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				
				
				last_small_hop=false;
				
				
			
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			
			
			
			

			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//positive hops computation
			//-------------------------
			int rmax=25;//40;
	
			
			
			
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
			
					e2=oc-pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
			
				for (int j=4;j>=0;j--) {
				
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}

			//29/12/2014
			
			
			//colin
			
			rmax=25;
			int hop0i=pccr[hop1][hop0][rmax][4];
			int[] colin= new int[9];
			colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
			colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
			colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
			colin[3]=pccr[hop1][hop0i][rmax][3];
			colin[5]=pccr[hop1][hop0i][rmax][5];
		
		
			
			
			// correccion
			if (y%2==1)
			{
				if (hops[pix-img.width]==4)
					hop_number=4;
					counter_resta+=1;
			}
			
		
			
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][25][hop_number];
			
			hops[pix]=hop_number; 

			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			
			
			//segun h1 saltamos
			stepant=step;
			
			//step=(int)(0.5+max_hop1/hop1);
			step=(int)(0.5+max_hop1/hop1);
			//step=(max_hop1*max_hop1)/(hop1*hop1);
			//step=step*2;
			//System.out.println("step"+step);
					
	//		System.out.println(" b step"+step+"  x:"+x);
			if (x+step> img.width-1) step= img.width -x-1;
			//if (step==0)System.exit(0);
			float alfa=0;
					
			
			//propago el pixel actual para poder hacer la siguiente prediccion
			for (int i2=pix+1; i2<=pix+step;i2++)
				{
				
				result_YUV[i2]=result_YUV[i2-1];
				hops[i2]=0;//hops[i2-1];//habria que eliminarlo. no hay hops no los va a haber en estos pixels
				
				}
			//interpol linea
			//if (x<450)
			
			
			
			if ( pix-pixant>1)
			{
			if (pix-pixant>1) alfa=(result_YUV[pix]-result_YUV[pixant])/(pix-pixant);
			else alfa=0;
			if (pix % 512 ==0) alfa=0; //esto es lo mismo que decir si x llega al final de la scanline
			
			
			
			
			int umbral=3;
			if ((hops[pix]>=4+umbral || hops[pix]<=4-umbral) && (rectificando==false))
			{
				if (pix-pixant>1 && rectificando==false)
				{
					rectificando=true;
					int dif=(pix-pixant)/2;
					x=x-dif;
					pix=pix-dif;
					continue;
				}
				//if (x>pixant%512+1) continue;
				//rectificando=true
				//x=(pix%512+pixant%512+1)/2;
				//if (x>pixant%512+1) continue;
			}
			rectificando=false;
			umbral=300;
			
			//INterpolacion lineal del tramo anterior
			if (hops[pix]<4+umbral && hops[pix]>4-umbral)
			{
			 for (int i2=pixant+1; i2<pix;i2++)
			
				{
				//result_YUV[i2]=(int)(result_YUV[i2-1]+alfa);
				result_YUV[i2]=(int)(result_YUV[i2-1]+ alfa);
				}
			}
			//
			umbral=3;
			
			if (hops[pix]>=4+umbral || hops[pix]<=4-umbral)
			{
				int mitad=(pix-pixant)/2;
				mitad=mitad+pixant+1;
				//alfa=alfa*2;
				//mitad=pix-1; 
				 for (int i2=pixant+1; i2<mitad;i2++)
				
					{
					//result_YUV[i2]=(int)(result_YUV[i2-1]+alfa);
					result_YUV[i2]=(int)(result_YUV[i2-1]);
					}
				 //mitad=pixant+1;
				 //if (mitad>pixant+1)
				 
				 for (int i2=mitad; i2<pix;i2++)
						
					{
					 
					//result_YUV[i2]=(int)(result_YUV[i2-1]+alfa);
					result_YUV[i2]=(int)(result_YUV[pix]);
					}
				
				}	
			}
			
			//correccion
			if (step<=2 && stepant>2 )
			{
			//	stepant=1;
			//	step=-stepant;
			}
			
			x+=step+1;
			totales++;
			pixant=pix;
			
//System.out.println("step"+step+"  x:"+x);
			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			//pix++;
			pix+=step+1;
			
		}//for x
	}//for y
	
	/*
	}//iterations
	
	long end_time = System.currentTimeMillis();
	double total_time=end_time-start_time;
	double tpp=total_time/(img.width*img.height*iterations);
	double tpi=total_time/(iterations);
	System.out.println("tiempo_total:"+total_time+"  tpp:"+tpp+" ms"+ " tpi:"+tpi +" ms");
	*/
	System.out.println("quantization done");
	System.out.println("totales="+totales);
	
}//end function
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeOneHopPerPixel_LHE2_experimento10(int[] hops,int[] result_YUV)
{
	
	//OJO ESTA FUNCION AL SALTAR DE MUESTRA EN MUESTRA, PARA NO DEJAR EL ARRAY DE HOPS SIN RELLENAR
	//LO RELLENA y ENTONCES los BPP no se calculan bien, PUES LUEGO EL PROCESO HUFFMAN RECORRE LOS HOPS
	//Y PARA QUE NO ENCUENTRE HOPS NULOS LO HE RELLENADO pERO ES EXPERIMENTAL, POCO A POCO
	
	System.out.println("quantizying LHE2...");
	/*
	int iterations=1000;
	long start_time = System.currentTimeMillis();
	for (int xy=0;xy<iterations;xy++){
		*/
	
	int max_hop1=10;//8;//8;//16;//8;// hop1 interval 4..8
	int min_hop1=4;//4;// 
	int start_hop1=(max_hop1+min_hop1)/2;
	
	
	int hop1=start_hop1;//max_hop1;
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small

	int totales=0;
	int pixant=0;
	int stepant=1;
	int step=1;
	
	boolean rectificando=false;
	
	
	int[] steparray=new int [512*512];
	
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;){
				//x++)  { // he quitado el x++ porque ahora salta de step en step, siendo step variable

			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				last_small_hop=false;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			

			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//positive hops computation
			//-------------------------
			int rmax=25;//40; razon geometrica
			
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
			
					e2=oc-pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
			
				for (int j=4;j>=0;j--) {
				
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}

			//29/12/2014
			
			
			//colin . esto es para recolocar el valor de luminancia en el centro del intervalo
			//asimetrico. En realidad puede estar integrado en cache 
			
			rmax=25;
			int hop0i=pccr[hop1][hop0][rmax][4];
			int[] colin= new int[9];
			colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
			colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
			colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
			colin[3]=pccr[hop1][hop0i][rmax][3];
			colin[5]=pccr[hop1][hop0i][rmax][5];
		
		
			
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][25][hop_number];
			
			hops[pix]=hop_number; 

			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			int hop1_prev=hop1;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			
			
			
			//segun h1 saltamos
			stepant=step;
			step=(int)(0.5+max_hop1/hop1);
			//System.out.println("step"+step);
			
			/*
			if (hop1==4) {int u=4;if (step>u) step =step -1; if (step<u) step=step+1;}
			if (hop1==5) {int u=3;if (step>u) step =step -1; if (step<u) step=step+1;}
			if (hop1==6) {int u=2;if (step>u) step =step -1; if (step<u) step=step+1;}
			if (hop1>=7) {int u=1;if (step>u) step =step -1; if (step<u) step=step+1;}
			*/
			
			
			if (hop1==4) step =4;
			if (hop1==5) step =3;
			if (hop1==6) step =2;
			if (hop1>=7) step =1;
			
			//steparray[pix]=step;
			
			if (pix> img.width ) if (step> steparray[pix-img.width] ){step=step-1;}else if (step<steparray[pix-img.width]) step=step+1;//steparray[pix]=step+1;}
			//if ((x & 15) > ((x+step) & 15)) step=1;
			//if (pix> img.width ) if (step >1+steparray[pix-img.width] ){step=step-2;}
			
			//if (pix> img.width ) if (step> steparray[pix-img.width] ){step=step-1;}
			if ((x & 31) > ((x+step) & 31)) step=1;
			steparray[pix]=step;
			//if (pix> img.width && steparray[pix-img.width]==1 ){step=1;}
			
			//step=1;
			
			if ((x & 31) > ((x+step) & 31)) step=1;
			//if ((x & 15) > ((x+step) & 15)) step=1;
			
			//if (x<290 && x+step>290) step=1;
			
			System.out.println("step:"+step+ "    x:"+x);		
	
			if (x+step> img.width-1) step= img.width -x-1;
			
			
			//if (step==0) {System.out.println ("ey");x=img.width+1;continue;}
			
			float alfa=0;
					
			
			//propago el pixel actual para poder hacer la siguiente prediccion.
			//ojo, la siguiente, que aun no estamos en pix+step. esto es "adelantarnos"
			//se puede programar mucho mejor y optimizar pero esto es una prueba de concepto
			for (int i2=pix+1; i2<=pix+step;i2++)
				{
				
				result_YUV[i2]=result_YUV[i2-1];
				hops[i2]=0;//hops[i2-1];//habria que eliminarlo. no hay hops no los va a haber en estos pixels
				
				
				//new
				steparray[i2]=step;
				}
		
		
			//si hay mas de un pixel entre el pix anterior y este podemos optar por
			//una interpolacion lineal o bien no hacer nada y es como dejarlo en vecino cercano
			//pues la propagacion ya hace de vecino
			if ( pix-pixant>1)
			{
			alfa=(result_YUV[pix]-result_YUV[pixant])/(pix-pixant);
			if (pix % 512 ==0) alfa=0; //esto es lo mismo que decir si x llega al final de la scanline 
			
			
			 for (int i2=pixant+1; i2<pix;i2++)
			
				{
				//si descomento, paso a bilineal
				 result_YUV[i2]=(int)(result_YUV[i2-1]+ alfa);
				}
			
			//
				
			}
			
			if (step==0) step=1;//solo ocurre al final de la scanline
			
			x+=step;//+1;
			
			totales++;
			pixant=pix;
			
			//System.out.println("step"+step+"  x:"+x);
			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			//pix++;
			pix+=step;//+1;
			
		}//for x
	}//for y
	
	/*
	}//iterations
	
	long end_time = System.currentTimeMillis();
	double total_time=end_time-start_time;
	double tpp=total_time/(img.width*img.height*iterations);
	double tpi=total_time/(iterations);
	System.out.println("tiempo_total:"+total_time+"  tpp:"+tpp+" ms"+ " tpi:"+tpi +" ms");
	*/
	System.out.println("quantization done");
	System.out.println("-------------------------");
	System.out.println("   samples totales="+totales);
	System.out.println("-------------------------");
	
}//end function

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeOneHopPerPixel_LHE2_experimento20(int[] hops,int[] result_YUV)
{
	
	//OJO ESTA FUNCION AL SALTAR DE MUESTRA EN MUESTRA, PARA NO DEJAR EL ARRAY DE HOPS SIN RELLENAR
	//LO RELLENA y ENTONCES los BPP no se calculan bien, PUES LUEGO EL PROCESO HUFFMAN RECORRE LOS HOPS
	//Y PARA QUE NO ENCUENTRE HOPS NULOS LO HE RELLENADO pERO ES EXPERIMENTAL, POCO A POCO
	
	System.out.println("quantizying LHE2...");
	/*
	int iterations=1000;
	long start_time = System.currentTimeMillis();
	for (int xy=0;xy<iterations;xy++){
		*/
	
	int max_hop1=10;//8;//8;//16;//8;// hop1 interval 4..8
	int min_hop1=4;//4;// 
	int start_hop1=(max_hop1+min_hop1)/2;
	
	
	int hop1=start_hop1;//max_hop1;
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small

	int totales=0;
	int pixant=0;
	int stepant=1;
	int step=1;
	
	boolean rectificando=false;
	int step_countdown=1; //contador de pixeles dentro de un pixel que mide step
	int back_counter=0;//cuantas veces se corrige
	boolean back_flag=false;//ultima muestra hicimos back
	
	int[] steparray=new int [512*512];
	
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;){
				//x++)  { // he quitado el x++ porque ahora salta de step en step, siendo step variable

			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				last_small_hop=false;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			

			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//positive hops computation
			//-------------------------
			int rmax=25;//40; razon geometrica
			
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
			
					e2=oc-pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
			
				for (int j=4;j>=0;j--) {
				
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}

			//29/12/2014
			
			
			//colin . esto es para recolocar el valor de luminancia en el centro del intervalo
			//asimetrico. En realidad puede estar integrado en cache 
			
			rmax=25;
			int hop0i=pccr[hop1][hop0][rmax][4];
			int[] colin= new int[9];
			colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
			colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
			colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
			colin[3]=pccr[hop1][hop0i][rmax][3];
			colin[5]=pccr[hop1][hop0i][rmax][5];
		
		
			
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][25][hop_number];
			
			hops[pix]=hop_number; 

			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			int hop1_prev=hop1;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			
			if( (small_hop) && (last_small_hop))  {
				
				
				
				//hop1=hop1-1;
				//hop1=hop1-1;//step;
			    hop1=hop1-stepant;
				//hop1=hop1-step;
				if (hop1<min_hop1) hop1=min_hop1;
				
				
			} 
			else {
				hop1=max_hop1;
			}
			
			//hop1=min_hop1;
			
					
			//step_countdown-=1;
			
			
			
			  stepant=step;			  
			  if (hop1==4) step =4;
			  else if (hop1==5) step =3;
			  else if (hop1==6) step =2;
			  else if (hop1>=7) step =1;
			  
			  if (stepant>step && step==1)
		      {//begin logica back
			    x=x-stepant+1;
			    pixant=pix-stepant;
			    pix=pix-stepant+1;			    
			    back_counter++;
			    step=1;
			    stepant=1;
			    continue;
		      }//end logica back
			 
			
			//System.out.println("step:"+step+ "    x:"+x);		
			if (x+step> img.width-1) {step=img.width -x-1;}
			//if (x+step> img.width-1) {step=1;step_countdown=1;}
			
			//if (step==0) {System.out.println ("ey");x=img.width+1;continue;}
			
			
					
			
			//lo que sigue lo hago si step_countdown==step)
			//if (step_countdown==step)//primer pixel del intervalo cubierto por la muestra
			{	
			float alfa=0;	
			//propago el pixel actual para poder hacer la siguiente prediccion.
			//ojo, la siguiente, que aun no estamos en pix+step. esto es "adelantarnos"
			//se puede programar mucho mejor y optimizar pero esto es una prueba de concepto
			for (int i2=pix+1; i2<=pix+step;i2++)
				{
				
				result_YUV[i2]=result_YUV[i2-1];
				//hops[i2]=0;//hops[i2-1];//habria que eliminarlo. no hay hops no los va a haber en estos pixels
				
				
				//new
				//steparray[i2]=step;
				}
		
		
			//si hay mas de un pixel entre el pix anterior y este podemos optar por
			//una interpolacion lineal o bien no hacer nada y es como dejarlo en vecino cercano
			//pues la propagacion ya hace de vecino
			if ( pix-pixant>1)
			{
			alfa=(result_YUV[pix]-result_YUV[pixant])/(pix-pixant);
			if (pix % 512 ==0) alfa=0; //esto es lo mismo que decir si x llega al final de la scanline 
			
			
			 for (int i2=pixant+1; i2<pix;i2++)
			
				{
				//si descomento, paso a bilineal
				 result_YUV[i2]=(int)(result_YUV[i2-1]+ alfa);
				}
			
			//
				
			}
			
			
			totales++;
			pixant=pix;
			}//step_countdown
			
			if (step==0) step=1;//solo ocurre al final de la scanline
			
			x+=step;//step;//1;//step;//+1;
			
			//totales++;
			//pixant=pix;
			
			//System.out.println("step"+step+"  x:"+x);
			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			//pix++;
			pix+=step;//step;//1;//step;//+1;//step
			
		}//for x
	}//for y
	
	/*
	}//iterations
	
	long end_time = System.currentTimeMillis();
	double total_time=end_time-start_time;
	double tpp=total_time/(img.width*img.height*iterations);
	double tpi=total_time/(iterations);
	System.out.println("tiempo_total:"+total_time+"  tpp:"+tpp+" ms"+ " tpi:"+tpi +" ms");
	*/
	System.out.println("quantization done");
	System.out.println("-------------------------");
	System.out.println("   samples totales="+totales);
	System.out.println("   back_counter="+back_counter);
	System.out.println("-------------------------");
	
}//end function

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

public void quantizeOneHopPerPixel_LHE2(int[] hops,int[] result_YUV)
{
	
	//OJO ESTA FUNCION AL SALTAR DE MUESTRA EN MUESTRA, PARA NO DEJAR EL ARRAY DE HOPS SIN RELLENAR
	//LO RELLENA y ENTONCES los BPP no se calculan bien, PUES LUEGO EL PROCESO HUFFMAN RECORRE LOS HOPS
	//Y PARA QUE NO ENCUENTRE HOPS NULOS LO HE RELLENADO pERO ES EXPERIMENTAL, POCO A POCO
	
	System.out.println("quantizying LHE2...");
	/*
	int iterations=1000;
	long start_time = System.currentTimeMillis();
	for (int xy=0;xy<iterations;xy++){
		*/
	
	int max_hop1=10;//8;//8;//16;//8;// hop1 interval 4..8
	int min_hop1=4;//4;// 
	int start_hop1=(max_hop1+min_hop1)/2;
	
	
	int hop1=start_hop1;//max_hop1;
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small

	int totales=0;
	int pixant=0;
	int stepant=1;
	int step=1;
	
	boolean rectificando=false;
	int step_countdown=1; //contador de pixeles dentro de un pixel que mide step
	int back_counter=0;//cuantas veces se corrige
	boolean back_flag=false;//ultima muestra hicimos back
	
	int[] steparray=new int [512*512];
	
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;){
				//x++)  { // he quitado el x++ porque ahora salta de step en step, siendo step variable

			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				last_small_hop=false;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			

			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//positive hops computation
			//-------------------------
			int rmax=25;//40; razon geometrica
			
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
			
					e2=oc-pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
			
				for (int j=4;j>=0;j--) {
				
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}

			//29/12/2014
			
			
			//colin . esto es para recolocar el valor de luminancia en el centro del intervalo
			//asimetrico. En realidad puede estar integrado en cache 
			
			rmax=25;
			int hop0i=pccr[hop1][hop0][rmax][4];
			int[] colin= new int[9];
			colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
			colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
			colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
			colin[3]=pccr[hop1][hop0i][rmax][3];
			colin[5]=pccr[hop1][hop0i][rmax][5];
		
		
			
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][25][hop_number];
			
			hops[pix]=hop_number; 

			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			int hop1_prev=hop1;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			
			if( (small_hop) && (last_small_hop))  {
				
				
				
				//hop1=hop1-1;
				//hop1=hop1-1;//step;
			    hop1=hop1-stepant;
				//hop1=hop1-step;
				if (hop1<min_hop1) hop1=min_hop1;
				
				
			} 
			else {
				hop1=max_hop1;
			}
			
			//hop1=min_hop1;
			
					
			//step_countdown-=1;
			
			
			
			  stepant=step;			  
			  if (hop1==4) step =4;
			  else if (hop1==5) step =3;
			  else if (hop1==6) step =2;
			  else if (hop1>=7) step =1;
			  
			  if (stepant>step && step==1)
		      {//begin logica back
			    x=x-stepant+1;
			    pixant=pix-stepant;
			    pix=pix-stepant+1;			    
			    back_counter++;
			    step=1;
			    stepant=1;
			    continue;
		      }//end logica back
			 
			
			//System.out.println("step:"+step+ "    x:"+x);		
			if (x+step> img.width-1) {step=img.width -x-1;}
			//if (x+step> img.width-1) {step=1;step_countdown=1;}
			
			//if (step==0) {System.out.println ("ey");x=img.width+1;continue;}
			
			
					
			
			//lo que sigue lo hago si step_countdown==step)
			//if (step_countdown==step)//primer pixel del intervalo cubierto por la muestra
			{	
			float alfa=0;	
			//propago el pixel actual para poder hacer la siguiente prediccion.
			//ojo, la siguiente, que aun no estamos en pix+step. esto es "adelantarnos"
			//se puede programar mucho mejor y optimizar pero esto es una prueba de concepto
			for (int i2=pix+1; i2<=pix+step;i2++)
				{
				
				result_YUV[i2]=result_YUV[i2-1];
				//hops[i2]=0;//hops[i2-1];//habria que eliminarlo. no hay hops no los va a haber en estos pixels
				
				
				//new
				//steparray[i2]=step;
				}
		
		
			//si hay mas de un pixel entre el pix anterior y este podemos optar por
			//una interpolacion lineal o bien no hacer nada y es como dejarlo en vecino cercano
			//pues la propagacion ya hace de vecino
			if ( pix-pixant>1)
			{
			alfa=(result_YUV[pix]-result_YUV[pixant])/(pix-pixant);
			if (pix % 512 ==0) alfa=0; //esto es lo mismo que decir si x llega al final de la scanline 
			
			
			 for (int i2=pixant+1; i2<pix;i2++)
			
				{
				//si descomento, paso a bilineal
				 result_YUV[i2]=(int)(result_YUV[i2-1]+ alfa);
				}
			
			//
				
			}
			
			
			totales++;
			pixant=pix;
			}//step_countdown
			
			if (step==0) step=1;//solo ocurre al final de la scanline
			
			x+=step;//step;//1;//step;//+1;
			
			//totales++;
			//pixant=pix;
			
			//System.out.println("step"+step+"  x:"+x);
			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			//pix++;
			pix+=step;//step;//1;//step;//+1;//step
			
		}//for x
	}//for y
	
	/*
	}//iterations
	
	long end_time = System.currentTimeMillis();
	double total_time=end_time-start_time;
	double tpp=total_time/(img.width*img.height*iterations);
	double tpi=total_time/(iterations);
	System.out.println("tiempo_total:"+total_time+"  tpp:"+tpp+" ms"+ " tpi:"+tpi +" ms");
	*/
	System.out.println("quantization done");
	System.out.println("-------------------------");
	System.out.println("   samples totales="+totales);
	System.out.println("   back_counter="+back_counter);
	System.out.println("-------------------------");
	
}//end function


//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeOneHopPerPixel_LHE2_experimento30(int[] hops,int[] result_YUV)
{
	System.out.println("quantizying...hello");
	
	int max_hop1=10;//8;//8;//16;//8;// hop1 interval 4..8
	int min_hop1=4;//4;// 
	int start_hop1=(max_hop1+min_hop1)/2;
	
	
	int hop1=start_hop1;//max_hop1;
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small

	
	float error_center=0;
	float error_avg=0;
	
	

	int counter_resta=0;
	
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;x++)  {

			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				
				if (y%2==0) hop0= result_YUV[pix-1];//esto es mejorable
				else hop0= result_YUV[pix-img.width];
				
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	

				//hop0= result_YUV[pix-img.width];
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				
				
				last_small_hop=false;
				
				
				//hop1=max_hop1;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			
			//paeth
			/*
			int A=0;
			int B=0;
			int C=0;
			if (x>0) A=result_YUV[pix-1];
			if (y>0) B=result_YUV[pix-img.width];
			if (x>0 && y>0) C=result_YUV[pix-img.width-1];
			if (x==0) {A=B;C=B;}
			if (y==0) {B=A;C=A;}
			if (x==0 && y==0)hop0=oc;
			else
			{
				int pred=A+B-C;
				int a1=Math.abs(A-pred);
				int b1=Math.abs(B-pred);
				int c1=Math.abs(C-pred);
				if (a1<=b1 && a1<=c1) hop0=A;
				else if (b1<=a1 && b1<=c1) hop0=B;
				else hop0=C;
				//System.out.println("paeth");
			}
			*/
			
			

			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//hop0 is prediction
			//if (hop0>255)hop0=255;
			//else if (hop0<0) hop0=0; 

			//max_hop1=16;//(int)((float)hop0 *0.02f +0.5f);
			//System.out.println("hop1max:"+max_hop1);
			//positive hops computation
			//-------------------------
			int rmax=25;//40;
			//hop1=8;
			
			//min_hop1=(int)(0.5f+(float)hop0*0.04f);//no puede ser cero
			//if (min_hop1<4) min_hop1=4;
			//if (hop1<min_hop1) hop1=min_hop1;
			
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
				//for (int j=4;j<=5;j++) {
					e2=oc-pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
				//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
				//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
				for (int j=4;j>=0;j--) {
				//	for (int j=4;j>=3;j--) {
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}

			//29/12/2014
			
			
			//colin
			
			rmax=25;
			int hop0i=pccr[hop1][hop0][rmax][4];
			int[] colin= new int[9];
			colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
			colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
			colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
			colin[3]=pccr[hop1][hop0i][rmax][3];
			colin[5]=pccr[hop1][hop0i][rmax][5];
		
			int startcolin=6;
			int endcolin=3;
			
			for (int j=startcolin; j<8;j++)
				{colin[j]=(int)(+1f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
				}
				
			for (int j=1; j<endcolin;j++)
			{	colin[j]=(int)(-0.5f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
		    }
			
			
			// correccion
			int mini=3;
			int maxi=5;
			img.LHE2_removed_pix[pix]=oc;
			
					    if (y%2==1 && x>1)
						//if (y>1 && x<511 && x>1)	
						{
						   if (hops[pix-img.width]>=mini  && hops[pix-img.width]<=maxi)
							   
					    	//if (hops[pix-1]==4)
							{
								hop_number=4;//hops[pix-img.width];//mg.width];//4;
								counter_resta+=1;
								img.LHE2_removed_pix[pix]=0;
							}
						}
			
					    if (y>0 && x>0)
					    	if (x%2==1 )
					    //if (x%2==1 && y%2!=1)
							//if (y>1 && x<511 && x>1)	
							{
							   //if (hops[pix-img.width]==4 && hops[pix-1]==4)
							  // if (hops[pix-img.width]==4)// && hops[pix-1]==4)
								   if (hops[pix-1]>=mini && hops[pix-1]<=maxi)
								{
									hop_number=4;
									counter_resta+=1;
									img.LHE2_removed_pix[pix]=0;
								}
							}
				
			
		
			
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][25][hop_number];
			//result_YUV[pix]=colin[hop_number];//pccr[hop1][hop0][25][hop_number];
			
			//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
			//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
			hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

			
			//calculo de errores medios
			//---------------------------
			error_center+=(oc-result_YUV[pix]);
			error_avg+=Math.abs((oc-result_YUV[pix]));
			
			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			//if (hop_number>=6) small_hop=true;
			//if (hop_number<=6 && hop_number>=2) small_hop=true;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			pix++;            
		}//for x
	}//for y
	
	/*
	}//iterations
	
	long end_time = System.currentTimeMillis();
	double total_time=end_time-start_time;
	double tpp=total_time/(img.width*img.height*iterations);
	double tpi=total_time/(iterations);
	System.out.println("tiempo_total:"+total_time+"  tpp:"+tpp+" ms"+ " tpi:"+tpi +" ms");
	*/
	System.out.println("quantization done");
	
	System.out.println("center of  error:"+error_center/(img.width*img.height));
	System.out.println("average of  error:"+error_avg/(img.width*img.height));
	System.out.println("----------------------------------------------------------");
	System.out.println("counter_resta="+counter_resta);
	
	
}//end function



//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeOneHopPerPixel_LHE2_experimento33(int[] hops,int[] result_YUV)
{
	System.out.println("quantizying...hello");
	
	int max_hop1=10;//8;//8;//16;//8;// hop1 interval 4..8
	int min_hop1=4;//4;// 
	int start_hop1=(max_hop1+min_hop1)/2;
	
	
	int hop1=start_hop1;//max_hop1;
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small

	
	float error_center=0;
	float error_avg=0;
	
	

	int counter_resta=0;
	
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;x++)  {

			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				
				//if (y%2==0) hop0= result_YUV[pix-1];//esto es mejorable
				//else hop0= result_YUV[pix-img.width];
				
				int deltax=0;
				if (x>1)
				{
					deltax=result_YUV[pix-1]-result_YUV[pix-2];
					if (deltax>hop1) deltax=0;//max_hop1;
				}
				hop0=result_YUV[pix-1];//+ deltax/3;
				if (hop0>255) hop0=255;
				if (hop0<1) hop0=1;
				//hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2; //fast approach (good enough)
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7; //slow approach	

				//hop0= result_YUV[pix-img.width];
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				
				
				last_small_hop=false;
				
				
				//hop1=max_hop1;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			
			//paeth
			/*
			int A=0;
			int B=0;
			int C=0;
			if (x>0) A=result_YUV[pix-1];
			if (y>0) B=result_YUV[pix-img.width];
			if (x>0 && y>0) C=result_YUV[pix-img.width-1];
			if (x==0) {A=B;C=B;}
			if (y==0) {B=A;C=A;}
			if (x==0 && y==0)hop0=oc;
			else
			{
				int pred=A+B-C;
				int a1=Math.abs(A-pred);
				int b1=Math.abs(B-pred);
				int c1=Math.abs(C-pred);
				if (a1<=b1 && a1<=c1) hop0=A;
				else if (b1<=a1 && b1<=c1) hop0=B;
				else hop0=C;
				//System.out.println("paeth");
			}
			*/
			
			

			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//hop0 is prediction
			//if (hop0>255)hop0=255;
			//else if (hop0<0) hop0=0; 

			//max_hop1=16;//(int)((float)hop0 *0.02f +0.5f);
			//System.out.println("hop1max:"+max_hop1);
			//positive hops computation
			//-------------------------
			int rmax=25;//40;
			//hop1=8;
			
			//min_hop1=(int)(0.5f+(float)hop0*0.04f);//no puede ser cero
			//if (min_hop1<4) min_hop1=4;
			//if (hop1<min_hop1) hop1=min_hop1;
			
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
				//for (int j=4;j<=5;j++) {
					e2=oc-pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
				//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
				//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
				for (int j=4;j>=0;j--) {
				//	for (int j=4;j>=3;j--) {
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}

			//29/12/2014
			
			
			//colin
			
			rmax=25;
			int hop0i=pccr[hop1][hop0][rmax][4];
			int[] colin= new int[9];
			colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
			colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
			colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
			colin[3]=pccr[hop1][hop0i][rmax][3];
			colin[5]=pccr[hop1][hop0i][rmax][5];
		
			int startcolin=6;
			int endcolin=3;
			
			for (int j=startcolin; j<8;j++)
				{colin[j]=(int)(+1f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
				}
				
			for (int j=1; j<endcolin;j++)
			{	colin[j]=(int)(-0.5f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
		    }
			
			
			// correccion
			int mini=2;
			int maxi=6;
			img.LHE2_removed_pix[pix]=255;//oc;
			
			
			boolean quitar=true;
			if (quitar)
			{
			//int up=4;	
			//int left=4;
			//if (y>0 ) up=hops[pix-img.width];
			//if (x>0) left=hops[pix-1];
			//int criteria= y%2;
			//int criteriax=1;
			boolean removed=false;
			
					    if (y%2==1 && x>1)
						//if (y>1 && x<511 && x>1)	
						{
						   if (hops[pix-img.width]>=mini  && hops[pix-img.width]<=maxi)
							   
					    	//if (hops[pix-1]==4)
							{
							   
							   
								hop_number=4;//hops[pix-img.width];//mg.width];//4;
								
								hop_number=4;//(up+left)/2;
								counter_resta+=1;
								img.LHE2_removed_pix[pix]=0;
								removed=true;
							}
						}
			
					    if (y>0 && x>0 && !removed)
					    	if (x%2==1 )
					    	//	if (x%2==criteriax )
					    //if (x%2==1 && y%2!=1)
							//if (y>1 && x<511 && x>1)	
							{
							   //if (hops[pix-img.width]==4 && hops[pix-1]==4)
							  // if (hops[pix-img.width]==4)// && hops[pix-1]==4)
								   if (hops[pix-1]>=mini && hops[pix-1]<=maxi)
								{
									hop_number=4;
									hop_number=4;
									 counter_resta+=1;
									img.LHE2_removed_pix[pix]=0;
									removed=true;
								}
							}
				
			}
		
			
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][25][hop_number];
			
			//if (removed)
			{
				//result_YUV[pix]=result_YUV[pix-img.width]
			}
			
			
			//result_YUV[pix]=colin[hop_number];//pccr[hop1][hop0][25][hop_number];
			
			//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
			//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
			hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

			
			//calculo de errores medios
			//---------------------------
			error_center+=(oc-result_YUV[pix]);
			error_avg+=Math.abs((oc-result_YUV[pix]));
			
			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			//if (hop_number>=6) small_hop=true;
			//if (hop_number<=6 && hop_number>=2) small_hop=true;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			pix++;            
		}//for x
	}//for y
	
	/*
	}//iterations
	
	long end_time = System.currentTimeMillis();
	double total_time=end_time-start_time;
	double tpp=total_time/(img.width*img.height*iterations);
	double tpi=total_time/(iterations);
	System.out.println("tiempo_total:"+total_time+"  tpp:"+tpp+" ms"+ " tpi:"+tpi +" ms");
	*/
	System.out.println("quantization done");
	
	System.out.println("center of  error:"+error_center/(img.width*img.height));
	System.out.println("average of  error:"+error_avg/(img.width*img.height));
	System.out.println("----------------------------------------------------------");
	System.out.println("counter_resta="+counter_resta);
	
	LHE2_resta=counter_resta;
	postfilter_LHE2(hops,result_YUV);
	
	
}//end function
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void postfilter_LHE2(int[] hops,int[] result_YUV)
{

	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;x++)  {
			
			int pix=x+y*img.width;
			//check eliminado
			
			if (x>0 && y>0 && x<511 && y<511)
			{	
			if (img.LHE2_removed_pix[pix]==0)
			{
				
					
				if (y%2!=1)//tengo izquierdo y derecho disponible
				  {
				 result_YUV[pix]=(result_YUV[pix-1]+result_YUV[pix+1])/2;
				 //img.LHE2_removed_pix[pix]=(img.LHE2_removed_pix[pix-1]+img.LHE2_removed_pix[pix+1])/2;
			      }
				else
				  {
					if (x%2==0)
					{result_YUV[pix]=(result_YUV[pix-img.width]+result_YUV[pix+img.width])/2;
					//img.LHE2_removed_pix[pix]=255;//(img.LHE2_removed_pix[pix-1]+img.LHE2_removed_pix[pix+1])/2;
					}
					else
					{
					result_YUV[pix]=(result_YUV[pix-img.width]+result_YUV[pix+img.width])/2;
					}
				  }
				
			}
			}//
			
		}
		}
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeOneHopPerPixel_LHE2_experimento35(int[] hops,int[] result_YUV)
{
	System.out.println("quantizying...hello");
	
	int max_hop1=10;//8;//8;//16;//8;// hop1 interval 4..8
	int min_hop1=4;//4;// 
	int start_hop1=(max_hop1+min_hop1)/2;
	
	
	int hop1=start_hop1;//max_hop1;
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small

	
	float error_center=0;
	float error_avg=0;
	
	

	int counter_resta=0;
	
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;x++)  {

			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				
				//if (y%2==0) hop0= result_YUV[pix-1];//esto es mejorable
				//else hop0= result_YUV[pix-img.width];
				
				int deltax=0;
				if (x>1)
				{
					deltax=result_YUV[pix-1]-result_YUV[pix-2];
					if (deltax>hop1) deltax=0;//max_hop1;
				}
				hop0=result_YUV[pix-1];//+ deltax/3;
				if (hop0>255) hop0=255;
				if (hop0<1) hop0=1;
				//hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2; //fast approach (good enough)
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7; //slow approach	

				//hop0= result_YUV[pix-img.width];
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				
				
				last_small_hop=false;
				
				
				//hop1=max_hop1;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			
			//paeth
			/*
			int A=0;
			int B=0;
			int C=0;
			if (x>0) A=result_YUV[pix-1];
			if (y>0) B=result_YUV[pix-img.width];
			if (x>0 && y>0) C=result_YUV[pix-img.width-1];
			if (x==0) {A=B;C=B;}
			if (y==0) {B=A;C=A;}
			if (x==0 && y==0)hop0=oc;
			else
			{
				int pred=A+B-C;
				int a1=Math.abs(A-pred);
				int b1=Math.abs(B-pred);
				int c1=Math.abs(C-pred);
				if (a1<=b1 && a1<=c1) hop0=A;
				else if (b1<=a1 && b1<=c1) hop0=B;
				else hop0=C;
				//System.out.println("paeth");
			}
			*/
			
			

			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//hop0 is prediction
			//if (hop0>255)hop0=255;
			//else if (hop0<0) hop0=0; 

			//max_hop1=16;//(int)((float)hop0 *0.02f +0.5f);
			//System.out.println("hop1max:"+max_hop1);
			//positive hops computation
			//-------------------------
			int rmax=25;//40;
			//hop1=8;
			
			//min_hop1=(int)(0.5f+(float)hop0*0.04f);//no puede ser cero
			//if (min_hop1<4) min_hop1=4;
			//if (hop1<min_hop1) hop1=min_hop1;
			
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
				//for (int j=4;j<=5;j++) {
					e2=oc-pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
				//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
				//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
				for (int j=4;j>=0;j--) {
				//	for (int j=4;j>=3;j--) {
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}

			//29/12/2014
			
			
			//colin
			
			rmax=25;
			int hop0i=pccr[hop1][hop0][rmax][4];
			int[] colin= new int[9];
			colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
			colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
			colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
			colin[3]=pccr[hop1][hop0i][rmax][3];
			colin[5]=pccr[hop1][hop0i][rmax][5];
		
			int startcolin=6;
			int endcolin=3;
			
			for (int j=startcolin; j<8;j++)
				{colin[j]=(int)(+1f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
				}
				
			for (int j=1; j<endcolin;j++)
			{	colin[j]=(int)(-0.5f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
		    }
			
			
			// correccion
			int mini=2;
			int maxi=6;
			img.LHE2_removed_pix[pix]=255;//oc;
			
			
			boolean quitar=true;
			if (quitar)
			{
			int up=3;	
			int left=5;
			if (y>0 ) up=hops[pix-img.width];
			if (x>0) left=hops[pix-1];
			//int criteria= y%2;
			//int criteriax=1;
			boolean removed=false;
			
			int criteria=3;
					    if (y%criteria!=1 && x>1 && y>0)
					    //if (y%2!=criteria && x>1 && y>0)
						//if (y>1 && x<511 && x>1)	
						{
						   if (hops[pix-img.width]>=mini  && hops[pix-img.width]<=maxi)
							   
					    	//if (hops[pix-1]==4)
							{
							   
							   
								hop_number=4;//hops[pix-img.width];//mg.width];//4;
								
								hop_number=4;//(up+left)/2;
								counter_resta+=1;
								img.LHE2_removed_pix[pix]=0;
								removed=true;
							}
						}
			
					    if (y>0 && x>0)
					    	if (x%criteria!=1 )
					    	//if (x%criteria!=1 )
					    	//	if (x%2==criteriax )
					    //if (x%2==1 && y%2!=1)
							//if (y>1 && x<511 && x>1)	
							{
							   //if (hops[pix-img.width]==4 && hops[pix-1]==4)
							  // if (hops[pix-img.width]==4)// && hops[pix-1]==4)
								   if (hops[pix-1]>=mini && hops[pix-1]<=maxi)
								{
									hop_number=4;
									hop_number=4;
									counter_resta+=1;
									img.LHE2_removed_pix[pix]=0;
									removed=true;
								}
							}
				
			}
		
			
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][25][hop_number];
			
			//if (removed)
			{
				//result_YUV[pix]=result_YUV[pix-img.width]
			}
			
			
			//result_YUV[pix]=colin[hop_number];//pccr[hop1][hop0][25][hop_number];
			
			//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
			//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
			hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

			
			//calculo de errores medios
			//---------------------------
			error_center+=(oc-result_YUV[pix]);
			error_avg+=Math.abs((oc-result_YUV[pix]));
			
			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			//if (hop_number>=6) small_hop=true;
			//if (hop_number<=6 && hop_number>=2) small_hop=true;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			pix++;            
		}//for x
	}//for y
	
	/*
	}//iterations
	
	long end_time = System.currentTimeMillis();
	double total_time=end_time-start_time;
	double tpp=total_time/(img.width*img.height*iterations);
	double tpi=total_time/(iterations);
	System.out.println("tiempo_total:"+total_time+"  tpp:"+tpp+" ms"+ " tpi:"+tpi +" ms");
	*/
	System.out.println("quantization done");
	
	System.out.println("center of  error:"+error_center/(img.width*img.height));
	System.out.println("average of  error:"+error_avg/(img.width*img.height));
	System.out.println("----------------------------------------------------------");
	System.out.println("counter_resta="+counter_resta);
	
	LHE2_resta=counter_resta;
	postfilter_LHE2(hops,result_YUV);
	
	
}//end function
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void esperanza_matematica_v001(int[] hops,int[] result_YUV)
{
	System.out.println("esperanza...hello");
	
	int [][] hope=new int [256][256];
	
	
	int max_hop1=10;//8;//8;//16;//8;// hop1 interval 4..8
	int min_hop1=4;//4;// 
	int start_hop1=(max_hop1+min_hop1)/2;
	
	
	int hop1=start_hop1;//max_hop1;
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small

	
	float error_center=0;
	float error_avg=0;
	
	

	int counter_resta=0;
	
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;x++)  {

			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				
				//if (y%2==0) hop0= result_YUV[pix-1];//esto es mejorable
				//else hop0= result_YUV[pix-img.width];
				
				int deltax=0;
				if (x>1)
				{
					deltax=result_YUV[pix-1]-result_YUV[pix-2];
					if (deltax>hop1) deltax=0;//max_hop1;
				}
				hop0=result_YUV[pix-1];//+ deltax/3;
				if (hop0>255) hop0=255;
				if (hop0<1) hop0=1;
				//hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2; //fast approach (good enough)
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7; //slow approach	

				//hop0= result_YUV[pix-img.width];
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				
				
				last_small_hop=false;
				
				
				//hop1=max_hop1;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			
			hope[hop0][oc]++;
			
			//paeth
			/*
			int A=0;
			int B=0;
			int C=0;
			if (x>0) A=result_YUV[pix-1];
			if (y>0) B=result_YUV[pix-img.width];
			if (x>0 && y>0) C=result_YUV[pix-img.width-1];
			if (x==0) {A=B;C=B;}
			if (y==0) {B=A;C=A;}
			if (x==0 && y==0)hop0=oc;
			else
			{
				int pred=A+B-C;
				int a1=Math.abs(A-pred);
				int b1=Math.abs(B-pred);
				int c1=Math.abs(C-pred);
				if (a1<=b1 && a1<=c1) hop0=A;
				else if (b1<=a1 && b1<=c1) hop0=B;
				else hop0=C;
				//System.out.println("paeth");
			}
			*/
			
			

			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//hop0 is prediction
			//if (hop0>255)hop0=255;
			//else if (hop0<0) hop0=0; 

			//max_hop1=16;//(int)((float)hop0 *0.02f +0.5f);
			//System.out.println("hop1max:"+max_hop1);
			//positive hops computation
			//-------------------------
			int rmax=25;//40;
			//hop1=8;
			
			//min_hop1=(int)(0.5f+(float)hop0*0.04f);//no puede ser cero
			//if (min_hop1<4) min_hop1=4;
			//if (hop1<min_hop1) hop1=min_hop1;
			
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
				//for (int j=4;j<=5;j++) {
					e2=oc-pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
				//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
				//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
				for (int j=4;j>=0;j--) {
				//	for (int j=4;j>=3;j--) {
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;}
					else break;
				}
			}

			//29/12/2014
			
			
			//colin
			
			rmax=25;
			int hop0i=pccr[hop1][hop0][rmax][4];
			int[] colin= new int[9];
			colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
			colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
			colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
			colin[3]=pccr[hop1][hop0i][rmax][3];
			colin[5]=pccr[hop1][hop0i][rmax][5];
		
			int startcolin=6;
			int endcolin=3;
			
			for (int j=startcolin; j<8;j++)
				{colin[j]=(int)(+1f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
				}
				
			for (int j=1; j<endcolin;j++)
			{	colin[j]=(int)(-0.5f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
		    }
			
			
			// correccion
			int mini=2;
			int maxi=6;
			img.LHE2_removed_pix[pix]=255;//oc;
			
			
			boolean quitar=true;
			if (quitar)
			{
			int up=3;	
			int left=5;
			if (y>0 ) up=hops[pix-img.width];
			if (x>0) left=hops[pix-1];
			//int criteria= y%2;
			//int criteriax=1;
			boolean removed=false;
			
			int criteria=3;
					    if (y%criteria!=1 && x>1 && y>0)
					    //if (y%2!=criteria && x>1 && y>0)
						//if (y>1 && x<511 && x>1)	
						{
						   if (hops[pix-img.width]>=mini  && hops[pix-img.width]<=maxi)
							   
					    	//if (hops[pix-1]==4)
							{
							   
							   
								hop_number=4;//hops[pix-img.width];//mg.width];//4;
								
								hop_number=4;//(up+left)/2;
								counter_resta+=1;
								img.LHE2_removed_pix[pix]=0;
								removed=true;
							}
						}
			
					    if (y>0 && x>0)
					    	if (x%criteria!=1 )
					    	//if (x%criteria!=1 )
					    	//	if (x%2==criteriax )
					    //if (x%2==1 && y%2!=1)
							//if (y>1 && x<511 && x>1)	
							{
							   //if (hops[pix-img.width]==4 && hops[pix-1]==4)
							  // if (hops[pix-img.width]==4)// && hops[pix-1]==4)
								   if (hops[pix-1]>=mini && hops[pix-1]<=maxi)
								{
									hop_number=4;
									hop_number=4;
									counter_resta+=1;
									img.LHE2_removed_pix[pix]=0;
									removed=true;
								}
							}
				
			}
		
			
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][25][hop_number];
			
			//if (removed)
			{
				//result_YUV[pix]=result_YUV[pix-img.width]
			}
			
			
			//result_YUV[pix]=colin[hop_number];//pccr[hop1][hop0][25][hop_number];
			
			//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
			//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
			hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

			
			//calculo de errores medios
			//---------------------------
			error_center+=(oc-result_YUV[pix]);
			error_avg+=Math.abs((oc-result_YUV[pix]));
			
			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			//if (hop_number>=6) small_hop=true;
			//if (hop_number<=6 && hop_number>=2) small_hop=true;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			pix++;            
		}//for x
	}//for y
	
	/*
	}//iterations
	
	long end_time = System.currentTimeMillis();
	double total_time=end_time-start_time;
	double tpp=total_time/(img.width*img.height*iterations);
	double tpi=total_time/(iterations);
	System.out.println("tiempo_total:"+total_time+"  tpp:"+tpp+" ms"+ " tpi:"+tpi +" ms");
	*/
	System.out.println("quantization done");
	
	System.out.println("center of  error:"+error_center/(img.width*img.height));
	System.out.println("average of  error:"+error_avg/(img.width*img.height));
	System.out.println("----------------------------------------------------------");
	System.out.println("counter_resta="+counter_resta);
	
	LHE2_resta=counter_resta;
	//postfilter_LHE2(hops,result_YUV);
	
	
	System.out.println("");
	System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
	for (int i=0;i<256;i++) System.out.print(","+i);
	System.out.println("");
	for (int i=0;i<256;i++)
	{
		int menor=0;
		int mayor=0;
		int margen=32;
		System.out.print(""+i);
		for (int j=0;j<256;j++)
		{
			System.out.print(","+hope[i][j]);
			if (i+margen<j) mayor+=hope[i][j];
			if (i-margen>j) menor+=hope[i][j];
			
		}
		System.out.print (","+menor+","+mayor);
		System.out.println("");
	}
	
	System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
	
}//end function
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeOneHopPerPixel_LHE2_experimento36(int[] hops,int[] result_YUV)
{
	System.out.println("quantizying...hello");
	
	int max_hop1=10;//8;//8;//16;//8;// hop1 interval 4..8
	int min_hop1=6;//4;// 
	int start_hop1=(max_hop1+min_hop1)/2;
	int rmax=27;
	
	int hop1=start_hop1;//max_hop1;
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small

	
	
	float error_center=0;
	float error_avg=0;
	
	

	int counter_resta=0;
	
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;x++)  {

			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				
				//if (y%2==0) hop0= result_YUV[pix-1];//esto es mejorable
				//else hop0= result_YUV[pix-img.width];
				
				//int deltax=0;
				//if (x>1)
				//{
				//	deltax=result_YUV[pix-1]-result_YUV[pix-2];
				//	if (deltax>hop1) deltax=0;//max_hop1;
				//}
				//hop0=result_YUV[pix-1];//+ deltax/3;
				//if (hop0>255) hop0=255;
				//if (hop0<1) hop0=1;
				//hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2; //fast approach (good enough)
				//hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7; //slow approach	
				
				hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2; //fast approach
				//hop0= result_YUV[pix-1];
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				
				
				last_small_hop=false;
				
				
				//hop1=max_hop1;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			
			//paeth
			/*
			int A=0;
			int B=0;
			int C=0;
			if (x>0) A=result_YUV[pix-1];
			if (y>0) B=result_YUV[pix-img.width];
			if (x>0 && y>0) C=result_YUV[pix-img.width-1];
			if (x==0) {A=B;C=B;}
			if (y==0) {B=A;C=A;}
			if (x==0 && y==0)hop0=oc;
			else
			{
				int pred=A+B-C;
				int a1=Math.abs(A-pred);
				int b1=Math.abs(B-pred);
				int c1=Math.abs(C-pred);
				if (a1<=b1 && a1<=c1) hop0=A;
				else if (b1<=a1 && b1<=c1) hop0=B;
				else hop0=C;
				//System.out.println("paeth");
			}
			*/
			
			

			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//hop0 is prediction
			//if (hop0>255)hop0=255;
			//else if (hop0<0) hop0=0; 

			//max_hop1=16;//(int)((float)hop0 *0.02f +0.5f);
			//System.out.println("hop1max:"+max_hop1);
			//positive hops computation
			//-------------------------
			//hop1=8;
			
			//min_hop1=(int)(0.5f+(float)hop0*0.04f);//no puede ser cero
			//if (min_hop1<4) min_hop1=4;
			//if (hop1<min_hop1) hop1=min_hop1;
			
			
			int wf=4;
			
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
				//for (int j=4;j<=5;j++) {
					e2=oc-pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;
					if (e2<wf) break;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
				//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
				//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
				for (int j=4;j>=0;j--) {
				//	for (int j=4;j>=3;j--) {
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;
					if (e2<wf) break;}
					else break;
				}
			}

			//29/12/2014
			
			
			//colin
			
			//rmax=25;
			int hop0i=pccr[hop1][hop0][rmax][4];
			int[] colin= new int[9];
			colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
			colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
			colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
			colin[3]=pccr[hop1][hop0i][rmax][3];
			colin[5]=pccr[hop1][hop0i][rmax][5];
		
			int startcolin=6;
			int endcolin=3;
			
			for (int j=startcolin; j<8;j++)
				{colin[j]=(int)(+1f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
				}
				
			for (int j=1; j<endcolin;j++)
			{	colin[j]=(int)(-0.5f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
		    }
			
			
			// correccion
			int mini=2;//2;//2;
			int maxi=6;//6;//6;
			img.LHE2_removed_pix[pix]=255;//oc;
			
			
			boolean quitar=true;
			if (quitar)
			{
			//int up=3;	
			//int left=5;
			//if (y>0 ) up=hops[pix-img.width];
			//if (x>0) left=hops[pix-1];
			//int criteria= y%2;
			//int criteriax=1;
			boolean removed=false;
			
			
					    if (y%2==1 && x>1 && y>0)
					    {
						   //if (hops[pix-1]>=mini && hops[pix-1]<=maxi)
						   if (hops[pix-img.width]>=mini && hops[pix-img.width]<=maxi)
					    	{
								hop_number=4;
								counter_resta+=1;
								img.LHE2_removed_pix[pix]=0;
								removed=true;
							}
						}
					    
			            if (!removed && x%2==1 && y>0 && x>0)
					    
					    
					    	//if (x%criteria!=1 )
					    	//	if (x%2==criteriax )
					    //if (x%2==1 && y%2!=1)
							//if (y>1 && x<511 && x>1)	
							{
							   //if (hops[pix-img.width]==4 && hops[pix-1]==4)
							   //if (hops[pix-img.width]==4)// && hops[pix-1]==4)
								//   if (hops[pix-1]>=mini && hops[pix-1]<=maxi)
					    	  // if (hops[pix-img.width]>=mini && hops[pix-img.width]<=maxi)
			            	if (hops[pix-1]>=mini && hops[pix-1]<=maxi)
								{
									
									hop_number=4;
									counter_resta+=1;
									img.LHE2_removed_pix[pix]=0;
									removed=true;
								}
							}
					   
					    //if (1>2)
			            if (!removed)
					    if (y%2!=1 && x%2!=1 && x>2 && x<511 && y>2)
					    if (x>2 && y>2)	
					    {
					    	//System.out.println("hola");
					    	//if (img.LHE2_removed_pix[pix-2]!=0) //no quitado
					    	//if (img.LHE2_removed_pix[pix-2*img.width]!=0)//no quitado
					    	
					    	//if (x%2==1 && y%2==1)//el pixel que queda
					    	//img.LHE2_removed_pix[pix]=128;	
					    	if (hops[pix-2]>=4 && hops[pix-2*img.width]>=4)//nulo
					    	if (hops[pix-2]<=4 && hops[pix-2*img.width]<=4)//nulo
					    	if (hops[pix-2*img.width-2]<=4 && hops[pix-2*img.width-2]<=4)//nulo	
					    	
					    	if (img.LHE2_removed_pix[pix-2]==255)//oc;
					    	if (img.LHE2_removed_pix[pix-2*img.width]==255)//oc;
					    	if (img.LHE2_removed_pix[pix-2*img.width-2]==255)//oc;	
					    	//if (1>2)
					    	//if ( hops[pix-2*img.width-2]==4)//nulo	
					    	//if (hops[pix-2]>=mini && hops[pix-2]<=maxi)//nulo
					    	//if (hops[pix-1]<=mini && hops[pix-1]>=maxi)//nulo
					    	//if (hops[pix-2]>=4 && hops[pix-2]<=4)//nulo	
					    	//if (hops[pix-3*img.width]>=4 && hops[pix-3*img.width]<=4) //nulo	
					    	//if (hops[pix-2*img.width]>=4 && hops[pix-2*img.width]<=4) //nulo
					    	//if (hops[pix-img.width]>=mini && hops[pix-img.width]<=maxi) //nulo
					    	//if (1>2)
							{
								
								hop_number=4;
								counter_resta+=1;
								img.LHE2_removed_pix[pix]=0;
								removed=true;
								//System.out.println ("Hola");
							}
					    }
					    
				
			}
		 // System.out.println ("Hola");
			
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][rmax][hop_number];
			
			//System.out.print (result_YUV[pix]+" "+hop_number);
			//result_YUV[pix]=colin[hop_number];
			//if (removed)
			{
				//result_YUV[pix]=result_YUV[pix-img.width]
			}
			
			
			//result_YUV[pix]=colin[hop_number];//pccr[hop1][hop0][25][hop_number];
			
			//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
			//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
			hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

			
			//calculo de errores medios
			//---------------------------
			error_center+=(oc-result_YUV[pix]);
			error_avg+=Math.abs((oc-result_YUV[pix]));
			
			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			//if (hop_number>=6) small_hop=true;
			//if (hop_number<=6 && hop_number>=2) small_hop=true;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			pix++;            
		}//for x
	}//for y
	
	/*
	}//iterations
	
	long end_time = System.currentTimeMillis();
	double total_time=end_time-start_time;
	double tpp=total_time/(img.width*img.height*iterations);
	double tpi=total_time/(iterations);
	System.out.println("tiempo_total:"+total_time+"  tpp:"+tpp+" ms"+ " tpi:"+tpi +" ms");
	*/
	System.out.println("quantization done");
	
	System.out.println("center of  error:"+error_center/(img.width*img.height));
	System.out.println("average of  error:"+error_avg/(img.width*img.height));
	System.out.println("----------------------------------------------------------");
	System.out.println("counter_resta="+counter_resta);
	
	LHE2_resta=counter_resta;
	postfilter_LHE2(hops,result_YUV);
	//postfilter_LHE2_v002(hops,result_YUV);
	System.out.println("hola");
	//postresta_LHE2_v001(hops,result_YUV);
}//end function
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void postfilter_LHE2_v002(int[] hops,int[] result_YUV)
{

	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;x++)  {
			
			int pix=x+y*img.width;
			//check eliminado
			
			if (x>0 && y>0 && x<511 && y<511)
			{	
			if (img.LHE2_removed_pix[pix]==0)
			{
				result_YUV[pix]=(result_YUV[pix-1]+result_YUV[pix-img.width+1])/2;
			/*
			
				if (y%2!=1)//tengo izquierdo y derecho disponible
				  {
				 result_YUV[pix]=(result_YUV[pix-1]+result_YUV[pix+1])/2;
				 //img.LHE2_removed_pix[pix]=(img.LHE2_removed_pix[pix-1]+img.LHE2_removed_pix[pix+1])/2;
			      }
				else
				  {
					if (x%2==0)
					{result_YUV[pix]=(result_YUV[pix-img.width]+result_YUV[pix+img.width])/2;
					//img.LHE2_removed_pix[pix]=255;//(img.LHE2_removed_pix[pix-1]+img.LHE2_removed_pix[pix+1])/2;
					}
					else
					{
					result_YUV[pix]=(result_YUV[pix-img.width]+result_YUV[pix+img.width])/2;
					}
				  }
			*/	
			}
			
			}//
			
		}
		}
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void postresta_LHE2_v001(int[] hops,int[] result_YUV)
{
System.out.println("entrada en postresta...");
	int resta_extra=0;
	int count=0;
	int GRUPO=32;
	int resta=GRUPO;
	int checks=0;
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;x++)  {
			
			int pix=x+y*img.width;
			//check eliminado
			
			
			
			
				if (y>0)
				{
				if (img.LHE2_removed_pix[pix]==0) continue;	
				//if (hops[pix]!=hops[pix-img.width]) resta=0;
				if (hops[pix]!=4) resta=0;
				count=count +1;
				if (count==GRUPO) {resta_extra+=resta;count=0;resta=GRUPO;checks++;}
				}
//			resta_extra-=1;
						
		}
		}
	System.out.println ("resta_extra="+(resta_extra-checks)+"     checks:"+checks);
	LHE2_resta+=resta_extra-checks;
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//**************************************************************************************************
/**
 * This is a very fast LHE quantization function, used for initial quantization in order to 
 * perform Perceptual Relevance Metrics.
 * Later quantization over downwsampled image allows more tunning ( k value) and therefore 
 * require more complex calculation (but over a reduced image)
 * 
 * image luminance array is the input for this function. 
 *   This luminance array is suposed to be stored at img.YUV[0][pix]; 
 *   Image luminance array is not modified
 * 
 * hops numbering:
 *   >negative hops: 0,1,2,3
 *   >null hop: 4
 *   >positive hops: 5,6,7,8
 * 
 * result_YUV is output array. it can not be removed, because these luminance & chroma values are part of
 *   algorithm to choose next hop
 * 
 * 
 * @param hops
 * @param result_YUV
 */
public void quantizeOneHopPerPixel_improved(int[] hops,int[] result_YUV)
{
	System.out.println("quantizying...");
	
	//prefilter();
	
	/*
	int iterations=1000;
	long start_time = System.currentTimeMillis();
	for (int xy=0;xy<iterations;xy++){
		*/
	//ESTA ES LA FUNCION BUENA
	//img.width=1920;
	//img.height=1080;

	//img.width=1280;
	//img.height=720;

	//img.width=720;
	//img.height=576;
	//img.width=429;
	//img.height=429;
	//img.width=576;
	//img.height=256;
	int max_hop1=10;//10;//8;//8;//16;//8;// hop1 interval 4..8
	int min_hop1=6;//4;//4;// 
	
	//min_hop1=32;
	//max_hop1=48;
	
	int start_hop1=(max_hop1+min_hop1)/2;
	int rmax=25;//40;
	rmax=27;//27;
	//hop1=8;
	
	
	int hop1=start_hop1;//max_hop1;
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small

	
	
	float error_center=0;
	float error_avg=0;
	
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;x++)  {

			oc=img.YUV[0][pix];
			//oc=oc/2;
			//if (y>0) oc=(img.YUV[0][pix]-img.YUV[0][pix-img.width])/2+128;
			
			//if (oc>10)
			//oc=oc/2;//(int)Math.pow(oc,0.5f);
			
			//if (oc>10) oc=10+ (oc-10)/10;
			
			//if (y>0) img_tmp[pix]=(result_YUV[pix]-128)*2+img_tmp[pix-img.width];
			//if (y>0) img_tmp[pix]=(img.YUV[0][pix]- img.YUV[0][pix-img.width])/2+128;
			
			//if (y>0 ) oc=(img.YUV[0][pix]-result_YUV[pix-img.width])/2+128;
			
			
			//System.out.println("oc="+oc);
			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;	
				
			hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2;
				//hop0=result_YUV[pix+1-img.width];
				//	System.out.println(" result_YUV[pix-1]:"+result_YUV[pix-1]+"  result_YUV[pix+1-img.width]: "+result_YUV[pix+1-img.width]);
				
				
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				
				
				last_small_hop=false;
				
				
				//hop1=max_hop1;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			
			//paeth
			/*
			int A=0;
			int B=0;
			int C=0;
			if (x>0) A=result_YUV[pix-1];
			if (y>0) B=result_YUV[pix-img.width];
			if (x>0 && y>0) C=result_YUV[pix-img.width-1];
			if (x==0) {A=B;C=B;}
			if (y==0) {B=A;C=A;}
			if (x==0 && y==0)hop0=oc;
			else
			{
				int pred=A+B-C;
				int a1=Math.abs(A-pred);
				int b1=Math.abs(B-pred);
				int c1=Math.abs(C-pred);
				if (a1<=b1 && a1<=c1) hop0=A;
				else if (b1<=a1 && b1<=c1) hop0=B;
				else hop0=C;
				//System.out.println("paeth");
			}
			*/
			
			

			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//hop0 is prediction
			//if (hop0>255)hop0=255;
			//else if (hop0<0) hop0=0; 

			//max_hop1=16;//(int)((float)hop0 *0.02f +0.5f);
			//System.out.println("hop1max:"+max_hop1);
			//positive hops computation
			//-------------------------
			
			
			//min_hop1=(int)(0.5f+(float)hop0*0.04f);//no puede ser cero
			//if (min_hop1<4) min_hop1=4;
			//if (hop1<min_hop1) hop1=min_hop1;
			
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
				//for (int j=4;j<=5;j++) {
					//if (j==4) rmax=20;
					//if (j==5) rmax=20;
					//if (j==6) rmax=25;
					//if (j==7) rmax=28;
					//if (j==8) rmax=30;
					
					e2=oc-pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;
					              //if (e2<min_hop1) break;
									if (e2<4) break;
					              }
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
				//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
				//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
				for (int j=4;j>=0;j--) {
				//	for (int j=4;j>=3;j--) {
					//if (j==4) rmax=20;
				    //if (j==3) rmax=20;
			        //if (j==2) rmax=25;
					//if (j==1) rmax=28;
					//if (j==0) rmax=30;
					
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;
					            //if (e2<min_hop1) break;
								if (e2<4) break;
					            }
					else break;
				}
			}
//System.out.println("hola");
			//29/12/2014
			
			
			//colin
			
			//rmax=25;
			//rmax=24;
			int hop0i=pccr[hop1][hop0][rmax][4];
			int[] colin= new int[9];
			colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
			colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
			colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
			colin[3]=pccr[hop1][hop0i][rmax][3];
			colin[5]=pccr[hop1][hop0i][rmax][5];
		
			int startcolin=6;
			int endcolin=3;
			
			for (int j=startcolin; j<8;j++)
				{colin[j]=(int)(+1f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
				}
				
			for (int j=1; j<endcolin;j++)
			{	colin[j]=(int)(-0.5f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
		    }
			
			
			
			
			
			
		
			
			//assignment of final color value
			//--------------------------------
			
			result_YUV[pix]=pccr[hop1][hop0][rmax][hop_number];
			
			if (y>1)
			{
				//result_YUV[pix-img.width]=result_YUV[pix-img.width]*2;
				//if (hops[pix-img.width]==5 || hops[pix-img.width]==3)result_YUV[pix-img.width]=255; 
				//if (hops[pix-img.width]>5 || hops[pix-img.width]<3)result_YUV[pix-img.width]=255;
			}
			
			//if (y>0)
			//{
			//result_YUV[pix]=(result_YUV[pix]-128)*2+result_YUV[pix-img.width];
			//}
			
			
			//if (hop_number>2) result_YUV[pix]=colin[hop_number]; COLIN NO FUNCIONA!!
			
			//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
			//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
			hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

			
			//calculo de errores medios
			//---------------------------
			error_center+=(oc-result_YUV[pix]);
			error_avg+=Math.abs((oc-result_YUV[pix]));
			
			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			//if (hop_number>=6) small_hop=true;
			//if (hop_number<=6 && hop_number>=2) small_hop=true;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			
			//hop1=6;
			
			//hop1=8;
			//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			pix++;            
		}//for x
	}//for y
	
	/*
	}//iterations
	
	long end_time = System.currentTimeMillis();
	double total_time=end_time-start_time;
	double tpp=total_time/(img.width*img.height*iterations);
	double tpi=total_time/(iterations);
	System.out.println("tiempo_total:"+total_time+"  tpp:"+tpp+" ms"+ " tpi:"+tpi +" ms");
	*/
	System.out.println("quantization done");
	
	System.out.println("center of  error:"+error_center/(img.width*img.height));
	System.out.println("average of  error:"+error_avg/(img.width*img.height));
	System.out.println("----------------------------------------------------------");
	
	//posfilter(result_YUV);
	
	//postRLC(hops,result_YUV);
	
}//end function


//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeDownsampledBlock_R4_improved(Block b, int[] hops,int[] result_YUV, int[] src_YUV,int[] boundaries_YUV)
{

	//System.out.println("colorin");
	
	//block b contains the coordinates to set the limits of this function
	
	
	//some parts of this code are identical to the function quantizeOneHopPerPixel() 
	
	//ESTOS DOS VALORES LUEGO SE AJUSTAN
	int max_hop1=10;//8;// hop1 interval 4..8
	int min_hop1=6;//6;//4;//8;//4;//
	
	//queda mejor 6 que el punto medio (en peppers)
	//int start_hop1=(max_hop1+min_hop1)/2;// hop1 at begining (up-left corner) of each block
	
	//int hop1=start_hop1;
	
	float hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	
	
	int rmax=20;
	//se escoge el juego de hops segun el PR cuantizado
	
	if (b.PRavg==1.0) rmax=30;//bordes abruptos, iconos, ruido 
	//else if (b.PRavg>=0.625) rmax=25;//bordes 1,1,1,0.5
	else if (b.PRavg>=0.75) rmax=30;//bordes 1,1,1,0.5 LO HE CAMBIADO DESDE 25 . hoy es 5/3/2015 30
	else if (b.PRavg>=0.5) rmax=25;//bordes 1,1,1,0.5 NUEVO 2/01/2015
	else if (b.PRavg>=0.25) rmax=22;//25 CAMBIADO el 21/03/2015 asi es mejor
	//else if (b.PRavg>=0.25) rmax=25;//3;//bordes 1,1,1,0.5 NUEVO 5/3/2015 25<--- con 22 es mejor q 25 para lena
	//else if (b.PRavg>=0.3) rmax=25;
	
	//int rmaxini=rmax;
	
	//MEJORA 28/12/2014. alteramos h1max segun la PR y ademas el menor valor es 10 y no 8
	if (b.PRavg==1.0) {max_hop1=16;}//16;
	else if (b.PRavg>=0.75) {max_hop1=14;}//12;
	else if (b.PRavg>=0.5) {max_hop1=12;} // de 12 a 10?
	else if (b.PRavg>=0.25) {max_hop1=10;} 
	//else if (b.PRavg>=0.125) max_hop1=8; 
	//
	//else if (b.PRavg>=0.125) max_hop1=10;
	else {max_hop1=8;}// suave
	
	
    int start_hop1=(max_hop1+min_hop1)/2;// hop1 at begining (up-left corner) of each block
    
	int hop1=start_hop1;
	boolean colin_activo=true;//true;
	//boolean small_colin=false;;
	//boolean colin_activo=false;
	//if (b.PRavg>0.25f) colin_activo=true; 
	
	
	/*
	float pppavg=0;
	for (int i=0;i<2;i++)
		for (int j=0;j<4;j++)
			if (b.ppp[i][j]<=2f) colin_activo=false;;
		*/	
	//pppavg=pppavg/4f; //max es 8  1 min es 1/8=0.125
	//boolean colinoff=false;
	
	/*
	float pppavg=0;
	for (int i=0;i<2;i++)
		for (int j=0;j<4;j++)
			pppavg+=b.ppp[i][j];
	pppavg=pppavg/8f; //max es 8  1 min es 1/8=0.125
	boolean colinoff=false;
	*/
	//if (pppavg==1.0f) colinoff=true;//NUEVO
	//rmax=(int)(1000*b.PRavg/pppavg);
	//if (rmax>30) rmax=30;
	//if (rmax<20) rmax=20;
	//System.out.println("rmax"+rmax+ "     pppavg="+pppavg+"     pr:"+b.PRavg);
	
	//int hop1b=hop1;
	//min_hop1=4;
	
	//PARA HACER PRUEBAS CON LHE BASICO SE PUEDE:
	//PONER GRID A UN SOLO BLOQUE.
	//HACER ESTAS IGUALDADES:
	//max_hop1=8;//10;
	//rmax=20;//25; //si pongo 20, lena gana 43 db
	
	
	
	//System.out.println("PRavg:"+b.PRavg);
	//rmax=20;//valor original
	//max_hop1=19;
	//start_hop1=6;
	
	//System.out.println(" dato:"+img.width);
	int pix=b.yini*img.width+b.xini;//initial pixel possition        
	
	
	
	boolean last_small_hop=false;// indicates if last hop is small
	
	for (int y=b.yini;y<=b.downsampled_yfin;y++)  {
		
		//ponemos a hop minimo al comienzo de scan line de bloque
		//esto es bueno sobre todo en bloques lisos
		
		//debo de usar un array mejor
		//hop1=min_hop1;//NUEVO 10/09/2014
	//	int last_hop=4;
		for (int x=b.xini;x<=b.downsampled_xfin;x++)  {

			pix=y*img.width+x;
			oc=src_YUV[pix];
			//System.out.println(" A last_small_hop:"+last_small_hop);
			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			
			//inner pixels ( mostly of them. that is the reason for considering the 1st option)
			//---------------------------------------------------------------------------------
			 if ((y>b.yini) &&(x>b.xini) && x!=b.downsampled_xfin){
				hop0=(4f*result_YUV[pix-1]+3f*result_YUV[pix+1-img.width])/7f;	
				
				//System.out.println("a");
				//hop0=(4*result_YUV[pix-1]+4*result_YUV[pix-img.width])/8;	
				
				//hop0=(241*result_YUV[pix-1]+170*result_YUV[pix+1-img.width])/411;	
				//	System.out.println(" result_YUV[pix-1]:"+result_YUV[pix-1]+"  result_YUV[pix+1-img.width]: "+result_YUV[pix+1-img.width]);
			}
			//initial pix
			//-----------
			 else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
				//System.out.println("b");
			}	
			//upper side of the image. not include corner
		    //--------------------------------------------
			else if (y==0 && x>b.xini) {
				hop0=result_YUV[pix-1];
				//System.out.println("c");
			}
			
			//corners at upper side of image
			//-------------------------------
			else if (y==0 && x==b.xini) {
				hop0=boundaries_YUV[pix-1];
				
				//CAMBIO HOY
				last_small_hop=false;
				hop1=start_hop1;
				
				
				//System.out.println("d");
				
			}
			
			//left side of the image. not include corner
			//-----------------------------------------
			else if ((x==0) && (y>b.yini)){
			//	System.out.println("e");
				hop0=result_YUV[pix-img.width];
				//hop0=img.boundaries_YUV[0][pix-img.width];
			
				//esta variable puede variar es mejor no ponerla a false
				last_small_hop=false;//31/12/2014 se descomenta
				//last_small_hop=true;//porque no
				//comento esto 12/09/2014
				hop1=start_hop1;//31/12/2014 se descomenta
			}
			//corners of left side of the image.
			//----------------------------------------- 
          else if ((x==0) && (y==b.yini)){
        	//  System.out.println("f");
          	//System.out.println("NUNCA ENTRA");
				//hop0=result_YUV[pix-img.width];
				hop0=img.boundaries_YUV[0][pix-img.width];
				
				//hop0=(3*img.boundaries_YUV[0][pix-img.width]+4*img.boundaries_YUV[0][pix-img.width+1])/7;
				
				
				last_small_hop=false;
				//last_small_hop=true;//porque no
				
				//comento esto 12/09/2014
				hop1=start_hop1;
				
				
				//hop0=oc;
				
			}
			//left side of the block. not include corner
			//--------------------------------------
			else if ((x==b.xini) && (y>b.yini)){
				//System.out.println("g");
				//System.out.print("zulu");
					hop0=(4*img.boundaries_YUV[0][pix-1]+3*result_YUV[pix+1-img.width])/7;
					//hop0=img.boundaries_YUV[0][pix-1];
					//hop0=(4*img.boundaries_YUV[0][pix-1]+4*result_YUV[pix+1-img.width])/8;
					//hop0=result_YUV[pix+1-img.width];//oc;//img.boundaries_YUV[0][pix-1];
					//System.out.println(" el boundary v tiene color:"+img.boundaries_YUV[0][pix-1]+"  at  x:"+(x-1)+" ,y:"+y);
					//hop0=oc;
					
					//esto es nuevo 12/09/2014
					last_small_hop=false;//31/12/2014 se descomenta
					//last_small_hop=true;//porque no
					
					//hop1=max_hop1;
					hop1=start_hop1;//31/12/2014 se descomenta
					//hop0=oc;
					
					
			}
			//up-left corner of block
			//--------------
			else if ((x==b.xini) && (y==b.yini)){
			//	System.out.println("h");
				//System.out.print("zulu");
				hop0=(4*img.boundaries_YUV[0][pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;
				
				//esto es nuevo 12/09/2014
				last_small_hop=false;
				//last_small_hop=true;//porque no
				
				hop1=start_hop1;
				//hop1=8;//start_hop1;
				//hop0=255;//oc;
				//System.out.println("hop0 es "+hop0);
				
				//hop0=oc;
			}
			
			
			
			//right side of block (and right side of image. is the same case). Not includes corner
			//-----------------------------------------------------------------------------------
			else if ((x==b.downsampled_xfin) && (y>b.yini)) {
				//System.out.println("i");
				//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;	
				
				//nueva formula 03/12/2015
				hop0=(result_YUV[pix-1]+result_YUV[pix-img.width])/2;	
				
				
				//hop0=(4*result_YUV[pix-1]+0*result_YUV[pix-img.width])/4;
			}
			
				
			//upper side of block. not includes right corner
			//-----------------------------------------
		    else if (y==b.yini && x>0 && x!=b.downsampled_xfin) {
		    //	System.out.println("j");
		    	hop0=(4*result_YUV[pix-1]+3*img.boundaries_YUV[0][pix+1-img.width])/7;	
		    	//hop0=(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix+1-img.width])/6;
		    	//hop0=(4*result_YUV[pix-1]+1*img.boundaries_YUV[0][pix+1-img.width])/5;
		    	//hop0=result_YUV[pix-1];
		    	
		    	//hop0=oc;
		    }
			//up-right CORNER of block
			//-----------------------------------------------------
		    else if ((x==b.downsampled_xfin) && (y==b.yini)) {
		    	//System.out.println("k");
		    	
		    	
				//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;
				hop0=(4*result_YUV[pix-1]+2*img.boundaries_YUV[0][pix-img.width])/6;
				
				//aqui no se pueden usar los boundaries porque no estan escalados para este bloque
				//hop0=(4*result_YUV[pix-1]+3*img.boundaries_YUV[0][pix-img.width+1])/7;
				//if (result_YUV[pix-img.width]==0) System.out.println(" ey");
			}
		    	
			//hop0=oc;
			 /*
			 if ((x==b.xini) && (y==b.yini)){
				// hop0=oc;	 
			 }
			 */
			// System.out.println(" B last_small_hop:"+last_small_hop);
			 
			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//hop0 is prediction
			//if (hop0>255)hop0=255;
			//else if (hop0<0) hop0=0; 


			// el array PCC habria que escogerlo en funcion de kini
			
			//if (hop0>128) hop1=(int)Math.max(hop1,(float)hop0*0.03f);
			//if (hop0>200 && hop1<5) hop1=5;//(int)Math.max(hop1,(float)hop0*0.03f);
			//System.out.println("hola");
			//positive hops computation
			//-------------------------
			//hop1=8;
			//rmax=40;
			/*max_hop1=10;//4*rmax;
			if (max_hop1>19) max_hop1=19;
			if (max_hop1<6) max_hop1=6;
			*/
			
			//29/12/2014
			//min_hop1=(int)(0.5f+(float)hop0*0.04f);//no puede ser cero
			//if (min_hop1<=2) min_hop1=2;
			//if (hop1<min_hop1) hop1=min_hop1;
			//max_hop1=(int)((float)min_hop1*2.5f);
			//if (max_hop1>=20) max_hop1=19;
			//if (hop1>max_hop1) hop1=max_hop1;
			
			//min_hop1=(int)(0.5f+(float)hop0*0.08f);//no puede ser cero
			//if (min_hop1<6) min_hop1=6;
			//if (hop1<min_hop1) hop1=min_hop1;
			//max_hop1=12;
			
			int hop0i=(int)( hop0+0.5f);
			
			//int inf=pccr[hop1][hop0i][rmax][4];;
			//int sup=pccr[hop1][hop0i][rmax][4];;
			//===================================================================================================
			//OJO LA TECNICA DEL "COLIN" CONSISTE EN ASIGNAR EL PUNTO MEDIO DEL INTERVALO EN LUGAR DEL HOP
			//DA MEJOR RESULTADO PERO LIMITA EL MAYOR PSNR ALCANZABLE
			//ADEMAS HAGO UN PEQUEÑO AJUSTE SUMANDO O RESTANDO 1 A LOS POSITIVOS Y NEGATIVOS RESPECTIVAMENTE
			//POR ULTIMO EL HOP NULO (el 4) SE QUEDA COMO ESTA. PARA ELLO PRIMERO RESTO 1 YA QUE LUEGO SE SUMA 1
			//===================================================================================================
			//
			 
			//colin_activo=false;
			//if (colinoff) colin_activo=false;
			//if (b.PRavg>0.4)colin_activo=false;
			int[] colin= new int[9];
			colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
			colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
			colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
			colin[3]=pccr[hop1][hop0i][rmax][3];
			colin[5]=pccr[hop1][hop0i][rmax][5];
		//	colin[8]=pccr[hop1][hop0i][rmax][8]+(pccr[hop1][hop0i][rmax][8]-pccr[hop1][hop0i][rmax][7])/2;
			//colin[0]=pccr[hop1][hop0i][rmax][0]-(pccr[hop1][hop0i][rmax][1]-pccr[hop1][hop0i][rmax][0])/2;
			//esto imprime el ultimo hop number
			//System.out.println("hop1:"+hop1+ "   hop0:"+hop0+" oc:"+oc+ "   hop_number:"+hop_number);
			
			int startcolin=6;
			int endcolin=3;
			//if (!small_colin) {startcolin=6;endcolin=3;}
			
			for (int j=startcolin; j<8;j++)
			   //colin[j]=(int)(1f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
				{colin[j]=(int)(+1f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
			//colin[j]=(colin[j]+pccr[hop1][hop0i][rmax][j])/2;
				}
				
				//colin[j]=(int)((((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
			for (int j=1; j<endcolin;j++)
			   //colin[j]=(int)(-0.5f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
			{	colin[j]=(int)(-0.5f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
		       //colin[j]=(int)((((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
			//colin[j]=(colin[j]+pccr[hop1][hop0i][rmax][j])/2;
			}
			//colin[7]=pccr[hop1][hop0i][rmax][7];
			//colin[1]=pccr[hop1][hop0i][rmax][1];
			//System.out.println( "h4:"+pccr[hop1][hop0][rmax][1]+"    colin4:"+colin[1]);
			
			int colorin=-1;
			
			if (oc-hop0>=0) //hop0 es el flotante. 
			{
				for (int j=4;j<=8;j++) {
					e2=oc-pccr[hop1][hop0i][rmax][j];
					//e2=oc-colin[j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;if (e2<4) break;}
					else break;
				}
			}
			
			//negative hops computation
			//-------------------------
			else 
			{
				//OPTIMIZACION
				//creo que puedo evitar el j=4 NO, no se puede, pues el valor puede estar 
				//mas cerca del hop nulo que del hop -1
				for (int j=4;j>=0;j--) {
					e2=pccr[hop1][hop0i][rmax][j]-oc;
					//e2=oc-colin[j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;if (e2<4) break;}
					else break;
				}
			}
			
			//if (b.PRavg>=0.5) colin_activo=false;
				//colin_activo=false;
			if (colin_activo )//&& hop1==max_hop1)
			//if (colin_activo && hop1==max_hop1)
				{
				/*
				if (hop_number>=5 )//&& hop1==max_hop1)
				{
				if (Math.abs(oc-colin[hop_number])>= Math.abs(oc-colin[hop_number-1]))	hop_number--;	
				//System.out.println("holaaaaaaa");
				}
				else if (hop_number<=3 )//&& hop1==max_hop1)
				{
				if (Math.abs(oc-colin[hop_number])>= Math.abs(oc-colin[hop_number+1]))	hop_number++;	
				
				}
				*/
				
				colorin=colin[hop_number];
			}
			
			else colorin=pccr[hop1][hop0i][rmax][hop_number];
			//System.out.println("hop1:"+hop1+ "   hop0:"+hop0+" oc:"+oc+ "hop_number:"+hop_number+ "color:"+colorin);
			//if (hop_number==0) System.exit(0);
			
			if (colorin<1)colorin=1;
			if (colorin>255)colorin=255;
			
			result_YUV[pix]=colorin;//pccr[hop1][hop0][rmax][hop_number];
			//result_YUV[pix]=pccr[hop1][hop0i][rmax][hop_number];
			//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
			//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
			hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

			//System.out.println("hola");
			
			
			//tunning hop1 for the next hop
			//-------------------------------
			//System.out.println("minhop1:"+min_hop1+ "    hop_number:"+hop_number);//+ "   hop0:"+hop0+" oc:"+oc+ "hop_number:"+hop_number);
			boolean small_hop=false;
		//	boolean medium_hop=false;
			//if (hop_number>=6) small_hop=true;
			//if (hop_number<=6 && hop_number>=2) small_hop=true;
			//if (hop_number<=5 && hop_number>=3) small_hop=true;
			if (hop_number<=4 && hop_number>=4) small_hop=true; //HE CAMBIADO ESTO!!!! ahora es solo 4 antes era 345
			//else if (hop_number<=5 && hop_number>=3) medium_hop=true;
			//if (hop_number<=6 && hop_number>=2) medium_hop=true;   

		
			
			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				//colin_activo=true;
				//small_colin=true;
				// colin_activo=true;
				//rmax=20;
				//if (rmax<20) rmax=20;
				//hop1=min_hop1;
				//hop1b=hop1b-1;
				if (hop1<min_hop1) 
					{hop1=min_hop1;
					
					}
				//hop1=max_hop1;
				//if (hop1b<1 ) hop1b=1;
			}
			
			
			else 
				
			
			{ //colin_activo=false;
				//small_colin=false;
				hop1=max_hop1;//(min_hop1+max_hop1)/2;
				//rmax=rmaxini;
				//hop1b=hop1;
			}
			
			//System.out.println(" lastsmall:"+last_small_hop+"    small:"+small_hop);
			
			//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			
			//last_hop=hop_number;
			
			//pix++;            
		}//for x
		//esto es necesario al funcionar con bloques
		//pix+=img.width-b.lx_sc+1;
	}//for y

}//end function

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeOneHopPerPixel_LHE2_experimento38(int[] hops,int[] result_YUV)
{
	System.out.println("quantizying...hello");
	
	int max_hop1=10;//0;//8;//8;//16;//8;// hop1 interval 4..8
	int min_hop1=8;//6;//4;// 
	int start_hop1=(max_hop1+min_hop1)/2;
	int rmax=27;
	
	int hop1=start_hop1;//max_hop1;
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small

	
	
	float error_center=0;
	float error_avg=0;
	
	

	int counter_resta=0;
	
	for (int y=0;y<img.height;y++)  {
		
		float gradx=0f;
		float acumulx=0f;
		
		for (int x=0;x<img.width;x++)  {

			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				
				//if (y%2==0) hop0= result_YUV[pix-1];//esto es mejorable
				//else hop0= result_YUV[pix-img.width];
				
				//int deltax=0;
				//if (x>1)
				//{
				//	deltax=result_YUV[pix-1]-result_YUV[pix-2];
				//	if (deltax>hop1) deltax=0;//max_hop1;
				//}
				//hop0=result_YUV[pix-1];//+ deltax/3;
				//if (hop0>255) hop0=255;
				//if (hop0<1) hop0=1;
				//hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2; //fast approach (good enough)
				
				
				//hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7; //slow approach	
				
				//hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2; //fast approach
				//hop0=(result_YUV[pix-1]);
				
				System.out.println("hola");
				int m=1;
				gradx=0;
				//for (int i=0; i<m ;i++)
				//{
					
				   gradx=(result_YUV[pix-1]-result_YUV[pix-2]);//+
							//(result_YUV[pix-2]-result_YUV[pix-3]);
					//gradx=gradx/2f;
					if (result_YUV[pix-1]>result_YUV[pix-2]) gradx=1.0f;
					if (result_YUV[pix-1]<result_YUV[pix-2]) gradx=-1.0f;		
					if (gradx> 4) gradx=4;
					if (gradx<-4) gradx=-4;
					
					if( hop1<max_hop1-2) gradx=result_YUV[pix-1]-result_YUV[pix-2];
					else {gradx=0;acumulx=0;}
					//gradx=0;
					gradx=result_YUV[pix-1]-result_YUV[pix-2];
					float u=0.25f;
					if (gradx>u) gradx=1;
					if (gradx<-u) gradx=-1;
					
					int cosa=(result_YUV[pix-1]-result_YUV[pix-2])/2;
							
				//}
				//System.out.println ("gradx ="+gradx);
				//gradx=gradx/(float)m;
				
				acumulx=acumulx+gradx;
				hop0=(result_YUV[pix-1]+(int)acumulx); //fast approach
				//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-2])/6; //fast approach
				acumulx=acumulx-(int)acumulx;
				//hop0=result_YUV[pix-1];
				if (hop0>255) hop0=255;
				if (hop0<1) hop0=1;
				//System.out.println ("gradx"+gradx);
				//hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2; //fast approach
				 
				hop0= result_YUV[pix-1];
				//hop0=hop0-cosa/2;
				//if (hop0>255) hop0=255;
				//if (hop0<1) hop0=1;
				hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2; //fast approach
				
				
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				
				
				last_small_hop=false;
				
				
				//hop1=max_hop1;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			
			//paeth
			/*
			int A=0;
			int B=0;
			int C=0;
			if (x>0) A=result_YUV[pix-1];
			if (y>0) B=result_YUV[pix-img.width];
			if (x>0 && y>0) C=result_YUV[pix-img.width-1];
			if (x==0) {A=B;C=B;}
			if (y==0) {B=A;C=A;}
			if (x==0 && y==0)hop0=oc;
			else
			{
				int pred=A+B-C;
				int a1=Math.abs(A-pred);
				int b1=Math.abs(B-pred);
				int c1=Math.abs(C-pred);
				if (a1<=b1 && a1<=c1) hop0=A;
				else if (b1<=a1 && b1<=c1) hop0=B;
				else hop0=C;
				//System.out.println("paeth");
			}
			*/
			
			

			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//hop0 is prediction
			//if (hop0>255)hop0=255;
			//else if (hop0<0) hop0=0; 

			//max_hop1=16;//(int)((float)hop0 *0.02f +0.5f);
			//System.out.println("hop1max:"+max_hop1);
			//positive hops computation
			//-------------------------
			//hop1=8;
			
			//min_hop1=(int)(0.5f+(float)hop0*0.04f);//no puede ser cero
			//if (min_hop1<4) min_hop1=4;
			//if (hop1<min_hop1) hop1=min_hop1;
			int wf=4;
			
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
				//for (int j=4;j<=5;j++) {
					e2=oc-pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;
					if (e2<wf) break;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
				//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
				//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
				for (int j=4;j>=0;j--) {
				//	for (int j=4;j>=3;j--) {
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;
					if (e2<wf) break;}
					else break;
				}
			}

			//29/12/2014
			
			
			//colin
			
			//rmax=25;
			int hop0i=pccr[hop1][hop0][rmax][4];
			int[] colin= new int[9];
			colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
			colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
			colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
			colin[3]=pccr[hop1][hop0i][rmax][3];
			colin[5]=pccr[hop1][hop0i][rmax][5];
		
			int startcolin=6;
			int endcolin=3;
			
			for (int j=startcolin; j<8;j++)
				{colin[j]=(int)(+1f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
				}
				
			for (int j=1; j<endcolin;j++)
			{	colin[j]=(int)(-0.5f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
		    }
			
			
			// correccion
			int mini=2;//2;//2;
			int maxi=6;//6;//6;
			img.LHE2_removed_pix[pix]=255;//oc;
			
			
			boolean quitar=true;
			if (quitar)
			{
			//int up=3;	
			//int left=5;
			//if (y>0 ) up=hops[pix-img.width];
			//if (x>0) left=hops[pix-1];
			//int criteria= y%2;
			//int criteriax=1;
			boolean removed=false;
			
			
					    if (y%2==1 && x>1 && y>0)
					    {
						   //if (hops[pix-1]>=mini && hops[pix-1]<=maxi)
						   if (hops[pix-img.width]>=mini && hops[pix-img.width]<=maxi)
					    	{
								hop_number=4;
								counter_resta+=1;
								img.LHE2_removed_pix[pix]=0;
								removed=true;
							}
						}
					    
			            if (!removed && x%2==1 && y>0 && x>0)
					    
					    
					    	//if (x%criteria!=1 )
					    	//	if (x%2==criteriax )
					    //if (x%2==1 && y%2!=1)
							//if (y>1 && x<511 && x>1)	
							{
							   //if (hops[pix-img.width]==4 && hops[pix-1]==4)
							   //if (hops[pix-img.width]==4)// && hops[pix-1]==4)
								//   if (hops[pix-1]>=mini && hops[pix-1]<=maxi)
					    	  // if (hops[pix-img.width]>=mini && hops[pix-img.width]<=maxi)
			            	if (hops[pix-1]>=mini && hops[pix-1]<=maxi)
								{
									
									hop_number=4;
									counter_resta+=1;
									img.LHE2_removed_pix[pix]=0;
									removed=true;
								}
							}
					   
					    //if (1>2)
			            if (!removed)
					    if (y%2!=1 && x%2!=1 && x>2 && x<511 && y>2)
					    if (x>2 && y>2)	
					    {
					    	//System.out.println("hola");
					    	//if (img.LHE2_removed_pix[pix-2]!=0) //no quitado
					    	//if (img.LHE2_removed_pix[pix-2*img.width]!=0)//no quitado
					    	
					    	//if (x%2==1 && y%2==1)//el pixel que queda
					    	//img.LHE2_removed_pix[pix]=128;	
					    	if (hops[pix-2]>=4 && hops[pix-2*img.width]>=4)//nulo
					    	if (hops[pix-2]<=4 && hops[pix-2*img.width]<=4)//nulo
					    	//if (hops[pix-2]>=4 && hops[pix-2]<=4)
					    	//if (hops[pix-2*img.width-2]<=4 && hops[pix-2*img.width-2]<=4)//nulo	
					    	
					    	if (img.LHE2_removed_pix[pix-2]==255)//oc;
					    	if (img.LHE2_removed_pix[pix-2*img.width]==255)//oc;
					    	
					    	//if (img.LHE2_removed_pix[pix-2*img.width-2]==255)//oc;	
					    	
					    		//if (1>2)
					    	//if ( hops[pix-2*img.width-2]==4)//nulo	
					    	//if (hops[pix-2]>=mini && hops[pix-2]<=maxi)//nulo
					    	//if (hops[pix-1]<=mini && hops[pix-1]>=maxi)//nulo
					    	//if (hops[pix-2]>=4 && hops[pix-2]<=4)//nulo	
					    	//if (hops[pix-3*img.width]>=4 && hops[pix-3*img.width]<=4) //nulo	
					    	//if (hops[pix-2*img.width]>=4 && hops[pix-2*img.width]<=4) //nulo
					    	//if (hops[pix-img.width]>=mini && hops[pix-img.width]<=maxi) //nulo
					    	//if (1>2)
							{
								
								hop_number=4;
								counter_resta+=1;
								img.LHE2_removed_pix[pix]=0;
								removed=true;
								//System.out.println ("Hola");
							}
					    }
					    
				
			}
		 // System.out.println ("Hola");
			
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][rmax][hop_number];
			
			//System.out.print (result_YUV[pix]+" "+hop_number);
			//result_YUV[pix]=colin[hop_number];
			//if (removed)
			{
				//result_YUV[pix]=result_YUV[pix-img.width]
			}
			
			
			//result_YUV[pix]=colin[hop_number];//pccr[hop1][hop0][25][hop_number];
			
			//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
			//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
			hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

			
			//calculo de errores medios
			//---------------------------
			error_center+=(oc-result_YUV[pix]);
			error_avg+=Math.abs((oc-result_YUV[pix]));
			
			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			//if (hop_number>=6) small_hop=true;
			//if (hop_number<=6 && hop_number>=2) small_hop=true;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			pix++;            
		}//for x
	}//for y
	
	/*
	}//iterations
	
	long end_time = System.currentTimeMillis();
	double total_time=end_time-start_time;
	double tpp=total_time/(img.width*img.height*iterations);
	double tpi=total_time/(iterations);
	System.out.println("tiempo_total:"+total_time+"  tpp:"+tpp+" ms"+ " tpi:"+tpi +" ms");
	*/
	System.out.println("quantization done");
	
	System.out.println("center of  error:"+error_center/(img.width*img.height));
	System.out.println("average of  error:"+error_avg/(img.width*img.height));
	System.out.println("----------------------------------------------------------");
	System.out.println("counter_resta="+counter_resta);
	
	LHE2_resta=counter_resta;
	postfilter_LHE2(hops,result_YUV);
	//postfilter_LHE2_v002(hops,result_YUV);
	System.out.println("hola");
	postresta_LHE2_v001(hops,result_YUV);
}//end function
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeOneHopPerPixel_LHE2_experimento39(int[] hops,int[] result_YUV)
{
	System.out.println("quantizying...hello");
	
	int max_hop1=10;//0;//8;//8;//16;//8;// hop1 interval 4..8
	int min_hop1=8;//6;//4;// 
	int start_hop1=(max_hop1+min_hop1)/2;
	int rmax=27;
	
	int hop1=start_hop1;//max_hop1;
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small

	
	
	float error_center=0;
	float error_avg=0;
	
	

	int counter_resta=0;
	
	for (int y=0;y<img.height;y++)  {
		
		float gradx=0f;
		float acumulx=0f;
		
		for (int x=0;x<img.width;x++)  {

			oc=img.YUV[0][pix];

			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				
				//if (y%2==0) hop0= result_YUV[pix-1];//esto es mejorable
				//else hop0= result_YUV[pix-img.width];
				
				//int deltax=0;
				//if (x>1)
				//{
				//	deltax=result_YUV[pix-1]-result_YUV[pix-2];
				//	if (deltax>hop1) deltax=0;//max_hop1;
				//}
				//hop0=result_YUV[pix-1];//+ deltax/3;
				//if (hop0>255) hop0=255;
				//if (hop0<1) hop0=1;
				//hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2; //fast approach (good enough)
				//hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7; //slow approach	
				
				hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2; //fast approach
				
				int m=1;
				gradx=0;
				//for (int i=0; i<m ;i++)
				//{
					
				   gradx=(result_YUV[pix-1]-result_YUV[pix-2]);//+
							//(result_YUV[pix-2]-result_YUV[pix-3]);
					//gradx=gradx/2f;
					if (result_YUV[pix-1]>result_YUV[pix-2]) gradx=1.0f;
					if (result_YUV[pix-1]<result_YUV[pix-2]) gradx=-1.0f;		
					if (gradx> 4) gradx=4;
					if (gradx<-4) gradx=-4;
					
					if( hop1<max_hop1-2) gradx=result_YUV[pix-1]-result_YUV[pix-2];
					else {gradx=0;acumulx=0;}
					//gradx=0;
					gradx=result_YUV[pix-1]-result_YUV[pix-2];
					float u=0.25f;
					if (gradx>u) gradx=1;
					if (gradx<-u) gradx=-1;
					
					int cosa=(result_YUV[pix-1]-result_YUV[pix-2])/2;
							
				//}
				//System.out.println ("gradx ="+gradx);
				//gradx=gradx/(float)m;
				
				acumulx=acumulx+gradx;
				hop0=(result_YUV[pix-1]+(int)acumulx); //fast approach
				//hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-2])/6; //fast approach
				acumulx=acumulx-(int)acumulx;
				//hop0=result_YUV[pix-1];
				if (hop0>255) hop0=255;
				if (hop0<1) hop0=1;
				//System.out.println ("gradx"+gradx);
				//hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2; //fast approach
				 
				hop0= result_YUV[pix-1];
				//hop0=hop0-cosa/2;
				//if (hop0>255) hop0=255;
				//if (hop0<1) hop0=1;
				hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2; //fast approach
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				
				
				last_small_hop=false;
				
				
				//hop1=max_hop1;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			
			//paeth
			/*
			int A=0;
			int B=0;
			int C=0;
			if (x>0) A=result_YUV[pix-1];
			if (y>0) B=result_YUV[pix-img.width];
			if (x>0 && y>0) C=result_YUV[pix-img.width-1];
			if (x==0) {A=B;C=B;}
			if (y==0) {B=A;C=A;}
			if (x==0 && y==0)hop0=oc;
			else
			{
				int pred=A+B-C;
				int a1=Math.abs(A-pred);
				int b1=Math.abs(B-pred);
				int c1=Math.abs(C-pred);
				if (a1<=b1 && a1<=c1) hop0=A;
				else if (b1<=a1 && b1<=c1) hop0=B;
				else hop0=C;
				//System.out.println("paeth");
			}
			*/
			
			

			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//hop0 is prediction
			//if (hop0>255)hop0=255;
			//else if (hop0<0) hop0=0; 

			//max_hop1=16;//(int)((float)hop0 *0.02f +0.5f);
			//System.out.println("hop1max:"+max_hop1);
			//positive hops computation
			//-------------------------
			//hop1=8;
			
			//min_hop1=(int)(0.5f+(float)hop0*0.04f);//no puede ser cero
			//if (min_hop1<4) min_hop1=4;
			//if (hop1<min_hop1) hop1=min_hop1;
			int wf=4;
			
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
				//for (int j=4;j<=5;j++) {
					e2=oc-pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;
					if (e2<wf) break;}
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
				//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
				//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
				for (int j=4;j>=0;j--) {
				//	for (int j=4;j>=3;j--) {
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;
					if (e2<wf) break;}
					else break;
				}
			}

			//29/12/2014
			
			
			//colin
			
			//rmax=25;
			int hop0i=pccr[hop1][hop0][rmax][4];
			int[] colin= new int[9];
			colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
			colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
			colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
			colin[3]=pccr[hop1][hop0i][rmax][3];
			colin[5]=pccr[hop1][hop0i][rmax][5];
		
			int startcolin=6;
			int endcolin=3;
			
			for (int j=startcolin; j<8;j++)
				{colin[j]=(int)(+1f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
				}
				
			for (int j=1; j<endcolin;j++)
			{	colin[j]=(int)(-0.5f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
		    }
			
			
			// correccion
			int mini=2;//2;//2;
			int maxi=6;//6;//6;
			img.LHE2_removed_pix[pix]=255;//oc;
			
			
			boolean quitar=true;
			if (quitar)
			{
			//int up=3;	
			//int left=5;
			//if (y>0 ) up=hops[pix-img.width];
			//if (x>0) left=hops[pix-1];
			//int criteria= y%2;
			//int criteriax=1;
			boolean removed=false;
			
			
					    if (y%2==1 && x>1 && y>0)
					    {
						   //if (hops[pix-1]>=mini && hops[pix-1]<=maxi)
						   if (hops[pix-img.width]>=mini && hops[pix-img.width]<=maxi)
					    	{
								hop_number=4;
								counter_resta+=1;
								img.LHE2_removed_pix[pix]=0;
								removed=true;
							}
						}
					    
			            if (!removed && x%2==1 && y>0 && x>0)
					    
					    
					    	//if (x%criteria!=1 )
					    	//	if (x%2==criteriax )
					    //if (x%2==1 && y%2!=1)
							//if (y>1 && x<511 && x>1)	
							{
							   //if (hops[pix-img.width]==4 && hops[pix-1]==4)
							   //if (hops[pix-img.width]==4)// && hops[pix-1]==4)
								//   if (hops[pix-1]>=mini && hops[pix-1]<=maxi)
					    	  // if (hops[pix-img.width]>=mini && hops[pix-img.width]<=maxi)
			            	if (hops[pix-1]>=mini && hops[pix-1]<=maxi)
								{
									
									hop_number=4;
									counter_resta+=1;
									img.LHE2_removed_pix[pix]=0;
									removed=true;
								}
							}
					   
					    //if (1>2)
			            if (!removed)
					    if (y%2!=1 && x%2!=1 && x>2 && x<511 && y>2)
					    if (x>2 && y>2)	
					    {
					    	//System.out.println("hola");
					    	//if (img.LHE2_removed_pix[pix-2]!=0) //no quitado
					    	//if (img.LHE2_removed_pix[pix-2*img.width]!=0)//no quitado
					    	
					    	//if (x%2==1 && y%2==1)//el pixel que queda
					    	//img.LHE2_removed_pix[pix]=128;	
					    	if (hops[pix-2]>=4 && hops[pix-2*img.width]>=4)//nulo
					    	if (hops[pix-2]<=4 && hops[pix-2*img.width]<=4)//nulo
					    	//if (hops[pix-2]>=4 && hops[pix-2]<=4)
					    	//if (hops[pix-2*img.width-2]<=4 && hops[pix-2*img.width-2]<=4)//nulo	
					    	
					    	if (img.LHE2_removed_pix[pix-2]==255)//oc;
					    	if (img.LHE2_removed_pix[pix-2*img.width]==255)//oc;
					    	
					    	//if (img.LHE2_removed_pix[pix-2*img.width-2]==255)//oc;	
					    	
					    		//if (1>2)
					    	//if ( hops[pix-2*img.width-2]==4)//nulo	
					    	//if (hops[pix-2]>=mini && hops[pix-2]<=maxi)//nulo
					    	//if (hops[pix-1]<=mini && hops[pix-1]>=maxi)//nulo
					    	//if (hops[pix-2]>=4 && hops[pix-2]<=4)//nulo	
					    	//if (hops[pix-3*img.width]>=4 && hops[pix-3*img.width]<=4) //nulo	
					    	//if (hops[pix-2*img.width]>=4 && hops[pix-2*img.width]<=4) //nulo
					    	//if (hops[pix-img.width]>=mini && hops[pix-img.width]<=maxi) //nulo
					    	//if (1>2)
							{
								
								hop_number=4;
								counter_resta+=1;
								img.LHE2_removed_pix[pix]=0;
								removed=true;
								//System.out.println ("Hola");
							}
					    }
					    
				
			}
		 // System.out.println ("Hola");
			
			//assignment of final color value
			//--------------------------------
			result_YUV[pix]=pccr[hop1][hop0][rmax][hop_number];
			
			//densidad de scanline
			
			
			//System.out.print (result_YUV[pix]+" "+hop_number);
			//result_YUV[pix]=colin[hop_number];
			//if (removed)
			{
				//result_YUV[pix]=result_YUV[pix-img.width]
			}
			
			
			//result_YUV[pix]=colin[hop_number];//pccr[hop1][hop0][25][hop_number];
			
			//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
			//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
			hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

			
			//calculo de errores medios
			//---------------------------
			error_center+=(oc-result_YUV[pix]);
			error_avg+=Math.abs((oc-result_YUV[pix]));
			
			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			//if (hop_number>=6) small_hop=true;
			//if (hop_number<=6 && hop_number>=2) small_hop=true;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				if (hop1<min_hop1) hop1=min_hop1;
			} 
			else {
				hop1=max_hop1;
			}
			//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			pix++;            
		}//for x
	}//for y
	
	/*
	}//iterations
	
	long end_time = System.currentTimeMillis();
	double total_time=end_time-start_time;
	double tpp=total_time/(img.width*img.height*iterations);
	double tpi=total_time/(iterations);
	System.out.println("tiempo_total:"+total_time+"  tpp:"+tpp+" ms"+ " tpi:"+tpi +" ms");
	*/
	System.out.println("quantization done");
	
	System.out.println("center of  error:"+error_center/(img.width*img.height));
	System.out.println("average of  error:"+error_avg/(img.width*img.height));
	System.out.println("----------------------------------------------------------");
	System.out.println("counter_resta="+counter_resta);
	
	LHE2_resta=counter_resta;
	postfilter_LHE2(hops,result_YUV);
	//postfilter_LHE2_v002(hops,result_YUV);
	System.out.println("hola");
	postresta_LHE2_v001(hops,result_YUV);
}//end function
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public int postRLC_v01(int[] hops,int[] result_YUV, int xini, int xfin, int yini, int yfin)
{
System.out.println (" entrada en postRLC:");
	int counterh0=0;
	int counterh0_prev=0;
	int mode=0; //0=nhuffman, 1=rlc
	int mode_prev=mode;
	int ahorro=0;
	int gasto=0;
	
	TAMANO_RLC=4;//numero de bits del codigo RLC
	TAMANO_condicion=8;//TAMANO_RLC; num hops nulos como condicion de cambio a modo RLC.
	
	//System.out.println("  dynamic RLC:  TAMANO_RLC="+TAMANO_RLC+"  TAMANO_condicion="+TAMANO_condicion);
	//for (int y=0;y<img.height;y++)  {
		//for (int x=0;x<img.width;x++)  {
			
	for (int y=yini;y<yfin;y++)  {
		for (int x=xini;x<xfin;x++)  {
					
	
			//if (result_YUV[y*img.width+x]==0) continue; //esto es para lhe avanzado
			
			int hop=hops[y*img.width+x];
			if (hop<0) continue;// esto es para hops que vamos a downsamplear y desaparecen
			
			mode_prev=mode;
			counterh0_prev=counterh0;
			
			
			//if (x==0 )
			//{
				//fin de scanline debe implicar volver a mode huffman
			//	mode=0;
			//	counterh0=0;
			//}
			
			//else
				if (hop==4 ) 
				{
				counterh0=counterh0+1;
			    if (mode==0 && counterh0==TAMANO_condicion) {mode=1;counterh0=0;}
				}
			
			else 
			{
				mode=0;
				counterh0=0;
			}
			
			
			if (mode==1 && counterh0==(int)Math.pow(2, TAMANO_RLC)-1) 
			{
				counterh0=0;
				ahorro+=(int)(Math.pow(2, TAMANO_RLC)-1);
				gasto+=TAMANO_RLC;
				//gasto+=1;
				
			}
			if (mode_prev==1 && mode==0)
			{
				//gasto+=TAMANO_RLC+1;
				gasto+=TAMANO_RLC;
				ahorro+=counterh0_prev+1;//un bit extra porque sabemos que el siguiente sym no es h0
				
			}
			
		}//end y
		//fin de scanline debe implicar volver a mode huffman. ya se tiene en
		//cuenta esto en el bucle..
		
	}
	
	int neto=ahorro-gasto;
	//System.out.println("ahorro:"+ ahorro+"   gasto"+gasto);
	//System.out.println("NETO:"+ neto);
	return neto;

}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public int postRLC_v02(int[] hops,int[] result_YUV, int xini, int xfin, int yini, int yfin)
{
System.out.println (" entrada en postRLC:");
	int counterh0=0;
	int counterh0_prev=0;
	int mode=0; //0=nhuffman, 1=rlc
	int mode_prev=mode;
	int ahorro=0;
	int gasto=0;
	
	TAMANO_RLC=4;//numero de bits del codigo RLC
	TAMANO_condicion=8;//TAMANO_RLC; num hops nulos como condicion de cambio a modo RLC.
	
	//System.out.println("  dynamic RLC:  TAMANO_RLC="+TAMANO_RLC+"  TAMANO_condicion="+TAMANO_condicion);
	//for (int y=0;y<img.height;y++)  {
		//for (int x=0;x<img.width;x++)  {
			
	for (int y=yini;y<yfin;y++)  {
		for (int x=xini;x<xfin;x++)  {
					
	
			//if (result_YUV[y*img.width+x]==0) continue; //esto es para lhe avanzado
			
			int hop=hops[y*img.width+x];
			if (hop<0) continue;// esto es para hops que vamos a downsamplear y desaparecen
			
			mode_prev=mode;
			counterh0_prev=counterh0;
			
			
			//if (x==0 )
			//{
				//fin de scanline debe implicar volver a mode huffman
			//	mode=0;
			//	counterh0=0;
			//}
			
			//else
				if (hop==4 ) 
				{
				counterh0=counterh0+1;
			    if (mode==0 && counterh0==TAMANO_condicion) {mode=1;counterh0=0;}
				}
			
			else 
			{
				mode=0;
				counterh0=0;
			}
			
			
			if (mode==1 && counterh0==(int)Math.pow(2, TAMANO_RLC)-1) 
			{
				counterh0=0;
				ahorro+=(int)(Math.pow(2, TAMANO_RLC)-1);
				//gasto+=TAMANO_RLC;
				gasto+=1;
				
			}
			if (mode_prev==1 && mode==0)
			{
				gasto+=TAMANO_RLC+1;
				//gasto+=TAMANO_RLC;
				ahorro+=counterh0_prev+1;//un bit extra porque sabemos que el siguiente sym no es h0
				
			}
			
		}//end y
		//fin de scanline debe implicar volver a mode huffman. ya se tiene en
		//cuenta esto en el bucle..
		
	}
	
	int neto=ahorro-gasto;
	//System.out.println("ahorro:"+ ahorro+"   gasto"+gasto);
	//System.out.println("NETO:"+ neto);
	return neto;

}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void prefilter()
{
	int [] img_tmp=new int[img.width*img.height];
	
 	for (int y=0;y<img.height;y++)
		for (int x=0;x<img.width;x++)
		{
			int pix=y*img.width+x;
			if (y>0) img_tmp[pix]=(img.YUV[0][pix]- img.YUV[0][pix-img.width])/2+128;
		}
	
 	for (int y=0;y<img.height;y++)
		for (int x=0;x<img.width;x++)
		{
			int pix=y*img.width+x;
			img.YUV[0][pix]= img_tmp[pix];
		}
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void posfilter(int[] result_YUV)
{
	int [] img_tmp=new int[img.width*img.height];
	
	//for (int y=img.height-1;y>0;y--)
	for (int y=0;y<img.height;y++)
		for (int x=0;x<img.width;x++)
		{
			int pix=y*img.width+x;
	
			if (y==0)img_tmp[pix]=result_YUV[pix];
			//if (y>0) img_tmp[pix]=(img.YUV[0][pix]- img.YUV[0][pix-img.width])/2+128;
			//if (y>0) img_tmp[pix]=(result_YUV[pix]-128)*2+img_tmp[pix-img.width];
			//if (y>0) img_tmp[pix]=(result_YUV[pix]-128)*2+img_tmp[pix-img.width];
			//if (y>0) img_tmp[pix]=(result_YUV[pix]-128)*2+result_YUV[pix-img.width];
			
			img_tmp[pix]=(result_YUV[pix]+128)*2;
			
		}
	
	
	
	for (int y=0;y<img.height;y++)
		for (int x=0;x<img.width;x++)
		{
			int pix=y*img.width+x;
			//result_YUV[pix]= result_YUV[pix]*result_YUV[pix];//img_tmp[pix];
		    //if (result_YUV[pix]>10)	result_YUV[pix]=(int)Math.pow(result_YUV[pix], 4;
			//if (result_YUV[pix]>10) result_YUV[pix]=(result_YUV[pix]-10)*10+10;
			//result_YUV[pix]=(int)Math.pow(result_YUV[pix], 2);
			//result_YUV[pix]=result_YUV[pix]*2;
			result_YUV[pix]=img_tmp[pix];
		}
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void quantizeOneHopPerPixel_improved02(int[] hops,int[] result_YUV)
{
	System.out.println("quantizying...");
	
	//prefilter();
	
	/*
	int iterations=1000;
	long start_time = System.currentTimeMillis();
	for (int xy=0;xy<iterations;xy++){
		*/
	//ESTA ES LA FUNCION BUENA
	//img.width=1920;
	//img.height=1080;

	//img.width=1280;
	//img.height=720;

	//img.width=720;
	//img.height=576;
	//img.width=429;
	//img.height=429;
	//img.width=576;
	//img.height=256;
	int max_hop1=10;//10;//8;//8;//16;//8;// hop1 interval 4..8
	int min_hop1=6;//4;//4;// 
	
	//min_hop1=8;
	//max_hop1=16;
	
	int grd=0;
	
	int start_hop1=(max_hop1+min_hop1)/2;
	int rmax=25;//40;
	rmax=27;//27;
	//hop1=8;
	
	
	int hop1=start_hop1;//max_hop1;
	int hop0=0; // predicted signal
	int emin;//error of predicted signal
	int hop_number=4;//selected hop // 4 is NULL HOP
	int oc=0;// original color
	int pix=0;//pixel possition, from 0 to image size        
	boolean last_small_hop=false;// indicates if last hop is small

	
	
	float error_center=0;
	float error_avg=0;
	
	
	float hop1f=hop1;
	
	for (int y=0;y<img.height;y++)  {
		for (int x=0;x<img.width;x++)  {

			oc=img.YUV[0][pix];
			
			//oc=oc/2+128;
			//oc=oc/hop1;
			
			//if (y>0) oc=(img.YUV[0][pix]-img.YUV[0][pix-img.width])/2+128;
			
			//if (oc>10)
			//oc=oc/2;//(int)Math.pow(oc,0.5f);
			
			//if (oc>10) oc=10+ (oc-10)/10;
			
			//if (y>0) img_tmp[pix]=(result_YUV[pix]-128)*2+img_tmp[pix-img.width];
			//if (y>0) img_tmp[pix]=(img.YUV[0][pix]- img.YUV[0][pix-img.width])/2+128;
			
			//if (y>0 ) oc=(img.YUV[0][pix]-result_YUV[pix-img.width])/2+128;
			
			
			//System.out.println("oc="+oc);
			//prediction of signal (hop0) , based on pixel's coordinates 
			//----------------------------------------------------------
			if ((y>0) &&(x>0) && x!=img.width-1){
				hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;
				
				
				
				hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2;
				hop0=hop0+grd;
				//hop0=result_YUV[pix+1-img.width];
				//	System.out.println(" result_YUV[pix-1]:"+result_YUV[pix-1]+"  result_YUV[pix+1-img.width]: "+result_YUV[pix+1-img.width]);
				
				
			}
			else if ((x==0) && (y>0)){
				hop0=result_YUV[pix-img.width];
				
				
				last_small_hop=false;
				
				
				//hop1=max_hop1;
				hop1=start_hop1;
			}
			else if ((x==img.width-1) && (y>0)) {
				hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
			}else if (y==0 && x>0) {
				hop0=result_YUV[x-1];
			}else if (x==0 && y==0) {  
				hop0=oc;//first pixel always is perfectly predicted! :-)  
			}			

			
			//paeth
			/*
			int A=0;
			int B=0;
			int C=0;
			if (x>0) A=result_YUV[pix-1];
			if (y>0) B=result_YUV[pix-img.width];
			if (x>0 && y>0) C=result_YUV[pix-img.width-1];
			if (x==0) {A=B;C=B;}
			if (y==0) {B=A;C=A;}
			if (x==0 && y==0)hop0=oc;
			else
			{
				int pred=A+B-C;
				int a1=Math.abs(A-pred);
				int b1=Math.abs(B-pred);
				int c1=Math.abs(C-pred);
				if (a1<=b1 && a1<=c1) hop0=A;
				else if (b1<=a1 && b1<=c1) hop0=B;
				else hop0=C;
				//System.out.println("paeth");
			}
			*/
			
			

			//hops computation. initial values for errors
			emin=256;//current minimum prediction error 
			int e2=0;//computed error for each hop 

			//hop0 is prediction
			//if (hop0>255)hop0=255;
			//else if (hop0<0) hop0=0; 

			//max_hop1=16;//(int)((float)hop0 *0.02f +0.5f);
			//System.out.println("hop1max:"+max_hop1);
			//positive hops computation
			//-------------------------
			
			
			//min_hop1=(int)(0.5f+(float)hop0*0.04f);//no puede ser cero
			//if (min_hop1<4) min_hop1=4;
			//if (hop1<min_hop1) hop1=min_hop1;
			
			if (oc-hop0>=0) 
			{
				for (int j=4;j<=8;j++) {
				//for (int j=4;j<=5;j++) {
					//if (j==4) rmax=20;
					//if (j==5) rmax=20;
					//if (j==6) rmax=25;
					//if (j==7) rmax=28;
					//if (j==8) rmax=30;
					
					e2=oc-pccr[hop1][hop0][rmax][j];
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;
					              //if (e2<min_hop1) break;
									if (e2<4) break;
					              }
					else break;
				}
			}
			//negative hops computation
			//-------------------------
			else 
			{
				//	System.out.println("x:"+x+" y:"+y+"   hop0:"+hop0);
				//if (cf3[hop1][hop0][4]-oc<=emin) {hop_number=8;emin=cf3[hop1][hop0][4]-oc;}
				for (int j=4;j>=0;j--) {
				//	for (int j=4;j>=3;j--) {
					//if (j==4) rmax=20;
				    //if (j==3) rmax=20;
			        //if (j==2) rmax=25;
					//if (j==1) rmax=28;
					//if (j==0) rmax=30;
					
					e2=pccr[hop1][hop0][rmax][j]-oc;
					if (e2<0) e2=-e2;
					if (e2<emin) {hop_number=j;emin=e2;
					            //if (e2<min_hop1) break;
								if (e2<4) break;
					            }
					else break;
				}
			}
//System.out.println("hola");
			//29/12/2014
			
			
			//colin
			
			//rmax=25;
			//rmax=24;
			int hop0i=pccr[hop1][hop0][rmax][4];
			int[] colin= new int[9];
			colin[4]=hop0i;//pccr[hop1][hop0i][rmax][4];// 
			colin[8]=pccr[hop1][hop0i][rmax][8];;//AJUSTE. no puedo considerar el 255 pq puede estar muy lejos
			colin[0]=pccr[hop1][hop0i][rmax][0];//AJUSTE no puedo considerar el 0 pq puede estar muy lejos
			colin[3]=pccr[hop1][hop0i][rmax][3];
			colin[5]=pccr[hop1][hop0i][rmax][5];
		
			int startcolin=6;
			int endcolin=3;
			
			for (int j=startcolin; j<8;j++)
				{colin[j]=(int)(+1f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
				}
				
			for (int j=1; j<endcolin;j++)
			{	colin[j]=(int)(-0.5f+(((float)pccr[hop1][hop0i][rmax][j-1]+(float)pccr[hop1][hop0i][rmax][j])/2f+((float)pccr[hop1][hop0i][rmax][j]+(float)pccr[hop1][hop0i][rmax][j+1])/2f)/2f);
		    }
			
			
			
			
			
			
		
			
			//assignment of final color value
			//--------------------------------
			//int hopaux=hop_number;
			//if (hop_number==5) {hop_number=4;grd=hop1/2;}
			//else if (hop_number==3) {hop_number=4;grd=-hop1/2;}
			//else grd=0;
			
			//result_YUV[pix]=colin[hop_number];
			result_YUV[pix]=pccr[hop1][hop0][rmax][hop_number];
			
			//hop_number=hopaux;
			//if (hop_number==5) {grd=-1;}
			//else if (hop_number==3) {grd=1;}
			//else  grd=0;
			
			grd=-hop_number+4; //mejora la prediccion
			//grd=(hop_number)
			
			if (y>1)
			{
				//result_YUV[pix-img.width]=result_YUV[pix-img.width]*2;
				//if (hops[pix-img.width]==5 || hops[pix-img.width]==3)result_YUV[pix-img.width]=255; 
				//if (hops[pix-img.width]>5 || hops[pix-img.width]<3)result_YUV[pix-img.width]=255;
			}
			
			//if (y>0)
			//{
			//result_YUV[pix]=(result_YUV[pix]-128)*2+result_YUV[pix-img.width];
			//}
			
			
			//if (hop_number>2) result_YUV[pix]=colin[hop_number]; COLIN NO FUNCIONA!!
			
			//if (result_YUV[pix]==0) result_YUV[pix]=1;// esto ya se hace en init
			//	System.out.println(" result:"+result_YUV[pix]+"    hop"+hop_number);
			hops[pix]=hop_number; //Le sumo 1 porque el original no usa 0

			
			//calculo de errores medios
			//---------------------------
			error_center+=(oc-result_YUV[pix]);
			error_avg+=Math.abs((oc-result_YUV[pix]));
			
			//tunning hop1 for the next hop
			//-------------------------------
			boolean small_hop=false;
			//if (hop_number>=6) small_hop=true;
			//if (hop_number<=6 && hop_number>=2) small_hop=true;
			if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
			else small_hop=false;     

			//if ((float)pix/2f==(float)(pix/2))
			//{
			
			if( (small_hop) && (last_small_hop))  {
				hop1=hop1-1;
				hop1f=hop1f-0.1f;
				if (hop1<min_hop1) hop1=min_hop1;
				if (hop1f<min_hop1) hop1f=min_hop1;
				
			} 
			else {
				hop1=max_hop1;
				hop1f=max_hop1;
			}
			
			//hop1=(int)hop1f;
			
			//}
			
			
			//hop1=6;
			
			//hop1=8;
			//else if (hop_number>=7 || hop_number<=1){hop1=max_hop1;}

			//lets go for the next pixel
			//--------------------------
			last_small_hop=small_hop;
			pix++;            
		}//for x
	}//for y
	
	/*
	}//iterations
	
	long end_time = System.currentTimeMillis();
	double total_time=end_time-start_time;
	double tpp=total_time/(img.width*img.height*iterations);
	double tpi=total_time/(iterations);
	System.out.println("tiempo_total:"+total_time+"  tpp:"+tpp+" ms"+ " tpi:"+tpi +" ms");
	*/
	System.out.println("quantization done");
	
	System.out.println("center of  error:"+error_center/(img.width*img.height));
	System.out.println("average of  error:"+error_avg/(img.width*img.height));
	System.out.println("----------------------------------------------------------");
	
	//
	
	//posfilter(result_YUV);
	
	//postRLC(hops,result_YUV);
	
}//end function
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

public void quantize_SIMPLELHE_001(int[] hops,int[] result_YUV)
{

System.out.println("quantizying LHE3...");

int max_hop1=10;//8;//10;//10;
int min_hop1=4;//4;//6; 


int start_hop1=(max_hop1+min_hop1)/2;
int rmax=25;//40;
rmax=27;//27;

rmax=25;

//rmax=40;

int hop1=start_hop1;//max_hop1;
int hop0=0; // predicted signal
int emin;//error of predicted signal
int hop_number=4;//selected hop // 4 is NULL HOP
int oc=0;// original color
int pix=0;//pixel possition, from 0 to image size        
boolean last_small_hop=false;// indicates if last hop is small



float error_center=0;
float error_avg=0;


int  prediccion=0;//0=izq, 1 = up

for (int y=0;y<img.height;y++)  {
	for (int x=0;x<img.width;x++)  {

		oc=img.YUV[0][pix];
		
		
		//prediction of signal (hop0) , based on pixel's coordinates 
		//----------------------------------------------------------
		if ((y>0) &&(x>0) && x!=img.width-1){
			hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;
			
			//prediccion simple
 		    hop0=(result_YUV[pix-1]+result_YUV[pix+1-img.width])/2;
			
 		   // if (hop_number!=4) if (prediccion==0 )prediccion=1; else prediccion=0;
 		   // if (prediccion==0) hop0=result_YUV[pix-1];
 		   // else if (y>0) hop0=result_YUV[pix-img.width];
 		    
			
		}
		else if ((x==0) && (y>0)){
			hop0=result_YUV[pix-img.width];
			last_small_hop=false;
			hop1=start_hop1;
		}
		else if ((x==img.width-1) && (y>0)) {
			hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-img.width])/6;				
		}else if (y==0 && x>0) {
			hop0=result_YUV[x-1];
		}else if (x==0 && y==0) {  
			hop0=oc;//first pixel always is perfectly predicted! :-)  
		}			

		
		
		
		

		//hops computation. initial values for errors
		emin=256;//current minimum prediction error 
		int e2=0;//computed error for each hop 

		boolean compute_hop=true;
		int maxh=6;
		int minh=2;
		
		//CON ESTA LINEA QUITAMOS HOPS. LUEGO EN LA FUNCION POSTHOPS LOS PASAMOS A VALOR -1 PARA QUE NO SE CUENTEN
		//se puede concebir como un cierto downsampling
		//se podria mejor hacer una PR unidimensional con las fluctuaciones y decidir un sampleado basado en una traducion pr a ppp
		//if (y%2==1 && hops[pix-img.width]<=maxh && hops[pix-img.width]>=minh) {compute_hop=false;hop_number=4;}
		
		
		
		//if (y>0 && y%2==0 && hops[pix-img.width]<=maxh && hops[pix-img.width]>=minh) {compute_hop=false;hop_number=4;}
		//if (x%2==1 && hops[pix-1]<=maxh && hops[pix-1]>=minh) {compute_hop=false;hop_number=4;}
		//if (y>0 && hops[pix-img.width]<=maxh && hops[pix-img.width]>=minh) {compute_hop=false;hop_number=4;}
		
		if (compute_hop)
		{
				
		if (oc-hop0>=0) 
		{
			for (int j=4;j<=8;j++) {
			//for (int j=4;j<=5;j++) {
				e2=oc-pccr[hop1][hop0][rmax][j];
				if (e2<0) e2=-e2;
				if (e2<emin) {hop_number=j;emin=e2;
				              //if (e2<min_hop1) break;
								if (e2<4) break;
				              }
				else break;
			}
		}
		//negative hops computation
		//-------------------------
		else 
		{
			for (int j=4;j>=0;j--) {
			//for (int j=4;j>=3;j--) {
				e2=pccr[hop1][hop0][rmax][j]-oc;
				if (e2<0) e2=-e2;
				if (e2<emin) {hop_number=j;emin=e2;
				            //if (e2<min_hop1) break;
							if (e2<4) break;
				            }
				else break;
			}
		}
		}//endif computehop
		
		
		//ESTE ES UN NUEVO MODO DE COMPRESION!!!!
		//if (hop_number>4 && hop_number<6) hop_number=4;
		//if (hop_number<4 && hop_number>2) hop_number=4;
		
		/*
		 if (hop_number>4)
			 if (hop_number<7) hop_number=4;
			 else hop_number=6;
		 if (hop_number<4)
			 if (hop_number>1) hop_number=4;
			 else hop_number=2;
		 */
		// if (hop_number<3 && hop_number>-1) hop_number=2;
		//else if (hop_number==5) hop_number=4;
		//else if (hop_number==3) hop_number=4;
		
		
		//assignment of final color value
		//--------------------------------
		
		int val_medio=pccr[hop1][hop0][rmax][hop_number];
		/*
		if (hop_number>5)
		{	val_medio=(pccr[hop1][hop0][rmax][hop_number]+pccr[hop1][hop0][rmax][hop_number-1])/2;}
		if (hop_number<3)
		{val_medio=(pccr[hop1][hop0][rmax][hop_number]+pccr[hop1][hop0][rmax][hop_number+1])/2;}
		*/
		
		
		//result_YUV[pix]=pccr[hop1][hop0][rmax][hop_number];
		result_YUV[pix]=val_medio;
		hops[pix]=hop_number; 
        
		//if (compute_hop==false && y%2==1) result_YUV[pix]=result_YUV[pix-img.width];
        //if (compute_hop==false && x%2==1) result_YUV[pix]=result_YUV[pix-1];
		
		
		//calculo de errores medios
		//---------------------------
		error_center+=(oc-result_YUV[pix]);
		error_avg+=Math.abs((oc-result_YUV[pix]));
		
		//tunning hop1 for the next hop
		//-------------------------------
		boolean small_hop=false;
		if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
		else small_hop=false;     

		if( (small_hop) && (last_small_hop))  {
			hop1=hop1-1;
			if (hop1<min_hop1) hop1=min_hop1;
		} 
		else {
			hop1=max_hop1;
		}
		
		//lets go for the next pixel
		//--------------------------
		last_small_hop=small_hop;
		pix++;            
	}//for x
}//for y

System.out.println("quantization done");

//System.out.println("center of  error:"+error_center/(img.width*img.height));
//System.out.println("average of  error:"+error_avg/(img.width*img.height));
System.out.println("----------------------------------------------------------");

//posfilter(result_YUV);



//posthops(hops, result_YUV);


 

}//end function


//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void posthops(int[] hops, int[] result_yuv)
{
	int maxh=6;
	int minh=2;
for (int y=0;y<img.height;y++)  {
	for (int x=0;x<img.width;x++)  {
		int pix=y*img.width+x;
		//result_yuv[pix]=255;
		if (y%2==1 && hops[pix-img.width]>=minh && hops[pix-img.width]<=maxh ) {hops[pix]=-1;
		//result_yuv[pix]=0;
		}
		//if (x%2==1 && hops[pix-1]<=maxh && hops[pix-1]>=minh) {hops[pix]=-1;}
		//if (y>0 && y%2==0 && hops[pix-1]<=maxh && hops[pix-1]>=minh){hops[pix]=-1;}
		
		
	}
}
}//end function
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

public void quantize_LHE2_experimento_40(int[] hops,int[] result_YUV)
{

System.out.println("quantizying LHE3...");

int max_hop1=10;//10;//8;//8;//16;//8;// hop1 interval 4..8
int min_hop1=6;//4;//4;// 


int start_hop1=(max_hop1+min_hop1)/2;
int rmax=25;//40;
rmax=27;//27;


int hop1=start_hop1;//max_hop1;
int hop0=0; // predicted signal
int emin;//error of predicted signal
int hop_number=4;//selected hop // 4 is NULL HOP
int oc=0;// original color
int pix=0;//pixel possition, from 0 to image size        
boolean last_small_hop=false;// indicates if last hop is small



float error_center=0;
float error_avg=0;


int  prediccion=0;//0=izq, 1 = up


int num_segments=32;

float[] pppxi=new float[num_segments];//1.0f;
for (int i=0; i<num_segments;i++)pppxi[i]=2.0f;//valores iniciales
float pppx=pppxi[0];//current pppx
float pppx_prev=pppx;



int segment_size=img.width/num_segments;

int segment_index=0;

int color=0;
int colorcount=0;

int sign=0; //0 pos 1 neg
int signprev=0;
float[] pr=new float[num_segments];
int[] counterpix=new int[num_segments];//1.0f;

int pppy=1;

//inicializo todo como si no fuese a ser tenido en cuenta. en realidad es algo 
//que hago sobre la marcha pero por alguna causa no se hace bien
for (int y=0;y<img.height;y++)  {
	
for (int x=0;x<img.width;x++)  {
	hops[y*img.width+x]=-1;
}
}


for (int y=0;y<img.height;)  {
	//for (int x=0;x<img.width;x++)  {
	segment_index=0;
	pix=y*img.width;//+(int)(pppx/2f);
	for (int x=0;x<img.width;)  {
		
		if (segment_index==num_segments) break;
		 pppx=pppxi[segment_index];
		 x=(pix%img.width);
		 
		 //if (x==0)x=x+(int)(pppx/2);
		if (pix>img.width*img.height-1) break;
		
		//oc=img.YUV[0][pix];
		
	    oc=(img.YUV[0][pix]+color)/(colorcount+1);
	    oc=img.YUV[0][pix];
	    color=0;
	    colorcount=0;
	    if (oc>255) oc=255;
	    if (oc<0) oc=0;
	    
		
		//prediction of signal (hop0) , based on pixel's coordinates 
		//----------------------------------------------------------
		if ((y>0) &&(x>0) && x!=img.width-1){
			//hop0=(4*result_YUV[pix-1]+3*result_YUV[pix+1-img.width])/7;
			
			//prediccion simple
		    //hop0=(result_YUV[pix-1]+result_YUV[pix+(int)1-img.width])/2;
			
			int ur=0;
			 ur= pix+(int)pppx-pppy*img.width;
			//else ur= pix+(int)pppx-img.width;
			//System.out.println("pix-1 :"+(pix-1)+"  "+ur+"   pppx:"+pppx+" y:"+y+" pix:"+pix);
			
			hop0=(result_YUV[pix-1]+result_YUV[ur])/2;
			
			//hop0=(result_YUV[pix-(int)(pppx_prev/2)]+result_YUV[pix+(int)pppx-img.width])/2;
			
		    //hop0=result_YUV[pix-1];
		   // if (hop_number!=4) if (prediccion==0 )prediccion=1; else prediccion=0;
		   // if (prediccion==0) hop0=result_YUV[pix-1];
		   // else if (y>0) hop0=result_YUV[pix-img.width];
		    
			
		}
		else if ((x==0) && (y>0)){
			hop0=result_YUV[pix-pppy*img.width];
			last_small_hop=false;
			hop1=start_hop1;
		}
		else if ((x==img.width-1) && (y>0)) {
			hop0=(4*result_YUV[pix-1]+2*result_YUV[pix-pppy*img.width])/6;				
		}else if (y==0 && x>0) {
			hop0=result_YUV[x-1];
		}else if (x==0 && y==0) {  
			hop0=oc;//first pixel always is perfectly predicted! :-)  
		}			

		
		
		
		

		//hops computation. initial values for errors
		emin=256;//current minimum prediction error 
		int e2=0;//computed error for each hop 

		boolean compute_hop=true;
		int maxh=6;
		int minh=2;
		
		//CON ESTA LINEA QUITAMOS HOPS. LUEGO EN LA FUNCION POSTHOPS LOS PASAMOS A VALOR -1 PARA QUE NO SE CUENTEN
		//se puede concebir como un cierto downsampling
		//se podria mejor hacer una PR unidimensional con las fluctuaciones y decidir un sampleado basado en una traducion pr a ppp
		//if (y%2==1 && hops[pix-img.width]<=maxh && hops[pix-img.width]>=minh) {compute_hop=false;hop_number=4;}
		
		
		
		//if (y>0 && y%2==0 && hops[pix-img.width]<=maxh && hops[pix-img.width]>=minh) {compute_hop=false;hop_number=4;}
		//if (x%2==1 && hops[pix-1]<=maxh && hops[pix-1]>=minh) {compute_hop=false;hop_number=4;}
		//if (y>0 && hops[pix-img.width]<=maxh && hops[pix-img.width]>=minh) {compute_hop=false;hop_number=4;}
		
		if (compute_hop)
		{
				
		if (oc-hop0>=0) 
		{
			for (int j=4;j<=8;j++) {
				e2=oc-pccr[hop1][hop0][rmax][j];
				if (e2<0) e2=-e2;
				if (e2<emin) {hop_number=j;emin=e2;
				              //if (e2<min_hop1) break;
								if (e2<4) break;
				              }
				else break;
			}
		}
		//negative hops computation
		//-------------------------
		else 
		{
			for (int j=4;j>=0;j--) {
				//System.out.println(hop0);
				e2=pccr[hop1][hop0][rmax][j]-oc;
				if (e2<0) e2=-e2;
				if (e2<emin) {hop_number=j;emin=e2;
				            //if (e2<min_hop1) break;
							if (e2<4) break;
				            }
				else break;
			}
		}
		}//endif computehop
		
		
		//assignment of final color value
		//--------------------------------
		
		result_YUV[pix]=pccr[hop1][hop0][rmax][hop_number];
		hops[pix]=hop_number; 
      
		//if (compute_hop==false && y%2==1) result_YUV[pix]=result_YUV[pix-img.width];
      //if (compute_hop==false && x%2==1) result_YUV[pix]=result_YUV[pix-1];
		
		
		//calculo de errores medios
		//---------------------------
		error_center+=(oc-result_YUV[pix]);
		error_avg+=Math.abs((oc-result_YUV[pix]));
		
		//tunning hop1 for the next hop
		//-------------------------------
		boolean small_hop=false;
		if (hop_number<=5 && hop_number>=3) small_hop=true;// 4 is in the center, 4 is null hop
		else small_hop=false;     

		if( (small_hop) && (last_small_hop))  {
			hop1=hop1-1;
			if (hop1<min_hop1) hop1=min_hop1;
		} 
		else {
			hop1=max_hop1;
		}
		
		
		
		
		//lets go for the next pixel
		//--------------------------
		
		//ahora rellenamos los pixels en funcion del ppp
		/*
		x=pix%img.width;
		int segment_size=img.width/32;
		segment_index=x/segment_size;
		if (segment_index==32) {segment_index=0;}
		//if (x<125) pppxi[segment_index]=1;
		//else  pppxi[segment_index]=4;
		
        pppxi[segment_index]=segment_index;;
	    if (pppxi[segment_index]<1) pppxi[segment_index]=1;
		if (pppxi[segment_index]>8) pppxi[segment_index]=8;//maxpppx;
		*/
		
		//pppx=pppxi[segment_index];
		
		
		
		
		
		//rellenamos los pixels futuros para poder tomar la muestra
		
		for (int i=pix+1;i<pix+(int)(pppx+0.5f);i++)
			//for (int i=pix+1;i<512-x;i++)
		{
			color=0;
			colorcount=0;
			if (i>img.width && i<(img.width*img.height)) 
				{
				if (hops[i]!=-1) continue;
				//result_YUV[i]=(result_YUV[i-img.height]+4*result_YUV[i-1])/5;
				result_YUV[i]=result_YUV[i-1];
				color+=img.YUV[0][i];
				colorcount+=1;
				
				hops[i]=-1;
				}
			
		}		
		//interpolamos bien
		if (pix-(int)pppx_prev>=0)
		{
		float alfa =(result_YUV[pix]-result_YUV[pix-(int)pppx_prev])/(pppx_prev-0.5f);
		int k=0;///1;
				for (int i=pix-(int)pppx_prev;i<pix;i++)
				{
				
					if (hops[i]!=-1) continue;
					result_YUV[i]=result_YUV[pix-(int)pppx_prev]+(int)(alfa*k);
					if (result_YUV[i]>255) result_YUV[i]=255;
					if (result_YUV[i]<0) result_YUV[i]=0;
				k++;	
				}
		}
		 
		
		
		
		last_small_hop=small_hop;
		//pix++;
		pix+=(int)(pppx+0.5f);
		int xant=x;
		x+=(int)(pppx+0.5f);
		
		//x=pix%img.width;//esto es una correccion. aun no se que falla
		//if (x>511) x=511;
		
		
		//actualizamos pppx
		
		pppx_prev=pppx;
		float maxpppx=4;//4;
		float inc=0.33f;//0.25f;//0.25f;//maxpppx/8;//0.5f;
		//int segment_size=img.width/32;
		int segment_index_prev=segment_index;
		segment_index=(pix%img.width)/segment_size;
		if (segment_index==32) segment_index=31;
		//fin de bloque
		
		
		
		int segment_sizey=4;
		//
		//if ( y>0 && (y/segment_size>(y-1)/segment_size ) && (x/segment_size>xant/segment_size ))
		if (segment_index>0 && y>0 && (y/segment_sizey>(y-1)/segment_sizey ) && (segment_index_prev!= segment_index))
		{
		//hay que actualizar pppx en el segmento i segun la pr calculada en i
		//segun la pr toca un ppp pero debemos ajustarlo
		float ppp=0;
		pr[segment_index-1]=pr[segment_index-1]/counterpix[segment_index-1];//(segment_size*segment_size);
		
		//saturacion
		//if (pr[segment_index-1]>0.5f) pr[segment_index-1]=1;
		if (pr[segment_index-1]<0.25) pr[segment_index-1]=0;
		pr[segment_index-1]=pr[segment_index-1]*2;
		
		//System.out.println(" PR:"+pr[segment_index-1]+"----------------------------------------");
		ppp=1+maxpppx-pr[segment_index-1]*maxpppx;
		
		//ppp=(segment_index-1)/4;
		
		//ppp=4;//(float)Math.random()*16;
		
		int dere=segment_index;
		if (dere==num_segments) dere= segment_index-1;
		//System.out.println("segment_index:"+segment_index+"   dere:"+dere);
		
	    if (ppp>=pppxi[segment_index-1]+inc) pppxi[segment_index-1]=(pppxi[segment_index-1]+pppxi[dere])/2 +inc;
	    if (ppp<=pppxi[segment_index-1]-inc) pppxi[segment_index-1]=(pppxi[segment_index-1]+pppxi[dere])/2 -inc;
	    
	   
	    //pppxi[segment_index-1]=ppp;//x/segment_size;;
	    
	    //System.out.println("ppp:"+ppp+"  pppi="+pppxi[segment_index-1]);
	    if (pppxi[segment_index-1]<1) pppxi[segment_index-1]=1;
		if (pppxi[segment_index-1]>maxpppx) pppxi[segment_index-1]=maxpppx;
		
		
		
	    pr[segment_index-1]=0;
	    counterpix[segment_index-1]=0;
		}
		
		
		
		
		
		
		if (segment_index_prev==segment_index)	
		{
		//actualizacion de PR del segmento
			int jump=hop_number;
			if (jump>=4) jump=jump-4;
			else jump=4-jump;
			
			if (hop_number>4) sign=1;
			else if (hop_number<4) sign=-1;
			
			//if (Math.abs(sign-signprev)==2) pr[segment_index]+=jump;
			if (jump>0) jump--;
			pr[segment_index]+=jump;
			counterpix[segment_index]++;
		}	
	
		
		
		
		signprev=sign;
		
		//System.out.println("seg:"+segment_index+" pr:"+pr[segment_index]+"   tamseg="+segment_size+" pppx:"+pppx+ " x:"+x+" xant:"+xant+ " y:"+y);
		//System.out.println("xs="+x%segment_size+"  xas="+xant%segment_size);
		
		
		
	}//for x
	
	float minpppx=255;
	for (int i=0 ;i<num_segments;i++)
	{
		if (pppxi[i]<minpppx) minpppx=pppxi[i];
	}
	if (minpppx>1)pppy=2;
	else pppy=1;
	
	
	//pppy=1;
	System.out.println("y:"+y+"  pppy:"+pppy);
	if (pppy==2 && y<img.height)
		for (int i=0 ;i<img.width;i++)
		{
			hops[(y+1)*img.width+i]=-2;
		}
	
	y+=pppy;
}//for y

System.out.println("quantization done");

//System.out.println("center of  error:"+error_center/(img.width*img.height));
//System.out.println("average of  error:"+error_avg/(img.width*img.height));
System.out.println("----------------------------------------------------------");

//posfilter(result_YUV);



//posthops(hops, result_YUV);

desentrelazado(result_YUV,hops);


}//end function


//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void desentrelazado( int[] result_yuv,int[] hops)
{
	
for (int y=0;y<img.height;y++)  {
	for (int x=0;x<img.width;x++)  {
		int pix=y*img.width+x;
		//result_yuv[pix]=255;
		//if (y>0 && y<511 && y%2==1)
		if (y>0 && y<img.height-1)
		if (hops[y*img.width+x]==-2)
				
		result_yuv[pix]=(result_yuv[pix-img.width]+result_yuv[pix+img.width])/2;
		}
		//if (x%2==1 && hops[pix-1]<=maxh && hops[pix-1]>=minh) {hops[pix]=-1;}
		//if (y>0 && y%2==0 && hops[pix-1]<=maxh && hops[pix-1]>=minh){hops[pix]=-1;}
		
		
	}

}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void prefilter_002()
{
	int [] img_tmp=new int[img.width*img.height];
	
	//for (int y=0;y<img.height;y++)
		for (int y=img.height-1;y>=0;y--)
			
		//for (int x=0;x<img.width;x++)
			for (int x=img.width-1;x>=0;x--)
		{
			int pix=y*img.width+x;
			//if (y>0) img_tmp[pix]=(img.YUV[0][pix]- img.YUV[0][pix-img.width])/2+128;
			
			//if (y>0) img.YUV[0][pix]=(img.YUV[0][pix]- img.YUV[0][pix-img.width])/2+128;
			//if (img.YUV[0][pix]<-128)img.YUV[0][pix]=-128;
			//if (img.YUV[0][pix]>127)img.YUV[0][pix]=127;
			//if (x>0)if (img.YUV[0][pix]>img.YUV[0][pix-1]+32)img.YUV[0][pix]=img.YUV[0][pix-1]+32;
			//img.YUV[0][pix]=img.YUV[0][pix]*2-128;
			//if (img.YUV[0][pix]<0) img.YUV[0][pix]=0;
			//if (img.YUV[0][pix]>255) img.YUV[0][pix]=255;
			
			//alisar
			//img.YUV[0][pix]=(int)(img.YUV[0][pix]*0.8f);
			//img.YUV[0][pix]=(int)(img.YUV[0][pix]*(1f/1.5f));
			//img.YUV[0][pix]=(int)(img.YUV[0][pix]*(1f/4f));
			//img.YUV[0][pix]=img.YUV[0][pix]+64;
			//if (img.YUV[0][pix]>255) img.YUV[0][pix]=255;
			if (y>0 && x>0 && x<511 )
			{
				//img.YUV[0][pix]=(img.YUV[0][pix-1]+img.YUV[0][pix-img.width])/2;
				//img.YUV[0][pix]=img.YUV[0][pix-1];//+img.YUV[0][pix-img.width])/2;
				//img.YUV[0][pix]=img.YUV[0][pix]- ((img.YUV[0][pix-1]+img.YUV[0][pix-img.width+1])/2);
				int prediccion=((img.YUV[0][pix-1]+img.YUV[0][pix-img.width+1])/2);
				int dif=img.YUV[0][pix]-prediccion;
				if (dif>10) dif=10;
				if (dif<-10) dif=-10;
				
				//img_tmp[pix]=prediccion+dif;
				
			}
			//img.YUV[0][pix]=img.YUV[0][pix]/4;
			
		}
	/*
	//for (int y=0;y<img.height;y++)
		for (int y=img.height-1;y>=0;y--)
		for (int x=0;x<img.width;x++)
		{
			int pix=y*img.width+x;
			//if (y>0 && x>0 && x<511 && y<511)
			//img.YUV[0][pix]= img_tmp[pix]+ (img_tmp[pix-1]+img_tmp[pix+img.width+1])/2;
			img.YUV[0][pix]= img_tmp[pix];
		}
	*/
		
		//img.down(4,4,1);
		//img.YUVtoBMP("./img/lena_down.bmp",img.YUV[0]);
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void postfilter_002()
{
	System.out.println("entrada en postfilter002");
	//int [] img_tmp=new int[img.width*img.height];
	//if (1<2) return;
	for (int y=img.height-1;y>=0;y--)
		for (int x=0;x<img.width;x++)
		{
			int pix=y*img.width+x;
			//if (y>0) img_tmp[pix]=(img.YUV[0][pix]- img.YUV[0][pix-img.width])/2+128;
			//if (y>0) img.LHE_YUV[0][pix]=(img.LHE_YUV[0][pix]-128)*2+ img.LHE_YUV[0][pix-img.width];
			//if (img.LHE_YUV[0][pix]<0) img.LHE_YUV[0][pix]=0;
			//if (img.LHE_YUV[0][pix]>255) img.LHE_YUV[0][pix]=255;
			//if (y>0) img.LHE_YUV[0][pix]=img.LHE_YUV[0][pix]*2;//(img.LHE_YUV[0][pix]-128+ img.LHE_YUV[0][pix-img.width])*2;
			//img.LHE_YUV[0][pix]=(int)(img.LHE_YUV[0][pix]*1.25f);
			//img.LHE_YUV[0][pix]=(int)(img.LHE_YUV[0][pix]*1.5f);
			//img.LHE_YUV[0][pix]=(int)(img.LHE_YUV[0][pix]+128)/2;
			//img.LHE_YUV[0][pix]=(int)(img.LHE_YUV[0][pix]*4f);
			//if (x>0 && img.hops[pix]>4) img.LHE_YUV[0][pix]=(int)(img.LHE_YUV[0][pix]+img.LHE_YUV[0][pix-1])/2;
			
		}
	/*
	for (int y=0;y<img.height;y++)
		for (int x=0;x<img.width;x++)
		{
			int pix=y*img.width+x;
			img.YUV[0][pix]= img_tmp[pix];
		}
		*/
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
}//end class