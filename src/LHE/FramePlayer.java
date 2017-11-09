package LHE;

import java.util.Calendar;
import java.util.Date;

import kanzi.test.MySSIM;
import Qmetrics.PSNR;

public class FramePlayer {

	public boolean DEBUG=false;
	public String INTERPOL="BICUBIC";//[ "NN"| "BILINEAL"|"BICUBIC"]
	public Grid grid;
	public ImgUtil img;
	public String MODE="ELASTIC";//"ELASTIC"; // [ELASTIC | HOMO] : elastic downsampling or homogeneous downsampling

	public static String output_directory="./output_img";

	public static boolean ssim_active=false;//true;
	//*******************************************************
	/**
	 * this function interprete a lhe file and save a bmp file
	 * in addition returns % and PSNR
	 * @param path_img
	 * @param img
	 * @return % , psnr
	 */
	public float[] playFrame(String path_img)//, boolean lineal)
	{
		float[] result=new float[3]; //bpp , psnr,ssim
		
		//boolean bilineal=lineal;
		//INTERPOL="NN";//"BICUBIC";//"BILINEAL";//"NN";
		//DEBUG=true;
		//bilineal = false;
		//-----------------------------------------------
		//debo leer la imagen

		//ahora llamamos al decoder
		//--------------------------------------------------------

		//ahora interpretamos los hops+ la malla
		//---------------------------------------------------------	
		//cada vez que interpretamos un bloque, calculamos sus boundaries para interpretar los siguientes
		
		//System.out.println(" salvando");
		//img.YUVtoBMP("./output_video"+"/cosa.bmp",img.downsampled_YUV[0]);
		//System.out.println(" hecho");
		
		//ahora ya tenemos a downsampled LHE. procedemos calcular el PSNR de cada bloque escalado
		//---------------------------------------------------------------------------------------
		/*
		Qmetrics.PSNRutil my_psnr=new Qmetrics.PSNRutil() ;
		for ( int y=0 ; y<grid.number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<grid.number_of_blocks_H;x++)

			{
				Block bi=grid.bl[y][x];
				
				double mse=my_psnr.getMSE(img.downsampled_LHE_YUV[0],img.downsampled_YUV[0],bi.xini,bi.downsampled_xfin,bi.yini,bi.downsampled_yfin,img.width);
			
				 
			}
		}
	
				*/

		
		boolean flagAdaptive=false;
		boolean flagAdaptive2=false;
		if (INTERPOL.equals("ADAPTIVE")) flagAdaptive=true;
		if (INTERPOL.equals("ADAPTIVE2")) flagAdaptive2=true;
		
		//ahora ya tenemos a downsampled LHE. procedemos a reescalar
		//-------------------------------------------------------------
		//puedo hacer V+H en cada bloque o bien primero V en todos y luego H en todos. da igual
		//por razones de debug mejor lo hago todo v y luego todo h y asi genero la imagen intermedia
		//y veo como va
		if (DEBUG) System.out.println("re-scaling...");
		//---------------------------------------------------
		Block.img=img;
		
		//interpolamos los boundaries izq/up para despues usarlos al hacer la interpolacion de gaps
		//ojo por que de momento estoy interpolando los gaps en bicubic siempre
		
		if (INTERPOL.equals("BICUBIC"))
		{
		for ( int y=0 ; y<grid.number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<grid.number_of_blocks_H;x++)

			{
				Block bi=grid.bl[y][x];
				
		//bi.interpolateBoundariesIniHneighbour( img.downsampled_LHE_YUV, img.boundaries_ini_interH_YUV);
		bi.interpolateBoundariesIniVneighbour( img.downsampled_LHE_YUV, img.boundaries_ini_interV_YUV);
			}
		}
		}
		
		//ahora downsampling e interpolacion de fronteras en todos los tipos de interpol
		{
			for ( int y=0 ; y<grid.number_of_blocks_V;y++)
			{
				for ( int x=0 ; x<grid.number_of_blocks_H;x++)

				{
					Block bi=grid.bl[y][x];
					Block up=null;
					if (y>0) up=grid.bl[y-1][x];
					
					Block down=null;
					if (y<grid.number_of_blocks_V-1) down=grid.bl[y+1][x];
					
					//bi.interpolateBoundariesVneighbour(img.downsampled_LHE_YUV,img.boundaries_inter_YUV);
					//bi.interpolateBoundariesHneighbour(img.downsampled_LHE_YUV,img.boundaries_inter_YUV);
					
					//esto interpola en H el bloque y lo deja en frontierH
			        bi.interpolateNeighbourH( img.downsampled_LHE_YUV, img.frontierInterH_YUV);
	 		        //ahora hay que hacer un down de sus dos primeras filas pero con los ppp del bloque up
			        if (up!=null) bi.downsampleRow( img.frontierDownH_YUV, img.frontierInterH_YUV,up.ppp[0][2],up.ppp[0][3],bi.yini, up.lx_sc);
			        if (up!=null) bi.downsampleRow( img.frontierDownH_YUV, img.frontierInterH_YUV,up.ppp[0][2],up.ppp[0][3],bi.yini+1,up.lx_sc);
			        //ahora hay que hacer un down de sus dos ultimas filas pero con los ppp del bloque down
			        if (down!=null) bi.downsampleRow( img.frontierDownH_YUV, img.frontierInterH_YUV,down.ppp[0][0],down.ppp[0][1],bi.downsampled_yfin-1,down.lx_sc);
			        if (down!=null) bi.downsampleRow( img.frontierDownH_YUV, img.frontierInterH_YUV,down.ppp[0][0],down.ppp[0][1],bi.downsampled_yfin, down.lx_sc);
			        
			        //ahora copio las dos ultimas lineas al final. tienen la longitud del down, no de este bloque (bi)
			        if (down!=null)
			        for (int xi=down.xini;xi<=down.downsampled_xfin;xi++)
			           {
			        	img.frontierDownH_YUV[0][bi.yfin*img.width+xi]=img.frontierDownH_YUV[0][bi.downsampled_yfin*img.width+xi];
			        	img.frontierDownH_YUV[0][(bi.yfin-1)*img.width+xi]=img.frontierDownH_YUV[0][(bi.downsampled_yfin-1)*img.width+xi];
			           }
			        
			        /*
			        for (int xi=bi.xini;xi<=bi.xfin;xi++)
			           {
			        	img.frontierH_YUV[0][bi.yfin*img.width+xi]=img.frontierH_YUV[0][bi.downsampled_yfin*img.width+xi];
			        	img.frontierH_YUV[0][(bi.yfin-1)*img.width+xi]=img.frontierH_YUV[0][(bi.downsampled_yfin-1)*img.width+xi];
			           }
			           */
			//bi.interpolateBoundariesIniVneighbour( img.downsampled_LHE_YUV, img.boundaries_ini_interV_YUV);
				}
			}
			}
		
		
		
		//img.YUVtoBMP("./output_debug/iniboundH2.bmp",img.boundaries_ini_interH_YUV[0]);
		//ademas de interpolar los boundaries hay que downsamplearlos y luego interpretar los hops
		
		
		
	
		
		//vamos a interpolar V y luego h
		long lDateTime1 = new Date().getTime();
		System.out.println("Date() - Time in milliseconds: " + lDateTime1);
		//for (int cosa=1;cosa<1000;cosa++)
		{
		
		for ( int y=0 ; y<grid.number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<grid.number_of_blocks_H;x++)

			{
				Block bi=grid.bl[y][x];
				
				
				//bi.bilineal=bilineal;//esto no vale para nada
				
				//bi.interpolateIniBoundariesHneighbour( img.downsampled_LHE_YUV, img.boundaries_ini_inter_YUV);
				//bi.interpolateIniBoundariesVneighbour( img.downsampled_LHE_YUV, img.boundaries_ini_inter_YUV);
				
			    //if (bi.bilineal) bi.interpolateBilinealV(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
				
				
				//con esta funcion elegimos el tipo, que es diferente para cada bloque
				if (flagAdaptive)  computeAdaptiveInterpol(y,x);
				
				if (flagAdaptive2)  computeAdaptiveInterpol2(y,x);
				
				
				if (INTERPOL.equals("BICUBIC")) bi.interpolateBicubicV(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
				else if (INTERPOL.equals("BILINEAL"))bi.interpolateBilinealV(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
				else if (INTERPOL.equals("BILINEAL_FALSA"))bi.interpolateBilinealV_false(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
				else if (INTERPOL.equals("NN"))bi.interpolateNeighbourV(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
				else if (INTERPOL.equals("EXPERIMENTAL"))bi.interpolateAdaptV(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
				else if (INTERPOL.equals("NNL"))bi.interpolateNNLV(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
				else if (INTERPOL.equals("NNSR"))bi.interpolateNNSRV_001(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
				
				//else if (INTERPOL.equals("EPX"))bi.interpolateEPXV_001(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
				else if (INTERPOL.equals("EPX"))bi.interpolateNeighbourV(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
				
				
				
				//if (bi.bilineal) bi.interpolateBicubicV(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
			//else	bi.interpolateNeighbourV(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);

			}
		}
		//DEBUG=true;
		//if (DEBUG) System.out.println("Vertical interpolation ok");
		if (DEBUG) img.YUVtoBMP("./output_debug/interV.bmp",img.intermediate_interpolated_YUV[0]);
		if (DEBUG) img.YUVtoBMP("./output_debug/iniboundH.bmp",img.boundaries_ini_interH_YUV[0]);
		if (DEBUG) img.YUVtoBMP("./output_debug/iniboundV.bmp",img.boundaries_ini_interV_YUV[0]);
		
		
		
		
		
			
		for ( int y=0 ; y<grid.number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<grid.number_of_blocks_H;x++)

			{
				Block bi=grid.bl[y][x];
				
				//boolean pr;
				/*if (bi.PRavg<0.3f) {
					//System.out.println("jjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjj");
					bi.bilineal=false;
					
				}
				*/
				//con esta funcion elegimos el tipo, que es diferente para cada bloque
				if (flagAdaptive) 
					{computeAdaptiveInterpol(y,x);
					
					}
				if (flagAdaptive2)  computeAdaptiveInterpol2(y,x);
				
				//if (bi.bilineal)bi.interpolateBilinealH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				//if (bi.PRavg<0.25f)
				/*
				if (bi.bilineal) {
					//if (bi.PRavg<0.125f)bi.interpolateBilinealH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
					//else
					bi.interpolateBicubicH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				}
				else bi.interpolateNeighbourH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				*/
				if (INTERPOL.equals("BICUBIC")) bi.interpolateBicubicH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				else if (INTERPOL.equals("BILINEAL"))bi.interpolateBilinealH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				else if (INTERPOL.equals("BILINEAL_FALSA"))bi.interpolateBilinealH_false(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				//else if (INTERPOL.equals("BILINEAL_FALSA"))bi.interpolateNeighbourH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				else if (INTERPOL.equals("NN"))bi.interpolateNeighbourH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				else if (INTERPOL.equals("EXPERIMENTAL"))bi.interpolateAdaptH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				else if (INTERPOL.equals("NNL"))bi.interpolateNNLH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				else if (INTERPOL.equals("NNSR"))bi.interpolateNNSRH_001(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				
				//else if (INTERPOL.equals("EPX"))bi.interpolateEPXH_001(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				
				else if (INTERPOL.equals("EPX"))bi.interpolateNeighbourH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				
			}
		}
		
		}
		long lDateTime2 = new Date().getTime();
		System.out.println("Date() - Time in milliseconds: " + lDateTime2);
		System.out.println("total: " + (lDateTime2-lDateTime1));
		
		if (DEBUG) System.out.println("Horizontal interpolation ok");
		if (DEBUG) img.YUVtoBMP("./output_debug/interH.bmp",img.interpolated_YUV[0]);
		
		
		
		
		
		//--------------------interpolate gaps (=costuras)-------------------------------------

		//if (DEBUG) System.out.println("bilineal es "+bilineal);
		
		//Block.img=img;// SIN ESTO FALLAAAAAA
		
		//filtro EPX. solo aplica si interpolacion es EPX
		//if (INTERPOL.equals("EPX")) {
		//filterEPX2x();
		
		//}
		
		
		
		//lo dejo en adaptive para interpolar costuras
		//if (flagAdaptive) INTERPOL=new String("ADAPTIVE");
		
		//if (!INTERPOL.equals("NN"))
		
		
		//ponemos bilineal en caso adaptativo para hacer costuras
		if (flagAdaptive) INTERPOL=new String("BILINEAL");
		if (flagAdaptive2) INTERPOL=new String("NN"); //para que no haga costuras
		
		if (!INTERPOL.equals("NN") )	
		//if (bilineal)//bilineal
		{
			for ( int y=0 ; y<grid.number_of_blocks_V;y++)
			{
				for ( int x=0 ; x<grid.number_of_blocks_H;x++)

				{
					Block bi=grid.bl[y][x];
					Block b_left=null;
					if (x>0) b_left=grid.bl[y][x-1];
					//if (DEBUG) System.out.println("interpolando GAPH bloque:"+x+","+y);
					//if (bi.bilineal) 
					//	bi.interpolateGapH(b_left);
					
					if (INTERPOL.equals("BICUBIC")) bi.interpolateGapHBicubic(b_left);
					else if (INTERPOL.equals("BILINEAL"))bi.interpolateGapH(b_left);
					 
					

				}

			}
			//System.out.println(" hecho gaph");
			//img.YUVtoBMP("./img/gaph.bmp",img.interpolated_YUV[0]);
			//----
			if (DEBUG) System.out.println("interpolateGapH ok");
			if (DEBUG) img.YUVtoBMP("./output_debug/gaph.bmp",img.interpolated_YUV[0]);
			
			//img.YUVtoBMP("./output_debug/gaph.bmp",img.interpolated_YUV[0]);
			
			for ( int y=0 ; y<grid.number_of_blocks_V;y++)
			{
				for ( int x=0 ; x<grid.number_of_blocks_H;x++)

				{
					Block bi=grid.bl[y][x];
					Block b_up=null;
					if (y>0) b_up=grid.bl[y-1][x];
					//if (bi.bilineal) 
						//bi.interpolateGapV(b_up);
						
						//bi.interpolateGapVBicubic(b_up);
						if (INTERPOL.equals("BICUBIC")) bi.interpolateGapVBicubic(b_up);
						else if (INTERPOL.equals("BILINEAL"))bi.interpolateGapV(b_up);
					//bi.interpolateBoundaries(img.boundaries_YUV,img.downsampled_YUV);
					//bi.downsampleBoundaries(bilineal,img.boundaries_YUV,img.boundaries_YUV);
				}

			}

			//---
		}
       
		
		//en adaptativo  el epx se aplica solo a algunos bloques
		if (flagAdaptive || flagAdaptive2) 
			{

		for ( int y=0 ; y<grid.number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<grid.number_of_blocks_H;x++)

			{
				Block bi=grid.bl[y][x];
				if (flagAdaptive) computeAdaptiveInterpol(y,x);
				if (flagAdaptive2)  computeAdaptiveInterpol2(y,x);
				if (INTERPOL.equals("EPX"))
					bi.filterEPX2x(11,16);
				
			}
		}
			}
		
		
		//ojo el tipo epx es para toda la imagen
		else if (INTERPOL.equals("EPX")) {
			
			
			//filterEPX4x(16);
			filterEPX2x(11,16);
			
			
			//iterando queda bien para .1bpp
			//for (int it=1;it<5;it++)
			  //filterEPX2x(11,32);
			
			//filterEPX4x();
		}
		
		//---------------------------------------------------
		//saving file
		if (!MODE.equals("HOMO"))
			img.YUVtoBMP(output_directory+"/play.bmp",img.interpolated_YUV[0]);
		else
			img.YUVtoBMP(output_directory+"/play_homo.bmp",img.interpolated_YUV[0]);
		//img.interpolatedYUVtoBMP("./img/interpolated_lena.bmp");

		float percent=grid.getNumberOfDownsampledPixels();
		percent=100f*percent/(img.width*img.height);
		result[0]=percent;

		//---------------------------------------------------
		//calculating psnr
		//para que sea en gris, voy a generar primero una imagen en gris
		//ImgUtil grey=new ImgUtil();
		//grey.BMPtoYUV(path_img);
		//grey.YUVtoBMP("./output_debug/orig_YUV.bmp",grey.YUV[0]);
		
		double psnr=PSNR.printPSNR(path_img, output_directory+"/play.bmp");
		
		
		//double psnr=PSNR.printPSNR("./output_debug/orig_YUV.bmp", output_directory+"/play.bmp");
		System.out.println(" PSNR:"+psnr+"    percent:"+percent+"           file:"+path_img);
		System.out.println(" ---------------------------------------------------------------");
		result[1]=(float)psnr;

		float ssim=0;//
		if (ssim_active) ssim=MySSIM.getSSIM(path_img, output_directory+"/play.bmp");
		result[2]=ssim;
		
		//double psnrab=PSNR.printPSNR("./psnr/a.bmp", "./psnr/b.bmp");
		//System.out.println(" PSNR AB:"+psnrab);
		//System.out.println("hola");
		/*
   	    double mse=my_psnr.getMSE(img.interpolated_YUV[0],img.YUV[0],0,img.width-1,0,img.height-1,img.width);
		double psnr_mse=my_psnr.getPSNR(mse);
   	    System.out.println("total mse:"+mse+"   psnr:"+psnr_mse);
		*/
		
		
		
		return result;

		
		
		
	}
	//*************************************************************************
	//esta funcion elige el tipo de interpolacion para un bloque segun sus metricas de PR
	public void computeAdaptiveInterpol(int y, int x)
	{
		//Block b=grid.bl[y][x];
		/*
		float prx0=grid.prbl[y][x].PRx;
		float prx1=grid.prbl[y][x+1].PRx;
		float prx2=grid.prbl[y+1][x].PRx;
		float prx3=grid.prbl[y+1][x+1].PRx;
		
		float pry0=grid.prbl[y][x].PRy;
		float pry1=grid.prbl[y][x+1].PRy;
		float pry2=grid.prbl[y+1][x].PRy;
		float pry3=grid.prbl[y+1][x+1].PRy;
		*/
		
		// calculo de las PR minimas
		float prminy=1;
		float prminx=1;
		for (int i=y;i<=y+1;i++)
		{
			for (int j=x;j<=x+1;j++)
			{
				if (grid.prbl[i][j].PRx<prminx) prminx=grid.prbl[i][j].PRx;
				if (grid.prbl[i][j].PRy<prminy) prminy=grid.prbl[i][j].PRy;
			}
		}
		//si PR es cero o 0.125 nos vamos a bilineal
		if (prminy<=0.125 && prminx<=0.125) INTERPOL=new String ("BILINEAL");
		else
		{
		//si PPPmin es >2 nos vamos a EPX
			float pppxmin=8;
			float pppymin=8;
			float pppxmax=1;
			float pppymax=1;
			
			for (int i=0;i<=3;i++)
			{
					if (grid.bl[y][x].ppp[0][i]<pppxmin) pppxmin=grid.bl[y][x].ppp[0][i];
					if (grid.bl[y][x].ppp[1][i]<pppymin) pppymin=grid.bl[y][x].ppp[1][i];
					if (grid.bl[y][x].ppp[0][i]>pppxmax) pppxmax=grid.bl[y][x].ppp[0][i];
					if (grid.bl[y][x].ppp[1][i]>pppymax) pppymax=grid.bl[y][x].ppp[1][i];
				
			}
			
			// si el ppp es bajo, una bilineal queda muy bien, pero tambien un epx
			//if (pppxmax<3 && pppymax<3) INTERPOL=new String ("BILINEAL"); 
			//else
				//si el ppp supera 2 se puede aplicar epx
				if (pppxmin>=2 || pppymin>=2) INTERPOL=new String ("EPX");
		//else nos vamos a vecino (PPP<2)
			else INTERPOL=new String ("NN");
				
	
		}
		//finalmente debe escribir en la variable INTERPOL
		//INTERPOL=new String ("NN");
		
	}
	
	
	//*************************************************************************
	public void computeAdaptiveInterpol2(int y, int x)
	{
		Block b=grid.bl[y][x];
		/*
		float prx0=grid.prbl[y][x].PRx;
		float prx1=grid.prbl[y][x+1].PRx;
		float prx2=grid.prbl[y+1][x].PRx;
		float prx3=grid.prbl[y+1][x+1].PRx;
		
		float pry0=grid.prbl[y][x].PRy;
		float pry1=grid.prbl[y][x+1].PRy;
		float pry2=grid.prbl[y+1][x].PRy;
		float pry3=grid.prbl[y+1][x+1].PRy;
		*/
		
		// calculo de las PR minimas
		float prminy=1;
		float prminx=1;
		
		float pppxmin=8;
		float pppymin=8;
		float pppxmax=1;
		float pppymax=1;
		
		for (int i=y;i<=y+1;i++)
		{
			for (int j=x;j<=x+1;j++)
			{
				if (grid.prbl[i][j].PRx<prminx) prminx=grid.prbl[i][j].PRx;
				if (grid.prbl[i][j].PRy<prminy) prminy=grid.prbl[i][j].PRy;
			}
		}
		//ya tenemos PR min
		
			
			for (int i=0;i<=3;i++)
			{
					if (grid.bl[y][x].ppp[0][i]<pppxmin) pppxmin=grid.bl[y][x].ppp[0][i];
					if (grid.bl[y][x].ppp[1][i]<pppymin) pppymin=grid.bl[y][x].ppp[1][i];
					if (grid.bl[y][x].ppp[0][i]>pppxmax) pppxmax=grid.bl[y][x].ppp[0][i];
					if (grid.bl[y][x].ppp[1][i]>pppymax) pppymax=grid.bl[y][x].ppp[1][i];
				
			}
		//ya tenemos pppmin y pppmax
			
			if (pppxmax<1.5 && pppymax<1.5) INTERPOL=new String ("NN");
			else if (prminx<=0.125 && prminy<=0.125 )INTERPOL=new String ("EPX");//("BILINEAL_FALSA");
			else INTERPOL=new String ("EPX");
			
			//INTERPOL=new String ("BILINEAL_FALSA");
	
	
	}
	
	
	//*************************************************************************
	public float[] playFrameDiffNOUSO(String path_img)
	{
		float[] result=new float[2];
		boolean bilineal=true;
		//bilineal = false;
		//-----------------------------------------------
		//debo leer la imagen

		//ahora llamamos al decoder
		//--------------------------------------------------------

		//ahora interpretamos los hops+ la malla
		//---------------------------------------------------------	
		//cada vez que interpretamos un bloque, calculamos sus boundaries para interpretar los siguientes
		
		
		
	//	for (int i=0;i<(img.width*img.height);i++) img.interpolated_YUV[0][i]=0;
		
		
		//ahora ya tenemos a downsampled LHE. procedemos calcular el PSNR de cada bloque escalado
		//---------------------------------------------------------------------------------------
		Qmetrics.PSNRutil my_psnr=new Qmetrics.PSNRutil() ;
		for ( int y=0 ; y<grid.number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<grid.number_of_blocks_H;x++)

			{
				Block bi=grid.bl[y][x];
				
				float mse=my_psnr.getMSE(img.downsampled_LHE_YUV[0],img.downsampled_YUV[0],bi.xini,bi.downsampled_xfin,bi.yini,bi.downsampled_yfin,img.width);
				/*
			//	System.out.println(" block:"+y+","+x+":"+mse);
				float pravg=(grid.prbl[y][x].PRx+grid.prbl[y][x].PRy+
						     grid.prbl[y][x+1].PRx+grid.prbl[y][x+1].PRy+
						     grid.prbl[y+1][x].PRx+grid.prbl[y+1][x].PRy+
						     grid.prbl[y+1][x+1].PRx+grid.prbl[y+1][x+1].PRy)/8f;
				//if (mse>20)
				//System.out.println(" block:"+y+","+x+"     PRavg:"+pravg+"  MSE:"+mse +"  "+img.grid.prbl[y][x].PRx+","+img.grid.prbl[y][x].PRy+","+img.grid.prbl[y][x+1].PRx+","+img.grid.prbl[y][x+1].PRy+","+img.grid.prbl[y+1][x].PRx+","+img.grid.prbl[y+1][x].PRy+","+img.grid.prbl[y+1][x+1].PRx+","+img.grid.prbl[y+1][x+1].PRy);
				//if (pravg<0.5f)	bi.kinint=40;
				 */
				 
			}
		}
	
				

		//ahora ya tenemos a downsampled LHE. procedemos a reescalar
		//-------------------
	 
		//---------------------------------------------------
		for ( int y=0 ; y<grid.number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<grid.number_of_blocks_H;x++)

			{
				Block bi=grid.bl[y][x];
			if (bilineal) bi.interpolateBilinealV(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
				
			else	bi.interpolateNeighbourV(img.downsampled_YUV,img.intermediate_interpolated_YUV);

			}
		}
		if (DEBUG) img.YUVtoBMP("./img/interV.bmp",img.intermediate_interpolated_YUV[0]);
		for ( int y=0 ; y<grid.number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<grid.number_of_blocks_H;x++)

			{
				Block bi=grid.bl[y][x];
				if (bilineal)bi.interpolateBilinealH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				else bi.interpolateNeighbourH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
			}
		}

		if (DEBUG) img.YUVtoBMP("./img/interH.bmp",img.interpolated_YUV[0]);
		//--------------------interpolate gaps-------------------------------------

		
		
		
		if (bilineal)//bilineal
		{
			for ( int y=0 ; y<grid.number_of_blocks_V;y++)
			{
				for ( int x=0 ; x<grid.number_of_blocks_H;x++)

				{
					Block bi=grid.bl[y][x];
					Block b_left=null;
					if (x>0) b_left=grid.bl[y][x-1];
					//bi.interpolateGapHNeighbour(b_left);
					bi.interpolateGapH(b_left);
				}

			}
			//System.out.println(" hecho gaph");
			//img.YUVtoBMP("./img/gaph.bmp",img.interpolated_YUV[0]);
			//----

			for ( int y=0 ; y<grid.number_of_blocks_V;y++)
			{
				for ( int x=0 ; x<grid.number_of_blocks_H;x++)

				{
					Block bi=grid.bl[y][x];
					Block b_up=null;
					if (y>0) b_up=grid.bl[y-1][x];
					bi.interpolateGapV(b_up);
					//bi.interpolateGapVNeighbour(b_up);
					//bi.interpolateBoundaries(img.boundaries_YUV,img.downsampled_YUV);
					//bi.downsampleBoundaries(bilineal,img.boundaries_YUV,img.boundaries_YUV);
				}

			}

			//---
		}
		

		//---------------------------------------------------
		//saving file
		if (!MODE.equals("HOMO"))
			img.YUVtoBMP(output_directory+"/play.bmp",img.interpolated_YUV[0]);
		else
			img.YUVtoBMP(output_directory+"/play_homo.bmp",img.interpolated_YUV[0]);
		//img.interpolatedYUVtoBMP("./img/interpolated_lena.bmp");

		float percent=grid.getNumberOfDownsampledPixels();
		percent=100f*percent/(img.width*img.height);
		result[0]=percent;

		//---------------------------------------------------
		//calculating psnr
		
		double psnr=PSNR.printPSNR(path_img, output_directory+"/play.bmp");
		System.out.println(" PSNR:"+psnr+"    percent:"+percent+"           file:"+path_img);
		System.out.println(" --------sss-------------------------------------------------------");
		result[1]=(float)psnr;

		
   	    float mse=my_psnr.getMSE(img.interpolated_YUV[0],img.YUV[0],0,img.width-1,0,img.height-1,img.width);
		double psnr_mse=my_psnr.getPSNR(mse);
   	    System.out.println("total mse:"+mse+"   psnr:"+psnr_mse);
		
		
		
		
		return result;

		
		
		
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public float[] playFrameFrontierNOUSO(String path_img, boolean lineal)
	{
		float[] result=new float[2];
		boolean bilineal=lineal;
		//INTERPOL="NN";//"BICUBIC";//"BILINEAL";//"NN";
		//DEBUG=true;
		//bilineal = false;
		//-----------------------------------------------
		//debo leer la imagen

		//ahora llamamos al decoder
		//--------------------------------------------------------

		//ahora interpretamos los hops+ la malla
		//---------------------------------------------------------	
		//cada vez que interpretamos un bloque, calculamos sus boundaries para interpretar los siguientes
		
		//System.out.println(" salvando");
		//img.YUVtoBMP("./output_video"+"/cosa.bmp",img.downsampled_YUV[0]);
		//System.out.println(" hecho");
		
		//ahora ya tenemos a downsampled LHE. procedemos calcular el PSNR de cada bloque escalado
		//---------------------------------------------------------------------------------------
		/*
		Qmetrics.PSNRutil my_psnr=new Qmetrics.PSNRutil() ;
		for ( int y=0 ; y<grid.number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<grid.number_of_blocks_H;x++)

			{
				Block bi=grid.bl[y][x];
				
				double mse=my_psnr.getMSE(img.downsampled_LHE_YUV[0],img.downsampled_YUV[0],bi.xini,bi.downsampled_xfin,bi.yini,bi.downsampled_yfin,img.width);
			
				 
			}
		}
	
				*/

		//ahora ya tenemos a downsampled LHE. procedemos a reescalar
		//-------------------------------------------------------------
		//puedo hacer V+H en cada bloque o bien primero V en todos y luego H en todos. da igual
		//por razones de debug mejor lo hago todo v y luego todo h y asi genero la imagen intermedia
		//y veo como va
		if (DEBUG) System.out.println("re-scaling...");
		//---------------------------------------------------
		Block.img=img;
		
		//interpolamos los boundaries izq/up para despues usarlos al hacer la interpolacion de gaps
		//ojo por que de momento estoy interpolando los gaps en bicubic siempre
		//if (INTERPOL.equals("BICUBIC"))
		{
		for ( int y=0 ; y<grid.number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<grid.number_of_blocks_H;x++)

			{
				Block bi=grid.bl[y][x];
				Block up=null;
				if (y>0) up=grid.bl[y-1][x];
				
				Block down=null;
				if (y<grid.number_of_blocks_V-1) down=grid.bl[y+1][x];
				
				//bi.interpolateBoundariesVneighbour(img.downsampled_LHE_YUV,img.boundaries_inter_YUV);
				//bi.interpolateBoundariesHneighbour(img.downsampled_LHE_YUV,img.boundaries_inter_YUV);
				
				//esto interpola en H el bloque y lo deja en frontierH
		        bi.interpolateNeighbourH( img.downsampled_LHE_YUV, img.frontierInterH_YUV);
 		        //ahora hay que hacer un down de sus dos primeras filas pero con los ppp del bloque up
		        if (up!=null) bi.downsampleRow( img.frontierDownH_YUV, img.frontierInterH_YUV,up.ppp[0][2],up.ppp[0][3],bi.yini, up.lx_sc);
		        if (up!=null) bi.downsampleRow( img.frontierDownH_YUV, img.frontierInterH_YUV,up.ppp[0][2],up.ppp[0][3],bi.yini+1,up.lx_sc);
		        //ahora hay que hacer un down de sus dos ultimas filas pero con los ppp del bloque down
		        if (down!=null) bi.downsampleRow( img.frontierDownH_YUV, img.frontierInterH_YUV,down.ppp[0][0],down.ppp[0][1],bi.downsampled_yfin-1,down.lx_sc);
		        if (down!=null) bi.downsampleRow( img.frontierDownH_YUV, img.frontierInterH_YUV,down.ppp[0][0],down.ppp[0][1],bi.downsampled_yfin, down.lx_sc);
		        
		        //ahora copio las dos ultimas lineas al final. tienen la longitud del down, no de este bloque (bi)
		        if (down!=null)
		        for (int xi=down.xini;xi<=down.downsampled_xfin;xi++)
		           {
		        	img.frontierDownH_YUV[0][bi.yfin*img.width+xi]=img.frontierDownH_YUV[0][bi.downsampled_yfin*img.width+xi];
		        	img.frontierDownH_YUV[0][(bi.yfin-1)*img.width+xi]=img.frontierDownH_YUV[0][(bi.downsampled_yfin-1)*img.width+xi];
		           }
		        
		        /*
		        for (int xi=bi.xini;xi<=bi.xfin;xi++)
		           {
		        	img.frontierH_YUV[0][bi.yfin*img.width+xi]=img.frontierH_YUV[0][bi.downsampled_yfin*img.width+xi];
		        	img.frontierH_YUV[0][(bi.yfin-1)*img.width+xi]=img.frontierH_YUV[0][(bi.downsampled_yfin-1)*img.width+xi];
		           }
		           */
		//bi.interpolateBoundariesIniVneighbour( img.downsampled_LHE_YUV, img.boundaries_ini_interV_YUV);
			}
		}
		}
		//img.YUVtoBMP("./output_debug/iniboundH2.bmp",img.boundaries_ini_interH_YUV[0]);
		//ademas de interpolar los boundaries hay que downsamplearlos y luego interpretar los hops
		img.YUVtoBMP("./output_debug/frontierDownH.bmp",img.frontierDownH_YUV[0]);
		System.out.println(" holaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
		//if (1<2) System.exit(0);
		
		//vamos a interpolar V y luego h
		long lDateTime1 = new Date().getTime();
		System.out.println("Date() - Time in milliseconds: " + lDateTime1);
		//for (int cosa=1;cosa<1000;cosa++)
		{
		
		for ( int y=0 ; y<grid.number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<grid.number_of_blocks_H;x++)

			{
				Block bi=grid.bl[y][x];
				
				
				//bi.bilineal=bilineal;//esto no vale para nada
				
				//bi.interpolateIniBoundariesHneighbour( img.downsampled_LHE_YUV, img.boundaries_ini_inter_YUV);
				//bi.interpolateIniBoundariesVneighbour( img.downsampled_LHE_YUV, img.boundaries_ini_inter_YUV);
				
			    //if (bi.bilineal) bi.interpolateBilinealV(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
				
				
				Block b_down=null;
				if (y<grid.number_of_blocks_V-1) b_down=grid.bl[y+1][x];
				System.out.println(y);
				Block b_up=null;
				if (y>0) b_up=grid.bl[y-1][x];
				
				
				if (INTERPOL.equals("BICUBIC")) bi.interpolateBicubicV(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
				//else if (INTERPOL.equals("BILINEAL"))bi.interpolateBilinealVF(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
				else if (INTERPOL.equals("BILINEAL"))bi.interpolateBilinealVF2(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV,b_up,b_down);
				else if (INTERPOL.equals("NN"))bi.interpolateNeighbourV(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
				
				if (!INTERPOL.equals("NN")) bi.fillFrontierV(img.intermediate_interpolated_YUV);
				
				
				//if (bi.bilineal) bi.interpolateBicubicV(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);
			//else	bi.interpolateNeighbourV(img.downsampled_LHE_YUV,img.intermediate_interpolated_YUV);

			}
		}
		//DEBUG=true;
		//if (DEBUG) System.out.println("Vertical interpolation ok");
		//if (DEBUG) 
			img.YUVtoBMP("./output_debug/interV.bmp",img.intermediate_interpolated_YUV[0]);
			img.YUVtoBMP("./output_debug/frontierV.bmp",img.frontierInterV_YUV[0]);
		//if (DEBUG) img.YUVtoBMP("./output_debug/iniboundH.bmp",img.boundaries_ini_interH_YUV[0]);
		//if (DEBUG) img.YUVtoBMP("./output_debug/iniboundV.bmp",img.boundaries_ini_interV_YUV[0]);
		//if (1<2) System.exit(0);
		//img.YUVtoBMP("./output_debug/frontierV.bmp",img.frontierV_YUV[0]);
		
		for ( int y=0 ; y<grid.number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<grid.number_of_blocks_H;x++)

			{
				Block bi=grid.bl[y][x];
				
				//boolean pr;
				/*if (bi.PRavg<0.3f) {
					//System.out.println("jjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjjj");
					bi.bilineal=false;
					
				}
				*/
				
				
				//if (bi.bilineal)bi.interpolateBilinealH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				//if (bi.PRavg<0.25f)
				/*
				if (bi.bilineal) {
					//if (bi.PRavg<0.125f)bi.interpolateBilinealH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
					//else
					bi.interpolateBicubicH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				}
				else bi.interpolateNeighbourH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				*/
				if (INTERPOL.equals("BICUBIC")) bi.interpolateBicubicH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				else if (INTERPOL.equals("BILINEAL"))bi.interpolateBilinealHF(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				else if (INTERPOL.equals("NN"))bi.interpolateNeighbourH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
				
				
				
			}
		}
		
		}
		long lDateTime2 = new Date().getTime();
		System.out.println("Date() - Time in milliseconds: " + lDateTime2);
		System.out.println("total: " + (lDateTime2-lDateTime1));
		
		if (DEBUG) System.out.println("Horizontal interpolation ok");
		//if (DEBUG)
			img.YUVtoBMP("./output_debug/interH.bmp",img.interpolated_YUV[0]);
		//--------------------interpolate gaps-------------------------------------

		//if (DEBUG) System.out.println("bilineal es "+bilineal);
		
		//Block.img=img;// SIN ESTO FALLAAAAAA
		
		if (!INTERPOL.equals("NN"))
		//if (bilineal)//bilineal
		{
			for ( int y=0 ; y<grid.number_of_blocks_V;y++)
			{
				for ( int x=0 ; x<grid.number_of_blocks_H;x++)

				{
					Block bi=grid.bl[y][x];
					Block b_left=null;
					if (x>0) b_left=grid.bl[y][x-1];
					//if (DEBUG) System.out.println("interpolando GAPH bloque:"+x+","+y);
					//if (bi.bilineal) 
					//	bi.interpolateGapH(b_left);
					if (INTERPOL.equals("BICUBIC")) bi.interpolateGapHBicubic(b_left);
					else if (INTERPOL.equals("BILINEAL"))bi.interpolateGapH(b_left);
					 
					

				}

			}
			//System.out.println(" hecho gaph");
			//img.YUVtoBMP("./img/gaph.bmp",img.interpolated_YUV[0]);
			//----
			if (DEBUG) System.out.println("interpolateGapH ok");
			if (DEBUG) img.YUVtoBMP("./output_debug/gaph.bmp",img.interpolated_YUV[0]);
			
			//img.YUVtoBMP("./output_debug/gaph.bmp",img.interpolated_YUV[0]);
			
			for ( int y=0 ; y<grid.number_of_blocks_V;y++)
			{
				for ( int x=0 ; x<grid.number_of_blocks_H;x++)

				{
					Block bi=grid.bl[y][x];
					Block b_up=null;
					if (y>0) b_up=grid.bl[y-1][x];
					//if (bi.bilineal) 
						//bi.interpolateGapV(b_up);
						
						//bi.interpolateGapVBicubic(b_up);
						if (INTERPOL.equals("BICUBIC")) bi.interpolateGapVBicubic(b_up);
						else if (INTERPOL.equals("BILINEAL"))bi.interpolateGapV(b_up);
					//bi.interpolateBoundaries(img.boundaries_YUV,img.downsampled_YUV);
					//bi.downsampleBoundaries(bilineal,img.boundaries_YUV,img.boundaries_YUV);
				}

			}

			//---
		}

		//---------------------------------------------------
		//saving file
		if (!MODE.equals("HOMO"))
			img.YUVtoBMP(output_directory+"/play.bmp",img.interpolated_YUV[0]);
		else
			img.YUVtoBMP(output_directory+"/play_homo.bmp",img.interpolated_YUV[0]);
		//img.interpolatedYUVtoBMP("./img/interpolated_lena.bmp");

		float percent=grid.getNumberOfDownsampledPixels();
		percent=100f*percent/(img.width*img.height);
		result[0]=percent;

		//---------------------------------------------------
		//calculating psnr
		
		double psnr=PSNR.printPSNR(path_img, output_directory+"/play.bmp");
		System.out.println(" PSNR:"+psnr+"    percent:"+percent+"           file:"+path_img);
		System.out.println(" ---------------------------------------------------------------");
		result[1]=(float)psnr;

		//System.out.println("hola");
		/*
   	    double mse=my_psnr.getMSE(img.interpolated_YUV[0],img.YUV[0],0,img.width-1,0,img.height-1,img.width);
		double psnr_mse=my_psnr.getPSNR(mse);
   	    System.out.println("total mse:"+mse+"   psnr:"+psnr_mse);
		*/
		
		
		
		return result;

		
		
		
	}
	//*************************************************************************
	public void playCosturas(String filename)
	{
		//cargo la imagen en img
		img.BMPtoYUV(filename);
		Grid grid=new Grid();
		grid.createGrid(img.width, img.height);
		//voy a rellenar los PPP de la grid
		
		//hacemos como que la hemos interpolado
		img.interpolated_YUV=img.YUV; 
		
		Block.img=img;
		
		//si uso bicubica no funciona porque necesita las muestras extra
		INTERPOL="BILINEAL";
		
		//ya esta cargada. ahora interpolamos las costuras
		for ( int y=0 ; y<grid.number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<grid.number_of_blocks_H;x++)

			{
				Block bi=grid.bl[y][x];
				Block b_left=null;
				if (x>0) b_left=grid.bl[y][x-1];
				
				//voy a rellenar PPP
				for (int i=0;i<2;i++)
				{
					for (int j=0;j<2;j++)
					{
				    bi.ppp[i][j]=Block.MAX_PPP;
				    if (b_left!=null) b_left.ppp[i][j]=Block.MAX_PPP;
					}
				}
				//if (DEBUG) System.out.println("interpolando GAPH bloque:"+x+","+y);
				//if (bi.bilineal) 
				//	bi.interpolateGapH(b_left);
				if (INTERPOL.equals("BICUBIC")) bi.interpolateGapHBicubic(b_left);
				else if (INTERPOL.equals("BILINEAL"))bi.interpolateGapH(b_left);
				 
				

			}

		}
		//System.out.println(" hecho gaph");
		//img.YUVtoBMP("./img/gaph.bmp",img.interpolated_YUV[0]);
		//----
		if (DEBUG) System.out.println("interpolateGapH ok");
		if (DEBUG) img.YUVtoBMP("./output_debug/gaph_costuras.bmp",img.interpolated_YUV[0]);
		
		//img.YUVtoBMP("./output_debug/gaph.bmp",img.interpolated_YUV[0]);
		
		for ( int y=0 ; y<grid.number_of_blocks_V;y++)
		{
			for ( int x=0 ; x<grid.number_of_blocks_H;x++)

			{
				Block bi=grid.bl[y][x];
				Block b_up=null;
				if (y>0) b_up=grid.bl[y-1][x];
				//if (bi.bilineal) 
					//bi.interpolateGapV(b_up);
					
					//bi.interpolateGapVBicubic(b_up);
				//voy a rellenar PPP
				for (int i=0;i<2;i++)
				{
					for (int j=0;j<2;j++)
					{
				    bi.ppp[i][j]=Block.MAX_PPP;
				    if (b_up!=null) b_up.ppp[i][j]=Block.MAX_PPP;
					}
				}
				
				
				if (INTERPOL.equals("BICUBIC")) bi.interpolateGapVBicubic(b_up);
					else if (INTERPOL.equals("BILINEAL"))bi.interpolateGapV(b_up);
				//bi.interpolateBoundaries(img.boundaries_YUV,img.downsampled_YUV);
				//bi.downsampleBoundaries(bilineal,img.boundaries_YUV,img.boundaries_YUV);
			}

		}
		
		img.YUVtoBMP(output_directory+"/play.bmp",img.interpolated_YUV[0]);
		
		
	}
//*************************************************************************************
public void filterEPX2x(int u1,int u2)
{
//esta funcion filtra por EPX la imagen 
	int[] im=img.interpolated_YUV[0];
	System.out.println("filtering EPX...");
	for (int y=1; y<img.height-1;y++)
	{
		for (int x=1; x<img.width-1;x++)
		{
			//filter1pixEPX2x(im,y,x,u); //filtra 1pixel
			//filter1pixEPX2x_002(im,y,x,u); //filtra 1pixel
			filter1pixEPX2x_003(im,y,x,u1,u2); //filtra 1pixel
		}	
	}
	System.out.println("filtered !");
}
public void filterEPX4x(int u)
{
//esta funcion filtra por EPX la imagen 
	int[] im=img.interpolated_YUV[0];
	System.out.println("filtering EPX...");
	for (int y=1; y<img.height-2;y+=1)
	{
		for (int x=1; x<img.width-2;x+=1)
		{
			boolean modif=filter1pixEPX4x(im,y,x,u); //filtra 1pixel
			//if (modif) x=x+1;
		}	
	}
	System.out.println("filtered !");
}
//*************************************************************************************
public void filter1pixEPX2x(int[] im,int y,int x, int um)
{
	//012
	//345
	//678
	
	
	int[] matriz=new int[9];
	
	int umbral=11;
	umbral =um;
	int i=y*img.width+x;		
	matriz[0]=im[i-1-img.width];
	matriz[1]=im[i-img.width];
	matriz[2]=im[i+1-img.width];
	matriz[3]=im[i-1];
	matriz[4]=im[i];
	matriz[5]=im[i+1];
	matriz[6]=im[i-1+img.width];
	matriz[7]=im[i+img.width];
	matriz[8]=im[i+1+img.width];
	
	//marco arriba izquierdo 
	if ((Math.abs(matriz[1]-matriz[2])<umbral) &&
	    (Math.abs(matriz[3]-matriz[6])<umbral) &&
	    (Math.abs(matriz[1]-matriz[3])<umbral) 
	   // && (Math.abs(matriz[4]-matriz[1])>umbral) //nuevo
	    )
	{
		int mezcla=(matriz[1]+matriz[3])/2;
		//mezcla=(matriz[1]+matriz[2]+matriz[3]+matriz[6])/4;
		
		im[i]=mezcla;
	}
	//marco arriba derecho
	  if ((Math.abs(matriz[0]-matriz[1])<umbral) &&
		    (Math.abs(matriz[5]-matriz[8])<umbral) &&
		    (Math.abs(matriz[5]-matriz[1])<umbral) 
		//    && (Math.abs(matriz[4]-matriz[1])>umbral) //nuevo
		    )
	 {
		int mezcla=(matriz[1]+matriz[5])/2;
		 //mezcla=(matriz[0]+matriz[1]+matriz[5]+matriz[8])/4;
		 im[i]=mezcla;
	 }
	//marco abajo izq
	if ((Math.abs(matriz[7]-matriz[8])<umbral) &&
			    (Math.abs(matriz[0]-matriz[3])<umbral) &&
			    (Math.abs(matriz[3]-matriz[7])<umbral) 
		//	    && (Math.abs(matriz[4]-matriz[7])>umbral) //nuevo
			    )
		{
			int mezcla=(matriz[3]+matriz[7])/2;
			//mezcla=(matriz[0]+matriz[3]+matriz[7]+matriz[8])/4;
			im[i]=mezcla;
		}
		//marco abajo dere
		if ((Math.abs(matriz[6]-matriz[7])<umbral) &&
		    (Math.abs(matriz[2]-matriz[5])<umbral) &&
			(Math.abs(matriz[7]-matriz[5])<umbral) 
		//	&& (Math.abs(matriz[4]-matriz[7])>umbral) //nuevo
					    )
		{
		int mezcla=(matriz[7]+matriz[5])/2;
			//mezcla=(matriz[6]+matriz[7]+matriz[5]+matriz[2])/4;
			im[i]=mezcla;
		}
}

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//*************************************************************************************
public boolean filter1pixEPX4x(int[] im,int y,int x, int um)
{
	//0  1  2  3
	//4  5  6  7
	//8  9  10 11
	//12 13 14 15
	
	
	int[] matriz=new int[16];
	
	int umbral=10;
	umbral =um;
	int i=y*img.width+x;		
	matriz[0]=im[i-1-img.width];
	matriz[1]=im[i-img.width];
	matriz[2]=im[i+1-img.width];
	matriz[3]=im[i+2-img.width];
	matriz[4]=im[i-1];
	matriz[5]=im[i];
	matriz[6]=im[i+1];
	matriz[7]=im[i+2];
	matriz[8]=im[i-1+img.width];
	matriz[9]=im[i+img.width];
	matriz[10]=im[i+1+img.width];
	matriz[11]=im[i+2+img.width];
	matriz[12]=im[i-1+2*img.width];
	matriz[13]=im[i+2*img.width];
	matriz[14]=im[i+1+2*img.width];
	matriz[15]=im[i+2+2*img.width];
	
	boolean modif=false;
	//marco arriba izquierdo 
	if ((Math.abs(matriz[1]-matriz[2])<umbral) &&
	    (Math.abs(matriz[2]-matriz[3])<umbral) &&
	    (Math.abs(matriz[1]-matriz[4])<umbral) &&
	    (Math.abs(matriz[4]-matriz[8])<umbral) &&
	    (Math.abs(matriz[8]-matriz[12])<umbral) 
	    )
		{
		int mezcla=(matriz[1]+matriz[2]+matriz[3]+
				matriz[4]+matriz[8]+matriz[12])/6;
		mezcla=(matriz[1]+matriz[2]+
				matriz[4]+matriz[8])/4;
		
		im[i]=mezcla; 
		im[i+1]=mezcla;
		im[i+img.width]=mezcla;
		modif=true;
		}
	//marco arriba dere 
	 if ((Math.abs(matriz[0]-matriz[1])<umbral) &&
		    (Math.abs(matriz[1]-matriz[2])<umbral) &&
		    (Math.abs(matriz[2]-matriz[7])<umbral) &&
		    (Math.abs(matriz[7]-matriz[11])<umbral) &&
		    (Math.abs(matriz[11]-matriz[15])<umbral) 
		    )
			{
			int mezcla=(matriz[0]+matriz[1]+matriz[2]+
					matriz[7]+matriz[11]+matriz[15])/6;
			 mezcla=(matriz[1]+matriz[2]+
						matriz[7]+matriz[11])/4;
				
			im[i]=mezcla; 
			im[i+1]=mezcla;
			im[i+1+img.width]=mezcla;
			modif=true;
			}
	 
	//marco abajo derecho 
	 if ((Math.abs(matriz[12]-matriz[13])<umbral) &&
		    (Math.abs(matriz[13]-matriz[14])<umbral) &&
		    (Math.abs(matriz[14]-matriz[11])<umbral) &&
		    (Math.abs(matriz[11]-matriz[7])<umbral) &&
		    (Math.abs(matriz[7]-matriz[3])<umbral) 
		    )
			{
			int mezcla=(matriz[12]+matriz[13]+matriz[14]+
					matriz[11]+matriz[7]+matriz[3])/6;
			mezcla=(matriz[13]+matriz[14]+
					matriz[11]+matriz[7])/4;
			
			
			im[i+1]=mezcla; 
			im[i+1+img.width]=mezcla;
			im[i+img.width]=mezcla;
			modif=true;
			}
	//marco abajo izq 
		 if ((Math.abs(matriz[13]-matriz[14])<umbral) &&
			    (Math.abs(matriz[14]-matriz[15])<umbral) &&
			    (Math.abs(matriz[8]-matriz[13])<umbral) &&
			    (Math.abs(matriz[4]-matriz[8])<umbral) &&
			    (Math.abs(matriz[4]-matriz[0])<umbral) 
			    )
				{
				int mezcla=(matriz[13]+matriz[14]+matriz[15]+
						matriz[8]+matriz[4]+matriz[0])/6;
				mezcla=(matriz[13]+matriz[14]+
						matriz[8]+matriz[4])/4;
				im[i]=mezcla; 
				im[i+1+img.width]=mezcla;
				im[i+img.width]=mezcla;
				modif=true;
				}
	 	
		 return modif;
}
//*************************************************************************************
public void filter1pixEPX2x_002(int[] im,int y,int x, int um)
{
	//012
	//345
	//678
	//System.out.println("estoy en 002");
	
	int[] matriz=new int[9];
	boolean modif=false;
	
	int umbral=11;
	umbral =um;
	int u1=16;
	int u2=32;
	
	int i=y*img.width+x;		
	matriz[0]=im[i-1-img.width];
	matriz[1]=im[i-img.width];
	matriz[2]=im[i+1-img.width];
	matriz[3]=im[i-1];
	matriz[4]=im[i];
	matriz[5]=im[i+1];
	matriz[6]=im[i-1+img.width];
	matriz[7]=im[i+img.width];
	matriz[8]=im[i+1+img.width];
	
	//marco arriba izquierdo 
	if ((Math.abs(matriz[1]-matriz[2])<u1) &&
	    (Math.abs(matriz[3]-matriz[6])<u1) &&
	    (Math.abs(matriz[1]-matriz[3])<u2) 
	   // && (Math.abs(matriz[4]-matriz[1])>umbral) //nuevo
	    )
	{
		int mezcla=(matriz[1]+matriz[3])/2;
		//mezcla=(matriz[1]+matriz[2]+matriz[3]+matriz[6])/4;
		
		im[i]=mezcla;
		modif=true;
	}
	//marco arriba derecho
	  if ((Math.abs(matriz[0]-matriz[1])<u1) &&
		    (Math.abs(matriz[5]-matriz[8])<u1) &&
		    (Math.abs(matriz[5]-matriz[1])<u2) 
		//    && (Math.abs(matriz[4]-matriz[1])>umbral) //nuevo
		    )
	 {
		int mezcla=(matriz[1]+matriz[5])/2;
		 //mezcla=(matriz[0]+matriz[1]+matriz[5]+matriz[8])/4;
		 im[i]=mezcla;
		 modif=true;
	 }
	//marco abajo izq
	if ((Math.abs(matriz[7]-matriz[8])<u1) &&
			    (Math.abs(matriz[0]-matriz[3])<u1) &&
			    (Math.abs(matriz[3]-matriz[7])<u2) 
		//	    && (Math.abs(matriz[4]-matriz[7])>umbral) //nuevo
			    )
		{
			int mezcla=(matriz[3]+matriz[7])/2;
			//mezcla=(matriz[0]+matriz[3]+matriz[7]+matriz[8])/4;
			im[i]=mezcla;
			modif=true;
		}
		//marco abajo dere
		if ((Math.abs(matriz[6]-matriz[7])<u1) &&
		    (Math.abs(matriz[2]-matriz[5])<u1) &&
			(Math.abs(matriz[7]-matriz[5])<u2) 
		//	&& (Math.abs(matriz[4]-matriz[7])>umbral) //nuevo
					    )
		{
		int mezcla=(matriz[7]+matriz[5])/2;
			//mezcla=(matriz[6]+matriz[7]+matriz[5]+matriz[2])/4;
			im[i]=mezcla;
			modif=true;
		}
		
		//System.out.print("caca");
		//modif=true;
		if (!modif)
		{
			//marco arriba izquierdo 
			if (((Math.abs(matriz[1]-matriz[2])<u1) &&
			    //(Math.abs(matriz[3]-matriz[6])<umbral) &&
			    (Math.abs(matriz[1]-matriz[3])<u2) 
			   // && (Math.abs(matriz[4]-matriz[1])>umbral) //nuevo
			    ) || 
			    ((Math.abs(matriz[3]-matriz[6])<u1) &&
			     (Math.abs(matriz[1]-matriz[3])<u2)		
			    		))
			{
				int mezcla=(matriz[1]+matriz[3]+matriz[4])/3;
				mezcla=(matriz[1]+matriz[3])/2;
				im[i]=mezcla;
				modif=true;
			}
			//marco arriba derecho
			  if (((Math.abs(matriz[0]-matriz[1])<u1) &&
				   // (Math.abs(matriz[5]-matriz[8])<umbral) &&
				    (Math.abs(matriz[5]-matriz[1])<u2) 
				
				    )
				  ||
				  ((Math.abs(matriz[5]-matriz[8])<u1) &&
						  (Math.abs(matriz[5]-matriz[1])<u2) 
						  ))
			 {
				int mezcla=(matriz[1]+matriz[5]+matriz[4])/3;
				mezcla=(matriz[1]+matriz[5])/2;
				 im[i]=mezcla;
				 modif=true;
			 }
			//marco abajo izq
			if (((Math.abs(matriz[7]-matriz[8])<u1) &&
					    //(Math.abs(matriz[0]-matriz[3])<umbral) &&
					    (Math.abs(matriz[3]-matriz[7])<umbral) 
				//	    && (Math.abs(matriz[4]-matriz[7])>umbral) //nuevo
					    )
				||((Math.abs(matriz[0]-matriz[3])<u2) &&
						(Math.abs(matriz[3]-matriz[7])<umbral)
						
						))
				
				
				{
					int mezcla=(matriz[3]+matriz[7]+matriz[4])/3;
					mezcla=(matriz[3]+matriz[7])/2;
					im[i]=mezcla;
					modif=true;
				}
				//marco abajo dere
				if (((Math.abs(matriz[6]-matriz[7])<u1) &&
				    //(Math.abs(matriz[2]-matriz[5])<umbral) &&
					(Math.abs(matriz[7]-matriz[5])<u2) 
				//	&& (Math.abs(matriz[4]-matriz[7])>umbral) //nuevo
							    )
					||(
							(Math.abs(matriz[2]-matriz[5])<u1) &&
							(Math.abs(matriz[7]-matriz[5])<u2)
							))
				{
				int mezcla=(matriz[7]+matriz[5]+matriz[4])/3;
				mezcla=(matriz[7]+matriz[5])/2;
					im[i]=mezcla;
					modif=true;
				}
			
			
		}
		
}

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//*************************************************************************************
//*************************************************************************************
public void filter1pixEPX2x_003(int[] im,int y,int x, int um1, int um2)
{
	//012
	//345
	//678
	//System.out.println("estoy en 002");
	
	int[] matriz=new int[9];
	boolean modif=false;
	
	int umbral=11;
	//umbral =um;
	int u1=um1;//11;
	int u2=um2;//16;
	
	int i=y*img.width+x;		
	matriz[0]=im[i-1-img.width];
	matriz[1]=im[i-img.width];
	matriz[2]=im[i+1-img.width];
	matriz[3]=im[i-1];
	matriz[4]=im[i];
	matriz[5]=im[i+1];
	matriz[6]=im[i-1+img.width];
	matriz[7]=im[i+img.width];
	matriz[8]=im[i+1+img.width];
	
	//marco arriba izquierdo 
	if ((Math.abs(matriz[1]-matriz[2])<u1) &&
	    (Math.abs(matriz[3]-matriz[6])<u1) &&
	    (Math.abs(matriz[1]-matriz[3])<u2) 
	   // && (Math.abs(matriz[4]-matriz[1])>umbral) //nuevo
	    )
	{
		int mezcla=(matriz[1]+matriz[3])/2;
		//mezcla=(matriz[1]+matriz[2]+matriz[3]+matriz[6])/4;
		
		im[i]=mezcla;
		modif=true;
	}
	//marco arriba derecho
	  if ((Math.abs(matriz[0]-matriz[1])<u1) &&
		    (Math.abs(matriz[5]-matriz[8])<u1) &&
		    (Math.abs(matriz[5]-matriz[1])<u2) 
		//    && (Math.abs(matriz[4]-matriz[1])>umbral) //nuevo
		    )
	 {
		int mezcla=(matriz[1]+matriz[5])/2;
		 //mezcla=(matriz[0]+matriz[1]+matriz[5]+matriz[8])/4;
		//mezcla=matriz[2];
		 im[i]=mezcla;
		 modif=true;
	 }
	//marco abajo izq
	if ((Math.abs(matriz[7]-matriz[8])<u1) &&
			    (Math.abs(matriz[0]-matriz[3])<u1) &&
			    (Math.abs(matriz[3]-matriz[7])<u2) 
		//	    && (Math.abs(matriz[4]-matriz[7])>umbral) //nuevo
			    )
		{
			int mezcla=(matriz[3]+matriz[7])/2;
			//mezcla=(matriz[0]+matriz[3]+matriz[7]+matriz[8])/4;
		//	mezcla=matriz[6];
			im[i]=mezcla;
			modif=true;
		}
		//marco abajo dere
		if ((Math.abs(matriz[6]-matriz[7])<u1) &&
		    (Math.abs(matriz[2]-matriz[5])<u1) &&
			(Math.abs(matriz[7]-matriz[5])<u2) 
		//	&& (Math.abs(matriz[4]-matriz[7])>umbral) //nuevo
					    )
		{
		int mezcla=(matriz[7]+matriz[5])/2;
			//mezcla=(matriz[6]+matriz[7]+matriz[5]+matriz[2])/4;
		//mezcla=matriz[8];
			im[i]=mezcla;
			modif=true;
		}
		
		//System.out.print("caca");
		//modif=true;
		if (!modif)
		{
			//marco arriba izquierdo 
			if (((Math.abs(matriz[1]-matriz[2])<u1) &&
			    //(Math.abs(matriz[3]-matriz[6])<umbral) &&
			    (Math.abs(matriz[1]-matriz[3])<u2) 
			   // && (Math.abs(matriz[4]-matriz[1])>umbral) //nuevo
			    )) 
			    {
				int mezcla=(matriz[1]+matriz[3]+matriz[4])/3;
				//mezcla=(matriz[1]);//+matriz[3])/2;
				mezcla=(matriz[4]+matriz[1])/2;
				im[i]=mezcla;
				modif=true;
			    }
			  if  
			    ((Math.abs(matriz[3]-matriz[6])<u1) &&
			     (Math.abs(matriz[1]-matriz[3])<u2)		
			    		)
			{
				int mezcla=(matriz[1]+matriz[3]+matriz[4])/3;
				mezcla=(matriz[3]+matriz[4])/2;
				im[i]=mezcla;
				modif=true;
			}
			//marco arriba derecho
			  if ((Math.abs(matriz[0]-matriz[1])<u1) &&
				   // (Math.abs(matriz[5]-matriz[8])<umbral) &&
				    (Math.abs(matriz[5]-matriz[1])<u2) 
				
				    )
				    {
				  int mezcla=(matriz[1]+matriz[5]+matriz[4])/3;
					mezcla=(matriz[1]+matriz[4])/2;
					 im[i]=mezcla;
					 modif=true;
				    }
				    
				  if
				  ((Math.abs(matriz[5]-matriz[8])<u1) &&
						  (Math.abs(matriz[5]-matriz[1])<u2) 
						  )
			 {
				int mezcla=(matriz[1]+matriz[5]+matriz[4])/3;
				mezcla=(matriz[5]+matriz[4])/2;
				 im[i]=mezcla;
				 modif=true;
			 }
			//marco abajo izq
			if ((Math.abs(matriz[7]-matriz[8])<u1) &&
					    //(Math.abs(matriz[0]-matriz[3])<umbral) &&
					    (Math.abs(matriz[3]-matriz[7])<umbral) 
				//	    && (Math.abs(matriz[4]-matriz[7])>umbral) //nuevo
					    )
					    {
				int mezcla=(matriz[3]+matriz[7]+matriz[4])/3;
				mezcla=(matriz[7]+matriz[4])/2;
				im[i]=mezcla;
				modif=true;
				
					    }
					    
				if	    
					    
				((Math.abs(matriz[0]-matriz[3])<u2) &&
						(Math.abs(matriz[3]-matriz[7])<umbral)
						
						)
				
				
				{
					int mezcla=(matriz[3]+matriz[7]+matriz[4])/3;
					mezcla=(matriz[3]+matriz[4])/2;//+matriz[7])/2;
					im[i]=mezcla;
					modif=true;
				}
				//marco abajo dere
				if ((Math.abs(matriz[6]-matriz[7])<u1) &&
				    //(Math.abs(matriz[2]-matriz[5])<umbral) &&
					(Math.abs(matriz[7]-matriz[5])<u2) 
				//	&& (Math.abs(matriz[4]-matriz[7])>umbral) //nuevo
							    )
					  {
					int mezcla=(matriz[7]+matriz[5]+matriz[4])/3;
					mezcla=(matriz[7]+matriz[4])/2;//+matriz[5])/2;
						im[i]=mezcla;
						modif=true;
					   }
							    
					if (		    
					
							(Math.abs(matriz[2]-matriz[5])<u1) &&
							(Math.abs(matriz[7]-matriz[5])<u2)
							)
				{
				int mezcla=(matriz[7]+matriz[5]+matriz[4])/3;
				mezcla=(matriz[5]+matriz[4])/2;//+matriz[5])/2;
					im[i]=mezcla;
					modif=true;
				}
			
			
		}
		
}

}

