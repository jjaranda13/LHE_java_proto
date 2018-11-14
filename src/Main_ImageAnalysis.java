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

public class Main_ImageAnalysis {
	//static Scanner keyb = new Scanner (System.in);		
	
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public static void main(String [ ] args)
	{
		
		
		
		
		
		System.out.println("Menu:");
		System.out.println ("0) compress an image using Basic-LHE and apply edge detection");
		System.out.println ("1) compress an image using Basic-LHE and apply edge enhancement");
		System.out.println ("2) compress an image using Basic-LHE and apply noise reduction");
		System.out.println ("3) compress an image using Basic-LHE and apply blur");
		System.out.println ("4) compress an image using Basic-LHE and apply high pass");
		System.out.println ("5) compress an image using Basic-LHE and apply contrast threshold");
		System.out.println ("6) compress an image using Basic-LHE and apply contrast enhancement");
	
		
		System.out.println ("10) PR analysis");
		System.out.println ("11) object identification");
		System.out.println ("12) object tracking");
		
		String option =  readKeyboard();
		System.out.println ("your option is : "+option);
		
		Main_ImageAnalysis m=new Main_ImageAnalysis();
		if (option.equals("0") || option.equals("")) m.edge_detection();
		
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
		
		 //%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		public void edge_detection()
		{
			System.out.println("you have enter into edge detection");
			System.out.println ("Enter filename [default=./img/lena.bmp]:");
			String filename =  readKeyboard();
			if (filename.equals("")) filename=new String("./img/lena.bmp");
			System.out.println ("your filename is : "+filename);
			
			FrameCompressor fc=new LHE.FrameCompressor(1);
			fc.DEBUG=true; //en modo BASIC el debug se pone a true siempre, asi vemos los hops en un fichero
			fc.loadFrame(filename);	
			System.out.println(" width:"+fc.img.width);
			System.out.println(" height:"+fc.img.height);
			fc.compressBasicFrame("1",filename);
			
			fc.img.CDA_edgeDetection_v001("./output_CDA/egdes.bmp",2,true);
			//fc.img.CDA_blur_v001("./output_CDA/blur.bmp",2);
			fc.img.CDA_blur_v002("./output_CDA/blur.bmp",2);
			//fc.img.CDA_sharpen_v001("./output_CDA/sharpen.bmp",2);
			fc.img.CDA_sharpen_v002("./output_CDA/sharpen.bmp",1);
			fc.img.CDA_identity_v001("./output_CDA/identity.bmp");
			fc.img.CDA_denoise_v001("./output_CDA/denoise.bmp",4);
			fc.img.CDA_beauty_v001("./output_CDA/clean.bmp",2);
			fc.img.CDA_emboss_v001("./output_CDA/emboss.bmp",0);
			//fc.img.CDA_emboss_v002("./output_CDA/emboss2.bmp",1);
			fc.img.interpol_edgedetection("./output_CDA/interpol.bmp");
			
			fc.loadFrame(filename);//esto crea la grid
			fc.compressFrame(25);
			fc.img.CDVA_PRimage_v001(fc.grid,"./output_CDA/PR.bmp");
			fc.img.CDVA_PRdif_image_v001("./output_CDA/PR.bmp","./output_CDA/PR_ref.bmp","./output_CDA/PRres.bmp");
						
						
		}

		
		
			
}//end class