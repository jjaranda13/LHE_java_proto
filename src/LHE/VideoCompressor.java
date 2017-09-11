package LHE;

import java.io.File;
import java.util.Scanner;
import java.io.*;
import java.util.Comparator;
import java.util.Arrays;

import Qmetrics.*;
/**
 * Class VideoProcessing
 * @author josejavg
 *
 *This class provides methods for processing frames of a given video, before LHE encoding
 *
 *
 *BASIC VIDEO PROCESSING:
 * ---------------------- 
 * open a directory of frames
 *   read frame[0]
 *   videoframe[0]=LHECompress(frame 0)
 *   store videoframe[0]
 *   played_frame[0]=videoframe[0]
 *   store played_frame[0]
 *for (i in GOB)
 *   read frame[i]
 *   compute frame_diff=frame[i]-played_frame[i-1]
 *   videoframe[i]=LHECompres(frame_diff)
 *   store videoframe[i]
 *   played_frame[i] = played_frame[i-1]+video_frame[i] 
 *next i
 *
 *
 * ADVANCED VIDEO PROCESSING
 * -------------------------
 * open a directory of frames
 *   read frame 0
 *   videoframe[0]=LHECompress(frame 0) // compute PRmetrics[0]
 *   store videoframe[0]
 *   
 *   played_frame[0]=videoframe[0]
 *   store played_frame[0]
 *   
 *for (i in GOB)
 *   read frame[i]
 *   LHEframe[i]=LHECompress(frame i) // compute PRmetrics[i]
 *   if (i>0)  
 *     moved_videoframe[i-1]=move(played_frame[i-1], PRmetrics[i-1],PRmetrics[i])
 *   else 
 *     moved_videoframe[0]=videoframe[0]
 *   endif
 *   compute frame_diff=frame[i]-moved_videoframe[i-1]
 *   video_frame[i]=LHECompres(frame_aux)
 *   played_frame[i] = played_frame[i-1]+video_frame[i] 
 *   store video_frame[i]
 *   store played_frame[i]
 *next i
 *
 *
 */
public class VideoCompressor {

	
	public static String output_directory="./output_video";


//*****************************************************************************
	public void compressVideoDiffAlt(float cfini)
	{
		
float cf=cfini;//0.92f;//1.77f;
		//cf=1.5f;
		// get the directory and order the file list
		//------------------------------------------
		System.out.println ("Type directory name ( must be under input_video folder):");	
		Scanner teclado = new Scanner (System.in);		
		String directorio =  teclado.next();
		//teclado.close();

		directorio="./input_video/"+directorio;
		//read directory
		File file = new File(directorio);
		if (!file.exists()) {
			System.out.println("El directorio no existe");
			System.exit(0);
		}
		String [] frames = orderFileList(file.list());
		for (int i=0;i<frames.length;i++) {
			System.out.println(frames[i]);
		}
		
		//read frame 0 and compress it
		//-------------------------------
		LHE.FrameCompressor fc=new LHE.FrameCompressor(1);
		fc.DEBUG=false;
		fc.loadFrame(directorio+"/"+frames[0]);
		fc.compressFrame(cf);
		
		LHE.FramePlayer fp=new LHE.FramePlayer();
		fp.img=fc.img;
		fp.grid=fc.grid;
		fp.playFrame(directorio+"/"+frames[0]);
		
		//save file
		fp.img.YUVtoBMP(output_directory+"/"+frames[0],fp.img.interpolated_YUV[0]);
		
		// cf=0.9f;//
		//preparamos YUV para el bucle
		fp.img.YUV=fp.img.interpolated_YUV;
		ImgUtil lastvideoframe=fp.img;
		
	
		
		boolean nodif=true;
		//bucle
		//------
		
		float infomax=0;
		float infomin=1000;
		
		float tot_porcent=0;
		for (int i=1;i<frames.length;i++) {
			//for (float i_float=1;i_float<frames.length;i_float+=0.5f) {
			//int i=(int)(i_float);
			System.out.println(" NUEVO FRAME A HACER:"+i);
		//for (int i=1;i<10;i++) {
			//int jmax=1;
			//if (i==9) jmax=10;
			//for (int j=0;j<jmax;j++)
			//{	
			
			System.out.println ("ENTER:");	
		//	teclado.next();
			/*
			BufferedReader t = new BufferedReader(new InputStreamReader(System.in));
			try{t.readLine();
			t.close();
			}catch(Exception e){}
			*/
			
			try{
				//Thread.sleep(500);
				//Thread.sleep(100);
			}catch(Exception e){}

			LHE.FrameCompressor fc2=new LHE.FrameCompressor(1);
			fc2.loadFrame(directorio+"/"+frames[i]);
			//restamos frame anterior
			fc2.DEBUG=false;
			
			//LHE.FrameCompressor fc1=new LHE.FrameCompressor(1);
			
			LHE.FrameCompressor fc3=new LHE.FrameCompressor(1);
			fc3.loadFrame(directorio+"/"+frames[i]);
			fc3.DEBUG=false;
			
			if (i%2!=0 ) // frame 1 hacemos y3=y2-y1'
			{
				//cf=0.8f;
				System.out.println(" caso Y3=Y2'-Y1");
			    fc2.img.substractFrame(fp.img.YUV);
			//	fc2.img.substractFrame(fc1.img.YUV);
			}
			else
			{
				System.out.println(" caso Y3=Y1'-Y2");
				fc3.img.copyFrame(fp.img.YUV);
				fc3.img.substractFrame(fc2.img.YUV);
			}
			//else cf=1.5f;
			//fc.img.substractFrame(fc.img.YUV);
			//salvamos la resta
			//fc3.img.YUVtoBMP(output_directory+"/diff2_"+frames[i],fc2.img.YUV[0]);
			//comprimimos la resta
			//fc2.DEBUG=true;
			
			
			//cf+=inc;
			
			System.out.println("cf:"+cf);
			
			if (i%2!=0 )  {
				float info=fc2.img.info();
				System.out.println(" info:"+info+ " max:"+infomax+" min:"+infomin);
				if (info>infomax) infomax=info;
				if (info<infomin) infomin=info;
				//cf=info*30f;
				//cf=cfini*info*35;
				fc2.compressFrame(cf);//20%
				float porcent=fc2.img.getNumberOfNonZeroPixelsDown();
				System.out.println(" porcent:"+porcent);
				tot_porcent+=porcent;
				
			}
			else{         
				float info=fc3.img.info();
				System.out.println(" info:"+info+ " max:"+infomax+" min:"+infomin);
				if (info>infomax) infomax=info;
				if (info<infomin) infomin=info;
				//cf=info*30f;
				//cf=cfini*info*35;
				fc3.compressFrame(cf);//20%
				float porcent=fc3.img.getNumberOfNonZeroPixelsDown();
				System.out.println(" porcent:"+porcent);
				tot_porcent+=porcent;
			
			}
			
			System.out.println("  Y3 ha sido comprimida");
			
			LHE.FramePlayer fp2=new LHE.FramePlayer();
			if (i%2!=0) {fp2.img=fc2.img;fp2.grid=fc2.grid;}
			else {fp2.img=fc3.img;fp2.grid=fc3.grid;}
				
			
			//fp2.DEBUG=true;
			System.out.println("  playing Y3...");
			fp2.playFrame(directorio+"/"+frames[i]);
			System.out.println("  Y3 ha sido played. queda sumarla");
			
			//fp2.img.YUVtoBMP(output_directory+"/diff_play_"+frames[i],fp2.img.interpolated_YUV[0]);
			fp2.img.YUVtoBMP(output_directory+"/diff_playb_"+frames[i],fp2.img.downsampled_YUV[0]);
			
			//fp2.img.YUVtoBMP(output_directory+"/diff_playc_"+frames[i],fp2.img.downsampled_LHE_YUV[0]);
			//fp contiene lo que hay que sumar
			//fp.img.YUV=fp.img.interpolated_YUV;
			//fp=new LHE.FramePlayer();
			//fp.img.BMPtoYUV(directorio+"/"+frames[i-1]);
			//fp.img=lastvideoframe;
			
			//fp.img.watermarkFilter();

			if (i%2!=0)
			fp.img.addFrameDiff(fp2.img.interpolated_YUV);
				//fp.img.addFrameDiff(fp2.img.YUV);
			else
			{
			  //fp2.img.YUV=fp2.img.interpolated_YUV;
			  fp.img.substractFrame1(fp2.img.interpolated_YUV);
			
			}
			
			//else
			//fp.img.copyFrame(fp2.img.interpolated_YUV)	;
			//save file
			
			
			
			//fp.img.watermarkFilter_V2(fp2.img.interpolated_YUV);
			fp.img.YUVtoBMP(output_directory+"/"+frames[i],fp.img.YUV[0]);
			//lastvideoframe=fp.img;
			System.out.println("en media:"+(tot_porcent/(float)i)+"  %");
			
			
			//}//j
		}
		
		
	}
	//***********************************************************	
		String[] orderFileList(String[] lista)
		{
			
			//System.out.println("vamos a ordenar!");
			String[] ordered_list=new String[lista.length];
			
			ComparadorStrings c=new ComparadorStrings();
			Arrays.sort(lista, (Comparator)c);
			
			return lista;
		}
		public class ComparadorStrings implements Comparator<String>
		{
		
			public int compare(String s1, String s2)
			{
				//System.out.println("comparando "+s1+"      "+s2);
				System.out.flush();
				
				//para que frame101 sea mayor que frame20
				if (s1.length()>s2.length()) return 1;
				else if (s1.length()<s2.length()) return -1;
				else return s1.compareTo(s2);
				
				
			}
		}
	//********************************************************	
		public void compressVideoDirect(float cfini)
		{
	float cf=cfini;//0.92f;//1.77f;
			
			// get the directory and order the file list
			//------------------------------------------
			System.out.println ("Type directory name ( must be under input_video folder):");	
			Scanner teclado = new Scanner (System.in);		
			String directorio =  teclado.next();
			//teclado.close();

			directorio="./input_video/"+directorio;
			//read directory
			File file = new File(directorio);
			if (!file.exists()) {
				System.out.println("El directorio no existe");
				System.exit(0);
			}
			String [] frames = orderFileList(file.list());
			for (int i=0;i<frames.length;i++) {
				System.out.println(frames[i]);
			}
			
			//read frame 0 and compress it
			//-------------------------------
			LHE.FrameCompressor fc=new LHE.FrameCompressor(1);
			fc.DEBUG=false;
			fc.loadFrame(directorio+"/"+frames[0]);
			fc.compressFrame(cf);
			
			LHE.FramePlayer fp=new LHE.FramePlayer();
			fp.img=fc.img;
			fp.grid=fc.grid;
			fp.playFrame(directorio+"/"+frames[0]);
			
			//save file
			fp.img.YUVtoBMP(output_directory+"/"+frames[0],fp.img.interpolated_YUV[0]);
			
			// cf=0.9f;//
			//preparamos YUV para el bucle
			fp.img.YUV=fp.img.interpolated_YUV;
			ImgUtil lastvideoframe=fp.img;
			
		
			
			boolean nodif=true;
			//bucle
			//------
			float tot_porcent=0;
			for (int i=1;i<frames.length;i++) {
				
				System.out.println(" NUEVO FRAME A HACER:"+i);
			
				try{
					//Thread.sleep(500);
					//Thread.sleep(100);
				}catch(Exception e){}

				LHE.FrameCompressor fc2=new LHE.FrameCompressor(1);
				fc2.loadFrame(directorio+"/"+frames[i]);
				//restamos frame anterior
				fc2.DEBUG=false;
			
				
				System.out.println("cf:"+cf);
				
				fc2.compressFrame(cf);//20%
				
				float porcent=fc2.img.getNumberOfNonZeroPixelsDown();
				System.out.println(" porcent:"+porcent);
				tot_porcent+=porcent;
				
				
				LHE.FramePlayer fp2=new LHE.FramePlayer();
				fp2.img=fc2.img;fp2.grid=fc2.grid;
				
				
				fp2.playFrame(directorio+"/"+frames[i]);
				
				//else
				fp.img.copyFrame(fp2.img.interpolated_YUV)	;
				//save file
				fp.img.YUVtoBMP(output_directory+"/"+frames[i],fp.img.YUV[0]);
				//lastvideoframe=fp.img;
				
				
				
				//}//j
			}
			System.out.println(" average porcent:"+ (100*tot_porcent/frames.length)+" %");
		}
		//***********************************************************	
		public void compressVideoDiffBasic(float cfini)
		{
	float cf=cfini;//0.92f;//1.77f;
			
			// get the directory and order the file list
			//------------------------------------------
			System.out.println ("Type directory name ( must be under input_video folder):");	
			Scanner teclado = new Scanner (System.in);		
			String directorio =  teclado.next();
			teclado.close();

			directorio="./input_video/"+directorio;
			//read directory
			File file = new File(directorio);
			if (!file.exists()) {
				System.out.println("El directorio no existe");
				System.exit(0);
			}
			String [] frames = orderFileList(file.list());
			for (int i=0;i<frames.length;i++) {
				System.out.println(frames[i]);
			}
			
			//read frame 0 and compress it
			//-------------------------------
			LHE.FrameCompressor fc=new LHE.FrameCompressor(1);
			fc.DEBUG=false;
			fc.loadFrame(directorio+"/"+frames[0]);
			fc.compressFrame(cf);
			
			LHE.FramePlayer fp=new LHE.FramePlayer();
			fp.img=fc.img;
			fp.grid=fc.grid;
			fp.playFrame(directorio+"/"+frames[0]);
			
			//save file
			fp.img.YUVtoBMP(output_directory+"/"+frames[0],fp.img.interpolated_YUV[0]);
			
			// cf=0.9f;//
			//preparamos YUV para el bucle
			fp.img.YUV=fp.img.interpolated_YUV;
			ImgUtil lastvideoframe=fp.img;
			
		
			
			boolean nodif=true;
			//bucle
			//------
			float tot_porcent=0;
			for (int i=1;i<frames.length;i++) {
				
				System.out.println(" NUEVO FRAME A HACER:"+i);
			//for (int i=1;i<10;i++) {
				//int jmax=1;
				//if (i==9) jmax=10;
				//for (int j=0;j<jmax;j++)
				//{	
				
			//	System.out.println ("ENTER:");	
			//	teclado.next();
				/*
				BufferedReader t = new BufferedReader(new InputStreamReader(System.in));
				try{t.readLine();
				t.close();
				}catch(Exception e){}
				*/
				
				try{
					//Thread.sleep(500);
					//Thread.sleep(200);
				}catch(Exception e){}

				LHE.FrameCompressor fc2=new LHE.FrameCompressor(1);
				fc2.loadFrame(directorio+"/"+frames[i]);
				/*
				if (i%3==0)
				fc2.loadFrame(directorio+"/"+frames[i]);
				else if (i%3==1){
					//fc2.loadFrame(directorio+"/"+frames[i]);
					fc2.loadFrameGridPlus(directorio+"/"+frames[i],1);
				}
				else fc2.loadFrameGridPlus(directorio+"/"+frames[i],2);
					*/
				fc2.DEBUG=false;
				
				//LHE.FrameCompressor fc1=new LHE.FrameCompressor(1);
				//fc1.loadFrame(directorio+"/"+frames[i-1]);
				//fc1.DEBUG=false;
				
					//cf=0.8f;
				//restamos frame anterior
				
					System.out.println(" caso Y3=Y2'-Y1");
				   
					fc2.img.substractFrame(fp.img.YUV);
					
					
					
					//fc2.img.substractFrame(fc1.img.YUV);
				System.out.println("cf:"+cf);
				
				//fc2.img.watermarkFilter_V2(fc2.img.YUV);
				fc2.compressFrame(cf);//20%
				
				float porcent=fc2.img.getNumberOfNonZeroPixelsDown();
				System.out.println(" porcent:"+porcent);
				tot_porcent+=porcent;
				
				fc2.img.YUVtoBMP(output_directory+"/diff_down_play_"+frames[i],fc2.img.downsampled_LHE_YUV[0]);
				System.out.println("  Y3 ha sido comprimida");
				
				LHE.FramePlayer fp2=new LHE.FramePlayer();
				{fp2.img=fc2.img;fp2.grid=fc2.grid;}
					
				
				fp2.DEBUG=false;
				System.out.println("  playing Y3...");
				
				
				
				
				fp2.playFrameDiff(directorio+"/"+frames[i]);
				
				System.out.println("  Y3 ha sido played. queda sumarla");
				
				//fp2.img.YUVtoBMP(output_directory+"/diff_play_"+frames[i],fp2.img.interpolated_YUV[0]);
				
				//fp2.img.YUVtoBMP(output_directory+"/diff_playc_"+frames[i],fp2.img.downsampled_LHE_YUV[0]);
				//fp contiene lo que hay que sumar
				//fp.img.YUV=fp.img.interpolated_YUV;
				//fp=new LHE.FramePlayer();
				//fp.img.BMPtoYUV(directorio+"/"+frames[i-1]);
				//fp.img=lastvideoframe;
				
				fp.img.initY3();
				
				//fp.img.addFrameDiff(fp2.img.interpolated_YUV);
				
				//else
				//fp.img.copyFrame(fp2.img.interpolated_YUV)	;
				//save file
				//fp.img.watermarkFilter_V2();
				
				//fp.img.watermarkFilter_V3(fp2.img.interpolated_YUV);
				
				fp.img.YUVtoBMP(output_directory+"/"+frames[i],fp.img.YUV[0]);
				//lastvideoframe=fp.img;
				
				
				
				//}//j
			}
			System.out.println(" average porcent:"+ (100*tot_porcent/frames.length)+" %");
		}
		//***********************************************************	
		public void compressVideoDiffGob(int gob, float cfini)
		{
	//float cfini=0.92f;//1.77f;
	float cf=cfini;
			
			// get the directory and order the file list
			//------------------------------------------
			System.out.println ("Type directory name ( must be under input_video folder):");	
			Scanner teclado = new Scanner (System.in);		
			String directorio =  teclado.next();
			//teclado.close();

			directorio="./input_video/"+directorio;
			//read directory
			File file = new File(directorio);
			if (!file.exists()) {
				System.out.println("El directorio no existe");
				System.exit(0);
			}
			String [] frames = orderFileList(file.list());
			for (int i=0;i<frames.length;i++) {
				System.out.println(frames[i]);
			}
			
			//read frame 0 and compress it
			//-------------------------------
			LHE.FrameCompressor fc=new LHE.FrameCompressor(1);
			fc.DEBUG=false;
			fc.loadFrame(directorio+"/"+frames[0]);
			fc.compressFrame(cf);
			
			LHE.FramePlayer fp=new LHE.FramePlayer();
			fp.img=fc.img;
			fp.grid=fc.grid;
			fp.playFrame(directorio+"/"+frames[0]);
			
			//save file
			fp.img.YUVtoBMP(output_directory+"/"+frames[0],fp.img.interpolated_YUV[0]);
			
			// cf=0.9f;//
			//preparamos YUV para el bucle
			fp.img.YUV=fp.img.interpolated_YUV;
			ImgUtil lastvideoframe=fp.img;
			
		
			
			boolean nodif=true;
			
			//bucle
			//------
			for (int i=1;i<frames.length;i++) {
				
				cf=cfini;
				
				System.out.println(" NUEVO FRAME A HACER:"+i);
			//for (int i=1;i<10;i++) {
				//int jmax=1;
				//if (i==9) jmax=10;
				//for (int j=0;j<jmax;j++)
				//{	
				
				System.out.println ("ENTER:");	
			//	teclado.next();
				/*
				BufferedReader t = new BufferedReader(new InputStreamReader(System.in));
				try{t.readLine();
				t.close();
				}catch(Exception e){}
				*/
				
				try{
					//Thread.sleep(500);
					//Thread.sleep(100);
				}catch(Exception e){}

				LHE.FrameCompressor fc2=new LHE.FrameCompressor(1);
				fc2.loadFrame(directorio+"/"+frames[i]);
				//restamos frame anterior
				fc2.DEBUG=false;
				
				//LHE.FrameCompressor fc1=new LHE.FrameCompressor(1);
				
				LHE.FrameCompressor fc3=new LHE.FrameCompressor(1);
				fc3.loadFrame(directorio+"/"+frames[i]);
				fc3.DEBUG=false;
				
				if (i%gob==0)
				{
					//directo
					//fc2 contiene la imagen
				}
				else if (i%2!=0 ) // frame 1 hacemos y3=y2-y1'
				{
					//cf=0.8f;
					System.out.println(" caso Y3=Y2'-Y1");
				    fc2.img.substractFrame(fp.img.YUV);
				//	fc2.img.substractFrame(fc1.img.YUV);
				}
				else
				{
					System.out.println(" caso Y3=Y1'-Y2");
					fc3.img.copyFrame(fp.img.YUV);
					fc3.img.substractFrame(fc2.img.YUV);
				}
				//else cf=1.5f;
				//fc.img.substractFrame(fc.img.YUV);
				//salvamos la resta
				//fc3.img.YUVtoBMP(output_directory+"/diff2_"+frames[i],fc2.img.YUV[0]);
				//comprimimos la resta
				//fc2.DEBUG=true;
				
				
				//cf+=inc;
				
				System.out.println("cf:"+cf);
				if (i%gob==0)
				{
					cf=cfini*1.3f;//un poco mas
					fc2.compressFrame(cf);//20%
				}
				else if (i%2!=0 )  fc2.compressFrame(cf);//20%
				else          fc3.compressFrame(cf);//20%
				
				System.out.println("  Y3 ha sido comprimida");
				
				LHE.FramePlayer fp2=new LHE.FramePlayer();
				if (i%gob==0)
				{
					//directo
					fp2.img=fc2.img;fp2.grid=fc2.grid;
				}
				else if (i%2!=0) {fp2.img=fc2.img;fp2.grid=fc2.grid;}
				else {fp2.img=fc3.img;fp2.grid=fc3.grid;}
					
				
				//fp2.DEBUG=true;
				System.out.println("  playing Y3...");
				fp2.playFrame(directorio+"/"+frames[i]);
				System.out.println("  Y3 ha sido played. queda sumarla");
				
				//fp2.img.YUVtoBMP(output_directory+"/diff_play_"+frames[i],fp2.img.interpolated_YUV[0]);
				//fp contiene lo que hay que sumar
				//fp.img.YUV=fp.img.interpolated_YUV;
				//fp=new LHE.FramePlayer();
				//fp.img.BMPtoYUV(directorio+"/"+frames[i-1]);
				//fp.img=lastvideoframe;
				if (i%gob==0)
				{
					//directo
					fp.img.copyFrame(fp2.img.interpolated_YUV);
				}
				else if (i%2!=0)
				{fp.img.addFrameDiff(fp2.img.interpolated_YUV);
				
				}
				else
				{
				//fp2.img.YUV=fp2.img.interpolated_YUV;
				fp.img.substractFrame1(fp2.img.interpolated_YUV);
				
				}
				
				//else
				//fp.img.copyFrame(fp2.img.interpolated_YUV)	;
				//save file
				fp.img.YUVtoBMP(output_directory+"/"+frames[i],fp.img.YUV[0]);
				//lastvideoframe=fp.img;
				
				
				
				//}//j
			}
			
		}
		//***********************************************************
		
		//***********************************************************	
		public void compressVideoDiffBasicINV(float cfini)
		{
	float cf=cfini;//0.92f;//1.77f;
			
			// get the directory and order the file list
			//------------------------------------------
			System.out.println ("Type directory name ( must be under input_video folder):");	
			Scanner teclado = new Scanner (System.in);		
			String directorio =  teclado.next();
			teclado.close();

			directorio="./input_video/"+directorio;
			//read directory
			File file = new File(directorio);
			if (!file.exists()) {
				System.out.println("El directorio no existe");
				System.exit(0);
			}
			String [] frames = orderFileList(file.list());
			for (int i=0;i<frames.length;i++) {
				System.out.println(frames[i]);
			}
			
			//read frame 0 and compress it
			//-------------------------------
			LHE.FrameCompressor fc=new LHE.FrameCompressor(1);
			fc.DEBUG=false;
			fc.loadFrame(directorio+"/"+frames[0]);
			fc.compressFrame(cf);
			
			LHE.FramePlayer fp=new LHE.FramePlayer();
			fp.img=fc.img;
			fp.grid=fc.grid;
			fp.playFrame(directorio+"/"+frames[0]);
			
			//save file
			fp.img.YUVtoBMP(output_directory+"/"+frames[0],fp.img.interpolated_YUV[0]);
			
			// cf=0.9f;//
			//preparamos YUV para el bucle
			fp.img.YUV=fp.img.interpolated_YUV;
			ImgUtil lastvideoframe=fp.img;
			
		
			
			boolean nodif=true;
			//bucle
			//------
			for (int i=1;i<frames.length;i++) {
				
				System.out.println(" NUEVO FRAME A HACER:"+i);
			//for (int i=1;i<10;i++) {
				//int jmax=1;
				//if (i==9) jmax=10;
				//for (int j=0;j<jmax;j++)
				//{	
				
				System.out.println ("ENTER:");	
			//	teclado.next();
				/*
				BufferedReader t = new BufferedReader(new InputStreamReader(System.in));
				try{t.readLine();
				t.close();
				}catch(Exception e){}
				*/
				
				try{
					//Thread.sleep(500);
					Thread.sleep(200);
				}catch(Exception e){}

				LHE.FrameCompressor fc2=new LHE.FrameCompressor(1);
				fc2.loadFrame(directorio+"/"+frames[i]);
				fc2.DEBUG=false;
				
				
				LHE.FrameCompressor fc3=new LHE.FrameCompressor(1);
				fc3.loadFrame(directorio+"/"+frames[i]);
				fc3.DEBUG=false;
				
			
					System.out.println(" caso Y3=Y1'-Y2");
					fc3.img.copyFrame(fp.img.YUV);
					fc3.img.substractFrame(fc2.img.YUV);
			
				
				//LHE.FrameCompressor fc1=new LHE.FrameCompressor(1);
				//fc1.loadFrame(directorio+"/"+frames[i-1]);
				//fc1.DEBUG=false;
				
					//cf=0.8f;
				//restamos frame anterior
				/*
					System.out.println(" caso Y3=Y2'-Y1");
				   
					fc2.img.substractFrame(fp.img.YUV);
					//fc2.img.substractFrame(fc1.img.YUV);
					 
					 */
				System.out.println("cf:"+cf);
				
				//fc2.img.watermarkFilter_V2(fc2.img.YUV);
				
				fc3.compressFrame(cf);//20%
				float porcent=fc3.img.getNumberOfNonZeroPixelsDown();
				System.out.println(" porcent:"+porcent);
				
				
				//fc2.compressFrame(cf);//20%
				System.out.println("  Y3 ha sido comprimida");
				
				LHE.FramePlayer fp2=new LHE.FramePlayer();
				
				fp2.img=fc3.img;fp2.grid=fc3.grid;
				
				//{fp2.img=fc2.img;fp2.grid=fc2.grid;}
					
				
				//fp2.DEBUG=true;
				System.out.println("  playing Y3...");
				fp2.playFrame(directorio+"/"+frames[i]);
				System.out.println("  Y3 ha sido played. queda sumarla");
				
				fp2.img.YUVtoBMP(output_directory+"/diff_play_"+frames[i],fp2.img.interpolated_YUV[0]);
				
				//fp2.img.YUVtoBMP(output_directory+"/diff_playc_"+frames[i],fp2.img.downsampled_LHE_YUV[0]);
				//fp contiene lo que hay que sumar
				//fp.img.YUV=fp.img.interpolated_YUV;
				//fp=new LHE.FramePlayer();
				//fp.img.BMPtoYUV(directorio+"/"+frames[i-1]);
				//fp.img=lastvideoframe;
				
				 fp.img.substractFrame1(fp2.img.interpolated_YUV);
				//fp.img.addFrameDiff(fp2.img.interpolated_YUV);
				
				//else
				//fp.img.copyFrame(fp2.img.interpolated_YUV)	;
				//save file
				//fp.img.watermarkFilter_V2();
				
				//fp.img.watermarkFilter_V2(fp2.img.interpolated_YUV);
				
				fp.img.YUVtoBMP(output_directory+"/"+frames[i],fp.img.YUV[0]);
				//lastvideoframe=fp.img;
				
				
				
				//}//j
			}
			
		}
		//***********************************************************
		//*****************************************************************************
		public void compressVideoDiffAltOne(float cfini)
		{
			
	float cf=cfini;//0.92f;//1.77f;
			//cf=1.5f;
			// get the directory and order the file list
			//------------------------------------------
			System.out.println ("Type directory name ( must be under input_video folder):");	
			Scanner teclado = new Scanner (System.in);		
			String directorio =  teclado.next();
			//teclado.close();

			directorio="./input_video/"+directorio;
			//read directory
			File file = new File(directorio);
			if (!file.exists()) {
				System.out.println("El directorio no existe");
				System.exit(0);
			}
			String [] frames = orderFileList(file.list());
			//String [] frames = file.list();
			for (int i=0;i<frames.length;i++) {
				System.out.println(frames[i]);
			}
			
			//read frame 0 and compress it
			//-------------------------------
			LHE.FrameCompressor fc=new LHE.FrameCompressor(1);
			fc.DEBUG=false;
			fc.loadFrame(directorio+"/"+frames[0]);
			fc.compressFrame(cf);
			
			LHE.FramePlayer fp=new LHE.FramePlayer();
			fp.img=fc.img;
			fp.grid=fc.grid;
			fp.playFrame(directorio+"/"+frames[0]);
			
			//save file
			fp.img.YUVtoBMP(output_directory+"/"+frames[0],fp.img.interpolated_YUV[0]);
			
			// cf=0.9f;//
			//preparamos YUV para el bucle
			fp.img.YUV=fp.img.interpolated_YUV;
			ImgUtil lastvideoframe=fp.img;
			
		
			
			boolean nodif=true;
			//bucle
			//------
			
			float infomax=0;
			float infomin=1000;
			
			float tot_porcent=0;
			
			//BUCLE DE FOTOGRAMAS
			//--------------------
			for (int i=1;i<frames.length;i++) {
				
				System.out.println(" NUEVO FRAME A HACER:"+i);
			//for (int i=1;i<10;i++) {
				//int jmax=1;
				//if (i==9) jmax=10;
				//for (int j=0;j<jmax;j++)
				//{	
				
				System.out.println ("ENTER:");	
			//	teclado.next();
				/*
				BufferedReader t = new BufferedReader(new InputStreamReader(System.in));
				try{t.readLine();
				t.close();
				}catch(Exception e){}
				*/
				
				try{
					//Thread.sleep(500);
					Thread.sleep(100);
				}catch(Exception e){}

				LHE.FrameCompressor fc2=new LHE.FrameCompressor(1);
				fc2.loadFrame(directorio+"/"+frames[i]);
				//restamos frame anterior
				fc2.DEBUG=false;
				
				//LHE.FrameCompressor fc1=new LHE.FrameCompressor(1);
				
				LHE.FrameCompressor fc3=new LHE.FrameCompressor(1);
				fc3.loadFrame(directorio+"/"+frames[i]);
				fc3.DEBUG=false;
				
				//lo hacemos en este orden. otro orden fallaria
				System.out.println(" fc3: caso Y3=Y1'-Y2");
				fc3.img.copyFrame(fp.img.YUV);
				fc3.img.substractFrame(fc2.img.YUV);
				
				System.out.println(" fc2: caso Y3=Y2'-Y1");
				fc2.img.substractFrame(fp.img.YUV);
				
				//ImgUtil img3=fc3.img;
				fc3.grid=fc2.grid;//grid no incluye imgUtil
				
				fc2.preCompressFrame(cf);//calcula metrics y ppp
				//fc3.preCompressFrame(cf);
				
				fc2.postCompressFrame(cf);//down y LHE
				
				//fc3.preCompressFrame(cf);
				fc3.postCompressFrame(cf);//down y LHE
				
				
				
				fc2.img.YUVtoBMP(output_directory+"/diff_down_Y3"+frames[i],fc2.img.downsampled_YUV[0]);
				fc3.img.YUVtoBMP(output_directory+"/diff_down_Y3neg"+frames[i],fc3.img.downsampled_YUV[0]);
				
				//ya tengo y3 y y3neg
				//debemos unificar grid y ppps
				 float porcent=fc2.img.getNumberOfNonZeroPixelsDown();
					System.out.println(" porcent (f2):"+porcent);
					tot_porcent+=porcent;
					
					porcent=fc3.img.getNumberOfNonZeroPixelsDown();
					System.out.println(" porcent (f3):"+porcent);
					//tot_porcent+=porcent;
				//cf+=inc;
				
				System.out.println("cf:"+cf);
				
				//if (i%2!=0 ) 
				
		       //ya tengo las dos comprimidas
			    //ahora debo generar y2 con otra funcion
		
				System.out.println("  Y3 y Y3neg han sido comprimidas");
				
				//queda hacer una operacion nueva, que mezcle en down las dos, antes de sumarla o restarla
				
			    //VOY POR AQUI
				LHE.FramePlayer fp2=new LHE.FramePlayer();
				fp2.img=fc2.img;fp2.grid=fc2.grid;
				
				
				LHE.FramePlayer fp3=new LHE.FramePlayer();
				fp3.img=fc3.img;fp3.grid=fc3.grid;
				
				
				//sumamos antes de interpolar
				fp2.img.sumadown(fc2.img.downsampled_YUV,fc3.img.downsampled_YUV);
				
				//fp2.img.sumadown(fc2.img.downsampled_YUV,fc3.img.downsampled_YUV);
				Block.img=fp2.img;
				
				//fp2.img.YUVtoBMP(output_directory+"/diff_down_"+frames[i],fp2.img.downsampled_YUV[0]);
				//fp2.DEBUG=true;
				System.out.println("  playing Y3...");
				fp2.playFrame(directorio+"/"+frames[i]);
				
				//fp3.playFrame(directorio+"/y2b_"+frames[i]);
				
				//fp2.img.media(fp2.img.interpolated_YUV,fp3.img.interpolated_YUV);
				
				System.out.println("  Y3 ha sido played. queda sumarla");
				
				//fp2.img.YUVtoBMP(output_directory+"/diff_play_"+frames[i],fp2.img.interpolated_YUV[0]);
			//	fp2.img.YUVtoBMP(output_directory+"/diff_playb_"+frames[i],fp2.img.downsampled_YUV[0]);
				
				//fp2.img.YUVtoBMP(output_directory+"/diff_playc_"+frames[i],fp2.img.downsampled_LHE_YUV[0]);
				//fp contiene lo que hay que sumar
				//fp.img.YUV=fp.img.interpolated_YUV;
				//fp=new LHE.FramePlayer();
				//fp.img.BMPtoYUV(directorio+"/"+frames[i-1]);
				//fp.img=lastvideoframe;
				
				
			//	fp.img.watermarkFilter_V2(fp2.img.interpolated_YUV);
			
				fp.img.addFrameDiff(fp2.img.interpolated_YUV);
			
				//else
				//fp.img.copyFrame(fp2.img.interpolated_YUV)	;
				//save file
				//fp.img.watermarkFilter_V2(fp2.img.interpolated_YUV);
				fp.img.YUVtoBMP(output_directory+"/"+frames[i],fp.img.YUV[0]);
				//lastvideoframe=fp.img;
				System.out.println("en media:"+(tot_porcent/(float)i)+"  %");
				
				
				//}//j
			}
			
			
		}
		//***********************************************************
		
		
		
		public void compressVideoDiffY4(float cfini)
		{
	float cf=cfini;//0.92f;//1.77f;
			
			// get the directory and order the file list
			//------------------------------------------
			System.out.println ("Type directory name ( must be under input_video folder):");	
			Scanner teclado = new Scanner (System.in);		
			String directorio =  teclado.next();
			teclado.close();

			directorio="./input_video/"+directorio;
			//read directory
			File file = new File(directorio);
			if (!file.exists()) {
				System.out.println("El directorio no existe");
				System.exit(0);
			}
			String [] frames = orderFileList(file.list());
			for (int i=0;i<frames.length;i++) {
				System.out.println(frames[i]);
			}
			
			//read frame 0 and compress it
			//-------------------------------
			LHE.FrameCompressor fc=new LHE.FrameCompressor(1);
			fc.DEBUG=false;
			fc.loadFrame(directorio+"/"+frames[0]);
			fc.compressFrame(cf);
			
			LHE.FramePlayer fp=new LHE.FramePlayer();
			fp.img=fc.img;
			fp.grid=fc.grid;
			fp.playFrame(directorio+"/"+frames[0]);
			
			//save file
			fp.img.YUVtoBMP(output_directory+"/"+frames[0],fp.img.interpolated_YUV[0]);
			
			// cf=0.9f;//
			//preparamos YUV para el bucle
			fp.img.YUV=fp.img.interpolated_YUV;
			ImgUtil lastvideoframe=fp.img;
			
		
			
			boolean nodif=true;
			//bucle
			//------
			float tot_porcent=0;
			System.out.println("Y4 inicializando...");
			fp.img.initY4();
			System.out.println("Y4 inicializado");
			for (int i=1;i<frames.length;i++) {
				
				System.out.println(" NUEVO FRAME A HACER:"+i);
			//for (int i=1;i<10;i++) {
				//int jmax=1;
				//if (i==9) jmax=10;
				//for (int j=0;j<jmax;j++)
				//{	
				
			//	System.out.println ("ENTER:");	
			//	teclado.next();
				/*
				BufferedReader t = new BufferedReader(new InputStreamReader(System.in));
				try{t.readLine();
				t.close();
				}catch(Exception e){}
				*/
				
				try{
					//Thread.sleep(500);
					//Thread.sleep(200);
				}catch(Exception e){}

				LHE.FrameCompressor fc2=new LHE.FrameCompressor(1);
				fc2.loadFrame(directorio+"/"+frames[i]);
				/*
				if (i%3==0)
				fc2.loadFrame(directorio+"/"+frames[i]);
				else if (i%3==1){
					//fc2.loadFrame(directorio+"/"+frames[i]);
					fc2.loadFrameGridPlus(directorio+"/"+frames[i],1);
				}
				else fc2.loadFrameGridPlus(directorio+"/"+frames[i],2);
					*/
				fc2.DEBUG=false;
				
				//LHE.FrameCompressor fc1=new LHE.FrameCompressor(1);
				//fc1.loadFrame(directorio+"/"+frames[i-1]);
				//fc1.DEBUG=false;
				
					//cf=0.8f;
				//restamos frame anterior
				
					System.out.println(" caso Y4:");
				   
					
					
					//fc2.img.substractFrame(fp.img.YUV);
					fc2.img.initY4();
					System.out.println(" init ok");
					fc2.img.computeY4(fp.img.YUV);
					
					System.out.println(" Y4 ok");
					//fc2.img.substractFrame(fc1.img.YUV);
				System.out.println("cf:"+cf);
				
				//fc2.img.watermarkFilter_V2(fc2.img.YUV);
				
				
				// AHORA debemos determinar cuanto comprimimos la imagen
				//en funcion de su cantidad de info, respecto del cf inicial
				
				//voy a aumentar y disminuir cf en torno al cd inicial
				
				
				System.out.println("compressing...");
				fc2.compressFrame(cf);//20%
				
				float porcent=fc2.img.getNumberOfNonZeroPixelsDown();
				System.out.println(" porcent:"+porcent);
				tot_porcent+=porcent;
				
				//fc2.img.YUVtoBMP(output_directory+"/diff_down_play_"+frames[i],fc2.img.downsampled_LHE_YUV[0]);
				System.out.println("  Y3 ha sido comprimida");
				
				LHE.FramePlayer fp2=new LHE.FramePlayer();
				{fp2.img=fc2.img;fp2.grid=fc2.grid;}
					
				
				fp2.DEBUG=false;
				System.out.println("  playing Y3...");
				fp2.playFrameDiff(directorio+"/"+frames[i]);
				System.out.println("  Y3 ha sido played. queda sumarla");
				
				//fp2.img.YUVtoBMP(output_directory+"/diff_play_"+frames[i],fp2.img.interpolated_YUV[0]);
				
				//fp2.img.YUVtoBMP(output_directory+"/diff_playc_"+frames[i],fp2.img.downsampled_LHE_YUV[0]);
				//fp contiene lo que hay que sumar
				//fp.img.YUV=fp.img.interpolated_YUV;
				//fp=new LHE.FramePlayer();
				//fp.img.BMPtoYUV(directorio+"/"+frames[i-1]);
				//fp.img=lastvideoframe;
				
				
				//fp.img.addFrameDiff(fp2.img.interpolated_YUV);
				//fp2.img.YUVtoBMP(output_directory+"/i_diff_playc_"+frames[i],fp2.img.interpolated_YUV[0]);
				
				
				//primero me hago una copia de Y1
				ImgUtil img_ant=new ImgUtil();
				img_ant.BMPtoYUV(directorio+"/"+frames[i]);//para alocar memoria, nada mas
				img_ant.copyFrame(fp.img.YUV);
				
				fp.img.computeY2fromY4(fp2.img.interpolated_YUV);
				
				//else
				//fp.img.copyFrame(fp2.img.interpolated_YUV)	;
				//save file
				//fp.img.watermarkFilter_V2();
				
				//fp.img.watermarkFilter_V3(fp2.img.interpolated_YUV);
				
				//fp.img.watermarkFilter_V4(img_ant.YUV);
				
				fp.img.YUVtoBMP(output_directory+"/"+frames[i],fp.img.YUV[0]);
				//lastvideoframe=fp.img;
				
				
				
				//}//j
			}
			System.out.println(" average porcent:"+ (100*tot_porcent/frames.length)+" %");
		}
		//***********************************************************

		public void compressVideoLogY3(float cfini)
		{
	float cf=cfini;//0.92f;//1.77f;
			
			// get the directory and order the file list
			//------------------------------------------
			System.out.println ("Type directory name ( must be under input_video folder):");	
			Scanner teclado = new Scanner (System.in);		
			String directorio =  teclado.next();
			teclado.close();

			directorio="./input_video/"+directorio;
			//read directory
			File file = new File(directorio);
			if (!file.exists()) {
				System.out.println("El directorio no existe");
				System.exit(0);
			}
			String [] frames = orderFileList(file.list());
			for (int i=0;i<frames.length;i++) {
				System.out.println(frames[i]);
			}
			
			//read frame 0 and compress it
			//-------------------------------
			LHE.FrameCompressor fc=new LHE.FrameCompressor(1);
			fc.DEBUG=false;
			fc.loadFrame(directorio+"/"+frames[0]);
			fc.compressFrame(cf);
			
			LHE.FramePlayer fp=new LHE.FramePlayer();
			fp.img=fc.img;
			fp.grid=fc.grid;
			fp.playFrame(directorio+"/"+frames[0]);
			
			//save file
			fp.img.YUVtoBMP(output_directory+"/"+frames[0],fp.img.interpolated_YUV[0]);
			
			// cf=0.9f;//
			//preparamos YUV para el bucle
			fp.img.YUV=fp.img.interpolated_YUV;
			ImgUtil lastvideoframe=fp.img;
			
		
			
			boolean nodif=true;
			//bucle
			//------
			float tot_porcent=0;
			System.out.println("log Y3 inicializando...");
			fp.img.initLogY3();
			System.out.println("log Y3 inicializado");
			for (int i=1;i<frames.length;i++) {
				
				System.out.println(" NUEVO FRAME A HACER:"+i);
			//for (int i=1;i<10;i++) {
				//int jmax=1;
				//if (i==9) jmax=10;
				//for (int j=0;j<jmax;j++)
				//{	
				
			//	System.out.println ("ENTER:");	
			//	teclado.next();
				/*
				BufferedReader t = new BufferedReader(new InputStreamReader(System.in));
				try{t.readLine();
				t.close();
				}catch(Exception e){}
				*/
				
				try{
					//Thread.sleep(500);
					//Thread.sleep(200);
				}catch(Exception e){}

				LHE.FrameCompressor fc2=new LHE.FrameCompressor(1);
				fc2.loadFrame(directorio+"/"+frames[i]);
				/*
				if (i%3==0)
				fc2.loadFrame(directorio+"/"+frames[i]);
				else if (i%3==1){
					//fc2.loadFrame(directorio+"/"+frames[i]);
					fc2.loadFrameGridPlus(directorio+"/"+frames[i],1);
				}
				else fc2.loadFrameGridPlus(directorio+"/"+frames[i],2);
					*/
				fc2.DEBUG=false;
				
				//LHE.FrameCompressor fc1=new LHE.FrameCompressor(1);
				//fc1.loadFrame(directorio+"/"+frames[i-1]);
				//fc1.DEBUG=false;
				
					//cf=0.8f;
				//restamos frame anterior
				
					
					//fc2.img.substractFrame(fp.img.YUV);
					//fc2.img.initY4();
					fc2.img.initLogY3();
					System.out.println(" init LogY3 ok");
					
					fc2.img.computeLogY3(fp.img.YUV);
					//fc2.img.computeY4(fp.img.YUV);
					
					System.out.println(" Y4 ok");
					//fc2.img.substractFrame(fc1.img.YUV);
				System.out.println("cf:"+cf);
				
				//fc2.img.watermarkFilter_V2(fc2.img.YUV);
				
				
				// AHORA debemos determinar cuanto comprimimos la imagen
				//en funcion de su cantidad de info, respecto del cf inicial
				
				//voy a aumentar y disminuir cf en torno al cd inicial
				
				
				System.out.println("compressing...");
				fc2.compressFrame(cf);//20%
				
				float porcent=fc2.img.getNumberOfNonZeroPixelsDown();
				System.out.println(" porcent:"+porcent);
				tot_porcent+=porcent;
				
				fc2.img.YUVtoBMP(output_directory+"/diff_down_play_"+frames[i],fc2.img.downsampled_LHE_YUV[0]);
				System.out.println("  Y3 ha sido comprimida");
				
				LHE.FramePlayer fp2=new LHE.FramePlayer();
				{fp2.img=fc2.img;fp2.grid=fc2.grid;}
					
				
				fp2.DEBUG=false;
				System.out.println("  playing Y3...");
				fp2.playFrameDiff(directorio+"/"+frames[i]);
				System.out.println("  Y3 ha sido played. queda sumarla");
				
				//fp2.img.YUVtoBMP(output_directory+"/diff_play_"+frames[i],fp2.img.interpolated_YUV[0]);
				
				//fp2.img.YUVtoBMP(output_directory+"/diff_playc_"+frames[i],fp2.img.downsampled_LHE_YUV[0]);
				//fp contiene lo que hay que sumar
				//fp.img.YUV=fp.img.interpolated_YUV;
				//fp=new LHE.FramePlayer();
				//fp.img.BMPtoYUV(directorio+"/"+frames[i-1]);
				//fp.img=lastvideoframe;
				
				
				//fp.img.addFrameDiff(fp2.img.interpolated_YUV);
				//fp2.img.YUVtoBMP(output_directory+"/i_diff_playc_"+frames[i],fp2.img.interpolated_YUV[0]);
				
				
				//primero me hago una copia de Y1
				ImgUtil img_ant=new ImgUtil();
				img_ant.BMPtoYUV(directorio+"/"+frames[i]);//para alocar memoria, nada mas
				img_ant.copyFrame(fp.img.YUV);
				
				
				//con log es equivalente, sale de un array
				fp.img.computeY2fromY4(fp2.img.interpolated_YUV);
				//fp.img.computeY2fromLogY3(fp2.img.interpolated_YUV);
				
				//else
				//fp.img.copyFrame(fp2.img.interpolated_YUV)	;
				//save file
				//fp.img.watermarkFilter_V2();
				
				//fp.img.watermarkFilter_V3(fp2.img.interpolated_YUV);
				
				//fp.img.watermarkFilter_V4(img_ant.YUV);
				
				fp.img.YUVtoBMP(output_directory+"/"+frames[i],fp.img.YUV[0]);
				//lastvideoframe=fp.img;
				
				
				
				//}//j
			}
			System.out.println(" average porcent:"+ (100*tot_porcent/frames.length)+" %");
		}
		//***********************************************************	
		
		public void compressVideoDiffY3PRtrail(float cfini)
		{
	float cf=2;//cfini;//0.92f;//1.77f;
			cf=0;//cfini;
			// get the directory and order the file list
			//------------------------------------------
			System.out.println ("Type directory name ( must be under input_video folder):");	
			Scanner teclado = new Scanner (System.in);		
			String directorio =  teclado.next();
			teclado.close();

			directorio="./input_video/"+directorio;
			//read directory
			File file = new File(directorio);
			if (!file.exists()) {
				System.out.println("El directorio no existe");
				System.exit(0);
			}
			String [] frames = orderFileList(file.list());
			for (int i=0;i<frames.length;i++) {
				System.out.println(frames[i]);
			}
			
			//read frame 0 and compress it
			//-------------------------------
			LHE.FrameCompressor fc=new LHE.FrameCompressor(1);
			fc.DEBUG=false;
			fc.loadFrame(directorio+"/"+frames[0]);
			fc.compressFrame(cf);
			
			LHE.FramePlayer fp=new LHE.FramePlayer();
			fp.img=fc.img;
			fp.img.initY3();
			fp.grid=fc.grid;
			fp.playFrame(directorio+"/"+frames[0],true);
			fp.img.YUVtoBMP(output_directory+"/"+frames[0],fp.img.interpolated_YUV[0]);
			
			/*
			LHE.FramePlayer fpc=new LHE.FramePlayer();
			fpc.img=fc.img;
			fpc.img.initY3();
			fpc.grid=fc.grid;
			fpc.playFrame(directorio+"/"+frames[0],false);
			fpc.img.YUVtoBMP(output_directory+"/"+"C_"+frames[0],fp.img.interpolated_YUV[0]);
			*/
			
			
			//lo mismo pero en modo vecino
			/*
			LHE.FramePlayer fpN=new LHE.FramePlayer();
			fpN.img=fc.img;
			fpN.grid=fc.grid;
			//fpN.img.copyFrame(fc.img.downsampled_LHE_YUV);
			fpN.playFrame(directorio+"/"+frames[0],false);
			fpN.img.YUVtoBMP(output_directory+"/N_"+frames[0],fp.img.interpolated_YUV[0]);
			*/
			
			// cf=0.9f;//
			//preparamos YUV para el bucle
			fp.img.YUV=fp.img.interpolated_YUV;
			ImgUtil lastvideoframe=fp.img;
			
		
			
			boolean nodif=true;
			//bucle
			//------
			float tot_porcent=0;
			//System.out.println("log Y3 inicializando...");
			//fp.img.initLogY3();
			//System.out.println("log Y3 inicializado");
			
			
			//-----------------BUCLE DE FRAMES------------------------------------
			Grid grid_ant= fc.grid;
			cf=cfini;
			for (int i=1;i<frames.length;i++) {
				
				System.out.println(" NUEVO FRAME A HACER:"+i);
			
				//if (i/10==(float)i/10f) cf=3;
				//else cf=cfini;
				
				try{
				//	Thread.sleep(500);
					//Thread.sleep(200);
				}catch(Exception e){}

				LHE.FrameCompressor fc2=new LHE.FrameCompressor(1);
				fc2.loadFrame(directorio+"/"+frames[i]);
				
				fc2.DEBUG=false;
				
				

				//fc2.img.computeY3(fp.img.YUV, fp.img.mask);//usando Y1'
				
				LHE.FrameCompressor fc3=new LHE.FrameCompressor(1);
				fc3.loadFrame(directorio+"/"+frames[i-1]);//frame original anterior
				
				//para que funcione la historia de PR
				fc2.grid=grid_ant;
				
				//fc2.img.computeY3(fc3.img.YUV);//usando Y1
				
				//fc2.img.computeY3(f1p, fp.img.mask,y1, fp.img.countdown);//usando Y1' y mask y y1
				//fc2.img.computeY3(fp.img.YUV, fp.img.mask,fc3.img.YUV, fp.img.countdown);//usando Y1' y mask y y1

				
				//CAMBIO. METO FPN para calcular Y3, para el codec
				//fc2.img.computeY3(fpN.img.YUV, fp.img.mask,fc3.img.YUV, fp.img.countdown);//usando Y1' y mask y y1
				
				//voy a cambiar y1p por vecino
				
				
				//fc2.img.computeY3(fp.img.YUV, fp.img.mask,fc3.img.YUV, fp.img.countdown);//usando Y1' y mask y y1
				fc2.img.computeY3(fp.img.YUV, fp.img.mask,fc3.img.YUV, fp.img.countdown);//usando Y1' y mask y y1
				
				
				
				//fc2.img.YUVtoBMP(output_directory+"/diff_play_"+frames[i],fc2.img.YUV[0]);
				
				//if (i==3) System.exit(0);
				//ahora el img de fc2 contiene Y3
				
				System.out.println(" Y3 ok");
				
				//fc2.img.substractFrame(fc1.img.YUV);
				System.out.println("cf:"+cf);
				
				System.out.println("pre-compressing...");
				//la grid anterior esta almacenada en grid_ant 
				
				
				fc2.preCompressFrame(cf,grid_ant);//calcula metrics y ppp
				//fc3.preCompressFrame(cf);
				
				//ahora hay que corregir el PR
				//fc2.grid.reComputeMetrics();
				
				grid_ant=fc2.grid;
				System.out.println("post-compressing...");
				fc2.postCompressFrame(cf,true);//down y LHE. con false se desactiva lhe
				
				
				
				
				//fc2.compressFrame(cf);//20%
				
				float porcent=fc2.img.getNumberOfNonZeroPixelsDown();
				System.out.println(" porcent:"+porcent);
				tot_porcent+=porcent;
				
				fc2.img.YUVtoBMP(output_directory+"/diff_down_play_"+frames[i],fc2.img.downsampled_LHE_YUV[0]);
				//fc2.img.YUVtoBMP(output_directory+"/diff_play_"+frames[i],fc2.img.LHE_YUV[0]);
				System.out.println("  Y3 ha sido comprimida");
				
				LHE.FramePlayer fp2=new LHE.FramePlayer();
				{fp2.img=fc2.img;fp2.grid=fc2.grid;}
					
				
				fp2.DEBUG=false;
				System.out.println("  playing Y3...");
				//fp2.playFrameDiff(directorio+"/"+frames[i]);
				
				fp2.playFrame(directorio+"/"+frames[i],true);//play Y4
                
				//tenemos y4 en fp2.img.interpolated_YUV
				//----------------------------------------
				/*
                LHE.FrameCompressor fc5=new LHE.FrameCompressor(1);
                fc5.loadFrame(directorio+"/"+frames[i-1]);//por alocar memoria
				//fc5.loadFrame(output_directory+"/"+frames[i]);//frame resultado
				fc5.img.copyFrame(fp2.img.interpolated_YUV);//copiamos en YUV[0]
				fc5.grid.setMaxPR();
				fc5.preCompressFrame(cf,fc5.grid);//calcula metrics, ecualiza y ppp
				fc5.grid.setMaxPR();//lo llevamos todo a 1.0 que es lo maximo tras ecualizar
				//fc5.grid.topaPR(0.5f);
				fc5.grid.expandHistogramPR_04();
				fc5.grid.fromPRtoPPP(cf);
				
				//de nada serviria un SPS. quiero filtrar la posible estela.
				//ademas la estela no esta en Y3, la estela se genera en el player
				fc5.postCompressFrame(cf, false);//down y LHE
				fc5.img.YUVtoBMP(output_directory+"/diff_downL_"+frames[i],fc5.img.downsampled_LHE_YUV[0]);
				LHE.FramePlayer fp5=new LHE.FramePlayer();
				{fp5.img=fc5.img;fp5.grid=fc5.grid;}
				fp5.playFrame(directorio+"/"+frames[i],true);//interpolacion
				fp2.img.interpolated_YUV=fp5.img.interpolated_YUV;
				*/
				//-------------------------------------------
				//fp2.playFrame(directorio+"/N_"+frames[i],false);
				System.out.println("  Y3 ha sido played. queda sumarla");
				
				//fp2.img.YUVtoBMP(output_directory+"/diff_play_"+frames[i],fp2.img.interpolated_YUV[0]);
				
				//fp2.img.YUVtoBMP(output_directory+"/diff_playc_"+frames[i],fp2.img.downsampled_LHE_YUV[0]);
				//fp contiene lo que hay que sumar
				//fp.img.YUV=fp.img.interpolated_YUV;
				//fp=new LHE.FramePlayer();
				//fp.img.BMPtoYUV(directorio+"/"+frames[i-1]);
				//fp.img=lastvideoframe;
				
				
				//fp.img.addFrameDiff(fp2.img.interpolated_YUV);
				//fp2.img.YUVtoBMP(output_directory+"/i_diff_playc_"+frames[i],fp2.img.interpolated_YUV[0]);
				
				
				//primero me hago una copia de Y1. NO SE USA
				/*
				ImgUtil img_ant=new ImgUtil();
				img_ant.BMPtoYUV(directorio+"/"+frames[i]);//para alocar memoria, nada mas
				img_ant.copyFrame(fp.img.YUV);//copio el frame ant
				fp.img.filterEstelas(fp2.img.interpolated_YUV,img_ant.YUV);
				*/
				
				//fp.img.addFrameDiff(fp2.img.interpolated_YUV);
				//fp.img.initY3();
				//fp.img.go128();
				fp.img.computeY2fromY4(fp2.img.interpolated_YUV);//frame nuevo a img.YUV[0]
				
				
				//con log es equivalente, sale de un array
				//fp.img.computeY2fromY4(fp2.img.interpolated_YUV);
				//fp.img.computeY2fromLogY3(fp2.img.interpolated_YUV);
				
				//else
				//fp.img.copyFrame(fp2.img.interpolated_YUV)	;
				//save file
				//fp.img.watermarkFilter_V2();
				
				//fp.img.watermarkFilter_V3(fp2.img.interpolated_YUV);
				
				//fp.img.watermarkFilter_V4(img_ant.YUV);
				
				fp.img.YUVtoBMP(output_directory+"/"+frames[i],fp.img.YUV[0]);
				//
				
				
				//ahora hacemos una imagen sin-bilineal para el compresor
			//	fp2.playFrame(directorio+"/"+frames[i],false);//play Y4
				//fpc.img.computeY2fromY4(fp2.img.interpolated_YUV);//frame nuevo a img.YUV[0]
				//fpc.img.YUVtoBMP(output_directory+"/"+"C_"+frames[i],fp.img.YUV[0]);
				
				
				//lastvideoframe=fp.img;
				
				
				/*
				//ahora modo vecino
				//lo mismo pero en modo vecino
				LHE.FramePlayer fpN2=new LHE.FramePlayer();
				//fpN2.img.copyFrame(fc2.img.downsampled_LHE_YUV);
				fpN2.img=fc2.img;
				fpN2.grid=fc2.grid;
				fpN2.playFrame(directorio+"/"+frames[i],false);
				//fpN.img.YUVtoBMP(output_directory+"/N_"+frames[0],fp.img.interpolated_YUV[0]);
				//fp2.playFrame(output_directory+"/N_"+frames[i],false);//interpola Y4
				fpN.img.initY3();
				fpN.img.computeY2fromY4(fpN2.img.interpolated_YUV);//suma a Y1p y calcula Y2
				fpN.img.YUVtoBMP(output_directory+"/N_"+frames[i],fp.img.YUV[0]);//salva
				*/
				//fp.grid.setMaxPR();
				/*
				LHE.FrameCompressor fc4=new LHE.FrameCompressor(1);
				fc4.loadFrame(output_directory+"/"+frames[i]);//frame resultado
				fc4.grid.setMaxPR();
				fc4.preCompressFrame(cf,fc4.grid);//calcula metrics, ecualiza y ppp
				fc4.grid.setMaxPR();//lo llevamos todo a 1.0 que es lo maximo tras ecualizar
				fc4.grid.topaPR(0.5f);
				fc4.grid.expandHistogramPR_04();
				fc4.grid.fromPRtoPPP(cf);
				fc4.postCompressFrame(cf, false);//down y LHE
				//fc4.img.YUVtoBMP(output_directory+"/diff_downL_"+frames[i],fc4.img.downsampled_LHE_YUV[0]);
				LHE.FramePlayer fp4=new LHE.FramePlayer();
				{fp4.img=fc4.img;fp4.grid=fc4.grid;}
				fp4.playFrame(directorio+"/"+frames[i],true);//interpolacion
				//fp4.img.YUVtoBMP(output_directory+"/L_"+frames[i],fp4.img.interpolated_YUV[0]);
				fp.img.YUV[0]=fp4.img.interpolated_YUV[0];
			    */
				//img_ant.copyFrame(fp.img.YUV);
				//}//j
			}
			System.out.println(" average porcent:"+ (100*tot_porcent/frames.length)+" %");
		}
		//***********************************************************

public void compressVideoNONtrail(float cfini)
		{
	
	boolean bilineal=false;
	       float cf=0;//cfini;//cfini;//0.92f;//1.77f;
			
			// get the directory and order the file list
			//------------------------------------------
			System.out.println ("Type directory name ( must be under input_video folder):");	
			Scanner teclado = new Scanner (System.in);		
			String directorio =  teclado.next();
			teclado.close();
			directorio="./input_video/"+directorio;
			
			//read directory
			//--------------
			File file = new File(directorio);
			if (!file.exists()) {
				System.out.println("El directorio no existe");
				System.exit(0);
			}
			//ordena la lista de fotogramas por nombre e imprime
			//--------------------------------------------------
			String [] frames = orderFileList(file.list());
			for (int i=0;i<frames.length;i++) {
				System.out.println(frames[i]);
			}
			
			//read frame 0 and compress it
			//-------------------------------
			LHE.FrameCompressor fc=new LHE.FrameCompressor(1);
			fc.DEBUG=false;
			fc.loadFrame(directorio+"/"+frames[0]);
			fc.compressFrame(cf);
			
			//play the frame
			//--------------
			LHE.FramePlayer fp=new LHE.FramePlayer();
			fp.img=fc.img;
			fp.img.initY3(); //precompute deltaY (2 linear segments + vertical segment)
			fp.grid=fc.grid;
			fp.playFrame(directorio+"/"+frames[0],bilineal);// true=bilineal interpol
			// save the resulting file
			fp.img.YUVtoBMP(output_directory+"/"+frames[0],fp.img.interpolated_YUV[0]);
			
			
			//preparamos YUV para el bucle
			fp.img.YUV=fp.img.interpolated_YUV;
			ImgUtil lastvideoframe=fp.img;
			
			
			//  bucle of frames
			//--------------------------------------------------------------------
			float tot_porcent=0; //not consider initial spatial compressed frame
			Grid grid_ant= fc.grid;
			cf=cfini;
			
			for (int i=1;i<frames.length;i++) {
				
				System.out.println(" next frame to compress:"+i);
				
				//wait for hard disc
				//------------------
				try{
				//	Thread.sleep(500);
					//Thread.sleep(200);
				}catch(Exception e){}

				
				LHE.FrameCompressor fc2=new LHE.FrameCompressor(1);
				fc2.loadFrame(directorio+"/"+frames[i]);
				fc2.DEBUG=false;
				
				LHE.FrameCompressor fc3=new LHE.FrameCompressor(1);
				fc3.loadFrame(directorio+"/"+frames[i-1]);//frame original anterior
				
				//la img de fc2 debe ser siempre la misma. dentro de img se almacena countdown
				//util pq al cabo de n pasos de y2=y1 asignaremos 128 a delta
				fc2.img.computeY3(fp.img.YUV, fp.img.mask,fc3.img.YUV, fp.img.countdown);//usando Y1' y mask y y1
				
				//ahora tenemos deltaY en fc2.img.YUV
				//vamos a comprimir la delta y a calcular sus valores PR de su grid
				//----------------------------------------------------------------
				System.out.println("pre-compressing...");
				//los valores historicos de PR se van almacenando en grid_ant
				fc2.grid=grid_ant;
				fc2.preCompressFrame(cf,grid_ant);//calcula metrics y ppp
				//grid_ant=fc2.grid;//esto sobra
				System.out.println("post-compressing...");
				fc2.postCompressFrame(cf,true);//down y LHE. con false se desactiva lhe
				
				
				
				
				//fc2.compressFrame(cf);//20%
				
				float porcent=fc2.img.getNumberOfNonZeroPixelsDown();
				System.out.println(" porcent:"+porcent);
				tot_porcent+=porcent;
				
				//fc2.img.YUVtoBMP(output_directory+"/diff_down_play_"+frames[i],fc2.img.downsampled_LHE_YUV[0]);
				System.out.println("  Y3 ha sido comprimida");
				
				
				//ahora interpolamos la delta
				//-------------------------------
				LHE.FramePlayer fp2=new LHE.FramePlayer();
				{fp2.img=fc2.img;fp2.grid=fc2.grid;}
				fp2.DEBUG=false;
				System.out.println("  playing Y3...");
				fp2.playFrame(directorio+"/"+frames[i],false);//play Y4
                //tenemos deltaY en fp2.img 
				System.out.println(" delta Y ha sido played. queda sumarla");
				
				
				//vamos a generar una imagen Y1'' comprimida e interpolada segun la grid del deltaY
				//---------------------------------------------------------------------------------
				System.out.println(" creando Y1pp...");
				LHE.FrameCompressor fc_y1pp=new LHE.FrameCompressor(1);
				fc_y1pp.img=new ImgUtil(lastvideoframe);//constructor de copia
				//fc_y1pp.loadFrame(output_directory+"/"+frames[i-1]);
				//fc_y1pp.compressFrame(cf);//,false);
				//fc_y1pp.preCompressFrame(cf,grid_ant);
				fc_y1pp.grid=fc2.grid;//grid de la deltay
				PRblock.img=fc_y1pp.img;
				fc_y1pp.postCompressFrame(cf, false);//no aplicamos LHE logicamente y el cf da igual en postcompress
				System.out.println(" comprimida Y1p...");
				fc_y1pp.img.downsampled_YUV=fc_y1pp.img.downsampled_LHE_YUV;
				//fc_y1pp.img.YUVtoBMP(output_directory+"/Y1P_down_"+frames[i],fc_y1pp.img.downsampled_YUV[0]);
				LHE.FramePlayer fp_y1pp=new LHE.FramePlayer();
				fp_y1pp.img=fc_y1pp.img;
				fp_y1pp.grid=fc_y1pp.grid;
				System.out.println(" playing Y1pp...");
				//Block.MAX_PPP=10;
				fp_y1pp.playFrame(output_directory+"/"+frames[i-1], bilineal);
				//fp_y1pp.img.YUVtoBMP(output_directory+"/Y1PP_"+frames[i],fp_y1pp.img.interpolated_YUV[0]);
				//ahora tengo el y1pp en fp_y1pp.img.YUV
				//...............................................................
				
				//fp es y1p
				fp.img.computeY2fromDeltaY1pY1pp(fp2.img.interpolated_YUV,fp_y1pp.img.interpolated_YUV);
				//fp.img.computeY2fromY4(fp2.img.interpolated_YUV);//frame nuevo a img.YUV[0]
				//fp_y1pp.img.initY3();
				//fp_y1pp.img.computeY2fromY4(fp2.img.interpolated_YUV);//frame nuevo a img.YUV[0]
				//fp.img=fp_y1pp.img;
				
				
				
				fp.img.YUVtoBMP(output_directory+"/"+frames[i],fp.img.YUV[0]);
				//
				
				
				lastvideoframe=fp.img;
				
				
			}
			System.out.println(" average porcent:"+ (100*tot_porcent/frames.length)+" %");
		}
		//***********************************************************			
		 
//***********************************************************

public void compressVideoNONtrail_V2(float cfini)
		{
	
	boolean bilineal=true;
	       float cf=cfini;//cfini;//cfini;//0.92f;//1.77f;
			
			// get the directory and order the file list
			//------------------------------------------
			System.out.println ("Type directory name ( must be under input_video folder):");	
			Scanner teclado = new Scanner (System.in);		
			String directorio =  teclado.next();
			teclado.close();
			directorio="./input_video/"+directorio;
			
			//read directory
			//--------------
			File file = new File(directorio);
			if (!file.exists()) {
				System.out.println("El directorio no existe");
				System.exit(0);
			}
			//ordena la lista de fotogramas por nombre e imprime
			//--------------------------------------------------
			String [] frames = orderFileList(file.list());
			for (int i=0;i<frames.length;i++) {
				System.out.println(frames[i]);
			}
			
			//read frame 0 and compress it
			//-------------------------------
			LHE.FrameCompressor fc=new LHE.FrameCompressor(1);
			fc.DEBUG=false;
			fc.loadFrame(directorio+"/"+frames[0]);
			fc.compressFrame(cf);
			
			//play the frame
			//--------------
			LHE.FramePlayer fp=new LHE.FramePlayer();
			fp.img=fc.img;
			fp.img.initY3(); //precompute deltaY (2 linear segments + vertical segment)
			fp.grid=fc.grid;
			//fp.playFrame(directorio+"/"+frames[0],bilineal);// true=bilineal interpol
			fp.playFrame(directorio+"/"+frames[0]);// true=bilineal interpol
			// save the resulting file
			fp.img.YUVtoBMP(output_directory+"/"+frames[0],fp.img.interpolated_YUV[0]);
			
			//if (1==1) System.exit(0);
			//preparamos YUV para el bucle
			//OJO QUE SI NO PONGO ESTO NO FUNCIONA TAL COMO LO HE PROGRAMADO
			fp.img.YUV=fp.img.interpolated_YUV;
			
			ImgUtil lastvideoframe=fp.img;
			ImgUtil last_lowres_videoframe=fp.img;
			
			
			
			//  bucle of frames
			//--------------------------------------------------------------------
			float tot_porcent=0; //not consider initial spatial compressed frame
			Grid grid_ant= fc.grid;
			cf=cfini;
			
			double total_psnr=0;
			
			for (int i=1;i<frames.length;i++) {
				
				System.out.println(" next frame to compress:"+i);
				
				//wait for hard disc
				//------------------
				try{
				//	Thread.sleep(500);
					Thread.sleep(50);
				}catch(Exception e){}

				
				LHE.FrameCompressor fc2=new LHE.FrameCompressor(1);
				fc2.loadFrame(directorio+"/"+frames[i]); // Esto es Y2
			//	fc2.img.YUVtoBMP(output_directory+"/fotograma.bmp",fc2.img.YUV[0]);
				fc2.DEBUG=false;
				fc2.MODE=new String("ELASTIC");
				
				LHE.FrameCompressor fc3=new LHE.FrameCompressor(1);
				fc3.loadFrame(directorio+"/"+frames[i-1]);//Esto es Y1, es decirl, el frame original anterior
				
				//la img de fc2 debe ser siempre la misma. dentro de img se almacena countdown
				//util pq al cabo de n pasos de y2=y1 asignaremos 128 a delta
				//fc2.img.computeY3(y1p,mask, y1,count)
				fc2.img.computeY3(lastvideoframe.interpolated_YUV, fp.img.error,fc3.img.YUV, fp.img.countdown);//usando Y1' y mask y y1
				
				
				
				//ahora tenemos deltaY en fc2.img.YUV
				//vamos a comprimir la delta y a calcular sus valores PR de su grid
				//----------------------------------------------------------------
				System.out.println("pre-compressing...");
				//los valores historicos de PR se van almacenando en grid_ant
				fc2.grid=grid_ant;
				fc2.preCompressFrame(cf,grid_ant);//calcula metrics y ppp
				//grid_ant=fc2.grid;//esto sobra
				System.out.println("post-compressing...");
				fc2.postCompressFrame(cf,true);//down y LHE. con false se desactiva lhe
				System.out.println("  Y3 ha sido comprimida");
				
				//estadisticas de consumo espacial
				//----------------------------------------
				float porcent=fc2.img.getNumberOfNonZeroPixelsDown();
				System.out.println(" porcent:"+porcent);
				tot_porcent+=porcent;
				
				//fc2.img.YUVtoBMP(output_directory+"/diff_down_play_"+frames[i],fc2.img.downsampled_LHE_YUV[0]);
				
				
				
				//ahora interpolamos la delta. NO NO LA INTERPOLAMOS
				//-------------------------------
				//LHE.FramePlayer fp2=new LHE.FramePlayer();
				//{fp2.img=fc2.img;fp2.grid=fc2.grid;}
				//fp2.DEBUG=false;
				
				//primero la transformo en una funcion continua
				//para ello necesito y1pp o y1p
				//fp2.img.initY3();
				//fp2.img.regeneraDelta(lastvideoframe.YUV);
				
				//System.out.println("  playing Y3...");
				//fp2.playFrame(directorio+"/"+frames[i],false);//interpola delta en modo vecino
                //tenemos deltaY en fp2.img 
			
				
				//vamos a generar una imagen Y1'' comprimida e interpolada segun la grid del deltaY
				//---------------------------------------------------------------------------------
				System.out.println(" creando Y1pp...");
				LHE.FrameCompressor fc_y1pp=new LHE.FrameCompressor(1);
				fc_y1pp.MODE=new String("ELASTIC");
				
				
				fc_y1pp.img=new ImgUtil(lastvideoframe);//constructor de copia
				//fc_y1pp.loadFrame(output_directory+"/"+frames[i-1]);
				//fc_y1pp.compressFrame(cf);//,false);
				//fc_y1pp.preCompressFrame(cf,grid_ant);
				fc_y1pp.grid=fc2.grid;//grid de la deltay
				//PRblock.img=fc_y1pp.img;
				fc_y1pp.postCompressFrame(cf, false);//no aplicamos LHE logicamente y el cf da igual en postcompress
				System.out.println(" comprimida Y1p...");
				fc_y1pp.img.downsampled_YUV=fc_y1pp.img.downsampled_LHE_YUV;
				//fc_y1pp.img.YUVtoBMP(output_directory+"/Y1P_down_"+frames[i],fc_y1pp.img.downsampled_YUV[0]);
				
				//sumamos la delta. ambos tienen la misma resolucion NOOOOOO
				//--------------------------------------------------
				
				//primero regenero la delta
				LHE.FramePlayer fp2=new LHE.FramePlayer();
				{fp2.img=fc2.img;fp2.grid=fc2.grid;}
				fp2.DEBUG=false;
				fp2.img.initY3();
				Block.img=fp2.img;
				fp2.img.regeneraDownDelta(fc_y1pp.img.downsampled_YUV);
				//fp2.img.regeneraDownDelta(lastvideoframe.YUV);
				fp2.img.YUVtoBMP(output_directory+"/Y1ppDydown"+frames[i],fp2.img.downsampled_LHE_YUV[0]);
				//System.out.println("  saving delta...");
				//fp2.img.YUVtoBMP(output_directory+"/Y1ppDy1"+frames[i],fp2.img.interpolated_YUV[0]);
				System.out.println("  playing delta...");
				//playframe usa downsampled y no downsampledLHE, por eso lo igualo
				fp2.img.downsampled_YUV=fp2.img.downsampled_LHE_YUV;
				fp2.DEBUG=false;
				Block.img=fp2.img;
				//fp2.playFrame(directorio+"/"+frames[i],bilineal);//interpola delta en modo vecino
				fp2.INTERPOL=new String("NN");
				//fp2.INTERPOL=new String("BICUBIC");
				//fp2.INTERPOL=new String("BILINEAL");
				
				fp2.playFrame(directorio+"/"+frames[i]);//interpola delta en modo vecino
				//tenemos en fp2 interpolated a y1pp+delta, ya interpolado
				System.out.println("  played");
				//System.out.println("  saving delta interpolated...");
				//fp2.img.YUVtoBMP(output_directory+"/Y1ppDy"+frames[i],fp2.img.interpolated_YUV[0]);
				
				
				//fc_y1pp.img.initY3();
				//fc_y1pp.img.sumaDownDelta(fp2.img.downsampled_LHE_YUV);
				
				
				
				//interpolamos con la delta ya sumada
				//-----------------------------------
				LHE.FramePlayer fp_y1pp=new LHE.FramePlayer();
				
				fp_y1pp.img=fc_y1pp.img;
				fp_y1pp.grid=fc_y1pp.grid;
				System.out.println(" playing Y1pp...");
				Block.img=fp_y1pp.img;
				//fp_y1pp.playFrame(output_directory+"/"+frames[i-1], bilineal);
				
				
				fp_y1pp.INTERPOL=new String("NN");
				//fp_y1pp.INTERPOL=new String("BICUBIC");
				//fp_y1pp.INTERPOL=new String("BILINEAL");
				
				
				fp_y1pp.playFrame(output_directory+"/"+frames[i-1]);
				
				//fp_y1pp.img.YUVtoBMP(output_directory+"/Y1ppUP"+frames[i],fp_y1pp.img.interpolated_YUV[0]);
				//if (1==1) System.exit(0);
				//estas dos son las que se van a usar
				
				if (i<=1) last_lowres_videoframe=fp_y1pp.img; // de este modo dy saldra todo 128
				//fp_y1pp.img.YUVtoBMP(output_directory+"/Y1PP_"+frames[i],fp_y1pp.img.interpolated_YUV[0]);
				//fp_y1pp.img.YUVtoBMP(output_directory+"/LLR_"+frames[i],last_lowres_videoframe.interpolated_YUV[0]);
				//ahora tengo el y1pp en fp_y1pp.img.YUV
				//...............................................................
				
				
				
				//vamos a transformar Last Low Resolution frame a la nueva grid de baja resolucion
				//si la nueva grid tiene menos resolucion, evitaremos que parezca que algo ha cambiado
				//si tiene mas, esto es inocuo
				//-----------------------------------------------------------------------------------
				/*
				System.out.println(" creando Y1ppLLR...");
				LHE.FrameCompressor fc_y1ppLLR=new LHE.FrameCompressor(1);
				//fc_y1ppLLR.img=new ImgUtil(last_lowres_videoframe);//constructor de copia
				fc_y1ppLLR.img=new ImgUtil(lastvideoframe);//constructor de copia
				fc_y1ppLLR.grid=fc2.grid;//grid de la deltay
				PRblock.img=fc_y1ppLLR.img;
				fc_y1ppLLR.postCompressFrame(cf, false);//no aplicamos LHE logicamente y el cf da igual en postcompress
				System.out.println(" comprimida Y1pLLR...");
				fc_y1ppLLR.img.downsampled_YUV=fc_y1ppLLR.img.downsampled_LHE_YUV;
				
				//no sumo el dy pues ya lo tiene de antes, si el dy es cero el resultado no cambiara
				
				LHE.FramePlayer fp_y1ppLLR=new LHE.FramePlayer();
				fp_y1ppLLR.img=fc_y1ppLLR.img;
				fp_y1ppLLR.grid=fc_y1ppLLR.grid;
				System.out.println(" playing Y1ppLR...");
				//Block.MAX_PPP=10;
				fp_y1ppLLR.playFrame(output_directory+"/"+frames[i-1], bilineal);
				last_lowres_videoframe=fp_y1ppLLR.img;
				//fp_y1pp.img.YUVtoBMP(output_directory+"/LLR_"+frames[i],last_lowres_videoframe.interpolated_YUV[0]);
				//-----------------------------------------------------------------------------------
				 
				 */
				
				
				
				
				//fp es y1p
				//fp.img.initY3();
				//fp.img.computeY2fromDeltaY1pY1ppV2(fp2.img.interpolated_YUV,fp_y1pp.img.interpolated_YUV,last_lowres_videoframe.interpolated_YUV);
				
				
				
				
				
				//fp.img.computeY2fromDeltaY1pY1ppV2(fp2.img.interpolated_YUV,fp_y1pp.img.interpolated_YUV,last_lowres_videoframe.interpolated_YUV);
				fp.img.computeY2fromDeltaY1pY1ppV3(fp2.img.interpolated_YUV,fp_y1pp.img.interpolated_YUV,last_lowres_videoframe.interpolated_YUV);
				//fp.img.computeY2fromDeltaY1pY1ppV2(fp2.img.y2menosy1,fp_y1pp.img.interpolated_YUV,last_lowres_videoframe.interpolated_YUV);
				
				
			//	if (i==2) System.exit(0);
				//fp.img.computeY2fromY4(fp2.img.interpolated_YUV);//frame nuevo a img.YUV[0]
				//fp_y1pp.img.initY3();
				//fp_y1pp.img.computeY2fromY4(fp2.img.interpolated_YUV);//frame nuevo a img.YUV[0]
				//fp.img=fp_y1pp.img;
				
				
				
				fp.img.YUVtoBMP(output_directory+"/"+frames[i],fp.img.YUV[0]);
				
				Qmetrics.PSNRutil my_psnr=new Qmetrics.PSNRutil() ;
				LHE.FrameCompressor fc_psnr=new LHE.FrameCompressor(1);
				fc_psnr.loadFrame(directorio+"/"+frames[i]); // Esto es Y2
				float mse=(float)my_psnr.getMSE(fp.img.YUV[0],fc_psnr.img.YUV[0],0,fp.img.width-1,0,fp.img.height-1,fp.img.width);
				double psnr_mse=my_psnr.getPSNR(mse);
				
				double this_psnr=psnr_mse;//Qmetrics.PSNR.printPSNR(output_directory+"/fotograma.bmp",output_directory+"/"+frames[i]);
				//Qmetrics.PSNR.printPSNR(directorio+"/"+frames[i],output_directory+"/"+frames[i]);
				total_psnr+=this_psnr;
				//Qmetrics.PSNR.printPSNR(output_directory+"/fotograma.bmp",output_directory+"/"+frames[i]);
				System.out.println("this PSNR:"+this_psnr+"   total PSNR:"+total_psnr);
				
				lastvideoframe=fp.img;
				last_lowres_videoframe=fp_y1pp.img;
				//fp.img.YUVtoBMP(output_directory+"/LLR_"+frames[i],fp_y1pp.img.interpolated_YUV[0]);
				
			}
			System.out.println(" average porcent:"+ (100*tot_porcent/frames.length)+" %");
			System.out.println(" average PSNR:"+ (total_psnr/frames.length)+" dB");
		}
		//***********************************************************			
		 
//***********************************************************

public void compressVideoTesis001(float ql, String interpol_type)
		{
	
	       ///boolean bilineal=true;
	       //float cf=cfini;//cfini;//cfini;//0.92f;//1.77f;
	       
			
			// get the directory and order the file list
			//------------------------------------------
			System.out.println ("Type directory name ( must be under input_video folder):");	
			Scanner teclado = new Scanner (System.in);		
			String directorio =  teclado.next();
			teclado.close();
			directorio="./input_video/"+directorio;
			
			String output_directory="./output_video";//aqui van todos los fotogramas que se compriman
			
			//read directory
			//--------------
			File file = new File(directorio);
			if (!file.exists()) {
				System.out.println("El directorio no existe");
				System.exit(0);
			}
			//ordena la lista de fotogramas por nombre e imprime
			//--------------------------------------------------
			String [] frames = orderFileList(file.list());
			for (int i=0;i<frames.length;i++) {
				System.out.println(frames[i]);
			}
			
			//read frame 0 and compress it
			//-------------------------------
			LHE.FrameCompressor fc=new LHE.FrameCompressor(1);
			fc.DEBUG=false;
			fc.loadFrame(directorio+"/"+frames[0]);
			float[] resfc=new float[2];
			
			//resfc= fc.compressFrame(100);
			resfc= fc.compressFrame(ql+10);
			
			System.out.println(" bit rate: "+resfc[1]+" bpp");
			//play the frame
			//--------------
			LHE.FramePlayer fp=new LHE.FramePlayer();
			fp.img=fc.img;
			fp.img.initY3(); //precompute deltaY (2 linear segments + vertical segment)
			fp.grid=fc.grid;
			fp.INTERPOL=interpol_type;//NN, BILINEAL , BICUBIC
			FramePlayer.ssim_active=false;
			
			float[] resfp=fp.playFrame(directorio+"/"+frames[0]);// true=bilineal interpol
			
			// save the resulting file
			//-------------------------
			fp.img.YUVtoBMP(output_directory+"/"+frames[0],fp.img.interpolated_YUV[0]);
			
			//preparamos YUV para el bucle
			//OJO QUE SI NO PONGO ESTO NO FUNCIONA TAL COMO LO HE PROGRAMADO
			fp.img.YUV=fp.img.interpolated_YUV; //ahora en img.YUV se encuentra Y1'
			
			//set last videoframe (Y1') and auxiliar frame
			//--------------------------------------------
			ImgUtil lastvideoframe=fp.img; //Y1'
			//ImgUtil last_lowres_videoframe=fp.img;//Y1''
			
			
			
			//  bucle of frames
			//--------------------------------------------------------------------
			float tot_porcent=0; //not consider initial spatial compressed frame
			Grid grid_ant= fc.grid; //almaceno la grid del anterior fotograma
			
			//ponemos todo a quieto
			fp.img.setMinCountdown();
			
			double total_psnr=0;// para calcular la media
			double total_bpp=0;
			double total_ssim=0;
			
			total_psnr+=resfp[1];//ya tenemos el primer fotograma, sumo su psnr. tambien tengo el ssim
			total_ssim+=resfp[2];//ya tenemos el primer fotograma, sumo su ssim
			total_bpp+=resfc[1];//ya tenemos el primer fotograma, sumo su bpp
			//int ki=0;
			
			for (int i=1;i<frames.length;i++) {
				
				System.out.println(" next frame to compress:"+i);
				
				//wait for hard disc
				//------------------
				try{
				//	Thread.sleep(500);
					Thread.sleep(500);
				}catch(Exception e){}
				System.gc();//yo que se...lo mismo mejora el rendimiento de java
				

				//fc2 carga Y2
				//-------------
				LHE.FrameCompressor fc2=new LHE.FrameCompressor(1);
				fc2.loadFrame(directorio+"/"+frames[i]); // Esto es Y2
				fc2.DEBUG=false;
				fc2.MODE=new String("ELASTIC");//esto es by default. no hace falta
				
				
				//fc3 no es un compresor, solo es para cargar el frame anterior y asi generar dy=f(y1,y2,y1')
				LHE.FrameCompressor fc3=new LHE.FrameCompressor(1);
				fc3.loadFrame(directorio+"/"+frames[i-1]);//Esto es Y1, es decirl, el frame original anterior
				
				//compute dy =f (Y1',                            ,  nada,      Y1,        nada). ya tiene y2
				//fc2.img.computeY3(lastvideoframe.interpolated_YUV, fp.img.error,fc3.img.YUV, fp.img.countdown);
				
				//compute dy =f (Y1',Y1,countdown). ya tiene y2 . countdown debe ser siempre la misma
				//el restultado de compute_dy se almacena en fc.img.YUV
				//------------------------------------------------------
				fc2.img.compute_dy(lastvideoframe.interpolated_YUV, fc3.img.YUV, fp.img.countdown);
				
				//salvar dy solo en experimentos:
				//fc2.img.YUVtoBMP(output_directory+"/dy_"+frames[i],fc2.img.YUV[0]);
				
				//ahora tenemos dy en fc2.img.YUV
				//vamos a comprimir la dy y a calcular sus valores PR de su grid
				//----------------------------------------------------------------
				//System.out.println("pre-compressing...");
				//los valores historicos de PR se van almacenando en grid_ant
				//uso todo el rato el mismo objeto grid, no almacena datos de PR anteriores ¿porque? no me acuerdo!!
				fc2.grid=grid_ant;//con esto ya garantizo que todo se guarde en el mismo objeto grid y por tanto en los mismos Prbl que tienen la historia
				//fc2.preCompressFrame(ql,grid_ant);//calcula metrics y ppp
				//fc2.preCompressFrame(ql,null);//calcula metrics y ppp
				
				//System.out.println("post-compressing...");
				
				System.out.println("compressing dy...");
				//fc2.postCompressFrame(true);//down y LHE. con false se desactiva lhe
				resfc=new float[2];
				
				float ql2=ql;
				//ql2=ql+ki;
				//ki=ki+1;if (ki==10) ki=0;
				//if (i%10==0) ql2=ql+10;// un miniajuste para mejorar levemente el psnr en zonas quietas
				///if (i%2==0) ql2=ql+2;// un miniajuste para mejorar levemente el psnr en zonas quietas
				resfc = fc2.compressFrame(ql2);
				
				total_bpp+=resfc[1];
				/*
				float bpf=600000f/(25f*(fc2.img.width*fc2.img.height));
				if (resfc[1]<bpf) ql=ql+2;
				if (resfc[1]>bpf) ql=ql-2f;
				if (ql<0)ql=0;
				if (ql>50) ql=50;
				*/
				System.out.println(" bit rate: "+resfc[1]+" bpp");
				//puedo sustituir por compress frame...? NO porque el precompress almacena la historia de pR
				//aunque como ya no necesito la historia de PR podria perfectamente
				
				
				System.out.println("  Y3 ha sido comprimida ( es decir dy)");
				
				//estadisticas de consumo espacial
				//----------------------------------------
				float porcent=fc2.img.getNumberOfNonZeroPixelsDown();
				System.out.println(" porcent:"+porcent);
				tot_porcent+=porcent;
				
				//fc2.img.YUVtoBMP(output_directory+"/diff_down_play_"+frames[i],fc2.img.downsampled_LHE_YUV[0]);
				
				
				
			
				
				//vamos a generar una imagen (Y1'd) comprimida e interpolada (y1'')segun la grid del deltaY
				//---------------------------------------------------------------------------------
				System.out.println(" creando Y1pp...");
				LHE.FrameCompressor fc_y1pp=new LHE.FrameCompressor(1);
				fc_y1pp.MODE=new String("ELASTIC"); //es by default
				
				
				fc_y1pp.img=new ImgUtil(lastvideoframe);//constructor de copia
				
				fc_y1pp.grid=fc2.grid;//grid de la deltay. para comprimir segun sus valores de pr
				//PRblock.img=fc_y1pp.img; se hace ya en post
				
				
				
				
				fc_y1pp.postCompressFrame(false);//no aplicamos LHE logicamente y el cf da igual en postcompress
				System.out.println(" comprimida Y1p...");
				//como no he usado LHE en esto debeo igualar pues el resultado esta en LHE_YUV
				// en realidad no hace falta. ya se hace dentro de post
				//fc_y1pp.img.downsampled_YUV=fc_y1pp.img.downsampled_LHE_YUV;
				
				
				
				//fc_y1pp.img.YUVtoBMP(output_directory+"/Y1P_down_"+frames[i],fc_y1pp.img.downsampled_YUV[0]);
				
				//sumamos la delta. ambos tienen la misma resolucion NOOOOOO
				//--------------------------------------------------
				
				//primero regenero la delta
				LHE.FramePlayer fp2=new LHE.FramePlayer();
				{fp2.img=fc2.img;fp2.grid=fc2.grid;} //he cargado el dy
				fp2.DEBUG=false;
				fp2.img.initY3();// Y2''= f(dy,y1'')
				Block.img=fp2.img;
				
				
				//esta es la funcion que trasforma dy en Y2'd. es posible directamente, ya que y2'd=f (y1'd,dy) 
				//-------------------------------------------------------------------------------------------
				//pero ojo porque y2'' no es y2'
				fp2.img.regeneraDownDelta(fc_y1pp.img.downsampled_YUV);//resultado en downsampled_LHE_YUV
				//---------------------------------------------------------------------------------
				//explicacion:
				//el objeto img de fp2 es el mismo que el de fc2, por lo tanto 
				//contiene en downsampled_LHE_YUV a dy downsampled y cuantizado con LHE
				//la funcion regeneraDownDelta() lo que hace es coger ese dy y transformaro en Y2'd
				//--------------------------------------------------------------------------------
				//fp2.img.YUVtoBMP(output_directory+"/Y1ppDydown"+frames[i],fp2.img.downsampled_LHE_YUV[0]);
				
				
				//System.out.println("  saving delta...");
				//fp2.img.YUVtoBMP(output_directory+"/Y1ppDy1"+frames[i],fp2.img.interpolated_YUV[0]);
				System.out.println("  playing delta...");
				
				
				//esto no hace falta porque play frame usa downsampled_LHE
				//playframe usa downsampled y no downsampledLHE, por eso lo igualo<--mal
				//fp2.img.downsampled_YUV=fp2.img.downsampled_LHE_YUV;<--no hace falta
				
				fp2.DEBUG=false;
				Block.img=fp2.img;
				fp2.INTERPOL=interpol_type;//NN, BILINEAL , BICUBIC
				
				//con este play ya tenemos Y2''. esto no es Y2'
				fp2.playFrame(directorio+"/"+frames[i]);
				//ahora tenemos en fp2 interpolated a y1pp+delta, ya interpolado, es decir Y2''
				
				System.out.println("  Y2'' played !");
			
				//System.out.println("  saving delta interpolated...");
				//fp2.img.YUVtoBMP(output_directory+"/Y1ppDy"+frames[i],fp2.img.interpolated_YUV[0]);
				
				
				//fc_y1pp.img.initY3();
				//fc_y1pp.img.sumaDownDelta(fp2.img.downsampled_LHE_YUV);
				
				
				
				//interpolamos con la delta ya sumada
				//-----------------------------------
				LHE.FramePlayer fp_y1pp=new LHE.FramePlayer();
				
				fp_y1pp.img=fc_y1pp.img;
				fp_y1pp.grid=fc_y1pp.grid;
				System.out.println(" playing Y1pp...");
				Block.img=fp_y1pp.img;
				//fp_y1pp.playFrame(output_directory+"/"+frames[i-1], bilineal);
				
				
				fp_y1pp.INTERPOL=interpol_type;
				
				
				fp_y1pp.playFrame(output_directory+"/"+frames[i-1]);
				
				//fp_y1pp.img.YUVtoBMP(output_directory+"/Y1ppUP"+frames[i],fp_y1pp.img.interpolated_YUV[0]);
				//if (1==1) System.exit(0);
				//estas dos son las que se van a usar
				
				
				
				//EEEEEEEE
				//Esta sentencia me mosquea. solo se ejecuta la primera vez  HOY HE HECHO ESTOOOOOOOOOOOO
				
				//if (i<=1) last_lowres_videoframe=fp_y1pp.img; // de este modo dy saldra todo 128
				//last_lowres_videoframe=fp_y1pp.img; //no se usa
				
				
				
				//fp_y1pp.img.YUVtoBMP(output_directory+"/Y1PP_"+frames[i],fp_y1pp.img.interpolated_YUV[0]);
				//fp_y1pp.img.YUVtoBMP(output_directory+"/LLR_"+frames[i],last_lowres_videoframe.interpolated_YUV[0]);
				//ahora tengo el y1pp en fp_y1pp.img.YUV
				//...............................................................
				
				
				
				
				
				//fp.img.computeY2fromDeltaY1pY1ppV2(fp2.img.interpolated_YUV,fp_y1pp.img.interpolated_YUV,last_lowres_videoframe.interpolated_YUV);
				
				//fp.img.computeY2fromDeltaY1pY1ppV3(fp2.img.interpolated_YUV,fp_y1pp.img.interpolated_YUV,last_lowres_videoframe.interpolated_YUV);
				
				//esta funcion deja el resultado en YUV
				//-------------------------------------
				fp.img.computeY2fromDeltaY1pY1ppV3(fp2.img.interpolated_YUV,fp_y1pp.img.interpolated_YUV);//,last_lowres_videoframe.interpolated_YUV);
				//fp.img.computeY2fromDeltaY1pY1ppV3(fp2.img.interpolated_YUV,fp_y1pp.img.interpolated_YUV,fp_y1pp.img.interpolated_YUV);
				
				
				//fp.img.computeY2fromDeltaY1pY1ppV2(fp2.img.y2menosy1,fp_y1pp.img.interpolated_YUV,last_lowres_videoframe.interpolated_YUV);
				
				
			//	if (i==2) System.exit(0);
				//fp.img.computeY2fromY4(fp2.img.interpolated_YUV);//frame nuevo a img.YUV[0]
				//fp_y1pp.img.initY3();
				//fp_y1pp.img.computeY2fromY4(fp2.img.interpolated_YUV);//frame nuevo a img.YUV[0]
				//fp.img=fp_y1pp.img;
				
				
				
				fp.img.YUVtoBMP(output_directory+"/"+frames[i],fp.img.YUV[0]);
				
				//Qmetrics.PSNRutil my_psnr=new Qmetrics.PSNRutil() ;
				//LHE.FrameCompressor fc_psnr=new LHE.FrameCompressor(1);
				//fc_psnr.loadFrame(directorio+"/"+frames[i]); // Esto es Y2
				
				ImgUtil grey=new ImgUtil();
				grey.BMPtoYUV(directorio+"/"+frames[i]);
				grey.YUVtoBMP("./output_debug/orig_YUV.bmp",grey.YUV[0]);
				double psnr2=PSNR.printPSNR("./output_debug/orig_YUV.bmp", output_directory+"/"+frames[i]);
				System.out.println("PSNR2="+psnr2);
				
				/*
				float mse=(float)my_psnr.getMSE(fp.img.YUV[0],fc_psnr.img.YUV[0],0,fp.img.width-1,0,fp.img.height-1,fp.img.width);
				double psnr_mse=my_psnr.getPSNR(mse);
				double this_psnr=psnr_mse;//Qmetrics.PSNR.printPSNR(output_directory+"/fotograma.bmp",output_directory+"/"+frames[i]);
				total_psnr+=this_psnr;
				*/
				
				//Qmetrics.PSNR.printPSNR(directorio+"/"+frames[i],output_directory+"/"+frames[i]);
				total_psnr+=psnr2;
				
				
				
				//Qmetrics.PSNR.printPSNR(output_directory+"/fotograma.bmp",output_directory+"/"+frames[i]);
				//System.out.println("this PSNR:"+this_psnr+"   total PSNR:"+total_psnr);
				double  bppavg=(total_bpp/(i+1));
				
				double kbps=(int)(25*fp.img.width*fp.img.height*bppavg/1000);
				System.out.println("this PSNR:"+psnr2+"  PSNRavg:"+total_psnr/(i+1)+"     this bpp:"+resfc[1]+ "  BPPavvg:"+(total_bpp/(i+1))+"   kbps:"+kbps+"  ql"+ql);
				
				
				
				lastvideoframe=fp.img;
				
				//last_lowres_videoframe=fp_y1pp.img; //NO SE USA
				//fp.img.YUVtoBMP(output_directory+"/LLR_"+frames[i],fp_y1pp.img.interpolated_YUV[0]);
				
			}
			System.out.println(" average porcent:"+ (100*tot_porcent/frames.length)+" %");
			System.out.println(" average PSNR:"+ (total_psnr/frames.length)+" dB");
			System.out.println(" average bpp:"+ (total_bpp/frames.length)+" bpp");
			double  bppavg=(total_bpp/(frames.length));
			double kbps2=(int)((25f*(double)fp.img.width*(double)fp.img.height*bppavg)/1000f);
			double kbps1=(int)(25*fp.img.width*fp.img.height*bppavg/1000);
			System.out.println(" average kbps:"+ kbps1+" kbps");//+ " otro calculo:"+kbps2);
			
		}
		//***********************************************************			
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

public void compressVideoLHEsampled()
		{
	
	       ///boolean bilineal=true;
	       //float cf=cfini;//cfini;//cfini;//0.92f;//1.77f;
	       
			float pppx=3;//2.5f;//2f;
			float pppy=3;//2.5f;//2f;
			int orig_ancho=0;
			int orig_alto=0;
			
			// get the directory and order the file list
			//------------------------------------------
			System.out.println ("Type directory name ( must be under input_video folder):");	
			Scanner teclado = new Scanner (System.in);		
			String directorio =  teclado.next();
			teclado.close();
			directorio="./input_video/"+directorio;
			
			String output_directory="./output_videoLHEsampled";//aqui van todos los fotogramas que se compriman
			
			//read directory
			//--------------
			File file = new File(directorio);
			if (!file.exists()) {
				System.out.println("El directorio no existe");
				System.exit(0);
			}
			//ordena la lista de fotogramas por nombre e imprime
			//--------------------------------------------------
			String [] frames = orderFileList(file.list());
			for (int i=0;i<frames.length;i++) {
				System.out.println(frames[i]);
			}
			
			//read frame 0 and compress it
			//-------------------------------
			LHE.FrameCompressor fc=new LHE.FrameCompressor(1);
			fc.DEBUG=false;
			fc.loadFrame(directorio+"/"+frames[0]);
			float[] resfc=new float[2];
			
			orig_ancho=fc.img.width;
			orig_alto=fc.img.height;
			//resfc= fc.compressFrame(100);
			//resfc= fc.compressFrame(ql+10);
			
			int modediferencial=1;
			
			//resfc=fc.compressSIMPLELHESAMPLED(pppx, 1,pppy,1,true);
			resfc=fc.compressSIMPLELHESAMPLED(pppx, 1,pppy,1,false);
			
			
			System.out.println(" bit rate: "+resfc[1]+" bpp");
			
			
			//play the frame
			//--------------
			
			
			// save the resulting file
			//-------------------------
			//fp.img.YUVtoBMP(output_directory+"/"+frames[0],fp.img.interpolated_YUV[0]);
			
			fc.img.YUVtoBMP(output_directory+"/"+frames[0],fc.img.LHE_YUV[0]);
			
			//preparamos YUV para el bucle
			//OJO QUE SI NO PONGO ESTO NO FUNCIONA TAL COMO LO HE PROGRAMADO
			//fp.img.YUV=fp.img.interpolated_YUV; //ahora en img.YUV se encuentra Y1'
			
			//set last videoframe (Y1') and auxiliar frame
			//--------------------------------------------
			ImgUtil lastvideoframe=fc.img; //Y1'
			//ImgUtil last_lowres_videoframe=fp.img;//Y1''
			
			
			
			//  bucle of frames
			//--------------------------------------------------------------------
			float tot_porcent=0; //not consider initial spatial compressed frame
			//Grid grid_ant= fc.grid; //almaceno la grid del anterior fotograma
			
			//ponemos todo a quieto
			//fp.img.setMinCountdown();
			
			double total_psnr=0;// para calcular la media
			double total_bpp=0;
			//double total_ssim=0;
			
			total_psnr+=resfc[0];//ya tenemos el primer fotograma, sumo su psnr. tambien tengo el ssim
			//total_ssim+=resfp[2];//ya tenemos el primer fotograma, sumo su ssim
			total_bpp+=resfc[1];//ya tenemos el primer fotograma, sumo su bpp
			//int ki=0;
			float last_bpp=0;
			float last_bps=0;
			
			String frametype=new String("P");
			
			for (int i=1;i<frames.length;i++) {
				System.out.println(" %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
				System.out.println(" next frame to compress:"+i+ " = "+frames[i]);
				
				//wait for hard disc
				//------------------
				try{
				//	Thread.sleep(500);
					Thread.sleep(500);
				}catch(Exception e){}
				System.gc();//yo que se...lo mismo mejora el rendimiento de java
				

				//fc2 carga Y2
				//-------------
				LHE.FrameCompressor fc2=new LHE.FrameCompressor(1);//1 thread
				fc2.loadFrame(directorio+"/"+frames[i]); // Esto es Y2, el nuevo frame
				fc2.DEBUG=false;
				fc2.img.YUVtoBMP(output_directory+"/"+"_origBN.bmp",fc2.img.YUV[0]);
				
				
				//fc3 no es un compresor, solo es para cargar el frame anterior y asi generar dy
				//deberia usar un tipo ImgUtil
				LHE.FrameCompressor fc3=new LHE.FrameCompressor(1);
				fc3.loadFrame(directorio+"/"+frames[i-1]);//Esto es Y1, es decirl, el frame original anterior
				
				
				double rate=(total_bpp*25)/(1000*(i+1));
				
				//conclusion: se puede cambar el ppp pero solo cuando haya un nuevo frame I
				if (rate>1000){if (pppx<8){ pppx+=0.5;pppy+=0.5f; frametype=new String("I");}}//lastvideoframe.filtersoft(lastvideoframe.LHE_YUV[0], fc2.img.hops[0]);}}
				//if (rate>1400){if (pppx<4){ pppx+=2*pppx;pppy+=2f*pppy; frametype=new String("I");}}//lastvideoframe.filtersoft(lastvideoframe.LHE_YUV[0], fc2.img.hops[0]);}}
				else if (rate<1000){if (pppx>1){ pppx-=0.5f;pppy-=0.5f;frametype=new String("P");}}
				//else if (rate<900){if (pppx>=2){ pppx=0.5f*pppx;pppy=0.5f*pppy;frametype=new String("P");}}
				else frametype=new String("P");
				
				frametype=new String("I");
				pppx=2;
				pppy=2;
				//if (last_bpp<resfc[1])
				
				//lastvideoframe.filtersoft(lastvideoframe.LHE_YUV[0], fc2.img.hops[0]);
				
				last_bpp=resfc[1];
				
				
				//decision de subir o bajar los ppp
				//bla bla
				
				// carga de la imagen original nueva
				// down de la imagen original nueva
				// bla bla bla
				//
				//si ppp nuevo> ppp anterior then down del last frame al mismo ppp
				//si ppp nuevo< ppp anterior then scale del last frame al mismo ppp
				
				//en este punto tenemos nuevo frame y frame anterior a la misa resolucion
				
				//calculamos dif
				
				//comprimimos dif
				
				// sumamos a frame anterior
				
				//escalamos el resultado y lo metemos en algun sitio, manteniendo el frame anterior a su resolucion
				
				
				//pasamos el filtro exp
				
				
				
				fc3.img.YUVtoBMP(output_directory+"/"+"_soft.bmp",lastvideoframe.LHE_YUV[0]);
				if ( frametype.equals("P"))
				{
				fc3.img.computedif(fc3.img.YUV[0],lastvideoframe.LHE_YUV[0]);
				//fc2.img.YUV[0]=fc3.img.dif;
				fc2.img.copy(fc3.img.dif,fc2.img.YUV[0]);
				}
				else 
				{
				fc2.img.copy(fc3.img.YUV[0],fc2.img.YUV[0]);
				//fc2.img.YUV[0]=fc3.img.YUV[0];
				}
				//la diferencia es dif=(A-B)/2+128
				
				//en fc2 ahora lo que tengo es lo que quiero comprimir
				//fc2.img.YUV[0]=fc3.img.dif;
				
				System.out.println("compressing dy...");
				resfc=new float[2];
				
				
				
				resfc=fc2.compressSIMPLELHESAMPLED(pppx, 1,pppy,1, false);//false= no filterepx
				//fc2.img.hops tiene los hops
				total_bpp+=resfc[1];
				
				
				
				System.out.println ("-----------------rate:"+rate+"   ppp:"+pppx);
				
				System.out.println(" bpp (bitsperpixel) : "+(resfc[1]/(orig_ancho*orig_alto))+" bpp");
				System.out.println(" bit rate medio: "+(total_bpp*25)/(1000*(i+1))+" kbps");
				//System.out.println("  dY ha sido comprimida ");
				
				//ahora debemos sumar la imagen anterior a lo calculado
				//guardo diferencial
				//fc2.img.YUVtoBMP(output_directory+"/d_"+frames[i],fc2.img.LHE_YUV[0]);
				
				//sumo frame anterior
				if ( frametype.equals("P"))
				{
				fc2.img.sumadif(lastvideoframe.LHE_YUV[0],fc2.img.LHE_YUV[0],fc3.img.LHE_YUV[0]);
				}
				else
				{
					
					fc2.img.copy(fc2.img.LHE_YUV[0],fc3.img.LHE_YUV[0]);
				}
				
				//fc2.img.copy( lastvideoframe.LHE_YUV[0],fc2.img.LHE_YUV[0]);
				
				//guardo imagen anterior 
				fc2.img.copy(fc3.img.LHE_YUV[0], lastvideoframe.LHE_YUV[0]);
				
				fc3.img.filterEPX(fc3.img.LHE_YUV[0],16,16);
				fc3.img.YUVtoBMP(output_directory+"/"+frames[i],fc3.img.LHE_YUV[0]);
				
				//double psnr=PSNR.printPSNR(directorio+"/"+frames[i], output_directory+"/"+frames[i]);
				//esta funcion solo esta bien si es en BN
				
				double psnr=PSNR.printPSNR(output_directory+"/_origBN.bmp", output_directory+"/"+frames[i]);
				total_psnr+=psnr;
				System.out.println(" PSNR  : "+psnr);
				System.out.println(" PSNR medio : "+(total_psnr/(i+1)));
				
				//lastvideoframe=fc2.img;
				
				//last_lowres_videoframe=fp_y1pp.img; //NO SE USA
				//fp.img.YUVtoBMP(output_directory+"/LLR_"+frames[i],fp_y1pp.img.interpolated_YUV[0]);
				
			}
			
			//System.out.println(" average porcent:"+ (100*tot_porcent/frames.length)+" %");
			System.out.println(" average PSNR:"+ (total_psnr/frames.length)+" dB");
			System.out.println(" average bpp:"+ (total_bpp/(orig_ancho*orig_alto*frames.length))+" bpp");
			float final_bpp=(float)(total_bpp/(orig_ancho*orig_alto*frames.length));
			//(total_bpp*25)/(1000*i)
			double  bppavg=(total_bpp/(frames.length));
			//double kbps2=(int)((25f*(double)orig_ancho*(double)orig_alto*bppavg)/1000f);
			double kbps1=(int)(total_bpp*25/(1000*frames.length));
			System.out.println(" average kbps:"+ kbps1+" kbps");//+ " otro calculo:"+kbps2);
			
		}

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

public void compressVideoLHEsampled_V002(double rate_target)
		{
	
	       ///boolean bilineal=true;
	       //float cf=cfini;//cfini;//cfini;//0.92f;//1.77f;
	       
	
	
			float pppx=1;//2.5f;//2f;
			float pppy=1;//2.5f;//2f;
			int orig_ancho=0;
			int orig_alto=0;
			
			// get the directory and order the file list
			//------------------------------------------
			System.out.println ("Type directory name ( must be under input_video folder):");	
			Scanner teclado = new Scanner (System.in);		
			String directorio =  teclado.next();
			teclado.close();
			directorio="./input_video/"+directorio;
			
			String output_directory="./output_videoLHEsampled";//aqui van todos los fotogramas que se compriman
			
			//read directory
			//--------------
			File file = new File(directorio);
			if (!file.exists()) {
				System.out.println("El directorio no existe");
				System.exit(0);
			}
			//ordena la lista de fotogramas por nombre e imprime
			//--------------------------------------------------
			String [] frames = orderFileList(file.list());
			for (int i=0;i<frames.length;i++) {
				System.out.println(frames[i]);
			}
			
			//read frame 0 and compress it
			//-------------------------------
			LHE.FrameCompressor fc=new LHE.FrameCompressor(1);
			fc.DEBUG=false;
			fc.loadFrame(directorio+"/"+frames[0]);
			float[] resfc=new float[2];
			
			orig_ancho=fc.img.width;
			orig_alto=fc.img.height;
			//resfc= fc.compressFrame(100);
			//resfc= fc.compressFrame(ql+10);
			
			int modediferencial=1;
			
			//resfc=fc.compressSIMPLELHESAMPLED(pppx, 1,pppy,1,true);
			
			
			
			//resfc=fc.compressSIMPLELHESAMPLED(pppx, 1,pppy,1,false);
			
			//empezamos sin downsampling
			resfc=fc.compressSIMPLELHE();//1, 1,1,1,false);
			
			System.out.println(" size: "+resfc[1]+" bits");
			
			
			//play the frame
			//--------------
			
			
			// save the resulting file
			//-------------------------
			//fp.img.YUVtoBMP(output_directory+"/"+frames[0],fp.img.interpolated_YUV[0]);
			
			fc.img.YUVtoBMP(output_directory+"/"+frames[0],fc.img.LHE_YUV[0]);
			
			//preparamos YUV para el bucle
			//OJO QUE SI NO PONGO ESTO NO FUNCIONA TAL COMO LO HE PROGRAMADO
			//fp.img.YUV=fp.img.interpolated_YUV; //ahora en img.YUV se encuentra Y1'
			
			//set last videoframe (Y1') and auxiliar frame
			//--------------------------------------------
			//ImgUtil lastvideoframe;//=fc.img; //Y1'
			ImgUtil lastvideoframe=new ImgUtil(fc.img);
			lastvideoframe.LHE_YUV=new int[1][orig_ancho*orig_alto];
			lastvideoframe.width=orig_ancho;
			lastvideoframe.height=orig_alto;
			lastvideoframe.copy(fc.img.LHE_YUV[0],lastvideoframe.LHE_YUV[0]);
			
			lastvideoframe.YUVtoBMP(output_directory+"/"+"_last1.bmp",lastvideoframe.LHE_YUV[0]);
			
			//ImgUtil last_lowres_videoframe=fp.img;//Y1''
			
			
			
			//  bucle of frames
			//--------------------------------------------------------------------
			float tot_porcent=0; //not consider initial spatial compressed frame
			//Grid grid_ant= fc.grid; //almaceno la grid del anterior fotograma
			
			//ponemos todo a quieto
			//fp.img.setMinCountdown();
			
			double total_psnr=0;// para calcular la media
			double total_bpp=0;
			//double total_ssim=0;
			
			total_psnr+=resfc[0];//ya tenemos el primer fotograma, sumo su psnr. tambien tengo el ssim
			//total_ssim+=resfp[2];//ya tenemos el primer fotograma, sumo su ssim
			total_bpp+=resfc[1];//ya tenemos el primer fotograma, sumo su bpp
			total_bpp=0;
			//int ki=0;
			float last_bpp=0;
			float last_bps=0;
			
			String frametype=new String("P");
			
			for (int i=1;i<frames.length;i++) {
				System.out.println(" %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
				System.out.println(" next frame to compress:"+i+ " = "+frames[i]);
				
				//wait for hard disc
				//------------------
				try{
				//	Thread.sleep(500);
					Thread.sleep(500);
				}catch(Exception e){}
				System.gc();//yo que se...lo mismo mejora el rendimiento de java
				

				
				
				//fc3 no es un compresor, solo es para cargar el frame anterior y asi generar dy
				//deberia usar un tipo ImgUtil
				
				
				//LHE.FrameCompressor fc3=new LHE.FrameCompressor(1);
				//fc3.loadFrame(directorio+"/"+frames[i-1]);//Esto es Y1, es decirl, el frame original anterior
				
				
				double rate=(total_bpp*25)/(1000*(i+1));
				System.out.println(" current rate:"+rate+" bps");
				//if (i==1) rate=rate_target;
				
				//conclusion: se puede cambar el ppp pero solo cuando haya un nuevo frame I
				//if (rate>1000){if (pppx<8){ pppx+=0.5;pppy+=0.5f; frametype=new String("I");}}//lastvideoframe.filtersoft(lastvideoframe.LHE_YUV[0], fc2.img.hops[0]);}}
				//if (rate>1400){if (pppx<4){ pppx+=2*pppx;pppy+=2f*pppy; frametype=new String("I");}}//lastvideoframe.filtersoft(lastvideoframe.LHE_YUV[0], fc2.img.hops[0]);}}
				//else if (rate<1000){if (pppx>1){ pppx-=0.5f;pppy-=0.5f;frametype=new String("P");}}
				//else if (rate<900){if (pppx>=2){ pppx=0.5f*pppx;pppy=0.5f*pppy;frametype=new String("P");}}
				//else frametype=new String("P");
				
				//frametype=new String("I");
				//pppx=2;
				//pppy=2;
				//if (last_bpp<resfc[1])
				
				//lastvideoframe.filtersoft(lastvideoframe.LHE_YUV[0], fc2.img.hops[0]);
				
				//last_bpp=resfc[1];
				
				
				//decision de subir o bajar los ppp
				float pppx_prev=pppx;
				//float pppy_prev=pppy;
				
				frametype=new String("P");
				
				
				if (i>=2)
				{	
				 if (rate>(rate_target*1.1f)){if (pppx<4){ pppx+=0.1;pppy+=0.1f; }}
				 else if (rate<(rate_target*0.9f)){if (pppx>=1f){ pppx-=0.1f;pppy-=0.1f;}}
				
				
				if (pppx<1) pppx=1;
				if (pppy<1) pppy=1;
				
				pppx=2f;
				pppy=2f;
				
			    }
				
				/*
				float current_bpp=resfc[1]/(orig_ancho*orig_alto);
				
				if (current_bpp<last_bpp)
				{
					if (pppx>=1.1)
					{
						pppx=pppx-0.1f;
						//pppy=pppy-0.1f;
					}	
				}
				else 
				{
					if (pppx<4)
					{
						pppx=pppx+0.1f;
						//pppy=pppy+0.1f;
					}
					
				}
				pppy=2;
				pppx=2;
				last_bpp=current_bpp;
				*/
				
				//if (i==1) {
					//  pppx=pppx_prev;
					//  pppy=pppy_prev;
				 // }
				  
				
				
				
				// carga de la imagen original nueva
				  //fc2 carga Y2
				  //-------------
				  LHE.FrameCompressor fc2=new LHE.FrameCompressor(1);//1 thread
				  fc2.loadFrame(directorio+"/"+frames[i]); // Esto es Y2, el nuevo frame
				  fc2.DEBUG=false;
				  fc2.img.YUVtoBMP(output_directory+"/"+"_origBN.bmp",fc2.img.YUV[0]);
				
				
				
				
				// down de la imagen original nueva
				  int typex=1;//avg
				  int typey=0;//avg
				  int ancho_final=(int)(orig_ancho/pppx);
				  int alto_final=(int)(orig_alto/pppy);
				  System.out.println(" anchofinal="+ancho_final+"   altofinal="+alto_final+"   pppx="+pppx+"  pppy="+pppy);
				  if (ancho_final!=fc2.img.width){
					  fc2.img.down(ancho_final,alto_final,typex,typey, fc2.img.YUV[0]);  
				  }
				  
				  fc2.img.YUVtoBMP(output_directory+"/"+"_origBN_down.bmp",fc2.img.YUV[0]);
				    
				  
				//si ppp nuevo> ppp anterior then down del last frame al mismo ppp
				//si ppp nuevo< ppp anterior then scale del last frame al mismo ppp
				  
				  lastvideoframe.YUVtoBMP(output_directory+"/"+"_last2.bmp",lastvideoframe.LHE_YUV[0]); 
				  
				
				  
				  //lastvideoframe.filtersoft(lastvideoframe.LHE_YUV[0],lastvideoframe.LHE_YUV[0]);
				  
				 //lastvideoframe.YUVtoBMP(output_directory+"/"+"_last2.bmp",lastvideoframe.LHE_YUV[0]);
				  //pppx y pppy son iguales
				  
				  if (pppx>pppx_prev)
				  {//hay que reducir
					  
					 // int ancho_final=(int)(orig_ancho/pppx);
					 // int alto_final=(int)(orig_alto/pppy);
					  System.out.println(" downsampling last...");
					  if (ancho_final!=lastvideoframe.width)
					  {
					  lastvideoframe.YUVtoBMP(output_directory+"/"+"_last_nodown.bmp",lastvideoframe.LHE_YUV[0]);
					  lastvideoframe.down(ancho_final,alto_final,1,1, lastvideoframe.LHE_YUV[0]);
					  lastvideoframe.YUVtoBMP(output_directory+"/"+"_last_down.bmp",lastvideoframe.LHE_YUV[0]);
					  }
					  //lastvideoframe.YUVtoBMP(output_directory+"/"+"_last_down.bmp",lastvideoframe.LHE_YUV[0]);
				  }
				  else if (pppx<pppx_prev)
				  {//hay que escalar
					  System.out.println(" escaling last...");
					  //int ancho_final=(int)(orig_ancho/pppx);
					  //int alto_final=(int)(orig_alto/pppy);
					  
					  System.out.println("lastvideoframe.width="+lastvideoframe.width+" height:"+lastvideoframe.height);
					  
					  if (ancho_final!=lastvideoframe.width)
					  {
					  lastvideoframe.scale(ancho_final,alto_final,0,0, lastvideoframe.LHE_YUV[0]);
					  }
					  lastvideoframe.YUVtoBMP(output_directory+"/"+"_last_esc.bmp",lastvideoframe.LHE_YUV[0]);
				  }
				  
				  //lastvideoframe.filtersoft(lastvideoframe.LHE_YUV[0],lastvideoframe.LHE_YUV[0]);
				  
				//en este punto tenemos nuevo frame y frame anterior a la misa resolucion
				//calculamos dif
				  
				 fc2.img.computedif(fc2.img.YUV[0],lastvideoframe.LHE_YUV[0]); 
				//ahora tengo dif en fc2.img.dif
				 fc2.img.copy(fc2.img.dif,fc2.img.YUV[0]);
				 fc2.img.YUVtoBMP(output_directory+"/"+"_dif.bmp",fc2.img.YUV[0]);
				 
				 //fc2.img.filtersoft(fc2.img.YUV[0],fc2.img.YUV[0]);
				 
				 //if (2>1) System.exit(0);
				 
				//comprimimos dif
				 resfc=fc2.compressSIMPLELHE();//1, 1,1,1,false);
				 //fc2.lhe.filter_multualinfo(img.hops[0],img.LHE_YUV[0]);
				 
				 fc2.img.YUVtoBMP(output_directory+"/"+"_LHE.bmp",fc2.img.LHE_YUV[0]);
				 
				 
				 total_bpp+=resfc[1];
				 System.out.println ("-----------------rate:"+rate+"   ppp:"+pppx);
				 System.out.println(" bpp (bitsperpixel) : "+(resfc[1]/(orig_ancho*orig_alto))+" bpp");
				 System.out.println(" bit rate medio: "+(total_bpp*25)/(1000*(i+1))+" kbps");
				 
				 //last_bpp=resfc[1]/(orig_ancho*orig_alto);
				 
				// sumamos a frame anterior
				 fc2.img.sumadif(lastvideoframe.LHE_YUV[0],fc2.img.LHE_YUV[0],lastvideoframe.LHE_YUV[0]);
				 
				 
				 
				//escalamos el resultado y lo metemos en algun sitio, manteniendo el frame anterior a su resolucion
				 LHE.FrameCompressor fc3=new LHE.FrameCompressor(1);
				 fc3.img.LHE_YUV=new int[1][orig_ancho*orig_alto];
						 //new ImgUtil();//fc.img);
					//lastvideoframe.LHE_YUV=new int[1][orig_ancho*orig_alto];	 
				
				 fc3.img.width=lastvideoframe.width;
				 fc3.img.height=lastvideoframe.height;
				 lastvideoframe.copy(lastvideoframe.LHE_YUV[0], fc3.img.LHE_YUV[0]);
				 
				 fc3.img.YUVtoBMP(output_directory+"/"+"_noesc.bmp",fc3.img.LHE_YUV[0]);
				 
				 //fc3.img.width=lastvideoframe.width;
				 //fc3.img.height=lastvideoframe.height;
				 
				 fc3.img.scale(orig_ancho,orig_alto,0,0,fc3.img.LHE_YUV[0]);
				 
				//pasamos el filtro exp
				 if (pppx>1.5f)	fc3.img.filterEPX(fc3.img.LHE_YUV[0],16,16);
				 
				//salvamos el frame resultante 
				 fc3.img.YUVtoBMP(output_directory+"/"+frames[i],fc3.img.LHE_YUV[0]);
				 //if (i==1) System.exit(0);
				 
				 double psnr=PSNR.printPSNR(output_directory+"/_origBN.bmp", output_directory+"/"+frames[i]);
				 total_psnr+=psnr;
				 System.out.println(" PSNR  : "+psnr);
				 System.out.println(" PSNR medio : "+(total_psnr/(i+1)));
						 
				 
				//if (i==3) break;
			}
			
			//System.out.println(" average porcent:"+ (100*tot_porcent/frames.length)+" %");
			System.out.println(" average PSNR:"+ (total_psnr/frames.length)+" dB");
			System.out.println(" average bpp:"+ (total_bpp/(orig_ancho*orig_alto*frames.length))+" bpp");
			//float final_bpp=(float)(total_bpp/(orig_ancho*orig_alto*frames.length));
			//(total_bpp*25)/(1000*i)
			//double  bppavg=(total_bpp/(frames.length));
			//double kbps2=(int)((25f*(double)orig_ancho*(double)orig_alto*bppavg)/1000f);
			double kbps1=(int)(total_bpp*25/(1000*frames.length));
			System.out.println(" average kbps:"+ kbps1+" kbps");//+ " otro calculo:"+kbps2);
			
		}


}

