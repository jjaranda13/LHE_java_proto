package LHE;
import huffman.Huffman;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;

import Qmetrics.PSNR;

/**
 * 
 * @author josejavg
 *
 *
 *
 *
 */
public class FrameCompressor {

public boolean DEBUG=false;	//traces and intermediate files saving
public String MODE="ELASTIC"; // [ELASTIC | HOMO] : elastic downsampling or homogeneous downsampling
//public String MODE="HOMO";//ratio must belong to 1..256 corresponding to 1:1 to 1:256


public String path_img; 

public ImgUtil img; //frame to compress
public Grid grid;   //grid of blocks, and PRblocks 
private LHEquantizer lhe;
private int number_of_threads=1;//for parallel processing. default is 1

public boolean LHE=true;

public String downmode="NORMAL";
//*****************************************************************
public FrameCompressor(int number_of_threads)
{
	//constructor
	this.number_of_threads=number_of_threads;
	img=new ImgUtil();
	grid = new Grid();
	lhe=new LHEquantizer();
	
}
//******************************************************************
public void loadFrame( String path_img)
{
	if (img==null)  img=new ImgUtil();
	lhe.img=img;
	img.BMPtoYUV(path_img);
	
	//si modifico aqui el tamaño puedo probar con diferentes tamaños de imagen para hacer down
	//y asi evaluar rendimientos
	//img.width=256;
	//img.height=256;
	
	
	this.path_img=path_img;
	//create the grid for this image size
	
	//Create the grid and compute the number of blocks and the PPP_MAX 
	grid.DEBUG=DEBUG;
	grid.createGrid(img.width, img.height);

	//img.grid=grid;
	
}
//******************************************************************
//******************************************************************
public void loadFrameGridPlus( String path_img,int plus)
{
	if (img==null)  img=new ImgUtil();
	lhe.img=img;
	img.BMPtoYUV(path_img);
	
	//create the grid for this image size
	
	//Create the grid and compute the number of blocks and the PPP_MAX 
	grid.createGridPlus(img.width, img.height,plus);
	//img.grid=grid;
	
}
//******************************************************************
public float[] compressBasicFrame(String optionratio, String path_img)
{
	
	//load image into memory, transforming into YUV format
	loadFrame( path_img);
	
	//compress the loaded frame 
	return compressBasicFrame(optionratio);
}


//******************************************************************
public float[] compressFrame( String path_img, float cf)
{
	
	//load image into memory, transforming into YUV format
	loadFrame( path_img);
	
	//compress the loaded frame 
	return compressFrame(cf);
}
//******************************************************************	
/**
 * use this function if you have already an ImgUtil object created and
 * an image loaded on it
 * 
 * this is your function if the source of the frame is not a file or
 * if you want to re-use the ImgUtil object
 * 
 * @param img
 * @param cf
 */
public float[] compressBasicFrame(String optionratio)
{
	float[] result=new float[2];//PSNR and bitrate
	
	img.YUVtoBMP("./output_debug/orig_YUV_BN.bmp",img.YUV[0]);
	
	//lhe.initGeomR();//initialize hop values 
	System.out.println(" quantizing into hops...");
	System.out.println(" result image is ./output_img/BasicLHE_YUV.bmp");
	
	//esta tiene el colin
	if (optionratio.equals("1"))
	//lhe.quantizeOneHopPerPixel_R(img.hops[0],img.LHE_YUV[0]);
	//lhe.quantizeOneHopPerPixel_initial(img.hops[0],img.LHE_YUV[0]);
	
	lhe.quantizeOneHopPerPixel_improved(img.hops[0],img.LHE_YUV[0]);
	//lhe.quantizeOneHopPerPixel_improved02(img.hops[0],img.LHE_YUV[0]);
	
	//lhe.quantizeOneHopPerPixel_R_LHE2(img.hops[0],img.LHE_YUV[0]);
	
	//esta no tiene el colin
	//lhe.quantizeOneHopPerPixel(img.hops[0],img.LHE_YUV[0]);
	
	
	if (optionratio.equals("2"))
	lhe.quantizeOneHopPerPixelBin(img.hops[0],img.LHE_YUV[0]);
	
	lhe.filter_multualinfo(img.hops[0],img.LHE_YUV[0]);
	
	
	//lhe.quantizeOneHopPerPixel_prueba(img.hops[0],img.LHE_YUV[0]);
	//PRblock.img=img;
	//grid.computeMetrics();//compute metrics of all Prblocks, equalize & quantize
	//ready to save the result in BMP format
	img.YUVtoBMP("./output_img/BasicLHE_YUV.bmp",img.LHE_YUV[0]);
	
	//ready to compute PSNR
	//double psnr=PSNR.printPSNR(this.path_img, "./output_img/BasicLHE_YUV.bmp");
	double psnr=PSNR.printPSNR("./output_debug/orig_YUV_BN.bmp", "./output_img/BasicLHE_YUV.bmp");
	System.out.println(" PSNR 1st LHE:"+psnr);
	result[0]=(float)psnr;
	
	//ready for compute bit rate
	BynaryEncoder be=new BynaryEncoder(img.width,img.height);
	int total_bits=0;//total bits taken by the image
	
	//save hops
	//en modo BASIC el debug se pone a true siempre desde MainTest, asi vemos los hops en un fichero
	if (DEBUG) img.saveHopsToTxt("./output_debug/hops1stLHE_signed.txt",true);
	if (DEBUG) img.saveHopsToTxt("./output_debug/hops1stLHE_unsigned.txt",false);
	
	//convert hops into symbols...
	//realmente esto no convierte a bits, sino a simbolos del 1 al 9
	// de todos modos lo que se usa para calcular luego los bpp son las estadisticas
	// de los simbolos. es decir, cuantos hay de cada y con huffman se hace
	be.hopsToBits_v3(img.hops[0],0,0, img.width-1,img.height-1,0,0);
	
	be.saveSymbolsToTxt("./output_debug/Symbols1stLHE.txt");
	
	
	//convert symbols into bits...
	Huffman huff=new Huffman(10);
	for (int l=0;l<10;l++)
		{System.out.println(" symbolos ["+l+"]="+be.down_stats_saco[0][l]);
		
		}
	int lenbin=huff.getLenTranslateCodes(be.down_stats_saco[0]);
	
	
	System.out.println("total_hops: "+be.totalhops);
	
	System.out.println("image_bits iniciales: "+lenbin+"   bpp iniciales:"+((float)lenbin/(img.width*img.height)));
	
	System.out.println("");
	System.out.println("Results after DYNAMIC RLC:");
	System.out.println("==========================");
	System.out.println("  dynamic RLC parameters->  TAMANO_RLC="+lhe.TAMANO_RLC+"  TAMANO_condicion="+lhe.TAMANO_condicion);
	///Dynamic RLC savings
	int net_savings=lhe.postRLC_v02(img.hops[0],img.LHE_YUV[0],0,img.width-1,0,img.height-1);
	lenbin=lenbin-net_savings;	
	System.out.println("image_bits: "+lenbin+ "   bpp:"+((float)lenbin/(img.width*img.height)));
	
	result[1]=(float)lenbin/(img.width*img.height);
	
	return result;
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public float[] compressSIMPLELHE()
{
	// esta funcion tiene un coder entropico estatico, no calcula arbol huffman
	// por ello es mucho mas simple
float[] result=new float[2];//PSNR and bitrate
	
	img.YUVtoBMP("./output_debug/orig_YUV_BN.bmp",img.YUV[0]);
	System.out.println(" quantizing into hops...");
	System.out.println(" result image is ./output_img/SIMPLE_LHE_YUV.bmp");
	
	//lhe.prefilter_002();
	lhe.quantize_SIMPLELHE_001(img.hops[0],img.LHE_YUV[0]);
	//ready to save the result in BMP format
	//lhe.postfilter_002();
	
	lhe.filter_multualinfo(img.hops[0],img.LHE_YUV[0]);
	
	img.YUVtoBMP("./output_img/SIMPLE_LHE_YUV.bmp",img.LHE_YUV[0]);
	
	
	
	
	//PSNR
	double psnr=PSNR.printPSNR("./output_debug/orig_YUV_BN.bmp", "./output_img/SIMPLE_LHE_YUV.bmp");
	// psnr=PSNR.printPSNR("./output_debug/orig_YUV_BN.bmp", "./output_img/SIMPLE_LHE_YUVinter2.bmp");
	System.out.println(" PSNR LHE3:"+psnr);
	result[0]=(float)psnr;
	
	//bitrate
	BynaryEncoder be=new BynaryEncoder(img.width,img.height);
	
	int lenbin=be.hopsToBits_simple(img.hops[0], 0, 0, img.width-1, img.height-1);
	int ahorroRLC=lhe.postRLC_v02(img.hops[0],img.LHE_YUV[0],0,img.width,0,img.height);
	
	System.out.println("lenbin:"+lenbin+"   rlc savings:"+ahorroRLC);
	System.out.println("SIMPLE LHE sin RLC: "+((float)lenbin/(img.width*img.height)));
	lenbin=lenbin-ahorroRLC;
	result[1]=lenbin;
	//img.width=512;
	//img.height=512;
	System.out.println("SIMPLE LHE image_bits: "+lenbin+ "   bpp:"+((float)lenbin/(img.width*img.height)));
	System.out.println("width:"+img.width+"  height"+img.height);
	return result;
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public float[] compressSIMPLELHESAMPLED(float ratiox, int typex,float ratioy,int typey, boolean filter)
{
	// esta funcion tiene un coder entropico estatico, no calcula arbol huffman
	// por ello es mucho mas simple
    float[] result=new float[2];//PSNR and bitrate
	 
    
     
	img.YUVtoBMP("./output_debug/orig_YUV_BN.bmp",img.YUV[0]);
	
	System.out.println(" result image is ./output_img/SIMPLE_LHESAMPLED_YUV.bmp");
	
	System.out.println(" sampling...");
	
	
	//downsampling de la imagen original
	//------------------------------------
	int ancho_orig=img.width;
	int alto_orig=img.height;
	int mode=1; 
	
	//img.prefilterhist_000(img.YUV[0]);
	
	img.down((int)(img.width/ratiox),(int)(img.height/ratioy),typex,typey, img.YUV[0]);
	
	//img.prefilterhist_000(img.YUV[0]);
	//img.down((int)(img.width/2f),(int)(img.height/2f),1,1, img.YUV[0]);
	//img.filtersoft(img.YUV[0],img.hops[0]);
	//img.scale(ancho_orig,alto_orig,0,0,img.YUV[0]);
	
	img.YUVtoBMP("./output_img/SAMPLED_YUV.bmp",img.YUV[0]);
	
	System.out.println(" quantizing into hops...");
	lhe.quantize_SIMPLELHE_001(img.hops[0],img.LHE_YUV[0]);
	//ready to save the result in BMP format
	
	//bitrate
	BynaryEncoder be=new BynaryEncoder(img.width,img.height);
	
	int lenbin=be.hopsToBits_simple(img.hops[0], 0, 0, img.width-1, img.height-1);
	int ahorroRLC=lhe.postRLC_v02(img.hops[0],img.LHE_YUV[0],0,img.width,0,img.height);
	
	System.out.println("lenbin:"+lenbin+"   rlc savings:"+ahorroRLC);
	System.out.println("SIMPLE LHE sin RLC: "+((float)lenbin/(ancho_orig*alto_orig)));
	
	
	
	
	lenbin=lenbin-ahorroRLC;
	
	/*
	ahorroRLC=lhe.postRLC_v02(img.hops[0],img.LHE_YUV[0],0,img.width,0,img.height);
	lenbin=lenbin-ahorroRLC;
	System.out.println("segundo nivel RLC:"+ahorroRLC);
	*/
	
	result[1]=lenbin;

	
	//antes del escalado, de cara al video, voy a guardar la imagen "error"
	//-------------------------------
	
	//escalado
	//-------------
    img.scale(ancho_orig,alto_orig,0,0,img.LHE_YUV[0]);
	
    //img.postfilterhist_000(img.LHE_YUV[0]);
    //AQUI TENGO QUE METER EL FILTRO
    //ojo, el filtro a 1ppp produce errores, logicamente
    //if (filter) img.filterEPX(img.LHE_YUV[0],16,16);
    
	img.YUVtoBMP("./output_img/SIMPLE_LHESAMPLED_YUV.bmp",img.LHE_YUV[0]);
	
	//PSNR
	double psnr=PSNR.printPSNR("./output_debug/orig_YUV_BN.bmp", "./output_img/SIMPLE_LHESAMPLED_YUV.bmp");
	// psnr=PSNR.printPSNR("./output_debug/orig_YUV_BN.bmp", "./output_img/SIMPLE_LHE_YUVinter2.bmp");
	System.out.println(" PSNR:"+psnr);
	result[0]=(float)psnr;
	
	
	System.out.println("SIMPLE LHE image_bits: "+lenbin+ "   bpp:"+((float)lenbin/(img.width*img.height)));
	System.out.println("width:"+img.width+"  height"+img.height);
	return result;
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


public float[] compressLHE2()
{
	float[] result=new float[2];//PSNR and bitrate
	
	img.YUVtoBMP("./output_debug/orig_YUV_BN.bmp",img.YUV[0]);
	
	//lhe.initGeomR();//initialize hop values 
	System.out.println(" quantizing into hops...");
	System.out.println(" result image is ./output_img/LHE2_YUV.bmp");
	
	
	//lhe.quantizeOneHopPerPixel_LHE2(img.hops[0],img.LHE_YUV[0]);
	//lhe.quantizeOneHopPerPixel_LHE2_experimento10(img.hops[0],img.LHE_YUV[0]);
	//lhe.quantizeOneHopPerPixel_LHE2_experimento20(img.hops[0],img.LHE_YUV[0]);
	//lhe.quantizeOneHopPerPixel_LHE2_experimento30(img.hops[0],img.LHE_YUV[0]);
	//lhe.quantizeOneHopPerPixel_LHE2_experimento31(img.hops[0],img.LHE_YUV[0]);
	//lhe.quantizeOneHopPerPixel_LHE2_experimento33(img.hops[0],img.LHE_YUV[0]);
	//lhe.quantizeOneHopPerPixel_LHE2_experimento35(img.hops[0],img.LHE_YUV[0]);
	//lhe.quantizeOneHopPerPixel_LHE2_experimento36(img.hops[0],img.LHE_YUV[0]);
	//lhe.quantizeOneHopPerPixel_LHE2_experimento38(img.hops[0],img.LHE_YUV[0]);//bueno
	
	//lhe.quantizeOneHopPerPixel_LHE2_experimento39(img.hops[0],img.LHE_YUV[0]);
	//lhe.esperanza_matematica_v001(img.hops[0],img.LHE_YUV[0]);
	
	lhe.quantize_LHE2_experimento_40(img.hops[0],img.LHE_YUV[0]);
	
	img.YUVtoBMP("./output_debug/LHE2_removed.bmp",img.LHE2_removed_pix);
	
	//lhe.quantizeOneHopPerPixel_prueba(img.hops[0],img.LHE_YUV[0]);
	//PRblock.img=img;
	//grid.computeMetrics();//compute metrics of all Prblocks, equalize & quantize
	//ready to save the result in BMP format
	
	//img.filterEPX(img.LHE_YUV[0],16,16);
	img.YUVtoBMP("./output_img/LHE2_YUV.bmp",img.LHE_YUV[0]);
	
	//ready to compute PSNR
	//double psnr=PSNR.printPSNR(this.path_img, "./output_img/BasicLHE_YUV.bmp");
	double psnr=PSNR.printPSNR("./output_debug/orig_YUV_BN.bmp", "./output_img/LHE2_YUV.bmp");
	System.out.println(" PSNR LHE2:"+psnr);
	result[0]=(float)psnr;
	
	//bitrate
		BynaryEncoder be=new BynaryEncoder(img.width,img.height);
		
	int lenbin=be.hopsToBits_simple(img.hops[0], 0, 0, img.width-1, img.height-1);
	int ahorroRLC=lhe.postRLC_v02(img.hops[0],img.LHE_YUV[0],0,img.width,0,img.height);
	System.out.println("lenbin:"+lenbin+"   rlc savings:"+ahorroRLC);
	lenbin=lenbin-ahorroRLC;
	result[1]=lenbin;
	System.out.println("LHE2 image_bits: "+lenbin+ "   bpp:"+((float)lenbin/(img.width*img.height)));
	
	return result;
}




//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

/**
 * use this function if you have already an ImgUtil object created and
 * an image loaded on it
 * 
 * this is your function if the source of the frame is not a file or
 * if you want to re-use the ImgUtil object
 * 
 * @param img
 * @param cf
 */
public float[] compressFrame(float ql)
	{
		//frame must be loaded and grid created
	System.out.println( "=============================");
	System.out.println( "entrada en compressFrame()");
		//-----------------------------------------------
	System.out.println("calculando cf a partir de ql="+ql);
	
		
	float p=(float)Block.MAX_PPP;
	float prmin=0.125f;//Perceptual relevance minimum
	if (MODE.equals("HOMO")) prmin=0f;// necesario pues en homo no hay cuantos
	float prmax=1;
	float cfmin=(1f+(p-1f)*prmin)/p;
	float cfmax=1+(p-1)*prmax;//0.75f;
	double r=Math.pow(cfmax/cfmin,1/99f);
	float cf=(float)((1f/p)*Math.pow(r,(99-ql)));
	
	if (!MODE.equals("HOMO")) System.out.println("  r:"+r+"  and QL:"+ql+"   THEN cf="+cf);
	//cf=2;
		//First LHE quantization
		//lhe.img=img;
		// IMPROVEMENT PENDING:
		//coding by blocks instead globally, this part is paralellizable  N^2-->2N+1
	//lhe.initGeomR();
		//lhe.quantizeOneHopPerPixel_R(img.hops[0],img.LHE_YUV[0]);
		
		lhe.quantizeOneHopPerPixel_initial(img.hops[0],img.LHE_YUV[0]);
		//lhe.quantizeOneHopPerPixel_improved(img.hops[0],img.LHE_YUV[0]);
		// now, hops are stored at img.hops[color component][coordinate]
		// they can be saved
		
		if (DEBUG) img.saveHopsToTxt("./output_debug/hops1stLHEunsigned.txt", false);
		if (DEBUG) img.saveHopsToTxt("./output_debug/hops1stLHEsigned.txt", true);
		
		if (DEBUG)
		{
			img.YUVtoBMP("./output_debug/1stLHE_YUV.bmp",img.LHE_YUV[0]);
			double psnr=PSNR.printPSNR(this.path_img, "./output_debug/1stLHE_YUV.bmp");
			System.out.println(" PSNR 1st LHE:"+psnr);
		}
		//-------------------------------------------------
		//loadFrame("./img/peppersBN.bmp");
		
		
		//PR metrics and PPP assignment
		PRblock.img=img;
		if (DEBUG) System.out.println(" computing PR metrics...");
		System.out.println(" computing metrics...");
		grid.computeMetrics();//compute metrics of all Prblocks, equalize & quantize
		//---------------------------------------------------
		//compute average PR per block and stores it on each block object
		//This value is used for adjust ratio of hop series at 2nd LHE encoding
		grid.computePRavgPerBlock();
		//---------------------------------------------------
		//assign PPP at each block's corners based on PR value at each block's corners 
		if (!MODE.equals("HOMO")) 
			//img.grid.fromPRtoPPP(grid.compression_factor);
			//img.grid.fromPRtoPPP(cf);
			grid.fromPRtoPPP(cf);//ademas de convertir, establece la max elasticidad a 3
		else 
		{
			System.out.println("MODE is HOMOGENEOUS");
			float ratio=cf;//the meaning of cf in homogeneous mode is the ratio 1:N (ratio is N)
			System.out.println("ratio 1:"+ql);
			ratio=ql;
			grid.setPPPHomogeneousDownsamplingRatio(ratio);
		}
		//--------------------------------------------------
		//downsampling
		
		/*System.out.println ("down mode?: 0=AVG_NORMAL, 1=SPS_ONESHOT");
		String option =  readKeyboard();
		if (option.equals("1")) grid.downmode="SPS_ONESHOT";
		else
		 
		grid.downmode=downmode;
		*/
		
		BynaryEncoder be=new BynaryEncoder(img.width,img.height);
		int total_bits=0;//total bits taken by the image
		
		
        if (DEBUG) System.out.println(" downsampling...");
		//this part is paralellizable N^2-->2N+1
		Block.img=img;
		
		img.hops=new int[3][img.width*img.height];//los dejo a cero	
		//for (int ju=0;ju<img.width*img.height;ju++) img.hops[0][ju]=25;
		
		//img.BMPtoYUV("./img/peppersBN.bmp");
		
		//for (int ku=0;ku<img.width*img.height;ku++)img.hops[0][ku]=100;//<---los inicializo a 100 para que de error si algo va mal
		//for ( int y=0 ; y<img.grid.number_of_blocks_V;y++)
		
		//RLC savings 
		int net_savings=0;
		
		for ( int y=0 ; y<grid.number_of_blocks_V;y++)
		{
			//for ( int x=0 ; x<img.grid.number_of_blocks_H;x++)
			for ( int x=0 ; x<grid.number_of_blocks_H;x++)
			{
				//take the block
				//Block bi=img.grid.bl[y][x];
				Block bi=grid.bl[y][x];
				
				//adapt corner's PPP to rectangle shape
				bi.pppToRectangleShape();

				//downsampling the block
				//bi.computeDownsampledLengths();
				
				
				//long lDateTime1 = new Date().getTime();
				//System.out.println("Date() - Time in milliseconds: " + lDateTime1);
				//for (int cosa=1;cosa<10000;cosa++)
				//{
				bi.downmode=downmode;	
				bi.downsampleBlock(true);//lo de true es mix , no SPS
				
			    //}
				//long lDateTime2 = new Date().getTime();
				//System.out.println("Date() - Time in milliseconds: " + lDateTime2);
				//System.out.println("total: " + (lDateTime2-lDateTime1));
			
				
				
				
				//before 2nd LHE, compute boundaries of adyacent blocks.    
				//we asume the boundaries are already interpolated
				
				
				bi.downsampleBoundariesH_FIX(img.boundaries_YUV,img.boundaries_inter_YUV);//, img.grid.bl[y+1][x]);
				bi.downsampleBoundariesV_FIX(img.boundaries_YUV,img.boundaries_inter_YUV);//,img.grid.bl[y][x+1]);
				
				
				//2nd LHE
				//boolean LHE=true;
				if (LHE==true)
				{
					//el R es el bueno. R de ratio
			    
					//lhe.quantizeDownsampledBlock_R(bi, img.hops[0],img.downsampled_LHE_YUV[0], img.downsampled_YUV[0],img.boundaries_YUV[0] );
				//lhe.quantizeDownsampledBlock_R3(bi, img.hops[0],img.downsampled_LHE_YUV[0], img.downsampled_YUV[0],img.boundaries_YUV[0] );
					
					
				lhe.quantizeDownsampledBlock_R4(bi, img.hops[0],img.downsampled_LHE_YUV[0], img.downsampled_YUV[0],img.boundaries_YUV[0] );
				//lhe.quantizeDownsampledBlock_R4_improved(bi, img.hops[0],img.downsampled_LHE_YUV[0], img.downsampled_YUV[0],img.boundaries_YUV[0] );
				
				//lhe.quantizeDownsampledBlock_R2(bi, img.hops[0],img.downsampled_LHE_YUV[0], img.downsampled_YUV[0],img.boundaries_YUV[0] );
				net_savings+=lhe.postRLC_v02(img.hops[0],img.downsampled_LHE_YUV[0],bi.xini,bi.downsampled_xfin, bi.yini,bi.downsampled_yfin);
				
				//esta es sin boundaries:
				    //lhe.quantizeDownsampledBlock_SinBoundaries(bi, img.hops[0],img.downsampled_LHE_YUV[0], img.downsampled_YUV[0],img.boundaries_YUV[0] );
				
				
				
				//lhe.quantizeDownsampledBlock_R(bi, img.hops[0],img.downsampled_LHE_YUV[0], img.downsampled_YUV[0],img.YUV[0] );
				
				
					//lhe.quantizeDownsampledBlock_R(bi, img.hops[0],img.downsampled_LHE_YUV[0], img.YUV[0],img.boundaries_YUV[0] );
				}
				else
				 img.downsampled_LHE_YUV[0]=img.downsampled_YUV[0];
			    
			    
			    
			    
				//interpolate boundaries for next blocks located at right and below
				bi.interpolateBoundariesVneighbour(img.downsampled_LHE_YUV,img.boundaries_inter_YUV);
				bi.interpolateBoundariesHneighbour(img.downsampled_LHE_YUV,img.boundaries_inter_YUV);
				
				//con esto descarto fallos en down boundaries y en interboundaries
				//img.boundaries_inter_YUV=img.YUV;
				
				//MAL
				//bi.interpolateBoundariesVlinear(img.downsampled_LHE_YUV,img.boundaries_inter_YUV);
				//bi.interpolateBoundariesHlinear(img.downsampled_LHE_YUV,img.boundaries_inter_YUV);
				
				//BIEN pero peor que vecino
				//bi.interpolateBoundariesVlinear(img.boundaries_inter_YUV,img.downsampled_LHE_YUV);
				//bi.interpolateBoundariesHlinear(img.boundaries_inter_YUV,img.downsampled_LHE_YUV);
				
				
				//aqui podemos hacer el binencoder
				//total_bits+=be.hopsToBits_v2(img.hops[0],bi.xini,bi.yini, bi.downsampled_xfin,bi.downsampled_yfin,bi.PRavgx,bi.PRavgy);
				
				//convert hops into symbols
				total_bits+=be.hopsToBits_v3(img.hops[0],bi.xini,bi.yini, bi.downsampled_xfin,bi.downsampled_yfin,bi.PRavgx,bi.PRavgy);
				
				
			}//end for x
        //if (DEBUG) System.out.print(".");
		}//end for y
		
		//-----------------NUEVO----------------------------------
		// comprimimos por huffman al final
		/*
		Huffman huff=new Huffman(9);
		int lendown=huff.getLenTranslateCodes(be.down_stats);
		total_bits=lendown;
		*/
		//NUEVO 4 sacos. AL FINAL SOLO HAY UNO
		total_bits=0;
		Huffman huff=new Huffman(10);
		System.out.println("comprimiendo por huffman el saco de codes");
		for (int saco=0;saco<4;saco++)
		{
			int saco_items=0;
			for (int i=0;i<10;i++) saco_items+=be.down_stats_saco[saco][i];
			System.out.println("saco items:"+saco_items);
			if (saco_items>0)
			{int lendown=huff.getLenTranslateCodes(be.down_stats_saco[saco]);
			//huff.getTranslateCodesString(be.down_stats_saco[saco]);
			
			//System.out.println("     lendown:"+lendown);
			total_bits+=lendown;
			}
		}
		
		
		//----------------------------------------------------------
		
		if (DEBUG) System.out.println(" compressed image");
		//System.out.println("correcciones:"+lhe.contafix+ "     bits extra:"+lhe.bits_fix);
		//total_bits+=lhe.bits_fix;
		//lhe.bits_fix=0;
		//lhe.contafix=0;
		float[] result=new float[2];
		
		
		float nonulos=grid.getNumberOfDownsampledPixels();;//img.getNumberOfNonZeroPixelsDown();
		System.out.println("No nulos:"+nonulos+"   segunbinenc:"+be.totalhops);
		System.out.println("image_bits="+total_bits+ "   bpp:"+((float)total_bits/(img.width*img.height))+ "   perhop:"+(float)total_bits/nonulos);
		
		
		
		
		result[0]+=100*(float)nonulos/(img.width*img.height);
		//result[1]+=((float)total_bits/(img.width*img.height));
		
		//be.printStatHops();
		be.DEBUG=DEBUG;
		if (DEBUG) be.statSymbols();
		int bits_grid=be.gridToBits(grid);
		result[1]+=((float)(total_bits+bits_grid)/(img.width*img.height));
		//result[1]+=((float)bits_grid/(img.width*img.height));
		System.out.println("total_bits="+(total_bits+bits_grid)+ "   bpp:"+result[1]);//+ "   perhop:"+(float)total_bits/nonulos);
		
		System.out.println("");
		System.out.println("RLC mejora:");
		System.out.println("===========");
		System.out.println("RLC net savings:"+net_savings+ " ---> final bpp="+((float)(total_bits+bits_grid-net_savings)/(img.width*img.height)));
		System.out.println("");
		
		//be.compressHopsHuffman();
		
		//salvamos los boundaries. la imagen contiene los interpolados y los escalados
		//-------------------------
		if (DEBUG)
		{	
			img.YUVtoBMP("./output_debug/boundaries_downsampled_LHE.bmp",img.boundaries_YUV[0]);
			img.YUVtoBMP("./output_debug/boundaries_interpolated_LHE.bmp",img.boundaries_inter_YUV[0]);
			//image before 2nd LHE
			img.YUVtoBMP("./output_debug/downsampled_YUV.bmp",img.downsampled_YUV[0]);
			//image after 2nd LHE
			img.YUVtoBMP("./output_debug/downsampled_LHE_YUV.bmp",img.downsampled_LHE_YUV[0]);
            //grid information. contains quantized PR values 
		    //---------------------------------------------------------
		     //img.grid.saveGridTXT("./output_debug/grid.txt");
			//grid.saveGridTXT("./output_debug/grid.txt");
			img.saveHopsToTxt("./output_debug/hops_unsigned.txt",false);
			img.saveHopsToTxt("./output_debug/hops_signed.txt",true);
			System.out.println("calculando PSNR del down. solo util si no hay compresion:");
			double psnr2=PSNR.printPSNR(this.path_img, "./output_debug/downsampled_LHE_YUV.bmp");
			System.out.println(" ----> PSNR 2nd LHE (down):"+psnr2);
		}
		//translate hops into bits, using BinaryEncoder	
		//--------------------------------------------------------
			//PENDING TO DO
		
		//translate Grid info into bits, using BinaryEncoder	
		//--------------------------------------------------------
				
			//PENDING TO DO
		return result;	
	}//end funcion	
//**************************************************************************
//public void preCompressFrame( float cf)
public void preCompressFrame( float ql)
{
	
	float p=(float)Block.MAX_PPP;
	float prmin=0.125f;//Perceptual relevance minimum
	if (MODE.equals("HOMO")) prmin=0f;// necesario pues en homo no hay cuantos
	float prmax=1;
	float cfmin=(1f+(p-1f)*prmin)/p;
	float cfmax=1+(p-1)*prmax;//0.75f;
	double r=Math.pow(cfmax/cfmin,1/99f);
	float cf=(float)((1f/p)*Math.pow(r,(99-ql)));
	if (!MODE.equals("HOMO")) System.out.println("  r:"+r+"  and QL:"+ql+"   THEN cf="+cf);
	
	//frame must be loaded and grid created
	
	//-----------------------------------------------
	//First LHE quantization
	//lhe.img=img;
	// IMPROVEMENT PENDING:
	//coding by blocks instead globally, this part is paralellizable  N^2-->2N+1
	lhe.quantizeOneHopPerPixel_R(img.hops[0],img.LHE_YUV[0]);
	// now, hops are stored at img.hops[color component][coordinate]
	// they can be saved
	if (DEBUG) img.saveHopsToTxt("./output_debug/hops1stLHE.txt");
	if (DEBUG) img.YUVtoBMP("./output_debug/1stLHE_YUV.bmp",img.LHE_YUV[0]);
	//-------------------------------------------------
	
	//PR metrics and PPP assignment
	PRblock.img=img;
	if (DEBUG) System.out.println(" computing PR metrics...");
	//img.
	grid.computeMetrics();//compute metrics of all Prblocks, equalize & quantize

	//---------------------------------------------------
	//compute average PR per block and stores it on each block object
	//This value is used for adjust ratio of hop series at 2nd LHE encoding
	grid.computePRavgPerBlock();
	//---------------------------------------------------
	//assign PPP at each block's corners based on PR value at each block's corners 
	if (!MODE.equals("HOMO")) 
		//img.grid.fromPRtoPPP(grid.compression_factor);
		//img.grid.fromPRtoPPP(cf);
		grid.fromPRtoPPP(cf);
	else 
	{
		float ratio=cf;//the meaning of cf in homogeneous mode is the ratio 1:N (ratio is N)
		grid.setPPPHomogeneousDownsamplingRatio(ratio);
	}
	//--------------------------------------------------
}

//**************************************************************************
//public void postCompressFrame( float cf, boolean LHE)
public void postCompressFrame(  boolean LHE)
{
	//downsampling
    if (DEBUG) System.out.println(" downsampling...");
	//this part is paralellizable N^2-->2N+1
	Block.img=img;
	PRblock.img=img;//NEW
	//for ( int y=0 ; y<img.grid.number_of_blocks_V;y++)
	for ( int y=0 ; y<grid.number_of_blocks_V;y++)
	{
		//for ( int x=0 ; x<img.grid.number_of_blocks_H;x++)
		for ( int x=0 ; x<grid.number_of_blocks_H;x++)
		{
			//take the block
			//Block bi=img.grid.bl[y][x];
			Block bi=grid.bl[y][x];
			
			//adapt corner's PPP to rectangle shape
			bi.pppToRectangleShape();

			//downsampling the block
			//bi.computeDownsampledLengths();
			bi.downsampleBlock(true);
			
			//before 2nd LHE, compute boundaries of adyacent blocks.    
			//we asume the boundaries are already interpolated
			bi.downsampleBoundariesH_FIX(img.boundaries_YUV,img.boundaries_inter_YUV);//, img.grid.bl[y+1][x]);
			bi.downsampleBoundariesV_FIX(img.boundaries_YUV,img.boundaries_inter_YUV);//,img.grid.bl[y][x+1]);
			
			
		
			
			//2nd LHE
			//boolean LHE=false;
			if (LHE==true)
		       ///lhe.quantizeDownsampledBlock_R(bi, img.hops[0],img.downsampled_LHE_YUV[0], img.downsampled_YUV[0],img.boundaries_YUV[0] );
			   lhe.quantizeDownsampledBlock_R4(bi, img.hops[0],img.downsampled_LHE_YUV[0], img.downsampled_YUV[0],img.boundaries_YUV[0] );
			else
			 img.downsampled_LHE_YUV[0]=img.downsampled_YUV[0];
		    
		    
		    
		    
			//interpolate boundaries for next blocks located at right and below
			bi.interpolateBoundariesVneighbour(img.downsampled_LHE_YUV,img.boundaries_inter_YUV);
			bi.interpolateBoundariesHneighbour(img.downsampled_LHE_YUV,img.boundaries_inter_YUV);
			
		}//end for x
    if (DEBUG) System.out.print(".");
	}//end for y
	if (DEBUG) System.out.println(" compressed image");
	
	
	//salvamos los boundaries. la imagen contiene los interpolados y los escalados
	//-------------------------
	if (DEBUG)
	{	
		img.YUVtoBMP("./output_debug/boundaries_downsampled_LHE.bmp",img.boundaries_YUV[0]);
		img.YUVtoBMP("./output_debug/boundaries_interpolated_LHE.bmp",img.boundaries_inter_YUV[0]);
		//image before 2nd LHE
		img.YUVtoBMP("./output_debug/downsampled_YUV.bmp",img.downsampled_YUV[0]);
		//image after 2nd LHE
		img.YUVtoBMP("./output_debug/downsampled_LHE_YUV.bmp",img.downsampled_LHE_YUV[0]);
        //grid information. contains quantized PR values 
	    //---------------------------------------------------------
	     //img.grid.saveGridTXT("./output_debug/grid.txt");
		grid.saveGridTXT("./output_debug/grid.txt");
	}
	
	//translate hops into bits, using BinaryEncoder	
	//--------------------------------------------------------
		//PENDING TO DO
	
	//translate Grid info into bits, using BinaryEncoder	
	//--------------------------------------------------------
			
		//PENDING TO DO
		
}
//**************************************************************************
//public void preCompressFrame( float cf, Grid grid_ant)
public void preCompressFrame( float ql, Grid grid_ant)
{
	
	
	float p=(float)Block.MAX_PPP;
	float prmin=0.125f;//Perceptual relevance minimum
	if (MODE.equals("HOMO")) prmin=0f;// necesario pues en homo no hay cuantos
	float prmax=1;
	float cfmin=(1f+(p-1f)*prmin)/p;
	float cfmax=1+(p-1)*prmax;//0.75f;
	double r=Math.pow(cfmax/cfmin,1/99f);
	float cf=(float)((1f/p)*Math.pow(r,(99-ql)));
	if (!MODE.equals("HOMO")) System.out.println("  r:"+r+"  and QL:"+ql+"   THEN cf="+cf);
	
	//frame must be loaded and grid created
	
	//-----------------------------------------------
	//First LHE quantization
	//lhe.img=img;
	// IMPROVEMENT PENDING:
	//coding by blocks instead globally, this part is paralellizable  N^2-->2N+1
	lhe.quantizeOneHopPerPixel_R(img.hops[0],img.LHE_YUV[0]);
	// now, hops are stored at img.hops[color component][coordinate]
	// they can be saved
	if (DEBUG) img.saveHopsToTxt("./output_debug/hops1stLHE.txt");
	if (DEBUG) img.YUVtoBMP("./output_debug/1stLHE_YUV.bmp",img.LHE_YUV[0]);
	//-------------------------------------------------
	
	//PR metrics and PPP assignment
	PRblock.img=img;
	if (DEBUG) System.out.println(" computing PR metrics...");
	//img.
	
	
	//calcula las metricas con la PR actual pero le pasa el objeto para que guarde cosas en prbl
	grid.computeMetrics(grid_ant);//compute metrics of all Prblocks, equalize & quantize
	
	//---------------------------------------------------
	//compute average PR per block and stores it on each block object
	//This value is used for adjust ratio of hop series at 2nd LHE encoding
	grid.computePRavgPerBlock();
	//---------------------------------------------------
	//assign PPP at each block's corners based on PR value at each block's corners 
	if (!MODE.equals("HOMO")) 
		//img.grid.fromPRtoPPP(grid.compression_factor);
		//img.grid.fromPRtoPPP(cf);
		grid.fromPRtoPPP(cf);
	else 
	{
		float ratio=cf;//the meaning of cf in homogeneous mode is the ratio 1:N (ratio is N)
		grid.setPPPHomogeneousDownsamplingRatio(ratio);
	}
	//--------------------------------------------------
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public static String readKeyboard()
	{
		String data=null;
		try{
		BufferedReader keyb=new BufferedReader(new InputStreamReader(System.in))	;
		data = keyb.readLine(); //keyb.next();
		}catch(Exception e){System.out.print(e);}
		return data;
	}
		
//**************************************************************************
}
