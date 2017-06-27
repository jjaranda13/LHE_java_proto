import java.io.*;
import java.util.Date;
import java.util.Scanner;








import kanzi.test.MySSIM;
//classes from package LHE
import LHE.Block;
import LHE.Grid;
import LHE.ImgUtil;
import LHE.LHEquantizer;
import LHE.PRblock;
import LHE.FrameCompressor;
import LHE.FramePlayer;
import LHE.VideoCompressor;

//classes from package Qmetrics
import Qmetrics.PSNR;

public class MainTest {
	//static Scanner keyb = new Scanner (System.in);		
	
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public static void main(String [ ] args)
	{
		
	//int kk=128;	
	//int[][][][][] cosa=new int [kk][kk][kk][kk][kk];	
		
	//cosa[0][0][0][0]=1;
	
			
	System.out.println("Menu:");
	System.out.println ("0) compress an image using Basic-LHE (bit-rate non elegible)");
	System.out.println ("1) compress an image [default if you press ENTER]");
	System.out.println ("2) compress a directory from 5% to 95% step 5%");
	System.out.println ("3) compress a directory at all bitrates from 0.05bpp, to 1.2bpp ");
	System.out.println ("4) compute SSIM for a given DIR origin and DIR destination");
	
	//sin implementar
	//System.out.println ("5) test performance");
	System.out.println ("5) compress a video ");
	//System.out.println ("6) decompress an .lhe file ");
	//System.out.println ("7) decompress a directory of .lhe files ");
	System.out.println ("6) compute PSNR for a given image origin and degraded image");
	System.out.println ("7) interpolate seams");
	System.out.println ("8) compress LHE2");
	System.out.println ("9) compress SIMPLE LHE ( = static huffman). fixed bit rate ");
	System.out.println ("10) compress image SIMPLE LHE sampled (preLHE2)");
	System.out.println ("11) compress video SIMPLE LHE sampled (preLHE2)");
	
	String option =  readKeyboard();
	System.out.println ("your option is : "+option);
	
	MainTest m=new MainTest();
	if (option.equals("1") || option.equals("")) m.compressImage();
	else if (option.equals("2")) m.compressDirFullPercent();
	else if (option.equals("3")) m.compressDirFullBpp();
	else if (option.equals("4")) m.SSIMDirFullBpp();
	
	else if (option.equals("5")) m.compressVideo();
	else if (option.equals("0")) m.compressImageBasicLHE();
	else if (option.equals("6")) m.computePSNR();
	//else if (option.equals("3")) m.performance();
	else if (option.equals("7")) m.interpolateSeams();
	else if (option.equals("8")) m.compressImageLHE2();
	else if (option.equals("9")) m.compressImageSIMPLELHE();
	else if (option.equals("10")) m.compressImageSIMPLELHE_sampled();
	else if (option.equals("11")) m.compressVideoSIMPLELHE_sampled();
	}
	
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public void compressImageSIMPLELHE()
			{
				System.out.println("you have enter into LHE3");
				System.out.println ("Enter filename [default=./img/lena.bmp]:");
				
				String filename =  readKeyboard();
				if (filename.equals("")) filename=new String("./img/lena.bmp");
				
				
				System.out.println ("your filename is : "+filename);
				
				FrameCompressor fc=new LHE.FrameCompressor(1);
				fc.DEBUG=true;
				fc.loadFrame(filename);
				
				
				System.out.println(" width:"+fc.img.width);
				System.out.println(" height:"+fc.img.height);
				fc.compressSIMPLELHE();//option,filename);
				
				
				//float ssim=MySSIM.getSSIM("./output_debug/orig_YUV_BN.bmp", "./output_img/BasicLHE_YUV.bmp");
				//System.out.println ("SSIM:"+ssim);
				
				
				
			}
			
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public void compressImageSIMPLELHE_sampled()
				{
					System.out.println("you have enter into LHE sampled");
					System.out.println ("Enter filename [default=./img/lena.bmp]:");
					String filename =  readKeyboard();
					if (filename.equals("")) filename=new String("./img/lena.bmp");
					System.out.println ("your filename is : "+filename);
					
					System.out.println ("select downsampling X ratio defaul[2]:");
					String ratioxs = readKeyboard();
					if (ratioxs.equals("")) ratioxs=new String("2");
					float ratiox=Float.parseFloat(ratioxs);
					System.out.println ("select downsampling X type 0)SPS 1)AVG,  defaul[1]:");
					String typexs = readKeyboard();
					if (typexs.equals("")) typexs=new String("1");
					int typex=Integer.parseInt(typexs);
					System.out.println ("select downsampling Y ratio defaul[2]:");
					String ratioys = readKeyboard();
					if (ratioys.equals("")) ratioys=new String("2");
					float ratioy=Float.parseFloat(ratioys);
					System.out.println ("select downsampling Y type 0)SPS 1)AVG,  defaul[1]:");
					String typeys = readKeyboard();
					if (typeys.equals("")) typeys=new String("1");
					int typey=Integer.parseInt(typeys);
					
					
					FrameCompressor fc=new LHE.FrameCompressor(1);
					fc.DEBUG=true;
					fc.loadFrame(filename);
					
					
					System.out.println(" width:"+fc.img.width);
					System.out.println(" height:"+fc.img.height);
					fc.compressSIMPLELHESAMPLED(ratiox,typex,ratioy,typey,true);//option,filename);
					
					
					//float ssim=MySSIM.getSSIM("./output_debug/orig_YUV_BN.bmp", "./output_img/BasicLHE_YUV.bmp");
					//System.out.println ("SSIM:"+ssim);
					
					
					
				}
				
			//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		public void compressImageLHE2()
		{
			System.out.println("you have enter into LHE2");
			System.out.println ("Enter filename [default=./img/lena.bmp]:");
			//System.out.println ("Enter filename [default=./img/LHE2_test002.bmp]:");
			String filename =  readKeyboard();
			if (filename.equals("")) filename=new String("./img/lena.bmp");
			//if (filename.equals("")) filename=new String("./img/LHE2_test002.bmp");
			
			System.out.println ("your filename is : "+filename);
			
			FrameCompressor fc=new LHE.FrameCompressor(1);
			fc.DEBUG=true;
			fc.loadFrame(filename);
			//solo para pruebas
			//------------------
			//fc.img.getQuarterImgYUV();
			//fc.img.YUVtoBMP("./img/quarter.bmp", fc.img.YUV[0]);
			//if (1==1)System.exit(0);
			//------------------
			
			//fc.img.YUVtoBMP("./output_debug/orig_YUV_BN.bmp",fc.img.YUV[0]);
			System.out.println(" width:"+fc.img.width);
			System.out.println(" height:"+fc.img.height);
			//fc.loadFrame("./output_debug/orig_YUV_BN.bmp");//load B/W version of the image
			//System.exit(0);
			
			//System.out.println ("choose log ratio");
			//System.out.println ("1)log ratio=2.5[default]   2) log ratio=2   ");
			
			//String option =  readKeyboard();
			//if (option.equals("")) option="1";
			//fc.compressBasicFrame(option,filename);
			fc.compressLHE2();//option,filename);
			
			
			float ssim=MySSIM.getSSIM("./output_debug/orig_YUV_BN.bmp", "./output_img/BasicLHE_YUV.bmp");
			System.out.println ("SSIM:"+ssim);
			
			//System.out.println ("OJO QUE LOS BPP NO ESTAN BIEN CALCULADOS EN LHE2. LO QUE ESTA BIEN SON LOS SAMPLES TOTALES");
			
		}
		
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public void compressImageBasicLHE()
	{
		System.out.println("you have enter into Basic-LHE");
		System.out.println ("Enter filename [default=./img/lena.bmp]:");
		String filename =  readKeyboard();
		if (filename.equals("")) filename=new String("./img/lena.bmp");
		System.out.println ("your filename is : "+filename);
		
		FrameCompressor fc=new LHE.FrameCompressor(1);
		fc.DEBUG=true; //en modo BASIC el debug se pone a true siempre, asi vemos los hops en un fichero
		fc.loadFrame(filename);
		//solo para pruebas
		//------------------
		//fc.img.getQuarterImgYUV();
		//fc.img.YUVtoBMP("./img/quarter.bmp", fc.img.YUV[0]);
		//if (1==1)System.exit(0);
		//------------------
		
		//fc.img.YUVtoBMP("./output_debug/orig_YUV_BN.bmp",fc.img.YUV[0]);
		System.out.println(" width:"+fc.img.width);
		System.out.println(" height:"+fc.img.height);
		//fc.loadFrame("./output_debug/orig_YUV_BN.bmp");//load B/W version of the image
		//System.exit(0);
		
		System.out.println ("choose log ratio");
		System.out.println ("1)log ratio=4.0 [default]   2) log ratio=2   ");
		
		String option =  readKeyboard();
		if (option.equals("")) option="1";
		fc.compressBasicFrame(option,filename);
		
		float ssim=MySSIM.getSSIM("./output_debug/orig_YUV_BN.bmp", "./output_img/BasicLHE_YUV.bmp");
		System.out.println ("SSIM:"+ssim);
	}
	
	
	
	
    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%5
	/**
	 * la funcion compressImage() comprime una imagen con parametros que se recogen por dialogo
	 *   es una funcion util para probar a comprimir una imagen, no para batch
	 * 
	 * 
	 */
	
	public  void compressImage()
	{
		boolean debug=true;
		int percent=0;
		float ql=0;
		float bpp=0;
		boolean ED=true; //ELASTIC DOWN
		
		System.out.println ("debug?[NO]:");
		String debugs =  readKeyboard();
		if (debugs.equals("YES")) debug=true;
		else debug=false;
		
		/*
		System.out.println ("Elastic Down(1) or Homogeneous Down(2)?[1]:");
		String eds =  readKeyboard();
		if (debugs.equals("2")) ED=false;
		else ED=true;
		*/
		
		String downmode="NORMAL";
		
		System.out.println ("Enter filename [default=./img/lena.bmp]:");
		String filename =  readKeyboard();
		if (filename.equals("")) filename=new String("./img/lena.bmp");
		System.out.println ("your filename is : "+filename);
		
		System.out.println ("select 1)AVG_down  or 2)SPS_oneshot_down [default=1]:");
		String optiondown = readKeyboard();
		if (optiondown.equals("")) optiondown=new String("1");
		if ((optiondown.equals("2")))
		{
			downmode="SPS_oneShot";
		}
		else downmode="NORMAL";
		
		
		
		
		System.out.println ("select 1)QL  or 2)% or 3)bpp  [default=1]:");
		String option = readKeyboard();
		if (option.equals("")) option=new String("1");
		
		
		//-------------------------------------------------
		if ((option.equals("1")))
		{
			System.out.println ("Enter QL [25]:");
			String qls = readKeyboard();
			if (qls.equals("")) qls="25";
			ql=Float.parseFloat(qls);
			System.out.println ("your QL is : "+ql);
		}
		//-------------------------------------------------
		else if (option.equals("2"))
		{
			System.out.println ("selction of elasticDown or homogeneousDown");
			System.out.println ("ELASTIC ON|OFF? [ON]");
			String eds = readKeyboard();
			if (eds.equals("")) ED=true;
			else if (eds.equals("OFF")) ED=false;
			
			
			if (ED) System.out.println ("select %");
			if (!ED) System.out.println ("select %  (in homo ratio 1:N is computed based on %");
			String porcents = readKeyboard();
			percent=Integer.parseInt(porcents);
		}
		//-------------------------------------------------
		else if (option.equals("3"))
		{
			System.out.println ("select bpp");
			String bpps = readKeyboard();
			bpp=Float.parseFloat(bpps);
		}
		//-------------------------------------------------
		
		System.out.println ("select interpolation method: 1)NN  or 2)BIL or 3)BIC or 4) EXPERIMENTAL 5) NNL 6) NNSR 7)EPX [default=3] :");
		String interpol = readKeyboard();
		if (interpol.equals("")) interpol=new String("3");
		
		
		
		FrameCompressor fc=new LHE.FrameCompressor(1);
		fc.DEBUG=debug;
		
		fc.downmode=downmode;
		
		
		if (ED==false)fc.MODE=new String("HOMO");
		else fc.MODE=new String("ELASTIC");
		
		
		fc.loadFrame(filename);//esto crea la grid
		
		System.out.println (" image loaded ok");
		//esto es fundamental para luego medir el PSNR en BN
		//------------------------------------------------------
		fc.img.YUVtoBMP("./output_debug/orig_YUV.bmp",fc.img.YUV[0]);
		System.out.println (" saved B/W image ok");
		//fc.img.YUVtoBMP("./output_debug/orig_YUV.bmp",fc.img.YUV[0]);
		float[] resfc=new float[2];
		//-------------------------------------------------
		if (option.equals("1"))resfc=fc.compressFrame(ql);
		//-------------------------------------------------
		else if (option.equals("2") && ED==true)
		{
			ql=-1;
			while (resfc[0]<percent)
				{ql+=0.5f;
				resfc=fc.compressFrame(ql);
				if (ql==100) break;
				}
		}
		else if (option.equals("2") && ED==false)
		{
			ql=100/(float)(percent);//*percent);
			resfc=fc.compressFrame(ql);
				
		}
		//-------------------------------------------------
		else if (option.equals("3"))
		{
			ql=-0.5f;
			while (resfc[1]<bpp)
				{ql+=0.5f;
				resfc=fc.compressFrame(ql);
				if (ql==100) break;
				}
		}
		//-------------------------------------------------
		System.out.println ("file "+filename+" is compressed at "+resfc[0]+"% and "+resfc[1]+"bpp");
		System.out.println ("...now creating player to decompress...");
		//ahora queda crear el player para saber que PSNR nos ha quedado.
		LHE.FramePlayer fp=new LHE.FramePlayer();
		
		if (interpol.equals("3")) fp.INTERPOL=new String("BICUBIC");
		else if (interpol.equals("2")) fp.INTERPOL=new String("BILINEAL");
		else if (interpol.equals("1")) fp.INTERPOL=new String("NN");
		else if (interpol.equals("4")) fp.INTERPOL=new String("EXPERIMENTAL");
		else if (interpol.equals("5")) fp.INTERPOL=new String("NNL");
		else if (interpol.equals("6")) fp.INTERPOL=new String("NNSR");
		else if (interpol.equals("7")) fp.INTERPOL=new String("EPX");
		
		fp.DEBUG=debug;
		fp.img=fc.img;
		fp.grid=fc.grid;
		//fp.playFrame( filename,true);
		//System.out.println ("hola");
		
		//float[] resfp=fp.playFrame( filename);//,true);
		float[] resfp=fp.playFrame( "./output_debug/orig_YUV.bmp");
		//float[] resfp=fp.playFrameFrontier( filename,true);
		
		//fp.img.YUVtoBMP("./output_debug/cosa.bmp",fp.img.interpolated_YUV[0]);
		float et=mideError(fp.img.interpolated_YUV[0],"./output_debug/orig_YUV.bmp");
		
		//double psnr=PSNR.printPSNR2("./output_debug/orig_YUV.bmp", "./output_img/play.bmp", et);
		
		
		float ratio=100/resfp[0];
		ratio=((float)((int)(ratio*100)))/100f;
		System.out.println("  Results:");
		System.out.println ("file "+filename+" is compressed at QL="+ql+" bitrate="+ resfc[1]+" bpp "+" percent="+resfp[0]+"%  ratio=1:"+ratio+" and PSNR="+resfp[1]+" dB");
		
		//System.out.println ("file "+filename+" is compressed at QL="+ql+" bitrate="+ resfc[1]+" bpp "+" percent="+resfp[0]+"% and PSNR="+resfp[1]+" dB");
		String qls=new String(""+ql).replace(".",",");
		String bitrates=new String(""+resfc[1]).replace(".",",");
		String percents=new String(""+resfp[0]).replace(".",",");
		String psnrs=new String(""+resfp[1]).replace(".",",");
		String resultado=new String(""+qls+";"+bitrates+";"+percents+";"+psnrs);
		System.out.println(resultado);
		//compressImage(1,50,filename);
		
	}
    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	/**
	 * la funcion compressImage(, , , , ,) comprime una imagen con parametros que hay que pasar
	 *   no pregunta nada. Es una funcion util para procesos batch
	 *   
	 * @param LHE ON/OFF para activar o no LHE y asi comprobar sin la degradacion de hops
	 * @param ED  ON/OFF elastic down. si se desactiva es down convencional
	 * @param type  1=QL, 2=percent, 3=BPP
	 * @param value  segun el type tiene un significado : QL o percent o bpp
	 * @param filename : path de imagen a comprimir
	 * 
	 * 
	 * @return ql, bpp, percent, psnr
	 * 
	 * result[0]=ql;//ql
	 * result[1]=resfc[1];//bpp
	 * result[2]=resfp[0];//percent
	 * result[3]=resfp[1];//psnr
	 */
	public  float[] compressImage(boolean LHE, boolean ED,int type, float value, String filename, float ql_start, String interpol)
	{
		boolean debug=false;
		int percent=0;
		float ql=0;
		float bpp=0;
		
		if (type==1) ql=value;//QL
		else if (type==2) percent=(int)value;//%
		else if (type==3) bpp=value;//bpp
		
		
		
		FrameCompressor fc=new LHE.FrameCompressor(1);
		fc.DEBUG=debug;
		fc.LHE=LHE;
		if (ED==false)fc.MODE=new String("HOMO");
		else fc.MODE=new String("ELASTIC");
		
		fc.loadFrame(filename);//esto crea la grid
		//aqui una demora para el disco
				try{Thread.sleep(100);}
				catch(Exception e){}
				
		//esto es fundamental para luego medir el PSNR en BN
		//------------------------------------------------------
		fc.img.YUVtoBMP("./output_debug/orig_YUV.bmp",fc.img.YUV[0]);
		
		
		
		
		float[] resfc=new float[2];
		//-------------------------------------------------
		if (type==1)resfc=fc.compressFrame(ql);
		//---------------modo homogeneo 1:N es 1:QL ----------------------------------
		else if (type==2 && ED==false) //PORCENTAJE
		{
			//ql=-1;
			//System.out.println("holak");
			ql=0.95f;
			resfc[0]=10000;
			//while (resfc[0]<percent)
			/*
			while (resfc[0]>percent)
				{//ql+=0.5f;
				
				ql+=0.05f;
				ql=100/(float)(percent*percent);
				resfc=fc.compressFrame(ql);
				System.out.println("filename:"+filename+" QL:"+ql+" percent:"+resfc[0]+" target:"+percent);
				if (ql==100) break;
				if (ql==0) break;
				}
			*/
			ql=100/(float)(percent);//*percent);
			resfc=fc.compressFrame(ql);
			System.out.println("filename:"+filename+" QL:"+ql+" percent:"+resfc[0]+" target:"+percent);
			//1:ql es 1:N por lo tanto: percent=1/n2-> ql=1/percent2
		}
		//-----------------------modo elastico-------------------------------
		
		else if (type==2 && ED==true) //PORCENTAJE
		{
			ql=-1;
			//resfc[0]=10000;
			//este while es horrible, hay que optimizar con un parametro
			ql=ql_start;
			
			while (resfc[0]<percent)
				{ql+=0.5f;
				resfc=fc.compressFrame(ql);
				System.out.println("filename:"+filename+" QL:"+ql+" percent:"+resfc[0]+" target:"+percent);
				if (ql==100) break;
				}
		}
		
		//-------------------------------------------------
		else if (type==3) //BPP
		{
			ql=-1;
			ql=ql_start;
			System.out.println(" buscando "+bpp+" bpp...");
			while (resfc[1]<bpp)
				{ql+=0.5f;
				resfc=fc.compressFrame(ql);
				if (ql==100) break;
				}
		}
		//-------------------------------------------------
		System.out.println ("file "+filename+" is compressed at "+resfc[0]+"% and "+resfc[1]+"bpp");
		System.out.println ("...now creating player to decompress...");
		//ahora queda crear el player para saber que PSNR nos ha quedado.
		LHE.FramePlayer fp=new LHE.FramePlayer();
		fp.DEBUG=debug;
		fp.img=fc.img;
		fp.grid=fc.grid;
		//fp.playFrame( filename,true);
		//System.out.println ("hola");
		
		if (interpol.equals("3")) fp.INTERPOL=new String("BICUBIC");
		else if (interpol.equals("2")) fp.INTERPOL=new String("BILINEAL");
		else if (interpol.equals("1")) fp.INTERPOL=new String("NN");
		else if (interpol.equals("7")) fp.INTERPOL=new String("EPX");
		
	//	float[] resfp=fp.playFrame( filename);//,true);
		
		float[] resfp=fp.playFrame("./output_debug/orig_YUV.bmp");
		
		
		mideError(fp.img.interpolated_YUV[0],"./output_debug/orig_YUV.bmp");
		
		float ratio=100/resfp[0];
		ratio=((float)((int)(ratio*100)))/100f;
		//System.out.println("  Results:");
		System.out.println ("file "+filename+" is compressed at QL="+ql+" bitrate="+ resfc[1]+" bpp "+" percent="+resfp[0]+"%  ratio=1:"+ratio+" and PSNR="+resfp[1]+" dB");
		String qls=new String(""+ql).replace(".",",");
		String bitrates=new String(""+resfc[1]).replace(".",",");
		String percents=new String(""+resfp[0]).replace(".",",");
		String psnrs=new String(""+resfp[1]).replace(".",",");
		String resultado=new String(""+qls+";"+bitrates+";"+percents+";"+psnrs);
		System.out.println(resultado);
		float[] result=new float[5];
		result[0]=ql;//ql
		result[1]=resfc[1];//bpp
		result[2]=resfp[0];//percent
		result[3]=resfp[1];//psnr
		result[4]=resfp[2];//ssim
		return result;
		
	}
    //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public void interpolateSeams()
	{
		LHE.FramePlayer fp=new LHE.FramePlayer();
		fp.DEBUG=true;
		fp.img=new ImgUtil();//luego la cargamos
		//fp.grid=fc.grid;
		System.out.println ("Enter filename [default=./img/sinconsturas.bmp]:");
		String filename =  readKeyboard();
		if (filename.equals("")) filename=new String("./img/sincosturas.bmp");
		System.out.println ("your filename is : "+filename);
		fp.playCosturas(filename);
		
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	
	public void compressDirFullPercent()
	{
		
		Scanner teclado = new Scanner (System.in);		
		System.out.println ("LHE ON/OFF: [ON]");	
				
		boolean LHE=true;
		String lhes =  teclado.next();
		if (lhes.equals("")) LHE=true;
		else if (lhes.equals("OFF")) LHE=false;
		
		System.out.println ("ELASTIC ON/OFF: [ON]");	
		
		boolean ED=true;
		String eds =  teclado.next();
		if (eds.equals("")) ED=true;
		else if (eds.equals("OFF")) ED=false;
		
		System.out.println ("select interpolation method: 1)NN  or 2)BIL  or 3)BIC  [default=3]:");
		String interpol = readKeyboard();
		if (interpol.equals("")) interpol=new String("3");
		
		System.out.println ("Type directory name:");	
		String directorio =  teclado.next();
		//teclado.close();

		//read directory
		File file = new File(directorio);
		if (!file.exists()) {
			System.out.println("El directorio no existe");
			System.exit(0);
		}
		String [] ficherosEnDirectorio = file.list();
		for (int i=0;i<ficherosEnDirectorio.length;i++) {
			//System.out.println(ficherosEnDirectorio[i]);
		}


		//ImgUtil img=new ImgUtil();
		
		//for each file
		//primero pasamos el file a yuvbnç
		String[] rows=new String[ficherosEnDirectorio.length];//resultado
		String[] rowssim=new String[ficherosEnDirectorio.length];//resultado
		for (int i=0;i<ficherosEnDirectorio.length;i++)
		{
			System.out.println(" processing file "+ficherosEnDirectorio[i]+" ...");
		//	img.BMPtoYUV(directorio+"/"+ficherosEnDirectorio[i]);
			rows[i]=new String("");
			rowssim[i]=new String("");
			float ql_start=-1;
			for (float percent=5;percent<100;percent+=5)
			{
				
				float[] res=compressImage(LHE,ED,2,percent,directorio+"/"+ficherosEnDirectorio[i],ql_start, interpol);
				String psnrs=new String(""+res[3]).replace(".",",");
				String ssim=new String(""+res[4]).replace(".",",");
				ql_start=res[0];
				
				//rows[i]=new String(rows[i]+";"+psnrs);
				rows[i]+=new String(psnrs+";");
				rowssim[i]+=new String(ssim+";");
				if (ql_start>=100) break;
			}
			
		}
		System.out.println("");
		System.out.println("------------------TABLE OF RESULTS PSNR----------------------------------");
		System.out.println("5;10;15;20;25;30;35;40;45;50;55;60;65;70;75;80;85;90;95");
		for (int i=0;i<ficherosEnDirectorio.length;i++)
		{
			System.out.println(rows[i]);
		}
		
		
		System.out.println("------------------TABLE OF RESULTS SSIM----------------------------------");
		System.out.println("5;10;15;20;25;30;35;40;45;50;55;60;65;70;75;80;85;90;95");
		for (int i=0;i<ficherosEnDirectorio.length;i++)
		{
			System.out.println(rowssim[i]);
		}
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public void compressImageFullBpp()
	{
		Scanner teclado = new Scanner (System.in);		
		System.out.println ("Enter filename [./img/lena.bmp]:");
		String filename =  teclado.next();
		teclado.close();
		System.out.println ("your option is : "+filename);
		
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
		
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public void performance()
	{
	long lDateTime1 = new Date().getTime();
	System.out.println("Date() - Time in milliseconds: " + lDateTime1);
     
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		public void compressDirFullBpp()
		{
			
			Scanner teclado = new Scanner (System.in);		
			System.out.println ("LHE ON/OFF: [ON]");	
					
			boolean LHE=true;
			String lhes =  teclado.next();
			if (lhes.equals("")) LHE=true;
			else if (lhes.equals("OFF")) LHE=false;
			
			System.out.println ("ELASTIC ON/OFF: [ON]");	
			
			boolean ED=true;
			String eds =  teclado.next();
			if (eds.equals("")) ED=true;
			else if (eds.equals("OFF")) ED=false;
			
			System.out.println ("select interpolation method: 1)NN  or 2)BIL  or 3)BIC  [default=3]:");
			String interpol = readKeyboard();
			if (interpol.equals("")) interpol=new String("3");
			
			
			System.out.println ("Type directory name:");	
			String directorio =  teclado.next();
			//teclado.close();

			//read directory
			File file = new File(directorio);
			if (!file.exists()) {
				System.out.println("El directorio no existe");
				System.exit(0);
			}
			String [] ficherosEnDirectorio = file.list();
			for (int i=0;i<ficherosEnDirectorio.length;i++) {
				//System.out.println(ficherosEnDirectorio[i]);
			}


			//ImgUtil img=new ImgUtil();
			
			//for each file
			//primero pasamos el file a yuvbnç
			String[] rows=new String[ficherosEnDirectorio.length];//resultado
			String[] rowssim=new String[ficherosEnDirectorio.length];//resultado
			
			int bbp_number=14;
			float[] table_bpp=new float[14];
			table_bpp[0]=0.05f;
			table_bpp[1]=0.1f;
			table_bpp[2]=0.15f;
			table_bpp[3]=0.21f;
			table_bpp[4]=0.31f;
			table_bpp[5]=0.41f;
			table_bpp[6]=0.51f;
			table_bpp[7]=0.61f;
			table_bpp[8]=0.71f;
			table_bpp[9]=0.81f;
			table_bpp[10]=0.91f;
			table_bpp[11]=1.01f;
			table_bpp[12]=1.11f;
			table_bpp[13]=1.21f;
			
			
			for (int i=0;i<ficherosEnDirectorio.length;i++)
			{
				System.out.println(" processing file "+ficherosEnDirectorio[i]+" ...");
			//	img.BMPtoYUV(directorio+"/"+ficherosEnDirectorio[i]);
				rows[i]=new String(""+ficherosEnDirectorio[i]+";");
				rowssim[i]=new String(""+ficherosEnDirectorio[i]+";");
				float ql_start=-1;
				for (int bpp_i=0;bpp_i<bbp_number;bpp_i+=1)
				{
					float bpp2=table_bpp[bpp_i];
					
					float[] res=compressImage(LHE,ED,3,bpp2,directorio+"/"+ficherosEnDirectorio[i],ql_start, interpol);
					String psnrs=new String(""+res[3]).replace(".",",");
					String ssim=new String(""+res[4]).replace(".",",");
					ql_start=res[0];
					//if (ql_start>=100) break;
					//rows[i]=new String(rows[i]+";"+psnrs);
					rows[i]+=new String(psnrs+";");
					rowssim[i]+=new String(ssim+";");
					if (ql_start>=100) break;
					
					//System.exit(0);
				}
				
			}
			System.out.println("");
			System.out.println("------------------TABLE OF RESULTS----------------------------------");
			System.out.print("filename;");
			for (int i=0;i<bbp_number;i++)System.out.print(table_bpp[i]+";");
			System.out.println("");
			for (int i=0;i<ficherosEnDirectorio.length;i++)
			{
				System.out.println(rows[i]);
			}

			System.out.println("------------------TABLE OF RESULTS SSIM----------------------------------");
			for (int i=0;i<bbp_number;i++)System.out.print(table_bpp[i]+";");
			System.out.println("");
			for (int i=0;i<ficherosEnDirectorio.length;i++)
			{
				//System.out.println(rowssim[i]);
				System.out.println(rowssim[i]);
			}
		}
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		public float  mideError(int[] result, String origen)
		{
			ImgUtil img=new ImgUtil();
			img.BMPtoYUV(origen);
			img.YUVtoBMP("./output_debug/cosa2.bmp",img.YUV[0]);
			float e=0;
			float ea=0;
			float e1=0;
			
			for (int i=0;i<img.width*img.height;i++)
			{
				//result[i]=result[i]+1;
				e1=result[i]-img.YUV[0][i];
			e+=e1;
			ea+=Math.sqrt(e1*e1);
			}
			System.out.println("el error total (imagen-orig):"+e+ " y el total abs:"+ea);
			return e;
		}
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		/*
		 * esta funcion entra en un directorio origen y en otro destino y busca el fichero comprimido
		 * para cada bpp, considerando que tanto el nombre origen como el bpp deben estar presente en el
		 * nombre destino. 
		 * si lo encuentra calcula el ssim. else pone un cero
		 */
		public void SSIMDirFullBpp()
		{
			/*
			int filter_width=11;
			int filter_widthy=1;
			float sigma_gauss=1.5f;
			int filter_length = filter_width*filter_widthy;
			float window_weights [] = new float [filter_length];
			double [] array_gauss_window = new double [filter_length];

			

				double value, distance = 0;
				int center = (filter_width/2);
		  		double total = 0;
				double sigma_sq=sigma_gauss;//*sigma_gauss;
				int pointer;
		      	  	for (int y = 0; y < filter_widthy; y++){
					for (int x = 0; x < filter_width; x++){
		         				//distance = Math.abs(x-center)*Math.abs(x-center)+Math.abs(y-center)*Math.abs(y-center);
						distance = Math.abs(x-center);
						pointer = y*filter_width + x;
		                			//array_gauss_window[pointer] = Math.exp(-0.5*distance/sigma_sq);
						array_gauss_window[pointer] = Math.exp(-distance/sigma_sq);
						total = total + array_gauss_window[pointer];
						//System.out.print(array_gauss_window[pointer]+",");
						
		  			}
					//System.out.println("");
		    		}
				for (pointer=0; pointer < filter_length; pointer++) {	
					array_gauss_window[pointer] = array_gauss_window[pointer] / total;
					window_weights [pointer] = (float) array_gauss_window[pointer];
				}
				
				for (int y = 0; y < filter_widthy; y++){
					for (int x = 0; x < filter_width; x++){
						pointer = y*filter_width + x;
						System.out.print((int)(100*array_gauss_window[pointer])+",");
						
		  			}
					System.out.println("");
		    		}
				if (1<2) System.exit(0);
			*/
			
			
			
			Scanner teclado = new Scanner (System.in);		
			//System.out.println ("LHE ON/OFF: [ON]");	
					
			
			
			System.out.println ("Type origin directory name:");	
			//String directorio =  teclado.next();
			String directorio =  new String("../kodakBN");
			//String directorio =  new String("../lena");
			//String directorio =  new String("../californiaBN");//aqui estan con nombres diferentes (sin ".")  
			//teclado.close();

			//read directory
			File file = new File(directorio);
			if (!file.exists()) {
				System.out.println("El directorio no existe");
				System.exit(0);
			}
			String [] ficherosEnDirectorio = file.list();
			
			System.out.println ("Type destination directory name:");	
			//String directoriodest =  new String("../kodakSSIM/JPEG");
			//String directoriodest =  new String("../kodakSSIM/JP2K");
			//String directoriodest =  new String("../californiaSSIM/JPEG");
			//String directoriodest =  new String("../californiaSSIM/JP2K");
			String directoriodest =  new String("../out_basiclhe");
			//String directoriodest =  teclado.next();
			
			//read directory
			File filedest = new File(directoriodest);
			if (!filedest.exists()) {
				System.out.println("El directorio destino no existe");
				System.exit(0);
			}
			
			
			System.out.println (" listado de ficheros en DIR origen");
			System.out.println (" ---------------------------------");
			for (int i=0;i<ficherosEnDirectorio.length;i++) {
				System.out.println("    - "+ficherosEnDirectorio[i]);
			}
			String [] ficherosEnDirectoriodest = filedest.list();
			System.out.println (" listado de ficheros en DIR dest");
			System.out.println (" ---------------------------------");
			for (int i=0;i<ficherosEnDirectoriodest.length;i++) {
				System.out.println("    - "+ficherosEnDirectoriodest[i]);
			}

			//ImgUtil img=new ImgUtil();
			
			//for each file
			//primero pasamos el file a yuvbnç
			//String[] rows=new String[ficherosEnDirectorio.length];//resultado
			String[] rowssim=new String[ficherosEnDirectorio.length];//resultado
			
			int bbp_number=16;
			float[] table_bpp=new float[16];
			table_bpp[0]=0.05f;
			table_bpp[1]=0.1f;
			table_bpp[2]=0.15f;
			table_bpp[3]=0.20f;
			table_bpp[4]=0.30f;
			table_bpp[5]=0.40f;
			table_bpp[6]=0.50f;
			table_bpp[7]=0.60f;
			table_bpp[8]=0.70f;
			table_bpp[9]=0.80f;
			table_bpp[10]=0.90f;
			table_bpp[11]=1.00f;
			table_bpp[12]=1.10f;
			table_bpp[13]=1.20f;
			table_bpp[14]=1.50f;//para jp2k
			table_bpp[15]=0.75f;//para jp2k
			
			for (int i=0;i<ficherosEnDirectorio.length;i++)
			{
				System.out.println(" processing file "+ficherosEnDirectorio[i]+" ...");
			//	img.BMPtoYUV(directorio+"/"+ficherosEnDirectorio[i]);
				//rows[i]=new String("");
				rowssim[i]=new String("");
				float ql_start=-1;
				
				for (int bpp_i=0;bpp_i<bbp_number;bpp_i+=1)
				{
					float bpp2=table_bpp[bpp_i];
					
					//ahora debo buscar cual es el fichero correspondiente al fichero origen en dir destino
					String name=ficherosEnDirectorio[i].replace(".bmp","");
					name=new String (name+"_"+bpp2);
					System.out.println(" searching for "+name);
					int index_dest=-1;
					for (int j=0;j<ficherosEnDirectoriodest.length;j++) {
						int k=ficherosEnDirectoriodest[j].indexOf(name);
						if (k!=-1) 
						{
							index_dest=j;
							break;
						}
					}
					float ssim=0;
					if (index_dest!=-1) 
						{System.out.println(" encontrado "+ficherosEnDirectoriodest[index_dest]);
						ssim=MySSIM.getSSIM(directorio+"/"+ficherosEnDirectorio[i], filedest+"/"+ficherosEnDirectoriodest[index_dest]);
						
						}
					
					
					//ya se que fichero es, ahora debo calcular su SSIM
					
					
					
					
					//float[] res=compressImage(LHE,ED,3,bpp2,directorio+"/"+ficherosEnDirectorio[i],ql_start, interpol);
					//String psnrs=new String(""+res[3]).replace(".",",");
					//String ssim=new String(""+res[4]).replace(".",",");
					//ql_start=res[0];
					//if (ql_start>=100) break;
					//rows[i]=new String(rows[i]+";"+psnrs);
					//rows[i]+=new String(psnrs+";");
					rowssim[i]+=new String(ssim+";").replace(".",",");;
					//if (ql_start>=100) break;
				}
				
			}
			System.out.println("");
			
			System.out.println("------------------TABLE OF RESULTS SSIM----------------------------------");
			for (int i=0;i<bbp_number;i++)System.out.print(table_bpp[i]+";");
			System.out.println("");
			for (int i=0;i<ficherosEnDirectorio.length;i++)
			{
				//System.out.println(rowssim[i]);
				System.out.println(rowssim[i]);
			}
		}
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		public void computePSNR()
		{
			System.out.println ("Enter origin image [default=./img/lena.bmp]:");
			String filename =  readKeyboard();
			if (filename.equals("")) filename=new String("./img/lena.bmp");
			System.out.println ("your origin filename is : "+filename);
			
			System.out.println ("Enter degraded image [default=./img/lena.bmp]:");
			String filename2 =  readKeyboard();
			if (filename.equals("")) filename2=new String("./img/lena.bmp");
			System.out.println ("your degraded filename is : "+filename2);
			

			double psnr=PSNR.printPSNR(filename, filename2);		
			//double psnr=PSNR.printPSNR("./output_debug/orig_YUV.bmp", output_directory+"/play.bmp");
			System.out.println(" PSNR:"+psnr+ "  dB");
			
		}
		
		
		
		
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		public void compressVideo()
		{
			VideoCompressor vc =new VideoCompressor();
			//la ql se pasa como parametro
			//vc.compressVideoNONtrail_V2(25);//1.05f);//0.9f);
			//parametros: ql, interpol_type
			//"NN", "BILINEAL", BICUBIC
			vc.compressVideoTesis001(30, "BICUBIC");//1.05f);//0.9f);
		}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		public void compressVideoSIMPLELHE_sampled()
		{
			VideoCompressor vc =new VideoCompressor();
			System.out.println ("Enter target rate [default=2000]:");
			String rate_targets =  readKeyboard();
			if (rate_targets.equals("")) rate_targets=new String("2000");
			
			double rate_target=(double)Float.parseFloat(rate_targets);
			vc.compressVideoLHEsampled_V002(rate_target);
			
		}
		
}//end class
