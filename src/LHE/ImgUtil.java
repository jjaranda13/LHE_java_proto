
package LHE;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import java.io.*;

/**
 * 
 * @author Jose Javier Garcia Aranda
 * 2014 
 * 
 */
public class ImgUtil {

	int [][] SOLY2;

	/**
	 * this class is the "bucket" of image information 
	 *    stores:
	 *     the YUV original image,
	 *     the intermediate_downsampled version of the image (in the non-preferred dimension)
	 *     the downsampled version of the image
	 *     the intermediate interpolated version of the image
	 *     the interpolated image
	 * 
	 * 
	 * this class comprises some image attributes, and basic archiving functionality
	 *   
	 * image loader ( from BMP to YUV) and saver (from YUV to BMP files)
	 * image components YUV, RGB
	 * color mode 444,420, 422, 400
	 * image grid
	 * image LHE hops
	 * downsampled image components based on grid and perceptual relevance coeficients
	 * LHE hops from downsampled image components
	 * LHE statistics for huffman purposes.
	 * interpolated image components 
	 */
	//image attributes
	public int width;
	public int height;	

	// components of image to load or save. not pure YUV but YCbCr
	// there is no unsigned bytes in Java. For simplicity we will use int
	// Each int value will consume 4 bytes (instead 1). Not optimal

	// YUV[0] is Y[], YUV[1] is U[], YUV[2] is V[]
	// black&white image has U=128, V=128 for all pixels
	public int[][] YUV; 


	public int[][] LC; 
	public int[][] lc_cache_colorLC;

	//image after load or before save
	public BufferedImage img=null;

	//image grid
	//public Grid grid;

	//image encoded unsing LHE hops.8+1 possible hops
	public int[][] hops;// hops[0] is the hops of luminance, hops[1] and hops[2] are chrominance


	public int[][] LHE_YUV;// resulting luminance and chrominance components after 1st LHE encoding process

	public int[][] intermediate_downsampled_YUV; //h downsampled
	public int[][] downsampled_YUV;//h+v downsampled
	public int[][] downsampled_LHE_YUV;// resulting luminance and chrominance components after 2nd LHE encoding process


	//downsampled image encoded using LHE hops
	public int[][] downsampled_hops;


	//interpolated image components
	public int [][] intermediate_interpolated_YUV; //vertical interpolation
	public int [][] interpolated_YUV;//v+h interpolation

	//mask before inter-block interpolation
	//public int [] mask;
	public int [] countdown;


	//boundaries for LHE encoding
	public int [][] boundaries_YUV;
	public int [][] boundaries_inter_YUV;
	public int [][] boundaries_inter2_YUV;
	
	//boundaries for bicubic interpol of gaps. Bilinear not need these special boundaries
	public int [][] boundaries_ini_interH_YUV;
	public int [][] boundaries_ini_interV_YUV;
	
	
	
	//nuevos boundaries que reemplazan los anteriores
	public int [][] frontierInterH_YUV; // interpolacion de vecino H sobre las 2 ultimas lineasH
	public int [][] frontierInterV_YUV; // es interpolacion lineal o bilineal V sobre 2 ultimas lineasV
	
	public int [][] frontierDownH_YUV; // interpolacion de vecino H sobre las 2 ultimas lineasH
	public int [][] frontierDownV_YUV; // es interpolacion lineal o bilineal V sobre 2 ultimas lineasV
	
	
	//experimental
	public int [][] y2menosy1;
	public int [][] error;
	
	
	//label, experimental for superresolution
	public float[][] label_YUV;// label[0] is the label of luminance, label[1] and label[2] are chrominance
	
	
	//experimental for LHE2
	public int [] LHE2_removed_pix;
	
	
	//color mode 0:444 , 1:422 2:420 3:400
	//color_mode=3 is black&white
	public int color_mode=0;
	
	
	
	//funcionalidad de downsampling
	int width_down=0;
	int height_down=0;
	
	//funcionalidad de scale
	int width_scaled=0;
	int height_scaled=0;
	
	//funcionalidad de diferencia entre 2 frames 
	int[] dif;
		
	
	//experimental nuevo color model
	public int[][] YCD;// 
	int[] cs;//cache sumas
	//******************************************************************************
	public ImgUtil()
	{}
//copy constructor
	//******************************************************************************
public ImgUtil(ImgUtil orig)
{
width=orig.width;
height=orig.height;

	
//y2menosy1=new int[3][width*height];
//error=new int[width*height][2];

	YUV=new int[3][width*height];
	LHE_YUV=new int[3][width*height];
	hops=new int[3][width*height];
	
	//labels . experimental
	label_YUV=new float[3][width*height];
	
	downsampled_hops=new int[3][width*height];	
	
	intermediate_downsampled_YUV=new int[3][width*height];//not strictly needed. we can colapse it on downsampledYUV
	downsampled_YUV=new int[3][width*height];
	
	//for encoder & decoder
	downsampled_LHE_YUV=new int[3][width*height];
	boundaries_YUV=new int[3][width*height];
	boundaries_inter_YUV=new int[3][width*height];
	boundaries_inter2_YUV=new int[3][width*height];

	//for decoder bicubic
			boundaries_ini_interH_YUV=new int[3][width*height];
			boundaries_ini_interV_YUV=new int[3][width*height];
			//nuevas 18/02/2015
			frontierInterH_YUV=new int[3][width*height];
			frontierDownH_YUV=new int[3][width*height];
			frontierInterV_YUV=new int[3][width*height];
			frontierDownV_YUV=new int[3][width*height];
			
			
			//boundaries_ini_inter_YUV=new int[3][width*height];
	
	//for decoder:
	intermediate_interpolated_YUV=new int[3][width*height];
	interpolated_YUV=new int[3][width*height];

	//mask=new int[width*height];
	countdown=new int[width*height];
	
	
	//removed_pix experimental for LHE2
	LHE2_removed_pix=new int[width*height];
	
	//now copy YUV and interpolated
	int pixels=height*width;
	for (int component=0;component<3;component++)
	{
	for (int i=0 ;i<pixels;i++)  {
			
			YUV[component][i]=orig.YUV[component][i];//orig.YUV[component][i];
			//downsampled_YUV[component][i]=orig.downsampled_YUV[component][i];
			//downsampled_LHE_YUV[component][i]=orig.downsampled_LHE_YUV[component][i];
			//interpolated_YUV[component][i]=interpolated_YUV[component][i];//orig.interpolated_YUV[component][i];
			//LHE_YUV[component][i]=orig.LHE_YUV[component][i];
			//if (YUV[component][i]==0)YUV[component][i]=1;
			//if (LHE_YUV[component][i]==0)LHE_YUV[component][i]=1;
			
			//fronteras
			boundaries_ini_interH_YUV[component][i]=orig.boundaries_ini_interH_YUV[component][i];
			boundaries_ini_interV_YUV[component][i]=orig.boundaries_ini_interV_YUV[component][i];
			
			frontierInterH_YUV[component][i]=orig.frontierInterH_YUV[component][i];
			frontierDownH_YUV[component][i]=orig.frontierDownH_YUV[component][i];
			frontierInterV_YUV[component][i]=orig.frontierInterV_YUV[component][i];
			frontierDownV_YUV[component][i]=orig.frontierDownV_YUV[component][i];
			
			
			//labels. experimental
			label_YUV[component][i]=orig.label_YUV[component][i];
	}//i
	}//component
	
	
	
}
	//******************************************************************************
	public void BMPtoYUV(String pathImagen)
	{
		System.out.println("loading: "+pathImagen);
		loadImageToBufferedImage(pathImagen);
		imgToInt();

		//for (int i=0;i<10;i++) System.out.println("Y[i]:"+Y[i]+" , U[i]:"+U[i]+" ,V[i]"+V[i] );

	}
	//*******************************************************************************
	public void YUVto3BMP(String pathImagen,int[][] YUV)
	{
		/**
		 * save images in BMP format, from YUV to 3 BMP images. one BMP for Y, one for U and one for V
		 */


		BufferedImage buff_Y=intToImg(YUV[0]);
		saveBufferedImage(pathImagen+"-Y.BMP", buff_Y);
		BufferedImage buff_U=intToImg(YUV[1]);
		saveBufferedImage(pathImagen+"-U.BMP", buff_U);
		BufferedImage buff_V=intToImg(YUV[2]);
		saveBufferedImage(pathImagen+"-V.BMP", buff_V);


	}
	//*******************************************************************************
	public void YUVtoBMP(String pathImagen, int[] component)
	{
		//save image component Only one YUV component) in BMP format

		BufferedImage buff_c=intToImg(component);
		saveBufferedImage(pathImagen, buff_c);

	}
	//*******************************************************************************
	public void  get512YUV()
	{

		for (int y=0;y<width;y+=2)
			for (int x=0;x<height;x+=2)
			{
				YUV[0][(y*width)/2+x/2]=YUV[0][(y*width)+x];
			}
	}
	//*******************************************************************************
	public void  getQuarterImgYUV()
		{

			for (int y=0;y<width;y+=2)
				for (int x=0;x<height;x+=2)
				{
					YUV[0][(y*width)/2+x/2]=YUV[0][(y*width)+x];
				}
		}
	//*******************************************************************************
	public void down(int width_final, int height_final, int modex,int modey, int[] pixels)
	{
		//ratio is denominator
		//1:1 = 1ppp
		//1:4 = 4ppp
		
		//primero hay que hacer down en x y luego down en Y
		//estas funciones ya cambian el tama�o
		downx(width_final,modex,pixels);
		downy(height_final,modey,pixels);
		
		
		
		
	}	
	//*******************************************************************************
	public void downx(int width_final,int mode, int[] pixels)
	{
		//mode=0 NN
		//mode=1 AVG
		
		int[] tmp=new int[width_final*height];
		
		//width_down=(int)(width/ratio);
		width_down=width_final;
		float ratio=(float)(width)/(float)(width_final);
		System.out.println(" ratio x="+ratio+ " widthdown="+width_down);
		//mode NN
		if (mode==0)
		{
		for (int y=0;y<height;y+=1)
			for (int x=0;x<width_down;x+=1)
			{
				//pixels[(y*width_down)+(int)(x)]=pixels[(y*width)+(int)(x*ratio)];
				tmp[(y*width_down)+x]=pixels[(y*width)+(int)(x*ratio)];
			}
		}//end mode NN
		//---------------------------------------
		else if (mode==1)
		{
		//float porcenti=0;
		//float porcentf=0;
		for (int y=0;y<height;y+=1)
		{
			float porcenti=0;
			float porcentf=0;
			float xinif=0;//(float)(x*ratio);
			float xfinf=0;
			for (int x=0;x<width_down;x+=1)
			{
				// forma sencilla
				/*
				int xini=(y*width)+(int)(x*ratio);
				int xfin=(y*width)+(int)((x+1)*ratio);
				int color=0;
				for (int i=xini;i<xfin;i++)	color+=pixels[i];
				//pixels[(y*width_down)+x]=color/(xfin-xini);
				tmp[(y*width_down)+x]=color/(xfin-xini);
				*/
			
				
				
				//System.out.println("hola");
				//  forma mas correcta 
				//----------------------
				xinif=xfinf;
				int xini=(int)(xinif);
				porcenti=1f-porcentf;
				xfinf=xinif+ratio;
				//if (xfinf>width-1) xfinf=width-1;
				
				if (xfinf>=width) xfinf=width-0.01f;
				if (x==width_down-1) {xfinf=width-0.01f;}
				
				//if (xfinf>=width) xfinf=width-0.001f;
				int xfin=(int)(xfinf);
				porcentf=xfinf-(int)xfin;
				float color=pixels[y*width+xini]*porcenti;
				for (int i=xini+1;i<xfin;i++) color+=pixels[y*width+i];
				color+=pixels[y*width+xfin]*porcentf;
				//pixels[(y*width_down)+x]=(int)(0.5f+color/(xfinf-xinif));
				tmp[(y*width_down)+x]=(int)(0.5f+color/(xfinf-xinif));
				
				//correccion?
				//if (xfinf==width-1) tmp[(y*width_down)+width-1]=pixels[y*width+xfin];
				
			}
		}
		}//end mode AVG
		
		
		for (int x=0;x<width_down;x++)
			for (int y=0;y<height;y++)
				pixels[y*width_down+x]=tmp[y*width_down+x];
		
		width=width_down;
	}
	//*******************************************************************************
	public void downy(int height_final ,  int mode, int[] pixels)
	{
		
		
		int[] tmp=new int[width*height_final];
		//mode=0 NN
		//mode=1 AVG
		height_down=height_final;
		float ratio=(float)(height)/(float)(height_final);
		System.out.println(" ratio y="+ratio+ " heightdown="+height_down);
		//mode NN
		if (mode==0)
		{
			
			for (int x=0;x<width;x+=1) 
				for (int y=0;y<height_down;y+=1)	
				
			      {
				  	
			      //pixels[(y*width)+x]=pixels[(int)(y*ratio) *width+x];
			      tmp[(y*width)+x]=pixels[(int)(0.5f+y*ratio) *width+x];
			      }
		}
		else if (mode==1)
		{
			
			for (int x=0;x<width;x+=1) 
			{
				float porcenti=0;
				float porcentf=0;
				float yinif=0;
				float yfinf=0;
				for (int y=0;y<height_down;y+=1)	
				{
					
					//forma sencilla
					//---------------
					/*
					int pixi=(int)(y*ratio) *width+x;
					int pixf=(int)((y+1)*ratio) *width+x;
					int color=0;
					for (int i=pixi;i<pixf;i+=width)
					color+=pixels[i];
					
					pixels[(y*width)+x]=color*width/(pixf-pixi);
					*/
					
					//forma mas correcta
					//------------------
					//float yinif=(float)(y*ratio);
					yinif=yfinf;//(float)(y*ratio);
					int yini=(int) (yinif);
					porcenti=1f-porcentf;
					//float yfinf=yinif+ratio;
					yfinf=yinif+ratio;
					//if (yfinf>height-1) {yfinf=height-1;}
					//if (yfinf>=height) {yfinf=height-0.01f;}
					
					if (y==height_down-1) {yfinf=height-0.01f;}
					
					
					//if (yfinf>height-1) yfinf=height-1;
					int yfin=(int)(yfinf);
					porcentf=yfinf-(int)yfin;
					float color=pixels[yini*width+x]*porcenti;
					for (int i=yini+1;i<yfin;i++)	color+=pixels[i*width+x];
					color+=pixels[yfin*width+x]*porcentf;
					//pixels[(y*width_down)+x]=(int)(0.5f+color/(yfinf-yinif));
					tmp[(y*width_down)+x]=(int)(0.5f+color/(yfinf-yinif));
					
					//if (tmp[(y*width_down)+x]<0) tmp[(y*width_down)+x]=0;
					//if (tmp[(y*width_down)+x]>255) tmp[(y*width_down)+x]=255;
					
					//correccion?
					//if (yfinf==height-1) tmp[(y*width_down)+width-1]=pixels[y*width+xfin];
					
				}
			
			
					}
		}//end mode 1
		
		for (int x=0;x<width;x++)
			for (int y=0;y<height_down;y++)
				pixels[y*width+x]=tmp[y*width+x];
		
		
		height=height_down;
	}
	//*******************************************************************************
	//*******************************************************************************
	public void scale(int width_final, int height_final, int modex,int modey,int[] pixels)
		{
			//ratio is denominator
			//1:1 = 1ppp
			//1:4 = 4ppp
			//width_scaled=(int)(width*ratiox+0.5f);
			//height_scaled=(int)(height*ratioy);
			
			
			//primero hay que hacer scale en y y luego down en x
			scaley(height_final,modey, pixels);
			scalex(width_final,modex,pixels);
			//scaley(ratioy,mode);
			//cambiamos el tamano.
			//como la imagen original es mas grande, los tamanos de los bufferes son 
			//todos grandes y no pasa nada
			
			
			
		}	
		//*******************************************************************************
	//*******************************************************************************
		public void scalex(int width_final,int mode, int[] pixels)
		{
			
			//width_scaled=(int)(width*ratio+0.5f );
			width_scaled=width_final;
			float ratio=(float)width_scaled/(float)width;
			
			int[] tmp=new int[width_final*height];
			
			System.out.println("scaling "+width+" to "+width_scaled);
			//mode NN
			if (mode==0)
			{
			//for (int y=height-1;y>=0;y--)
			for (int y=0;y <height;y++)	
				//
				//for (int x=width_scaled-1;x>=0;x--)
				for (int x=0;x<width_scaled;x++)
				{
					//pixels[(y*width_scaled)+x]=pixels[(y*width)+(int)(x/ratio)];
					//tmp[(y*width_scaled)+x]=pixels[(y*width)+(int)(x/ratio )];
					tmp[(y*width_scaled)+x]=pixels[(y*width)+(int)(0.0f+x/ratio )];
				}
			}//end mode NN
			else if (mode==1)//bilineal
			{
				//for (int y=height-1;y>=0;y--)
				for (int y=0;y<height;y++)	
					//for (int x=0;x<width_scaled;x++)
					for (int x=0;x<width;x++)
					{
						int pixi=(y*width)+x;
						int pixf=pixi;
						
						//if (x<width) pixf=(y*width)+x+1;//reparado
						
						if (x<width-1) pixf=(y*width)+x+1;//reparado
						
						int colorini=pixels[pixi];
						int colorfin=pixels[pixf];
						float alfa=(colorfin-colorini)/(ratio+1);
						int k=0;
						
						int pixis=(int)((x)*ratio+0.0f);
						//int pixis=(int)((x)*ratio);
						
						int pixfs=(int)((x+1)*ratio+0.0f);
						//int pixfs=(int)((x+1)*ratio);
						
						if (pixfs>width_scaled) pixfs=width_scaled;
						for (int i=pixis;i<pixfs;i++)
						{
						//pixels[y*width_scaled+i]=colorini+(int)(k*alfa);
						     tmp[y*width_scaled+i]=colorini+(int)(k*alfa);
						k++;
						}
					}
			}
			
		
			for (int x=0;x<width_scaled;x++)
				for (int y=0;y<height;y++)
					pixels[y*width_scaled+x]=tmp[y*width_scaled+x];
			
			
			width=width_scaled;
		}
	//*******************************************************************************
	public void scaley(int height_final,  int mode,int[] pixels)
		{
		
		//el modo bilineal es peor que el modo vecino en caso de usarse AVG en down
		//esto es debido a que el color mas parecido al real de los pixels que han sido
		//fusionados en un color es precisamente ese color y no una gradacion
		height_scaled=height_final;
		
		int[] tmp=new int[width*height_final];
		
		float ratio=(float)height_scaled/(float)height;
		
			//mode NN
			if (mode==0)
			{
			for (int x=width-1;x>=0;x--)
			 for (int y=height_scaled-1;y>=0;y--)		
				{
				 	//pixels[y*width+x]=pixels[(int)(y/ratio)*width+x];
				    tmp[y*width+x]=pixels[(int)(y/ratio)*width+x];
				}
			}
			else if (mode==1)//bilineal
			{
				//for (int x=width-1;x>=0;x--)
				for (int x=0;x<width;x++)	
				{
					//int yf=height_scaled-1;
					
					
					 //for (int y=height-1;y>=0;y--)
				     for (int y=0;y<height;y++)
				    	 
				    	 
				 	//for (int y=height_scaled-1;y>=0;y--)
						{
						 
						    //hay que multiplicar por y+1 por que el cero tambien cuenta
						    int pixi=(int)(y)*width+x;
							//int pixf=pixi;
							//if (y>0) 
							int	pixf=((y+1)*width)+x;
							
							if (y==height-1) pixf=pixi;
							
							int colorini=pixels[pixi];
							int colorfin=pixels[pixf];
							float alfa=(colorfin-colorini)/(ratio+1);
							int k=0;
							
							//int pixis=(int)((y+1)*ratio+0.5f);
							
							int pixis=(int)((y)*ratio+0.0f);
							
							//int pixis=(int)((y+1)*ratio);
							//System.out.println("y:"+y+"  -->"+pixis+" ratio:"+ratio);
							
							int pixfs=(int)((y+1)*ratio+0.0f);
							//int pixfs=(int)((y)*ratio);
							
							//if (pixfs<0) pixfs=0;
							//if (pixis>height_scaled-1) pixis=height_scaled-1;
							
							//if (pixfs>height_scaled-1) pixfs=height_scaled-1;
							if (pixfs>height_scaled) pixfs=height_scaled;
							//if (y==height-1) pixfs=height_scaled;
							//System.out.println("pixfs:"+pixfs);
							//for (int i=pixis;i>pixfs;i--)
							for (int i=pixis;i<pixfs;i++)
							  {
							  //pixels[i*width+x]=	colorini+(int)(k*alfa);
								tmp[i*width+x]=	colorini+(int)(k*alfa);
							  k++;
						  	  }
						 
						
						}	
				}
			}
		
		
		for (int x=0;x<width;x++)
			for (int y=0;y<height_final;y++)
				pixels[y*width+x]=tmp[y*width+x];
			
		height=height_scaled;
		}
	//*******************************************************************************
	public void prefilterhist_000(int[] pixels)
	{
		for (int y=1; y<height-1;y++)
		{
			for (int x=1; x<width-1;x++)
			{
				//if (pixels[y*width+x]-pixels[y*width+x-1]>32)
				//pixels[y*width+x]=(int)((pixels[y*width+x]-128)*0.25f)+128;
				//pixels[y*width+x]=20*(int)(pixels[y*width+x]/20f);
				//pixels[y*width+x]=(int)((pixels[y*width+x]-128)/10f)+128;
				//pixels[y*width+x]=pixels[y*width+x]/2;
				
				//pixels[y*width+x]=(pixels[y*width+x]+pixels[y*width+x-1]+pixels[(y-1)*width+x+1])/3;
				//if (pixels[y*width+x]>255) pixels[y*width+x]=255;
				//if (pixels[y*width+x]<0) pixels[y*width+x]=0;
			}
		}
	}
	//*******************************************************************************
		public void postfilterhist_000(int[] pixels)
		{
			for (int y=1; y<height-1;y++)
			{
				for (int x=1; x<width-1;x++)
				{
					//if (pixels[y*width+x]<8)
					//pixels[y*width+x]=(int)((pixels[y*width+x]-128)*10f)+128;
					//pixels[y*width+x]=pixels[y*width+x]/10;
					//pixels[y*width+x]=pixels[y*width+x]*10;
					//pixels[y*width+x]=(int)(pixels[y*width+x]*4f);
				}
			}
		}
	//*******************************************************************************
	public void filterEPX(int[] pixels, int u1,int u2)
	{
		
		
		System.out.println("filtering EPX...");
		for (int y=1; y<height-1;y++)
		{
			for (int x=1; x<width-1;x++)
			{
				//filter1pixEPX2x(im,y,x,u); //filtra 1pixel
				//filter1pixEPX2x_002(im,y,x,u); //filtra 1pixel
				//filter1pixEPX2x_003(im,y,x,u1,u2); //filtra 1pixel
				filter1pixEPX(pixels,y,x,u1,u2); //filtra 1pixel
			}	
		}
		System.out.println("filtered !");
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	
	//*************************************************************************************
	public void filter1pixEPX(int[] im,int y,int x, int um1, int um2)
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
		
		int i=y*width+x;		
		matriz[0]=im[i-1-width];
		matriz[1]=im[i-width];
		matriz[2]=im[i+1-width];
		matriz[3]=im[i-1];
		matriz[4]=im[i];
		matriz[5]=im[i+1];
		matriz[6]=im[i-1+width];
		matriz[7]=im[i+width];
		matriz[8]=im[i+1+width];
		
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
	
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		public void  create_gradient(float step)
		{
			System.out.println("ENTRADA EN GRADIENT");
			System.out.println( "width: "+width+ "   height:"+height);
			float colorf=0;
			for (int y=0;y<height;y+=1){
				for (int x=0;x<width;x+=1)
				{
					
					YUV[0][(y*width)+x]=(int)colorf;
					colorf=colorf+step;
					if (colorf>255) colorf=colorf-255;
					if (colorf<0) colorf=255-colorf;
				}
			colorf=YUV[0][(y*width)+1];
		    //if (colorf<0) colorf=colorf+255;
			//colorf=colorf % 255;
		}
		}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	
	public void  createChess()
	{
		int color=192;
		for (int y=0;y<width;y+=1)
			for (int x=0;x<height;x+=1)
			{
				if (x/8==(float)(x)/8f) {if (color==192) color=64;else color=192;}
				YUV[0][(y*width)+x]=color;
			}

	}
	//*******************************************************************************
	private void loadImageToBufferedImage(String pathImagen) {

		img=null;

		try {	
			img = ImageIO.read(new File(pathImagen));
		} catch (IOException e) {
			System.out.println("error loading image");
			System.exit(0);
		}


	}
	//*******************************************************************************
	private boolean saveBufferedImage(String pathImagen, BufferedImage buff_img) {

		if (img != null && pathImagen != null ) {


			try {
				ImageIO.write( buff_img, "BMP", new File(pathImagen));
				return true;
			} catch (Exception e){
				System.out.println("failure creating file");// error at saving
			}
			return false;		
		} else {

			return false;
		}
	}	
	//*******************************************************************************


	private void imgToInt() {

		width=img.getWidth(); 
		height=img.getHeight(); 

		//y2menosy1=new int[3][width*height];
		error=new int[width*height][2];
		//image buffers.
		//for encoder:
		
		
		
		YUV=new int[3][width*height];
		LHE_YUV=new int[3][width*height];
		hops=new int[3][width*height];
		
		//label.experimental
		label_YUV=new float[3][width*height];
		
		//System.out.println(" estamos en imgtoint w:"+width+"  h:"+height);

		downsampled_hops=new int[3][width*height];	


		System.out.println (" imgToInt: " +(width*height)+" pixels");




		intermediate_downsampled_YUV=new int[3][width*height];//not strictly needed. we can colapse it on downsampledYUV
		downsampled_YUV=new int[3][width*height];

		//for encoder & decoder
		downsampled_LHE_YUV=new int[3][width*height];
		boundaries_YUV=new int[3][width*height];
		boundaries_inter_YUV=new int[3][width*height];
		boundaries_inter2_YUV=new int[3][width*height];

		//for decoder bicubic
		boundaries_ini_interH_YUV=new int[3][width*height];
		boundaries_ini_interV_YUV=new int[3][width*height];
		
		//nuevas 18/02/2015
		frontierInterH_YUV=new int[3][width*height];
		frontierDownH_YUV=new int[3][width*height];
		frontierInterV_YUV=new int[3][width*height];
		frontierDownV_YUV=new int[3][width*height];
		
		//for decoder:
		intermediate_interpolated_YUV=new int[3][width*height];
		interpolated_YUV=new int[3][width*height];

		//mask=new int[width*height];
		countdown=new int[width*height];

		
		//removed_pix experimental for LHE2
		LHE2_removed_pix=new int[width*height];
		
		//this bucle converts BufferedImage object ( which is "img") into YUV array (luminance and chrominance)
		int i=0;
		for (int y=0;y<height;y++)  {
			for (int x=0;x<width;x++)  {
				int c=img.getRGB(x, y);

				int red=(c & 0x00ff0000) >> 16;
			int green=(c & 0x0000ff00) >> 8;
			int blue=(c & 0x000000ff);

			//identical formulas used in JPEG . model YCbCr (not pure YUV)
			YUV[0][i]=(red*299+green*587+blue*114)/1000; //lumminance [0..255]
			YUV[1][i]=128+(-168*red - 331*green + 500*blue)/1000; //chroma U [0.255]
			YUV[2][i]=128+ (500*red - 418*green -81*blue)/1000; //chroma V [0..255]

			if (YUV[0][i]>255) YUV[0][i]=255;
			if (YUV[1][i]>255)  YUV[1][i]=255;
			if (YUV[2][i]>255)  YUV[2][i]=255;

			if (YUV[0][i]<0) YUV[0][i]=0;
			if (YUV[1][i]<0)  YUV[1][i]=0;
			if (YUV[2][i]<0)  YUV[2][i]=0;

			//discarded formulas (correct, but not identical to JPEG)
			//U[i]=128+(-147*red - 289*green + 436*blue)/1000; //chroma U [16.239]
			//V[i]=128+ (615*red - 515*green -100*blue)/1000; //chroma V [0.255]


			i++;	
			}
		}


	}	

	//*******************************************************************************
	//******************************************************************************
			public void lc_LCtoBMP(String pathImagen)
			{
				
				BufferedImage buff_c=lc_LCtoImg();
				saveBufferedImage(pathImagen, buff_c);
				
				//for (int i=0;i<10;i++) System.out.println("Y[i]:"+Y[i]+" , U[i]:"+U[i]+" ,V[i]"+V[i] );

			}
			//******************************************************************************

	//******************************************************************************
		public void lc_BMPtoLC(String pathImagen)
		{
			System.out.println("loading: "+pathImagen);
			loadImageToBufferedImage(pathImagen);
			lc_imgToLC_test();

			//for (int i=0;i<10;i++) System.out.println("Y[i]:"+Y[i]+" , U[i]:"+U[i]+" ,V[i]"+V[i] );

		}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		public BufferedImage lc_LCtoImg()
		{
			img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
			for (int y=0;y<height;y++)  {
				for (int x=0;x<width;x++)  {

					//the set of formulas must be coherent with formulas used for RGB->YUV
					int i=x+(y)*width;
					
					int luz=LC[0][i];
					int color=LC[1][i];
					int rgb=lc_cache_colorLC[luz][color];
					img.setRGB(x, y, rgb);
					

				}//x
			}//y
			return img;
		}
		
		
		//******************************************************************************
			
	private void lc_imgToLC() {

		width=img.getWidth(); 
		height=img.getHeight(); 

		
		LC=new int[2][width*height];
		
		
		//this bucle converts BufferedImage object ( which is "img") into YUV array (luminance and chrominance)
		int i=0;
		for (int y=0;y<height;y++)  {
			for (int x=0;x<width;x++)  {
				int c=img.getRGB(x, y);

				int red=(c & 0x00ff0000) >> 16;
			int green=(c & 0x0000ff00) >> 8;
			int blue=(c & 0x000000ff);

			//identical formulas used in JPEG . model YCbCr (not pure YUV)
			/*
			YUV[0][i]=(red*299+green*587+blue*114)/1000; //lumminance [0..255]
			YUV[1][i]=128+(-168*red - 331*green + 500*blue)/1000; //chroma U [0.255]
			YUV[2][i]=128+ (500*red - 418*green -81*blue)/1000; //chroma V [0..255]

			if (YUV[0][i]>255) YUV[0][i]=255;
			if (YUV[1][i]>255)  YUV[1][i]=255;
			if (YUV[2][i]>255)  YUV[2][i]=255;
			 
			if (YUV[0][i]<0) YUV[0][i]=0;
			if (YUV[1][i]<0)  YUV[1][i]=0;
			if (YUV[2][i]<0)  YUV[2][i]=0;
			*/
			
			LC[0][i]=Math.min(red,Math.min(green,blue));
			LC[1][i]=
			
			i++;	
			}
		}


	}	
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	//*******************************************************************************

		private void lc_imgToLC_test() {

			width=img.getWidth(); 
			height=img.getHeight(); 

			lc_cache_colorLC=new int[256][256];
			LC=new int[2][width*height];
			
			
			
			
			
			//this bucle converts BufferedImage object ( which is "img") into YUV array (luminance and chrominance)
			int i=0;
			
			for (int y=0;y<height;y++)  {
				
				//reseteo aqui
				int luz=y%256;
				
				
				int color=0;
				
				int red=luz;// 
				int green=luz;
				int blue=luz;
				int tope=255;
				//int step=(int)(((float)((tope-1)-luz))/6.3f);
				int step=(int)(((tope-1)-luz)/5);
				boolean pintar=true;
				int estado=0;
				System.out.println("step: "+step);
				for (int x=0;x<width;x++)  {
					
				//red=unidades, green =decenas, blue=centenas...algo asi	
				/*	
				//partimos de rojo =max, green=min, blue=min	
				if (estado==0)	
				 {
					green+=step;
					if (green>=255) {green=255; estado=1;}
					//acaba en amarillo brillante ( R+G= amarillo)
				 }
				//cuando R y G llegan al max, empezamos a reducir R y a subir B
				else if (estado==1)
				{
					red=red-step;	if (red<luz) {red=luz; estado=2;}
					//blue+=step;
					//if (blue>255) {blue=255; estado=2;}
					//if (red==luz && blue==255) estado=2;
					//System.out.println("y:"+y+"  r:"+red+" g:"+green+" b:"+blue);
				}	
				else if (estado==2)// red==luz
				//blue  se queda arriba y baja G	
				{
					//pintar=false;
					blue+=step;
					if (blue>255) {blue=255; estado=3;}
					//red-=step;	if (red<luz) {red=luz;}
					//green-=step;
					//if (green<luz) {green=luz; estado=3;}
					
					
				}
				else if (estado==3)
				//vamos apagando blue
				{//pintar=false;
				  green-=step;
			  	 if (green<luz) {green=luz; estado=4;}
				
					//blue-=step;
					//if (blue<luz) blue=luz;
				}
				else if (estado==4)
				{
					//pintar=false;
					//blue-=step;
					//if (blue<luz) {blue=luz; pintar=false;}
					red+=step;
					if (red>255) {red=255; estado=5;}
				}
				if (estado==5)
				{
					blue-=step;
					red-=step;
					if (red<luz) {red=luz; }
					if (blue<luz) {blue=luz; pintar=false;}
				}
				*/
				//luz=Math.min(red,Math.min(green,blue));
				//if (luz<0) luz=0;
				//if (luz>255) luz=255;
				
				//if (red<luz) red=luz;
				//if (green<luz) green=luz;
				//if (blue<luz) blue=luz;
				
				//if (red>255) red=255;
				//if (green>255) green=255;
				//if (blue>255) blue=255;
				
				if (color>255) pintar=false;
				//luz=Math.min(red,Math.min(green,blue));
				
				//int luz=y % 255;
				if (pintar)
				 {
				  LC[0][i]=luz;//Math.min(red,Math.min(green,blue));
				  LC[1][i]=color;
				  
				  int rgb=red*65536+green*256+blue;
				  //int rgb=red+green*256+blue*65536;
				  
				  lc_cache_colorLC[luz][color]=rgb;
				  color++;
				 }	
				else 
				{
					LC[0][i]=0;//Math.min(red,Math.min(green,blue));
					LC[1][i]=0;
					  
				}
						
				i++;
				red+=step;
				if (red>=tope) {
					red=luz;
					green+=step;
					if (green>=tope)
					{
						green=luz;
						blue+=step;
						if (blue>=tope) pintar=false;
					}
				}
				
				}
			}


		}	
		
	
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	private BufferedImage intToImg(int[] component) {

		img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
		for (int y=0;y<height;y++)  {
			for (int x=0;x<width;x++)  {

				//the set of formulas must be coherent with formulas used for RGB->YUV
				int i=x+(y)*width;
				int red=component[i];//+(1402*(V[i]-128))/1000;
				int green=component[i];//- (334*(U[i]-128)-714*(V[i]-128))/1000;
				int blue=component[i];//+(177*(U[i]-128))/1000;

				int rgb=red+green*256+blue*65536;
				img.setRGB(x, y, rgb);

			}//x
		}//y
		return img;
	}

	//*******************************************************************************
	public void saveHopsToTxt(String path_file, boolean consigno)
	{
		try{
			System.out.println("Entrando en salvaTXT");
			DataOutputStream d = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path_file)));


			//primero escribo el ancho, alto y primer color:
			d.writeBytes(width+"\n");
			d.writeBytes(height+"\n");
			d.writeBytes(YUV[0][0]+"\n");
			for (int i=0;i<width*height;i++){

				if ((i%width==0)&& (i>0)) {d.writeBytes("\n");}



				//esto salva los hops normales[0..4..8]
				//d.writeBytes(hops[0][i]+"");

				//esto los salva con signo
				if (consigno)
				{
				if (hops[0][i]==0)d.writeBytes("-4");
				else if (hops[0][i]==1)d.writeBytes("-3");
				else if (hops[0][i]==2)d.writeBytes("-2");
				else if (hops[0][i]==3)d.writeBytes("-1");
				else if (hops[0][i]==4)d.writeBytes(" 0");
				else if (hops[0][i]==5)d.writeBytes("+1");
				else if (hops[0][i]==6)d.writeBytes("+2");
				else if (hops[0][i]==7)d.writeBytes("+3");
				else if (hops[0][i]==8)d.writeBytes("+4");
				else d.writeBytes("  ");
				}
				else d.writeBytes(hops[0][i]+"");
			}

			d.close();
		}catch(Exception e){System.out.println("ERROR writing hops in txt format:"+e);}	


	}
	//**************************************
	//*******************************************************************************
		public void saveHopsToTxtUnsigned(String path_file)
		{
			try{
				System.out.println("Entrando en salvaTXT");
				DataOutputStream d = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path_file)));


				//primero escribo el ancho, alto y primer color:
				d.writeBytes(width+"\n");
				d.writeBytes(height+"\n");
				d.writeBytes(YUV[0][0]+"\n");
				for (int i=0;i<width*height;i++){

					if ((i%width==0)&& (i>0)) {d.writeBytes("\n");}



					//esto salva los hops normales[0..4..8]
					//d.writeBytes(hops[0][i]+"");

					//esto los salva con signo
					/*
					if (hops[0][i]==0)d.writeBytes("-4");
					else if (hops[0][i]==1)d.writeBytes("-3");
					else if (hops[0][i]==2)d.writeBytes("-2");
					else if (hops[0][i]==3)d.writeBytes("-1");
					else if (hops[0][i]==4)d.writeBytes(" 0");
					else if (hops[0][i]==5)d.writeBytes("+1");
					else if (hops[0][i]==6)d.writeBytes("+2");
					else if (hops[0][i]==7)d.writeBytes("+3");
					else if (hops[0][i]==8)d.writeBytes("+4");
					else d.writeBytes("  ");
					*/
				}

				d.close();
			}catch(Exception e){System.out.println("ERROR writing hops in txt format:"+e);}	


		}
		//**************************************
	public float getNumberOfNonZeroPixelsDown()
	{
		float count=0;
		for (int y=0;y<height;y++)
			for (int x=0;x<width;x++)
			{
				if (downsampled_YUV[0][y*width+x]!=0) count++;
				//	System.out.println("color:"+YUV[0][y*width+x]);
			}
		return count/(float) (width*height);
	}
	//**************************************
	public float getNumberOfNonZeroPixels()
	{
		float count=0;
		for (int y=0;y<height;y++)
			for (int x=0;x<width;x++)
			{
				if (YUV[0][y*width+x]!=0) count++;
				//	System.out.println("color:"+YUV[0][y*width+x]);
			}
		return count/(float) (width*height);
	}
	//**********************************************
	public void substractFrame(int[][] frame)
	{
		//result is (y-y'+255)/2 belongs to 0..255
		for (int component=0;component<3;component++)
		{
			for (int i=0;i<width*height;i++)
			{
				//if (Math.abs(YUV[component][i]-frame[component][i])>16)
				{
					YUV[component][i]=(YUV[component][i]-frame[component][i]+256)/2;

					/*
			YUV[component][i]=(YUV[component][i]-frame[component][i]);//-255..255
			YUV[component][i]=(int) (0.1f*YUV[component][i]);
			YUV[component][i]=YUV[component][i]+128;
					 */

					if (YUV[component][i]<0  || YUV[component][i]>256)
					{
						System.out.println("warning");
						System.exit(0);
					}
					//	YUV[component][i]=(int)((YUV[component][i]-frame[component][i]/2+128)/1.5f);
					if (YUV[component][i]>255) YUV[component][i]=255;
					//if (YUV[component][i]<=128) YUV[component][i]=128;

					if (YUV[component][i]<=0) YUV[component][i]=1;
				}
			}


		}//for component
	}
	//**********************************************
	public void addFrameDiff(int[][] frame)
	{
		//result is (y-y'+255)/2 belongs to 0..255
		for (int component=0;component<3;component++)
		{
			for (int i=0;i<width*height;i++)
			{
				//if (frame[component][i]!=128)
				{
					//YUV[component][i]=YUV[component][i]+(frame[component][i]*2-256);
					YUV[component][i]=YUV[component][i]+2*frame[component][i]-255;
					//YUV[component][i]=(4*frame[component][i]-512)/2+YUV[component][i];

					if (YUV[component][i]>255) YUV[component][i]=255;
					if (YUV[component][i]<=0) YUV[component][i]=0;
				}

			}


		}//for component
	}
	public void copyFrame(int[][] frame)
	{



		for (int component=0;component<3;component++)
		{
			for (int i=0;i<width*height;i++)
			{
				//if (frame[component][i]!=128)
				{
					//YUV[component][i]=YUV[component][i]+(frame[component][i]*2-256);
					YUV[component][i]=frame[component][i];
					//YUV[component][i]=(YUV[component][i]/10)*10;
					if (YUV[component][i]>255) YUV[component][i]=255;
					if (YUV[component][i]<=0) YUV[component][i]=1;
				}

			}


		}//for component
	}
	public void addFrame(int[][] frame)
	{
		//result is (y-y'+255)/2 belongs to 0..255
		for (int component=0;component<3;component++)
		{
			for (int i=0;i<width*height;i++)
			{
				//if (frame[component][i]!=128)
				{
					//YUV[component][i]=YUV[component][i]+(frame[component][i]*2-256);
					YUV[component][i]=(YUV[component][i]+2*frame[component][i])/3;
					//YUV[component][i]=(YUV[component][i]/10)*10;
					if (YUV[component][i]>255) YUV[component][i]=255;
					if (YUV[component][i]<=0) YUV[component][i]=0;
				}

			}


		}//for component
	}
	//**********************************************
	public void substractFrame1(int[][] frame)
	{
		//result is (y-y'+255)/2 belongs to 0..255
		for (int component=0;component<3;component++)
		{
			for (int i=0;i<width*height;i++)
			{
				//if (Math.abs(YUV[component][i]-frame[component][i])>16)
				{
					//YUV[component][i]=2*frame[component][i]-YUV[component][i];
					YUV[component][i]=-2*frame[component][i]+255+YUV[component][i];


					//	YUV[component][i]=(int)((YUV[component][i]-frame[component][i]/2+128)/1.5f);
					if (YUV[component][i]>255) YUV[component][i]=255;
					//if (YUV[component][i]<=128) YUV[component][i]=128;

					if (YUV[component][i]<=0) YUV[component][i]=0;
				}
			}


		}//for component
	}
	//**************************************************************************
	public void watermarkFilter()
	{
		//hacemos una copia

		int[][] copy=new int[3][width*height];


		for (int component=0;component<3;component++)
		{
			for (int i=0;i<width*height;i++)
			{
				//if (Math.abs(YUV[component][i]-frame[component][i])>16)
				copy[component][i]=YUV[component][i];

			}
		}
		//ahora el filtro
		for (int component=0;component<3;component++)
		{
			for (int y=0;y<height;y++)
			{
				for (int x=0;x<width;x++)
				{
					//if (Math.abs(YUV[component][i]-frame[component][i])>16)

					if (x>0 && y>1 && x<width-1 && y<height-1)
					{
						int pos=y*width+x;
						float por=0.5f;
						float por2=(1f-por)/4f;
						YUV[component][pos]=(int)(por*copy[component][pos]+
								por2*copy[component][(y-1)*width+x]+
								por2*copy[component][(y)*width+x-1]+
								por2*copy[component][(y)*width+x+1]+
								por2*copy[component][(y+1)*width+x]

								);
						if (YUV[component][pos]>255) YUV[component][pos]=255;
						if (YUV[component][pos]<=0) YUV[component][pos]=1;
					}

				}//x
			}//y
		}
	}
	//**************************************************************************
	public void watermarkFilter_V2(int[][] diff)
	{
		//hacemos una copia

		int[][] copy=new int[3][width*height];

		int component=0;
		//	for (int component=0;component<3;component++)
		{
			for (int i=0;i<width*height;i++)
			{
				//if (Math.abs(YUV[component][i]-frame[component][i])>16)
				copy[component][i]=YUV[component][i];

			}
		}
		//ahora el filtro
		//for (int component=0;component<3;component++)
		{
			for (int y=0;y<height;y++)
			{
				for (int x=0;x<width;x++)
				{
					//if (Math.abs(YUV[component][i]-frame[component][i])>16)

					if (x>0 && y>1 && x<width-1 && y<height-1)
					{
						int pos=y*width+x;

						//float peso=Math.min(1f, 30*Math.abs(diff[component][pos]-128)/128f);
						float peso=Math.abs(diff[component][pos]-128)/128f;
						peso= (float)Math.pow(peso,1/5f);
						float peso1=1f-peso;

						peso=Math.abs(diff[component][pos]-128)/128f;
						//if (peso>1) 
						// if (peso!=0)   peso=1f;//0.8f;
						//else 
						//if (peso>1)  

						//SOLUCION FINAL ADOPTADA
						peso=0.75f;
						int dif=(int)(Math.abs(diff[component][pos]-128));

						//si hay menos diferencia no hay que filtrar porque "borramos"
						//si hay mucha diferencia podemos filtrar sin borrar
						float min=1f;//filtrado minimo
						float max_filtrado=1;//-min;
						peso=1-max_filtrado*Math.abs(diff[component][pos]-128)/128f;//+ dif -> + filtra
						peso=0.1f;//peso de alrededor. un numero bajo no filtra
						//mucha dif: filtrado maximo
						//poca dif: filtrado minimo
						//para aumentar la dif y asi filtrar menos meto el 0.5 (es poco. queda mal)
						float K=1f; // K=2 tambien va bien pero K=4 se nota
						peso=(1-K*Math.abs(diff[component][pos]-128)/128f);//+ dif -> + filtra
						if (peso<0) peso=0;
						if (peso>1) peso=1;
						//ejemplo
						//dif=128->es la maxima. peso=1-0=1--> peso invertido =0--> filtrado maximo
						//dif=0->es la min. peso=1-1=0--> peso invertido =1--> filtrado minimo


						//if (dif<=132 && dif >=124) peso=0;

						//SOLUCION ANTERIOR. ES PEOR
						/* 
			    	  peso=Math.abs(diff[component][pos]-128)/128f;
					  peso= (float)Math.pow(peso,1/5f);
						 */ 	

						//el peso1 minimo va a ser 0.2 y el maximo va a ser 1
						//	peso=0.8f*Math.abs(diff[component][pos]-128)/128f;	
						//peso= (float)Math.pow(peso,1/5f);

						peso1=1f-peso;

						float peso2=(1f-peso1)/4f;//(1f-peso1)/4f;

						YUV[component][pos]=(int)(peso1*copy[component][pos]+
								peso2*copy[component][(y-1)*width+x]+
								peso2*copy[component][(y)*width+x-1]+
								peso2*copy[component][(y)*width+x+1]+
								peso2*copy[component][(y+1)*width+x]

								);
						if (YUV[component][pos]>255) YUV[component][pos]=255;
						if (YUV[component][pos]<=0) YUV[component][pos]=1;
					}

				}//x
			}//y
		}
	}

	//***********************
	public float info()
	{
		float info=0;
		for (int i=0;i<width*height;i++)
		{
			//if (Math.abs(YUV[component][i]-frame[component][i])>16)
			info+=Math.abs(YUV[0][i]-128)/128f;

		}
		info=info/(float)(width*height);
		return info;
	}
	//***************************
	public void sumadown(int[][] y3, int[][] y3b)
	{
		System.out.println(" ENTRADA EN SUMADOWN");
		//deberia sumar solo los bloques, no toda la imagen
		//for (int component=0;component<3;component++)
		int component=0;
		{
			for (int y=0;y<height;y++)
			{

				for (int x=0;x<width;x++)
				{
					int pos=y*width+x;
					int a=y3[component][pos]-128; //+128...-128
					int b=y3b[component][pos]-128;
					int res=(a-b+256)/2;//(int)(0.5f+((float)a-(float)b)/2f);

					//if (res!=y3[component][pos]) System.out.print("*");
					//else System.out.print(".");



					if (y3[component][pos]==0)res=0;//(res+128);
					downsampled_YUV[component][pos]=res;

					if (downsampled_YUV[component][pos]>255) downsampled_YUV[component][pos]=255;
					if (downsampled_YUV[component][pos]<0) downsampled_YUV[component][pos]=0;
				}
				//System.out.println(""+y+"  ");
			}
		}
	}


	//***************************
	public void media(int[][] y2a, int[][] y2b)
	{

		//deberia sumar solo los bloques, no toda la imagen
		for (int component=0;component<3;component++)
		{
			for (int y=0;y<height;y++)
			{
				for (int x=0;x<width;x++)
				{
					int pos=y*width+x;


					interpolated_YUV[component][pos]=(y2a[component][pos]+y2b[component][pos])/2; 


				}
			}
		}
	}

	public void watermarkFilter_V3(int[][] diff)
	{
		//hacemos una copia

		int[][] copy=new int[3][width*height];

		int component=0;
		//	for (int component=0;component<3;component++)
		{
			for (int i=0;i<width*height;i++)
			{
				//if (Math.abs(YUV[component][i]-frame[component][i])>16)
				copy[component][i]=YUV[component][i];

			}
		}
		//ahora el filtro
		//for (int component=0;component<3;component++)
		{
			for (int y=0;y<height;y++)
			{
				for (int x=0;x<width;x++)
				{
					//if (Math.abs(YUV[component][i]-frame[component][i])>16)

					if (x>0 && y>1 && x<width-1 && y<height-1)
					{
						int pos=y*width+x;


						//mucha dif: filtrado maximo
						//poca dif: filtrado minimo
						//para aumentar la dif y asi filtrar menos meto el 0.5 (es poco. queda mal)
						float K=1f; // K=2 tambien va bien pero K=4 se nota
						float peso=(1-K*Math.abs(diff[component][pos]-128)/128f);//+ dif -> + filtra
						if (peso<0) peso=0;
						if (peso>1) peso=1;
						//peso=0.5f;
						//ejemplo
						//dif=128->es la maxima. peso=1-0=1--> peso invertido =0--> filtrado maximo
						//dif=0->es la min. peso=1-1=0--> peso invertido =1--> filtrado minimo


						//if (dif<=132 && dif >=124) peso=0;



						float peso1=1f-peso;

						float peso2=(1f-peso1)/4f;//(1f-peso1)/4f;

						YUV[component][pos]=(int)(peso1*copy[component][pos]+
								peso2*copy[component][(y-1)*width+x]+
								peso2*copy[component][(y)*width+x-1]+
								peso2*copy[component][(y)*width+x+1]+
								peso2*copy[component][(y+1)*width+x]

								);
						if (YUV[component][pos]>255) YUV[component][pos]=255;
						if (YUV[component][pos]<=0) YUV[component][pos]=1;
					}

				}//x
			}//y
		}
	}

	//***********************
	//**********************************************
	public void computeY4(int[][] frame)
	{
		//result is (y-y'+255)/2 belongs to 0..255
		for (int component=0;component<3;component++)
		{
			for (int i=0;i<width*height;i++)
			{
				//if (Math.abs(YUV[component][i]-frame[component][i])>16)
				{
					int y1=frame[component][i];
					int y2=	YUV[component][i];
					int y3=(y2-y1+256)/2;
					int y3bis=Math.abs(y2-y1); //from 0 to 255

					//int k2=Math.max(64,y3bis);
					int k2=64;//y3bis;//64;
					int k1=(255-k2);
					float suma=k1+k2;
					//k1=192;
					//k2=64;

					//float y4=(k1*y3+k2*100)/suma;
					float y4=(k1*y3+k2*y2)/suma;
					//	y4=y3;

					//if (y4>255) y4=255;
					//if (y4<=0) y4=1;

					//nuevo
					/*
					if (y1<129)
					{
						float k1f=y1/128f;
						float k2f=(128-y1)/128f;
						y4=(int) (k1f*y3+k2f*y2);
					}
					else
					{
						float k1f=(255-y1)/128f;
						float k2f=(y1-128)/128f;
						y4=(int)(k1f*y3+k2f*y2);
					}
					 */
					YUV[component][i]=(int)y4;

					/*
					YUV[component][i]=(YUV[component][i]-frame[component][i]);//-255..255
					YUV[component][i]=(int) (0.1f*YUV[component][i]);
					YUV[component][i]=YUV[component][i]+128;
					 */

					if (YUV[component][i]<0  || YUV[component][i]>256)
					{
						System.out.println("warning");
						System.exit(0);
					}
					//	YUV[component][i]=(int)((YUV[component][i]-frame[component][i]/2+128)/1.5f);
					if (YUV[component][i]>255) YUV[component][i]=255;
					//if (YUV[component][i]<=128) YUV[component][i]=128;

					if (YUV[component][i]<=0) YUV[component][i]=0;
				}
			}


		}//for component
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public void computeY2fromY4(int[][] frame)
	{
		//initY4();
		//result is (y-y'+255)/2 belongs to 0..255
		//for (int component=0;component<3;component++)
		int component=0;
		{
			for (int i=0;i<width*height;i++)
			{
				//if (frame[component][i]!=128)
				{
					//YUV[component][i]=YUV[component][i]+(frame[component][i]*2-256);
					int y1=YUV[component][i];
					int y4=frame[component][i];

					//int y2=y1+2*y4-256;
					//if (y1>=240) y1=240;

					int y2=y1;
					//if (y4!=0) 
					y2=SOLY2[y1][y4];

					//if (y2>240) y2=240;

					if (y2<0 || y2>255) 
					{
						System.out.println("computeY2fromY4:   y2 <0 con :    y1:"+y1+"  y4:"+y4+ "   ->y2"+y2);
						System.exit(0);
					}
					//if (y1==255) System.out.println("y1:255"+ " y2:"+y2+"  y4:"+y4);

					///if (y2==210) y2=0;
					//if (y2==0 && y1!=0) System.out.println ("warning: SOLY2[y1][y4]="+SOLY2[y1][y4]+"    y1:"+y1+"   y4:"+y4);


					YUV[component][i]=y2;//YUV[component][i]+2*frame[component][i]-256;
					//YUV[component][i]=(4*frame[component][i]-512)/2+YUV[component][i];

					if (YUV[component][i]>255) YUV[component][i]=255;
					if (YUV[component][i]<=0) YUV[component][i]=1;
				}

			}


		}//for component
	}


	void initY4()
	{
		SOLY2=new int[256][256];
		for (int y1=0;y1<=255;y1++)
		{
			int current=0;

			for (int y4=0;y4<=255;y4++)
			{
				SOLY2[y1][y4]=-1;
			}
		}

		for (float y1=0;y1<=256;y1+=0.1f)
		{
			for (float y2=0;y2<=256;y2+=0.1f)
			{

				float y3=(y2-y1+256f)/2f;
				float y3bis=(float)Math.abs(y2-y1);
				float k2=64;//y3bis;//64
				//int k2=Math.max(64,y3bis);
				float k1=(255-k2);

				float suma=k1+k2;
				//k1=256;//192;
				//k2=256-k1;//64;


				float y4=(k1*y3+k2*y2)/suma;
				//float y4=(k1*y3+k2*100)/suma;


				//System.out.println("y4:"+y4);
				//	if ((int)y1==255) System.out.println(" y1:"+y1+"  y2:"+y2+"   y3:"+y3+"   y4:"+y4);
				//if (y4>255)y4=255;
				//if (y4<=0) y4=0;
				//y2=y1+2*y3-256;
				//if (y2>255) y2=255;
				//if (y2<=0) y2=1;

				//if (SOLY2[y1][y4]!=y2 && SOLY2[y1][y4]!=0) System.out.println(" SOLY2[y1][y4]:"+SOLY2[y1][y4]+"    y2:"+y2);
				/*
					if (SOLY2[y1][y4]!=0)
					{
						int dife=y2-SOLY2[y1][y4];
						//System.out.println("DIFERENCIA:"+(y2-SOLY2[y1][y4]));//+"      "+y2);
						if (dife>5) {
							System.out.println("DIFERENCIA:"+(y2-SOLY2[y1][y4]));//+"      "+y2);
							System.exit(0);
						}
					}*/
				if ((int)y4<=255 && y4>=0)
					SOLY2[(int)y1][(int)y4]=(int)y2;//y1+2*y4-256;


			}
		}

		//comprobamos los huecos

		for (int y1=0;y1<=255;y1++)
		{
			int current=0;

			for (int y4=0;y4<=255;y4++)
			{

				if (y4==0)
				{
					for (int j=0;j<=255;j++)	
					{
						if (SOLY2[y1][j]!=-1) {current=SOLY2[y1][j];break;}
					}
					// if (current==0)  current =255;
				}



				if (SOLY2[y1][y4]==-1) SOLY2[y1][y4]=current;
				//current;
				else current=SOLY2[y1][y4];

				//   System.out.println(""+y1+","+y4+":"+SOLY2[y1][y4]);

			}

		}

		//System.exit(0);


	}

	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%	
	void initY4_mal()
	{
		SOLY2=new int[256][256];
		for (int y1=0;y1<=255;y1++)
		{
			int current=0;

			for (int y4=0;y4<=255;y4++)
			{
				SOLY2[y1][y4]=-1;
			}
		}

		for (int y1=0;y1<=128;y1+=1f)
		{
			for (int y2=0;y2<=255;y2+=1f)
			{

				int y3=(y2-y1+256)/2;
				float k1=(y1)/128f;
				float k2=(128-y1)/128f;


				int y4=(int)(k1*y3+k2*y2);
				if (y4<=255 && y4>=0)
				{
					SOLY2[y1][y4]=y2;
					//System.out.println(""+y1+","+y4+":"+SOLY2[y1][y4]);
				}
			}
		}
		for (int y1=129;y1<=255;y1+=1f)
		{
			for (int y2=0;y2<=255;y2+=1f)
			{

				int y3=(y2-y1+256)/2;

				float k1=(255-y1)/128f;//y3bis*2;//64
				//int k2=Math.max(64,y3bis);
				float k2=(y1-128)/128f;//(255-k2);

				int y4=(int)(k1*y3+k2*y2);
				if (y4<=255 && y4>=0)
				{
					SOLY2[(int)y1][(int)y4]=(int)y2;
					//System.out.println(""+y1+","+y4+":"+SOLY2[y1][y4]);
				}
			}
		}


		//comprobamos los huecos

		for (int y1=0;y1<=255;y1++)
		{
			int current=0;

			for (int y4=0;y4<=255;y4++)
			{

				if (y4==0)
				{
					for (int j=0;j<=255;j++)	
					{
						if (SOLY2[y1][j]!=-1) {current=SOLY2[y1][j];break;}
					}
					// if (current==0)  current =255;
				}



				if (SOLY2[y1][y4]==-1) SOLY2[y1][y4]=current;
				//current;
				else current=SOLY2[y1][y4];

				//  System.out.println(""+y1+","+y4+":"+SOLY2[y1][y4]);

			}

		}
		//System.exit(0);
	}

	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%	
	public void watermarkFilter_V4(int[][] ant)
	{
		//hacemos una copia

		int[][] copy=new int[3][width*height];

		int component=0;
		//	for (int component=0;component<3;component++)
		{
			for (int i=0;i<width*height;i++)
			{
				//if (Math.abs(YUV[component][i]-frame[component][i])>16)
				copy[component][i]=YUV[component][i];

			}
		}
		//ahora el filtro
		//for (int component=0;component<3;component++)
		{
			for (int y=0;y<height;y++)
			{
				for (int x=0;x<width;x++)
				{
					//if (Math.abs(YUV[component][i]-frame[component][i])>16)

					if (x>0 && y>1 && x<width-1 && y<height-1)
					{
						int pos=y*width+x;


						//mucha dif: filtrado minimo, son bordes
						//media dif: filtrado maximo
						//poca dif: filtrado minimo
						//para aumentar la dif y asi filtrar menos meto el 0.5 (es poco. queda mal)

						float dif=Math.abs(YUV[component][pos]-ant[component][pos]);


						float peso1=1;//peso del pixel
						float k=1f;
						if (dif<=128) peso1=(float) (128f-k*dif)/128f;
						if (dif>128)  peso1=(float) ((1f/k)*dif-128)/128f;

						//System.out.println("dif:"+dif);
						if (peso1>1) peso1=1f;
						if (peso1<0.25) peso1=0.25f;
						//System.out.println(" peso1:"+peso1);
						peso1=1;//0.25f;
						if (dif>16 && dif< 64) peso1=0.25f;
						peso1=1f;

						float peso2=(1f-peso1)/4f;//(1f-peso1)/4f;

						YUV[component][pos]=(int)(peso1*copy[component][pos]+
								peso2*copy[component][(y-1)*width+x]+
								peso2*copy[component][(y)*width+x-1]+
								peso2*copy[component][(y)*width+x+1]+
								peso2*copy[component][(y+1)*width+x]

								);
						if (YUV[component][pos]>255) YUV[component][pos]=255;
						if (YUV[component][pos]<=0) YUV[component][pos]=1;


						//YUV[component][pos]=ant[component][pos];
					}

				}//x
			}//y
		}
	}

	//***********************


	void initLogY3()
	{
		SOLY2=new int[256][256];
		for (int y1=0;y1<=255;y1++)
		{
			int current=0;

			for (int y4=0;y4<=255;y4++)
			{
				SOLY2[y1][y4]=-1;
			}
		}

		for (float y1=0;y1<256;y1+=1f)
		{
			for (float y2=0;y2<256;y2+=1f)
			{

				//float y3=(y2-y1+256f)/2f;
				float y3=Math.abs(y2-y1);
				float signo=(y2-y1)/y3;//+1 or -1

				float base=1.1f;
				float k=(float)(128/(Math.log(128f)/Math.log(base)));

				float numero=(y3/2f+1f);
				float y4=(float)(128f+signo*k*Math.log10(numero)/Math.log10(base));




				//float y3bis=(float)Math.abs(y2-y1);
				//System.out.println("y1:"+y1+"  y2:"+y2+"    y4:"+y4);

				if ((int)y4<=255 && y4>=0)
					SOLY2[(int)y1][(int)y4]=(int)y2;//y1+2*y4-256;


			}
		}
		//if (1<2) System.exit(0);
		//comprobamos los huecos

		for (int y1=0;y1<=255;y1++)
		{
			int current=0;

			for (int y4=0;y4<=255;y4++)
			{

				if (y4==0)
				{
					for (int j=0;j<=255;j++)	
					{
						if (SOLY2[y1][j]!=-1) {current=SOLY2[y1][j];break;}
					}
					// if (current==0)  current =255;
				}



				if (SOLY2[y1][y4]==-1) SOLY2[y1][y4]=current;
				//current;
				else current=SOLY2[y1][y4];

				//   System.out.println(""+y1+","+y4+":"+SOLY2[y1][y4]);

			}

		}

		//System.exit(0);


	}
	//**********************************************
	public void computeLogY3(int[][] frame)
	{
		//result is (y-y'+255)/2 belongs to 0..255
		for (int component=0;component<3;component++)
		{
			for (int i=0;i<width*height;i++)
			{
				//if (Math.abs(YUV[component][i]-frame[component][i])>16)
				{
					int y1=frame[component][i];
					int y2=	YUV[component][i];

					float y3=Math.abs(y2-y1);
					float signo=(y2-y1)/y3;//+1 or -1
					float base=1.1f;
					float k=(float)(128/(Math.log(128f)/Math.log(base)));

					float numero=(y3/2f+1f);
					float y4=(float)(128f+signo*k*Math.log10(numero)/Math.log10(base));

					if (y4<0) y4=0;
					if (y4>255) y4=255;

					YUV[component][i]=(int)y4;


					if (YUV[component][i]<0  || YUV[component][i]>256)
					{
						System.out.println("warning");
						System.exit(0);
					}
					//	YUV[component][i]=(int)((YUV[component][i]-frame[component][i]/2+128)/1.5f);
					if (YUV[component][i]>255) YUV[component][i]=255;
					//if (YUV[component][i]<=128) YUV[component][i]=128;

					if (YUV[component][i]<=0) YUV[component][i]=0;
				}
			}


		}//for component
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	//**********************************************
	public void computeY3_ok(int[][] frame, int[] mask, int[][] frame_y1, int[] countdown)
	{
		// hay que haber invocado a initY3Perfect()


		//result is (y-y'+255)/2 belongs to 0..255
		for (int component=0;component<3;component++)
		{
			for (int i=0;i<width*height;i++)
			{
				//if (Math.abs(YUV[component][i]-frame[component][i])>16)
				{
					int y1=frame_y1[component][i];
					int y1p=frame[component][i];
					int y2=	YUV[component][i];
					//System.out.println(" y1:"+y1+"   y1p:"+y1p);
					//float
					int y3p=(int)((y2-y1p+256f)/2f);
					//float 
					int y3=(int)((y2-y1+256f)/2f);

					if (y3!=128) 
						countdown[i]=5;// restart countdown 3...2...1
					else 
					{//no hay movimiento
						if (countdown[i]>0) countdown[i]--;//countdown decrease till 0
					}

					//si y3==128 hay dos casos
					// a) correccion de estela
					// b) objeto quieto. refinando
					// ambos casos son el mismo

					//countdown[i]=10;
					//int y3=(y2-y1+255);

					//int k2=Math.max(64,y3bis);
					//	float k3=0.1f*Math.abs(y2-y1);
					float k2=128;//y3bis;//64;
					float k1=(255-k2);
					//float suma=k1+k2;

					float suma=k1+k2;
					//k1=192;
					//k2=64;
					//float y4=0;

					int y4=0;

					//if (mask[i]!=1) y4=(k1*y3p+k2*y2)/suma;// bloque
					//else  y4=(k1*y3p+k2*y2)/suma;//bordes
					//System.out.println("countdown:"+countdown[i]);
					//if (countdown[i]<1) System.exit(0);

					y4=(int)((k1*y3p+k2*y2)/suma);// siempre y3p

					/*
								float kk2=Math.abs(y2-y1)/255f;
								float kk1=1-kk2;
								int y5=(int)((kk1*y3p+kk2*y2));
								y4=y5;
					 */

					if (y4<0) y4=0;
					if (y4>255) y4=255;

					if (countdown[i]==3)
						YUV[component][i]=YUV[component][i]=(int)y4;
					else if (countdown[i]>0)
						YUV[component][i]=(int)y4;//refinamos
					//YUV[component][i]=(int)((k1*128+k2*y2)/suma);
					else 
					{
						//asumimos que hemos acertado en y1p y no tratamos de refinar
						//para evitar el efecto de cuadricula
						YUV[component][i]=(int)((k1*128+k2*y1p)/suma);

						//si usamos k1*y3p+k2*y2 no podemos asumir que hemos acertado
						YUV[component][i]=(int)y4;//refinamos normalmente

						//si usamos no lineal el resultado debe ser 128
						//y5=(int)((kk1*128+kk2*y2));
						//YUV[component][i]=128;
					}

					//esto no hace nada diferente
					//me gustaria que para lo que se queda muy quieto usasemos otro valor

					//if (countdown[i]==-10 ) {YUV[component][i]=255; countdown[i]=3;}

					float d=(-countdown[i]+5f)/5f; //desde 1.1 hasta 3
					int y3px=(int)((y2-y1p)/d+128f);
					//int y4x=(int)(k1*y3px+k2*y2/d)/suma;// siempre y3p
					//if (countdown[i]<0)	YUV[component][i]=y4x;
					//YUV[component][i]=(int)y4;
					//if (mask[i]==1)YUV[component][i]=128;//(y1p-y2+256)/2;;

					/*
								if (mask[i]!=1)	YUV[component][i]=(int)y4;
								else {


									YUV[component][i]=(int)((y1-y2+255f)/2f);;
								}
					 */

					mask[i]=0;

				}
			}


		}//for component
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



	void initY3()
	{
		//initY3puro();
		//initY3Perfect();
		//initY3noLineal();
		initY3mejorada();
		//initY3Cuadratica();
		//if (1<2) System.exit(0);
		if (1<2) return;

		int tope=256;
		//SOLY2=new int[256][256];
		SOLY2=new int[tope][tope];
		for (int y1=0;y1<=255;y1++)
		{
			int current=0;

			for (int y4=0;y4<tope;y4++)
			{
				SOLY2[y1][y4]=-1;
			}
		}

		for (float y1=0;y1<=255;y1+=0.1f)
		{
			for (float y2=0;y2<=255;y2+=0.1f)
			{

				float y3=(y2-y1+256f)/2f;
				//float y3=(y2+y1)/2f;


				//float y3=(y2-y1+255f);
				//float y3bis=(float)Math.abs(y2-y1);
				//float k3=0.1f*Math.abs(y2-y1);
				float k2=64;//y3bis;//64
				//int k2=Math.max(64,y3bis);
				float k1=(255-k2);

				float suma=k1+k2;
				//k1=256;//192;
				//k2=256-k1;//64;
				float y4=(k1*y3+k2*y2)/suma;
				//System.out.println("y4:"+y4);
				//	if ((int)y1==255) System.out.println(" y1:"+y1+"  y2:"+y2+"   y3:"+y3+"   y4:"+y4);
				//if (y4>255)y4=255;
				//if (y4<=0) y4=0;
				//y2=y1+2*y3-256;
				//if (y2>255) y2=255;
				//if (y2<=0) y2=1;

				//if (SOLY2[y1][y4]!=y2 && SOLY2[y1][y4]!=0) System.out.println(" SOLY2[y1][y4]:"+SOLY2[y1][y4]+"    y2:"+y2);
				/*
							if (SOLY2[y1][y4]!=0)
							{
								int dife=y2-SOLY2[y1][y4];
								//System.out.println("DIFERENCIA:"+(y2-SOLY2[y1][y4]));//+"      "+y2);
								if (dife>5) {
									System.out.println("DIFERENCIA:"+(y2-SOLY2[y1][y4]));//+"      "+y2);
									System.exit(0);
								}
							}*/
				//if ((int)y4<=255 && y4>=0)
				if ((int)y4<=255 && y4>=0)
					SOLY2[(int)y1][(int)y4]=(int)y2;//y1+2*y4-256;


			}
		}

		//comprobamos los huecos

		for (int y1=0;y1<=255;y1++)
		{
			int current=0;

			for (int y4=0;y4<=255;y4++)
			{

				if (y4==0)
				{
					for (int j=0;j<=255;j++)	
					{
						if (SOLY2[y1][j]!=-1) {current=SOLY2[y1][j];break;}
					}
					// if (current==0)  current =255;
				}



				if (SOLY2[y1][y4]==-1) SOLY2[y1][y4]=current;
				//current;
				else current=SOLY2[y1][y4];



				//   System.out.println(""+y1+","+y4+":"+SOLY2[y1][y4]);

			}
			//esto es solo una precaucion
			// SOLY2[y1][255]=255;						
		}

		//System.exit(0);


		//nueva forma de calcular. SOLO VALE PARA Y3
		/*
					for (int y4=128;y4<256;y4++)
					{
						for (int y1=0;y1<256;y1++)
						{
							SOLY2[y1][y4]=y1+(2*y4-256);
							if (SOLY2[y1][y4]<0)SOLY2[y1][y4]=0;
							if (SOLY2[y1][y4]>255)SOLY2[y1][y4]=255;
						}
					}
					for (int y4=127;y4>=0;y4--)
					{
						for (int y1=0;y1<256;y1++)
						{
							SOLY2[y1][y4]=y1+(2*y4-256);
							if (SOLY2[y1][y4]<0)SOLY2[y1][y4]=0;
							if (SOLY2[y1][y4]>255)SOLY2[y1][y4]=255;
						}
					}
					for (int y4=0;y4<=255;y4++)
					{
						for (int y1=0;y1<256;y1++)
						{
							SOLY2[y1][y4]=y1+(2*y4-256);
							if (SOLY2[y1][y4]<0)SOLY2[y1][y4]=0;
							if (SOLY2[y1][y4]>255)SOLY2[y1][y4]=255;
						}
					}
		 */	
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	void initY3prueba()
	{
		int tope=256;
		//SOLY2=new int[256][256];
		SOLY2=new int[tope][tope];
		for (int y1=0;y1<=255;y1++)
		{
			for (int y4=0;y4<tope;y4++)
			{
				SOLY2[y1][y4]=-1;
			}
		}

		for (int y1=0;y1<=255;y1+=1)
		{
			for (int y2=0;y2<=255;y2+=1)
			{
				//para cada valor de y2 puede haber varios y4 que cumplen
				//aunque esto ocurre si hay sat y2=255 o y2=0
				int y3=(y2-y1+256)/2;
				int k2=64;//y3bis;//64
				int k1=(255-k2);
				int suma=k1+k2;
				int y4=(k1*y3+k2*y2)/suma;

				if (y4<256 && y4>=0)
					SOLY2[y1][y4]=y2;//y1+2*y4-256;
				//----tope superior
				if (y2==255)
				{
					for (int i=y1+1;i<256;i++)
					{    y3=(y2-i+256)/2;
					int y5=(k1*y3+k2*y2)/suma;
					if (y5<256 && y5>=0)
						SOLY2[i][y4]=y2;//y1+2*y4-256;
					}
				}
				//--tope inferior
				if (y2==0)
				{
					for (int i=y1-1;i>=0;i--)
					{    y3=(y2-i+256)/2;
					int y5=(k1*y3+k2*y2)/suma;
					if (y5<256 && y5>=0)
						SOLY2[i][y4]=y2;//y1+2*y4-256;
					}
				}

				//if (y4==128)SOLY2[y1][y4]=y1;


			}
		}


		for (int y4=0;y4<256;y4++)
		{
			for (int y1=0;y1<256;y1++)
			{
				//System.out.println("y1:"+y1+"  y4:"+y4+"  --> y2="+SOLY2[y1][y4]);
				if (SOLY2[y1][y4]==-1) System.exit(0);
			}
		}

		//System.exit(0);


		//nueva forma de calcular. SOLO VALE PARA Y3
		/*
					for (int y4=128;y4<256;y4++)
					{
						for (int y1=0;y1<256;y1++)
						{
							SOLY2[y1][y4]=y1+(2*y4-256);
							if (SOLY2[y1][y4]<0)SOLY2[y1][y4]=0;
							if (SOLY2[y1][y4]>255)SOLY2[y1][y4]=255;
						}
					}
					for (int y4=127;y4>=0;y4--)
					{
						for (int y1=0;y1<256;y1++)
						{
							SOLY2[y1][y4]=y1+(2*y4-256);
							if (SOLY2[y1][y4]<0)SOLY2[y1][y4]=0;
							if (SOLY2[y1][y4]>255)SOLY2[y1][y4]=255;
						}
					}
					for (int y4=0;y4<=255;y4++)
					{
						for (int y1=0;y1<256;y1++)
						{
							SOLY2[y1][y4]=y1+(2*y4-256);
							if (SOLY2[y1][y4]<0)SOLY2[y1][y4]=0;
							if (SOLY2[y1][y4]>255)SOLY2[y1][y4]=255;
						}
					}
		 */	
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	void initY3puro()
	{
		SOLY2=new int[256][256];
		for (int y4=0;y4<=255;y4++)
		{
			for (int y1=0;y1<256;y1++)
			{
				SOLY2[y1][y4]=y1+(2*y4-256);
				if (SOLY2[y1][y4]<0)SOLY2[y1][y4]=0;
				if (SOLY2[y1][y4]>255)SOLY2[y1][y4]=255;
			}//y1
		}//y4
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	void initY3Perfect()
	{

		float k2=128f;
		float k1=255-k2;
		float suma=k1+k2;
		SOLY2=new int[256][256];
		for (int y1=0;y1<=255;y1++)
		{
			for (int y4=0;y4<=255;y4++)
			{

				//asi no pueden faltar valores intermedios de y4. para cada y4 busco el valor
				for (int y2=-512;y2<=512;y2++)
				{
					float y3=(y2-y1+256)/2;
					int y5=(int)((k1*y3+k2*y2)/suma);
					if (y5>=0 && y5<=255 && y5==y4)
					{
						SOLY2[y1][y4]=y2;
						if (SOLY2[y1][y4]<0)SOLY2[y1][y4]=0;
						if (SOLY2[y1][y4]>255)SOLY2[y1][y4]=255;
						break;
					}

				}//y2


			}//y1
		}//y4
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	void initY3noLineal()
	{



		SOLY2=new int[256][256];
		//ponemos todo a -1
		for (int y1=0;y1<=255;y1++)
		{
			for (int y4=0;y4<=255;y4++)
			{
				SOLY2[y1][y4]=-1;
			}
		}

		//ahora rellenamos
		for (int y1=0;y1<=255;y1++)
		{
			for (int y4=0;y4<=255;y4++)
			{

				//asi no pueden faltar valores intermedios de y4. para cada y4 busco el valor
				/*
							if (y4==128)
							{
								SOLY2[y1][y4]=y1;
							}
				 */
				//else
				{	
					for (int y2=-512;y2<=512;y2++)
					{

						float y3=(y2-y1+256)/2;

						//float k2=Math.abs(y2-y1)/255f;
						//float k2=(float)Math.pow (Math.abs(y2-y1)/255f, 0.5f);
						float k2=(float)Math.abs(y2-y1)/255f;
						//k2=2*k2;
						//

						//k2=(float)Math.pow(k2,2f);
						if (k2>1) k2=1;
						//k2=0.5f;
						//float k2=(float)Math.pow(((y2-y1)*(y2-y1))/255f,0.5f);
						float k1=1f-k2;
						//k1=k1/2f;
						float sumaf=k2+k1;//k1+k2;
						int y5=(int)((k1*y3+k2*y2)/sumaf);

						if (y5>=0 && y5<=255 && y5==y4)
						{
							SOLY2[y1][y4]=y2;
							if (SOLY2[y1][y4]<0)SOLY2[y1][y4]=0;
							if (SOLY2[y1][y4]>255)SOLY2[y1][y4]=255;
							break;
						}

					}//y2

				}//else
			}//y1
		}//y4

		//check. NO se van a rellenar todos los valores de y4 pero es normal
		for (int y1=0;y1<=255;y1++)
		{
			for (int y4=0;y4<=255;y4++)
			{

				if (SOLY2[y1][y4]==-1)
				{
					int step=1;
					if (y4<128)
					{	
						for (int y4b=128;y4b>=0;y4b-=1)
						{
							if (SOLY2[y1][y4b]==-1) 
							{
								SOLY2[y1][y4]=SOLY2[y1][y4b+1];
								break;
							}
						}
					}//if
					else
					{
						for (int y4b=128;y4b>=0;y4b+=1)
						{
							if (SOLY2[y1][y4b]==-1) 
							{
								SOLY2[y1][y4]=SOLY2[y1][y4b-1];
								break;
							}
						}	
					}
					//System.out.println("fallo en init  y1:"+y1+"   y4:"+y4);
					//System.exit(0);
				}//for

			}
		}

	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	//**********************************************
	public void computeY3_nolineal(int[][] frame, int[] mask, int[][] frame_y1, int[] countdown)
	{
		//result is (y-y'+255)/2 belongs to 0..255
		for (int component=0;component<3;component++)
		{
			for (int i=0;i<width*height;i++)
			{
				//if (Math.abs(YUV[component][i]-frame[component][i])>16)
				{
					int y1=frame_y1[component][i];
					int y1p=frame[component][i];
					int y2=	YUV[component][i];
					//System.out.println(" y1:"+y1+"   y1p:"+y1p);
					//float
					int y3p=(int)((y2-y1p+256f)/2f);
					//float 
					int y3=(int)((y2-y1+256f)/2f);


					//countdown:
					//cada vez que algo se mueve o cambia, countdown vale lo maximo (N)
					// al quedarse quieto, tras N fotogramas pasa a cero y 
					//dejamos de enviar informacion de refinado. enviamos 128
					int max_countdown=3;

					if (y3!=128) //ojo miro y3 y no y3p
						countdown[i]=max_countdown;// restart countdown 3...2...1
					else 
					{//no hay movimiento
						if (countdown[i]>0) countdown[i]--;//countdown decrease till 0
					}

					//si y3==128 hay dos casos
					// a) correccion de estela
					// b) objeto quieto. refinando
					// ambos casos son el mismo

					//countdown[i]=10;
					//int y3=(y2-y1+255);

					//int k2=Math.max(64,y3bis);
					//	float k3=0.1f*Math.abs(y2-y1);
					float k2=128;//y3bis;//64;
					float k1=(255-k2);
					//float suma=k1+k2;

					float suma=k1+k2;
					//k1=192;
					//k2=64;
					//float y4=0;

					int y4=0;

					//if (mask[i]!=1) y4=(k1*y3p+k2*y2)/suma;// bloque
					//else  y4=(k1*y3p+k2*y2)/suma;//bordes
					//System.out.println("countdown:"+countdown[i]);
					//if (countdown[i]<1) System.exit(0);

					y4=(int)((k1*y3p+k2*y2)/suma);// siempre y3p


					//EL CALULO DE KK2 tiene dos opciones: con y2-y1 o y2-y1p

					float kk2=(float)Math.abs(y2-y1)/255f;
					//kk2=2*kk2;
					//kk2=(float)Math.pow(kk2,2f);
					if (kk2>1)kk2=1;

					float kk2p=(float)Math.abs(y2-y1p)/255f;
					//kk2p=2*kk2p;
					//kk2p=(float)Math.pow(kk2p,2f);
					if (kk2p>1)kk2p=1;

					//float kk2=(float)Math.pow(((y2-y1)*(y2-y1))/255f,0.5f);
					//float kk2=(float)Math.pow (Math.abs(y2-y1)/255f, 0.5f);
					//kk2=0.5f;
					//kk2p=0.5f;

					float kk1=1-kk2;//podemos hacer un kk1 basado en kk2p en lugar de kk2
					float kk1p=1-kk2p;//podemos hacer un kk1 basado en kk2p en lugar de kk2
					//kk1=kk1/2;
					if (kk1<0) kk1=0;
					if (kk1p<0) kk1p=0;

					float sumaf=kk1+kk2;
					//System.out.println("sumaf:"+sumaf);
					int y5=(int)((kk1*y3+kk2*y2)/sumaf);
					int y5p=(int)((kk1p*y3p+kk2p*y2)/sumaf);
					int y5px=(int)((kk1*y3p+kk2*y2)/sumaf);
					//y5p es mas precisa para llegar a y2 que lo que es y5
					//y4=y5p;

					//y4=(int)((k1*y3p+k2*y2)/suma);// siempre y3p
					//y5=y4;
					//y5p=y4;

					if (y5p<=0) y5p=1;
					if (y5p>255) y5p=255;
					if (y5<=0) y5=1;
					if (y5>255) y5=255;

					if (countdown[i]==max_countdown){
						//aqui es mejor y5 en lugar de y5p
						YUV[component][i]=YUV[component][i]=(int)y5p;
					}
					else if (countdown[i]>0){
						YUV[component][i]=(int)y5p;//refinamos
						//suponemos haber acertado pero dejamos que kk2 no sea cero
						//   kk2=(float)Math.abs(y2-y1p)/255f;
						// kk1=1-kk2;
						//sumaf=1f;
						// YUV[component][i]=(int)((kk1*y3p+kk2*y2)/sumaf);
					}
					//YUV[component][i]=(int)((k1*128+k2*y2)/suma);
					else //countdown=0;
					{
						//si usamos y4=y3p
						// asumimos que hemos acertado en y1p y no tratamos de refinar
						//para evitar el efecto de cuadricula
						//YUV[component][i]=(int)((k1*128+k2*y1p)/suma);

						//si usamos k1*y3p+k2*y2 hay que usar otra formula
						YUV[component][i]=(int)y5px;//no refinamos. Esto puede ser 128 pero asi es mas general

						//si usamos no lineal (k2 depende de y2) el resultado debe ser 128
						//de este modo queda mucho mejor lo que no se mueve
						//YUV[component][i]=128;

						// YUV[component][i]=(int)((kk1p*128+kk2p*y1p)/suma);
					}

					//esto no hace nada diferente
					//me gustaria que para lo que se queda muy quieto usasemos otro valor

					//if (countdown[i]==-10 ) {YUV[component][i]=255; countdown[i]=3;}

					//float d=(-countdown[i]+5f)/5f; //desde 1.1 hasta 3
					//int y3px=(int)((y2-y1p)/d+128f);
					//int y4x=(int)(k1*y3px+k2*y2/d)/suma;// siempre y3p
					//if (countdown[i]<0)	YUV[component][i]=y4x;
					//YUV[component][i]=(int)y4;
					//if (mask[i]==1)YUV[component][i]=128;//(y1p-y2+256)/2;;

					/*
										if (mask[i]!=1)	YUV[component][i]=(int)y4;
										else {


											YUV[component][i]=(int)((y1-y2+255f)/2f);;
										}
					 */

					mask[i]=0;

				}
			}


		}//for component
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	void initY3Cuadratica()
	{


		SOLY2=new int[256][256];
		//ponemos todo a -1
		for (int y1=0;y1<=255;y1++)
		{
			for (int y4=0;y4<=255;y4++)
			{
				SOLY2[y1][y4]=-1;
			}
		}
		float u=2;
		//ahora rellenamos
		for (int y1=0;y1<=255;y1++)
		{
			
			//ecuaciones cuadraticas
			//----------------------
			/*
			float a1=0;
			if (y1>0) a1= ((float)y1-128f)/(float)(y1*y1);
			float b1=1f-2f*a1*(float)y1;
			
			float a2=100;
			if (y1<255) a2=((float)y1-128f)/(255f*255f-2f*(float)y1*255f+(float)(y1*y1));
			float b2=1f-2f*a2*(float)y1;
			float c2=128f+a2*(float)(y1*y1)-(float)y1;
			*/
			//ecuaciones lineales
			//------------------------
			float alfa1=(128f-u)/(float)y1;
			//float beta1=0;
			
			
			float alfa2=(128f-u)/(float)(255f-y1);
			//float beta2=0;
			//if (y1==0) alfa1=0;
			//if (y1==255) alfa2=0;
			
			//System.out.println("alfa1:"+alfa1+"    alfa2:"+alfa2);
			//alfa1=0.5f;//EXPERIMENTO
			//beta1=(128-u)-y1*alfa1;//EXPERIMENTO
			//alfa2=0.5f;//EXPERIMENTO
			
			if (alfa1>1) 
				{alfa1=1;
				//falta beta1
				//beta1=(128-u)-y1;
				}
			if (alfa2>1) 
				{alfa2=1;
				
				}
			
			//System.out.println("y1:"+y1+"   alfa1:"+alfa1+"   alfa2:"+alfa2);
			
			for (int y4=0;y4<=255;y4++)
			{

				//for each Y4 value, search the y2 value
				
				
				//1er tramo
				//==========
					if (y4<128-u) //esto no lo puedo hacer asi si voy a sumar
					{	
						
					//busco entre todos los valores posibles de y2 el correcto	
					//for (int y2=0;y2<=y1;y2++)
						for (int y2=y1;y2>=0;y2--)
					{
						
						int  y5=0;
						//if (y1>0) 
							//y5=(int)(0.5f+a1*y2*y2+b1*y2);
						
							//y5=(int) (alfa1*y2 +beta1);
							
							
							y5=(int)(128-u-alfa1*(y1-y2));//NUEVO 16/11
							//if (y5<0) y5=0;//EXPERIMENTO
							
							//metemos a y2
							//y5=(int) (1.0f*(alfa1*y2 +beta1)+0.0f*y2);
							
							
						//if (y2==y1) System.out.println("y1:"+y1+" y4:"+y5);
						
						//if ( (int)y5==y4 )
							if ( y5==y4 )
						{
							//cazado! 
							
							
							SOLY2[y1][y4]= y2;
							//SOLY2[y1][(int)(1.0f*y4+0.0f*y2)]=y2;	
							//if (SOLY2[y1][y4]<=0) SOLY2[y1][y4]=1;
							//else if (SOLY2[y1][y4]>255) SOLY2[y1][y4]=255;
							
							break;//salimos del for, solo se puede asignar un valor
						}
						
							
							
							
					}//for y2
					
					/*if (!found)
					{
					System.out.println("y1:"+y1+",y4:"+y4+"  anterior:"+anterior+"   posterior:"+posterior);
					if (!found && anterior!=-1) SOLY2[y1][y4]=anterior;
					else if (!found && posterior!=-1) SOLY2[y1][y4]=posterior;
					}
					*/
					}
					
					
					//2nd tramo
					//===================
					
					else if (y4>128+u)//y4>128 2nd tramo
					{
						for (int y2=y1;y2<=255;y2++)
						{
						int y5=255;
								
							//y5=(int)(0.5f+a2*y2*y2+b2*y2+c2);
						
							
								//y5=128;
							
							y5=(int)(128f+u+alfa2*(y2-y1));
							
							//if (y5>255) y5=255;//EXPERIMENTO
						
							//metemos a y2
							//y5=(int) (1.0f*(128f+alfa2*(y2-y1))+0.0f*y2);
							//System.out.println("y1:"+y1+" y2:"+y2+"  y5:"+y5);
						//if (1<2) System.exit(0);

						//if ( (int)y5==y4)
							if ( y5==y4)
						{
							SOLY2[y1][y4]=y2;
							//SOLY2[y1][(int)(1.0f*y4+0.0f*y2)]=y2;
							//if (SOLY2[y1][y4]<=0) SOLY2[y1][y4]=1;
							//else if (SOLY2[y1][y4]>255) SOLY2[y1][y4]=255;
							break;
						}
								
					   }//end for y2
					}//else
					else
					{
						//tramo recto desde 128-u hasta 128+u, extremos incluidos
						//no necesito recorrer y2.
						SOLY2[y1][y4]=y1;
						
						//System.out.println("puta   y4:"+y4);
						//System.exit(0);
						//metemos a y2, que es igual que y1
						//SOLY2[y1][y4]=(int) (0.5f*y1+0.5f*y1);
					}

				
			}//y4
		}//y1

		//check si todo esta relleno
		
		for (int y1=0;y1<=255;y1++)
		{
			int current=-1;
			
			//para cada y4 establecemos el primer valor valido
			//---------------------------------------------------			
			for (int j=0;j<=255;j++)
			  {	if (SOLY2[y1][j]!=-1) 
			      {
				  current =SOLY2[y1][j];
				  //System.out.println(" current y4 es "+j+"   da y2="+current);
				  break;
			      }
			  }
			//current=0;
			//ya tenemos el primer valor valido. ahora recorremos todo y4
			//-----------------------------------------------------------
			for (int y4=0;y4<=255;y4++)
			{
			
				//set de correcciones por si acaso
				//if (y4==128) SOLY2[y1][y4]=y1;//correccion
				
				//if (y4>=128-u && y4<=128+u) SOLY2[y1][y4]=y1;//correccion
				
				//if (y1>=250 && y4>=128)	SOLY2[y1][y4]=y1;//correccion
				//if (y1<=10 && y4<=128)    SOLY2[y1][y4]=y1;
				
				
				//SOLY2[y1][y4]=y1;
				//if (y4==128)SOLY2[y1][y4]=y1;
				
					
				if (SOLY2[y1][y4]!=-1) current=SOLY2[y1][y4];
				//System.out.println(" y1:"+y1+"  y4:"+y4+"   >"+SOLY2[y1][y4]);
				
				if (SOLY2[y1][y4]==-1)
				{
				//System.out.println(" y1:"+y1+"  y4:"+y4+"   >"+SOLY2[y1][y4]);
				//System.exit(0);
				
					SOLY2[y1][y4]=current;
					//System.out.println(" y1:"+y1+"  y4:"+y4+"   >"+SOLY2[y1][y4]);
				}//for
			//if (y1==254)	System.out.println(" y1:"+y1+"  y4:"+y4+"   >"+SOLY2[y1][y4]);
				
				
				if (SOLY2[(int)y1][(int)y4]<=0) SOLY2[(int)y1][(int)y4]=1;
				else if (SOLY2[(int)y1][(int)y4]>255) SOLY2[(int)y1][(int)y4]=255;
			}
			
		}
		
		
//if (1<2) System.exit(0);
	}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%			
	//**********************************************
		public void computeY3(int[][] frame, int[][] error, int[][] frame_y1, int[] countdown)
		{
			//YUVtoBMP("./output_video"+"/Y1.bmp",frame_y1[0]);
			//result is (y-y'+255)/2 belongs to 0..255
			
			int component=0;
			//for (int component=0;component<3;component++)
			{
				for (int i=0;i<width*height;i++)
				{
					//if (Math.abs(YUV[component][i]-frame[component][i])>16)
					{
						
						int y1=frame_y1[component][i];
						int y1p=frame[component][i];
						
						int y2=	YUV[component][i];
						
						//y2menosy1[component][i]=y2-y1p;
						
						//y2menosy1[component][i]=(y2-y1p+255)/2;
						
						//y1p=y2;
						//y1p=y1;
						//if (mask[i]==1) //borde
						{
							//y1p=y2;
						}
						
					//	int umbralmax=245;//250;
                        //if (y2>=umbralmax && y1p>umbralmax) y2=umbralmax;
						//if (y2>=umbralmax ) y2=umbralmax;
                       // int umbralmin=10;//5;
                       // if (y2<=umbralmin && y1p<umbralmin) y2=umbralmin;
                       // if (y2<=umbralmin) y2=umbralmin;
                        
                        //if (y1>=252) y1=252;
                        //if (y1p>=252) y1p=252;
						

						//countdown:
						//cada vez que algo se mueve o cambia, countdown vale lo maximo (N)
						// al quedarse quieto, tras N fotogramas pasa a cero y 
						//dejamos de enviar informacion de refinado. enviamos 128
						int max_countdown=0;//0;
						int min_countdown=-2;//-20000;//-3;
						if (i>width*(height-1)) {
						//	System.out.println(" y1:"+y1+"   y1p:"+y1p+"   y2:"+y2+ "   i:"+(i-width*(height-1)));
							
						}
						//if (i==width*(height)-1)  System.exit(0);
						//if (y2!=y1) //ojo miro y1 y no y1p
						float z=0;
						//de lo contrario un incremento leve paulatino de brillo sobre
						//una zona quieta no seria percibido
						if (y2<y1-z || y2>y1+z) //ojo miro y1 y no y1p
						{
							//if (i>width*(height-1)) System.out.println(" y1:"+y1+"   y2:"+y2);
						//	if (y2>y1+4 || y2<y1-4) //ojo miro y1 y no y1p	
							countdown[i]=max_countdown;// restart countdown 3...2...1
						//	y1p=y2;
						}
						else 
						{//no hay movimiento ni cambio de tono pues y2=y1
							//refinamos un numero finito de veces, hasta que countdown llega al minimo
							int e=y2-y1p;
							if (e<0)e=-e;
							
							//la cuenta atras es para cada pixel
							if (countdown[i]==max_countdown)error[i][0]=e;//1000;
								
							int ureg=min_countdown;//-10 umbral 
							if (countdown[i]>ureg) countdown[i]--;//min debe ser al menos -2, si es -1 llega ya
							
							//if (countdown[i]==-30) countdown[i]=min_countdown+1;//regenera
							//int e=y2-y1p;
							//if (e<0)e=-e;
							
							//System.out.println(" POR AQUI NO PASA  "+ e+","+error[i][0]+"   : "+countdown[i]);
							
							if (countdown[i]==min_countdown && e<error[i][0])// apuntamos el nuevo error 
							{
								//error[i][0]=e;
								//System.out.println(" HOLAAAAAA");
								//countdown[i]=min_countdown+1;//reg condicional
								
							}
							else if (countdown[i]>min_countdown && e>error[i][0])
							{
								//countdown[i]=min_countdown;//PARADA CONDICIONAL
								//System.out.println(" HOLAAAAAA");
								//System.exit(0);
							}
							
							error[i][0]=e;
							
							//la primera vez ei0 vale 1000 y por tanto siempre entraremos aqui de modo que es como 
							//si count fuese 3
							//esto sirve para no parar y seguir regenerando, o bien para arrancar 
							//la regeneracion en cualquier momento
							if  (countdown[i]==ureg && e<error[i][0])
								{
								
								//countdown[i]=min_countdown+1;//reg condicional
								
								
								
								//System.out.println("infinito");
								//System.exit(0);;
								}
							//else if ( e>error[i][0])countdown[i]=min_countdown;
							
							
							/*
							if (countdown[i]==min_countdown && e<error[i][0])// apuntamos el nuevo error 
							{
								error[i][0]=e;
								//System.out.println(" POR AQUI NO PASA");
								//System.exit(0);
							}
							*/
							
							
							/*
							if ( Math.abs(y2-y1p)<Math.abs(y2-error[i][0]) && countdown[i]!=max_countdown)
							{
								error[i][0]=y1p;// contiene el minimo alcanzado
								countdown[i]=min_countdown;
							}
							else countdown[i]=max_countdown;
							*/
							
						//	else if (countdown[i]==-2) {countdown[i]=min_countdown;error[i][0]=y1p;}//parada
						//	else if ( Math.abs(y2-y1p)>Math.abs(y2-error[i][0]) ) { //rearranque
							//	countdown[i]=max_countdown;
								//error[i][0]=y1p;
							//}
							
						//	if (countdown[i]==-2) {countdown[i]=min_countdown;error[i][0]=y1p;}
							//else if ( Math.abs(y2-y1p)>Math.abs(y2-error[i][0]+2) && countdown[i]==min_countdown) 
							{
								//countdown[i]=max_countdown;
							}
							
							
							/*
		
							if ( Math.abs(y2-y1p)>=Math.abs(y2-error[i][0]) && countdown[i]!=min_countdown && countdown[i]!=max_countdown) 
								{
								countdown[i]=min_countdown;
								error[i][0]=y1p;
								}
							//else if (y1p>=error[i][0] && countdown[i]!=max_countdown) countdown[i]=min_countdown;
							else if ( Math.abs(y2-y1p)>Math.abs(y2-error[i][0]+8) && countdown[i]==min_countdown) 
							   {countdown[i]=max_countdown;
							   
							   }
							
							if (countdown[i]!=min_countdown) error[i][0]=y1p;
							
							//else if (y1p!=error[i][0] && Math.abs(y2-y1p)<=Math.abs(y2-error[i][0])) countdown[i]=max_countdown;
							*/
							 
							
							
							//countdown[i]=min_countdown;
							//int e=y2-y1p;
							//if (e<0)e=-e;
							int umbral_parada=20;
							int umbral_arranque=1;
							
							//if (e>=umbral_arranque)countdown[i]=max_countdown;
							//else 
							//if (e<=umbral_parada)countdown[i]=min_countdown;
							/*
							error[i][0]=Math.abs(y2-y1p);
							//if (error[i][0]>error[i][1] && countdown[i]!=max_countdown && countdown[i]!=min_countdown) //no merece la pena refinar mas
							if (error[i][0]==0  && countdown[i]!=min_countdown) //no merece la pena refinar mas
						   // if (error[i][0]== ) //no merece la pena refinar mas
								{
								countdown[i]=min_countdown;//entramos en parada
								//para la proxima vez que entremos en y2!=y1
								error[i][1]=error[i][0];//se queda fijo
								}
							else if (countdown[i]!=min_countdown && error[i][0]<error[i][1]) 
								{
								countdown[i]=max_countdown;error[i][1]=error[i][0];// en cualquier caso
								}
							else if (countdown[i]!=min_countdown )error[i][1]=error[i][0];
							*/
							//else  countdown[i]=max_countdown; 
							//error[i][1]=error[i][0];
							
							//else countdown[i]=max_countdown;
							//if (countdown[i]==min_countdown) y2=y1p;
							
							//else if (countdown[i]!=min_countdown) error[i][1]=error[i][0];//en camino
							
							//if (error[i][0]<error[i][1])error[i][1]=error[i][0];// en cualquier caso
							//else if (error[i][0]>error[i][1])countdown[i]=max_countdown;// en cualquier caso
							
							
							if (countdown[i]>min_countdown) 
							{		
								//countdown[i]--;//countdown decrease till minimum
							
							
							//error[i][1]=error[i][0];
						
							//error[i][0]=e;//Math.abs(y2-y1p);
							//if (countdown[i]==-2) countdown[i]=min_countdown;
							
							//if (e<=4 )//&& countdown[i]<=-2) 
								{
								//System.out.println( "//////////////////////////////////////////////////////"+countdown[i]);
								//countdown[i]=min_countdown;//+1;//un ultimo salto
								
								}
							//else if (e>16) countdown[i]=min_countdown+1;
							
							
							//y2=y1p;
							//y2=y1p;//+(y2-y1p)/(1-countdown[i]);
							int umbral_error=16;
							if (y2>y1p+umbral_error || y2<y1p-umbral_error) 
							{
								//if (y2>y1p) y2=y2+2;//potencio para compensar down al final del mov
								//else if (y2<y1p) y2=y2-2;
							}
							//else y2=y1p;
							
							
						    if (countdown[i]==-10) 
								if (y2>y1p+umbral_error || y2<y1p-umbral_error) 
								{
								//YUV[component][i]=(int)y5p;;//esto es el dy
								
									//countdown[i]=min_countdown+1;//correcciones
								//countdown[i]=0;
								}
							//countdown[i]=min_countdown+1;//correcciones
							//radical
							//y1p=y2;//como si ya hubiese llegado. no permite refinar
							//countdown[i]=min_countdown;
						}
						}
						
						
						float y5=128;
						//primer tramo
						//if (y1p<5 ) y1p=5;
						//if (y1p==255 ) y1p=254;
						
						float u=2f;
						
					    if (y2<y1p-z)//primer tramo
					    {
					    	float a1=0f;
					    	if (y1p>0) a1=((float)y1p-128f)/(float)(y1p*y1p);
					    	float b1=1f-2f*a1*(float)y1p;
					    	y5=128f;
					    	if (y1p>0) y5=a1*(float)(y2*y2)+b1*(float)y2;
					    	
					    	
					    	float alfa1=(128f-u)/(float)y1p;
					    	//if (y1p==0) alfa1=0;
					    	float beta1=0;
					    	
					    	//alfa1=0.5f;//EXPERIMENTO
					    	//beta1=(128-u)-y1*alfa1;//EXPERIMENTO
					    	
					    	
					    	if (alfa1>1) 
					    		{alfa1=1;
					    		beta1=128-u-y1p;
					    		}
					    	y5=alfa1*(float)y2+beta1;
					    	
					    	y5=128-u-alfa1*(y1p-y2);//NUEVO 16/11
					    	
					    	
					    }
					    else if (y2>y1p+z)//segundo tramo
					    {
					    	
					    	float a2=0;
					    	if (y1<255)a2=((float)y1p-128f)/(255f*255f-2f*(float)y1p*255f+(float)(y1p*y1p));
					    	float b2=1f-2f*a2*(float)y1p;
					    	float c2=128f+a2*(float)(y1p*y1p)-(float)y1p;
					    	y5=255;
					    	if (y1p<255) y5=a2*(float)(y2*y2)+b2*(float)y2+c2;
					    	
					    	float alfa2=(128f-u)/(float)(255-y1p);
					    	
					    	//System.out.println("alfa2:"+alfa2);
							//alfa2=0.5f;//EXPERIMENTO
					    	
					    	//if (y1p==255) alfa2=0;
					    	if (alfa2>1) alfa2=1;
					    	y5=128+u+alfa2*(float)(y2-y1p);
					    	
					    }
					    else //y2==y1p
					    {
					    	//no tiene porque ser 128, sino cualquier valor entre 128-u y 128+u
					    	//y5=128f;
					    	
					    	/*if (i>0 && YUV[component][i-1]<128) y5=YUV[component][i-1]+1;
					    	else if (i>0 && YUV[component][i-1]>128) y5=YUV[component][i-1]-1;
					    	else y5=128;
					    	*/
					    	y5=128;
					    }
						//y5=128;
						float y5p=y5;
						if (y5p<=0) y5p=1;
						if (y5p>255) y5p=255;

						
							
						if (countdown[i]==max_countdown){
							//aqui es mejor y5 en lugar de y5p
							YUV[component][i]=(int)y5p;
						}
						else if (countdown[i]>min_countdown){
							YUV[component][i]=(int)y5p;//refinamos
							//suponemos haber acertado pero dejamos que kk2 no sea cero
							//   kk2=(float)Math.abs(y2-y1p)/255f;
							// kk1=1-kk2;
							//sumaf=1f;
							// YUV[component][i]=(int)((kk1*y3p+kk2*y2)/sumaf);
							//if (mask[i]==1) YUV[component][i]=128;
						}
						//YUV[component][i]=(int)((k1*128+k2*y2)/suma);
						else //countdown=0;
						{
							//si usamos y4=y3p
							// asumimos que hemos acertado en y1p y no tratamos de refinar
							//para evitar el efecto de cuadricula
							//YUV[component][i]=(int)((k1*128+k2*y1p)/suma);

							//si usamos k1*y3p+k2*y2 hay que usar otra formula
							
							//esto es vital para no gastar mas info de la necesaria
							//float umbral=16;
						//	if (y5p>128-umbral && y5p<128+umbral)YUV[component][i]=128;
							//else
									
							//deberia dar igual y5p o 128
							//ahora y5p va hacia 128
							
							YUV[component][i]=128;//(int)y5p;//128;//no refinamos. Esto puede ser 128 pero asi es mas general
							//y2menosy1[component][i]=128;
							
							//if (y2>y1p+4)YUV[component][i]=128+4;
							//else if (y2<y1p-4) YUV[component][i]=128-4;
							
							// no se mueve pero el error es grave por degradacionasi que mandamos un dy!=128
							//esto es necesario. hay que poder corregir
							int umbral_error=160000;//16;hop minimo de LHE dividido entre 2
							//if (y2<y1p+umbral_error && y2>y1p-umbral_error)
							if (y2>y1p+umbral_error || y2<y1p-umbral_error) 
								{
								//if (y2>y1p+umbral_error )YUV[component][i]= 128+3;
								//else YUV[component][i]= 128-3;
								YUV[component][i]=(int)y5p;;//esto es el dy
								//countdown[i]=0;
								}
							
							
							
							//si usamos no lineal (k2 depende de y2) el resultado debe ser 128
							//de este modo queda mucho mejor lo que no se mueve
							//YUV[component][i]=128;

							// YUV[component][i]=(int)((kk1p*128+kk2p*y1p)/suma);
						}

//System.out.println(">"+YUV[component][i]);
						//aunque y2 no sea y1, si es igual a y1p podemos enviar 128
						/*
						int umbral_error=5;
						if (y2<=y1p+umbral_error && y2>=y1p-umbral_error) 
							{YUV[component][i]=128;;//esto es el dy
							//System.exit(0);
							}
						*/
						
						//ahora metemos a y2
						
						//System.out.println("encoder dy:"+dy[component][i]);
						
						//YUV[component][i]=(int) (1.0f*y5p+0.0f*y2);
						if (YUV[component][i]<=0) YUV[component][i]=1;
						if (YUV[component][i]>255) YUV[component][i]=255;
						
						//y2menosy1[component][i]=YUV[component][i];
						
						//YUV[component][i]=64;
						//esto no hace nada diferente
						//me gustaria que para lo que se queda muy quieto usasemos otro valor

						//if (countdown[i]==-10 ) {YUV[component][i]=255; countdown[i]=3;}

						//float d=(-countdown[i]+5f)/5f; //desde 1.1 hasta 3
						//int y3px=(int)((y2-y1p)/d+128f);
						//int y4x=(int)(k1*y3px+k2*y2/d)/suma;// siempre y3p
						//if (countdown[i]<0)	YUV[component][i]=y4x;
						//YUV[component][i]=(int)y4;
						//if (mask[i]==1)
							{
							//if (i>0)
							//YUV[component][i]=128;//YUV[component][i-1];//(y1p-y2+256)/2;;
							}

						/*
											if (mask[i]!=1)	YUV[component][i]=(int)y4;
											else {


												YUV[component][i]=(int)((y1-y2+255f)/2f);;
											}
						 */

						//mask[i]=0;

					}
				}


			}//for component
		}
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void filterEstelas(int[][] dif, int[][] ant)
{
	for (int i=0;i<width*height;i++)
	{
		//if (Math.abs(YUV[component][i]-frame[component][i])>16)
		{
			
			int y1=	YUV[0][i];
			
			int y3=(int)Math.abs(dif[0][i]-128);
			
			float f=y3/128f;
			//f=1-f;
			if (i>width && i<width*(height-1))
			{
			float u=ant[0][i-width];
			float l=ant[0][i-1];
			float r=ant[0][i+1];
			float d=ant[0][i+width];
			YUV[0][i]=(int)(y1*(1-f)+(f/4f)*u+(f/4f)*l+(f/4f)*r+(f/4f)*d);
			
			}
        }
	}
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void go128kk()
{
	for (int i=0;i<width*height;i++)
	{
		//if (Math.abs(YUV[component][i]-frame[component][i])>16)
		{
			float u=8;
			if (interpolated_YUV[0][i]>180-u && interpolated_YUV[0][i]<180+u)
				interpolated_YUV[0][i]=128;
		}
	}
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void sumaDownDelta(int[][] delta)
{
	
		for (int i=0;i<width*height;i++)
		{
			//System.out.println("i:"+i+ "  delta:"+delta[0][i]+" y1pp:"+downsampled_YUV[0][i]);
			
			//caso sin regenerar a funcion lineal
			//downsampled_YUV[0][i]=SOLY2[downsampled_YUV[0][i]][delta[0][i]];
			
			//caso ya regenerada
			downsampled_YUV[0][i]=downsampled_YUV[0][i]+delta[0][i]*2-255;
			
			if (downsampled_YUV[0][i]>255) downsampled_YUV[0][i]=255;
			if (downsampled_YUV[0][i]<1) downsampled_YUV[0][i]=1;
		}
	
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

public void computeY2fromDeltaY1pY1pp(int[][] delta,int[][] y1PP)
//computeY2fromDeltaY1pY1pp(fp2.img.interpolated_YUV,fp_y1pp.img.interpolated_YUV);
{
	
	int component=0;
	{
		for (int i=0;i<width*height;i++)
		{
			//if (frame[component][i]!=128)
			{
				//YUV[component][i]=YUV[component][i]+(frame[component][i]*2-256);
				int y1p=interpolated_YUV[component][i];
				int dy=delta[component][i];
				
				int dyant=-1;
				if (i>0) dyant=delta[component][i-1];
				
                int y1pp=y1PP[component][i];
                
				//int y2=y1+2*y4-256;
				//if (y1>=240) y1=240;

				int y2=0;
				//if (y4!=0) 
				//y2=(y1p+2*SOLY2[y1pp][dy])/3;

				y2=SOLY2[y1pp][dy];//y1pp esta comprimida a nivel de dy
				float k=(dy-128);///128f;//mayor cuanto mas se aleje de 128
				if (k>=0) k=k/Math.min((255-dy),128);
				else  k=k/Math.min(128,dy);
				k=1f-k;
				//k=k/2;
				int y3=SOLY2[y1p][dy];
				
				y2=(int)(((1-k)*(float)y2+(k)*y3)/(1));
				if (y2<=0) y2=1;
				if (y2>255) y2=255;
				if (y2==y1pp) y2=SOLY2[y1p][dy];
				//y2=(y1p+4*SOLY2[y1pp][dy])/5;
				//y2=(y3+9*SOLY2[y1pp][dy])/10;
				
				
				//esto funciona
				//y2=SOLY2[y1pp][dy];
				//if (y2==y1pp) y2=SOLY2[y1p][dy];
				
				//otra forma. esta es la mejor por el momento
				/*
				y2=SOLY2[y1p][dy];
				if (y2>y1p+16 || y2<y1p-16) y2=SOLY2[y1pp][dy];//y2=SOLY2[y1pp][dy];//4 es hop1 y 2 por la pendiente max 0.5
				else if (y2>y1p+4 || y2<y1p-4) y2=(SOLY2[y1pp][dy]+SOLY2[y1p][dy])/2;
				*/
				
				y2=SOLY2[y1p][dy];
				float l=(float)Math.abs(y2-y1p)/6f;
				if (l>1) l=1;
				y2=(int)(l*SOLY2[y1pp][dy]+(1-l)*SOLY2[y1p][dy]);
				
				l=(float)Math.abs(y1pp-y1p);
				if (l>32)y2=SOLY2[y1p][dy];
				
				
				y2=SOLY2[y1p][dy];
				l=(float)Math.abs(dy-128f)/8f;
				if (l>1) l=1;
				y2=(int)(l*SOLY2[y1pp][dy]+(1-l)*SOLY2[y1p][dy]);
				
				if (SOLY2[y1p][dy]==y1p) y2=y1p;//medida extra
				
				//----correccion
				float u=2;
				y2=SOLY2[y1p][dy];
				l=(float)Math.abs(dy-128f);
				if (l>u) l=l-u;
				else l=0; // still object
				//l=l/(8f-u);
				if (l>1) l=1;
				y2=(int)(l*SOLY2[y1pp][dy]+(1-l)*SOLY2[y1p][dy]);//transition from still, bright changing, to moving
				//if (l==1) y2=SOLY2[y1pp][dy];
				//else y2=SOLY2[y1p][dy];
				//if (SOLY2[y1p][dy]==y1p) y2=y1p;//medida extra
				
				//perfeccion
				u=2;
				l=(float)Math.abs(dy-128f);
				if (l>u) y2=SOLY2[y1pp][dy];
				else y2=SOLY2[y1p][dy];
				
				
				//suavidad para que se pueda usar countdown=2 en lineal, ya que un emborronamiento es fatal
				u=1;
				l=(float)Math.abs(dy-128f);
				if (l<=u) {
					y2=SOLY2[y1p][dy];
				}
				else
				{
				//y2=SOLY2[y1p][dy];
				//int u2=2;
				//if (y2>y1p-u2 && y2<y1p+u2) y2=SOLY2[y1p][dy];
				//else	y2=SOLY2[y1pp][dy];	
				y2=SOLY2[y1pp][dy];
				
				}
				//l=(float)Math.abs(y1pp-y1p)/32;
				//if (l>1) l=1;
				//y2=(int)((1-l)*SOLY2[y1pp][dy]+(l)*SOLY2[y1p][dy]);
				
				
				//else y2=SOLY2[y1pp][dy];
				/*
				y2=SOLY2[y1pp][dy];//degrada mucho
				y2=(SOLY2[y1pp][dy]+SOLY2[y1p][dy])/2;//degrada menos
				
				y2=SOLY2[y1p][dy];
				//if (y2==y1pp) y2=(SOLY2[y1pp][dy]+SOLY2[y1p][dy])/2;//y2=SOLY2[y1p][dy];//sin cambio
				if (y2==y1pp) y2=y1p;//SOLY2[y1p][dy];//sin cambio
				//else  if (y2<y1pp+0 && y2>y1pp-10) y2=SOLY2[y1p][dy];//y2=(SOLY2[y1pp][dy]+SOLY2[y1p][dy])/2; //cambio medio
				*/
				/*
				//if (2<1) y2=SOLY2[y1p][dy];
				else {
					int y=i/width;
					
					if (i>width && y!=width-1 && y<height-1)
					{
						//deteccion de simple cambio de brillo
					
						if (dy==delta[component][i-1] &&
							dy==delta[component][i-width] 
							//dy==delta[component][i-width-1] //&&
							//dy==delta[component][i+1] 
							//dy==delta[component][i+width] 
							)
							//y2=SOLY2[y1p][dy];
							//y2=(y2+2*SOLY2[y1p][dy])/3;
							
							//y2=(SOLY2[y1pp][dy]+SOLY2[y1p][dy])/2;
						
						y2=SOLY2[y1p][dy];
						//if (dy>dyant-0 && dy<dyant+0)y2=SOLY2[y1p][dy];
						//leve cambio
						//else
					    if (y2<y1pp+5 && y2>y1pp-5) y2=(SOLY2[y1pp][dy]+SOLY2[y1p][dy])/2;;//y2=SOLY2[y1p][dy];
						
					}
				}
				*/
				//y2=SOLY2[y1p][dy];
					//y2=SOLY2[y1pp][dy];
					//if (y2==y1pp)y2=SOLY2[y1p][dy];
				//}
				//SOLY2[y1pp][dy]
				//		y2=(SOLY2[y1pp][dy]+SOLY2[y1p][dy])/2;
				if (y2<0 || y2>255) 
				{
					//System.out.println("computeY2fromY4:   y2 <0 con :    y1:"+y1+"  y4:"+y4+ "   ->y2"+y2);
					System.out.println("y2 sale negativo o mayor que 255");
					System.exit(0);
				}
				//if (y1==255) System.out.println("y1:255"+ " y2:"+y2+"  y4:"+y4);

				///if (y2==210) y2=0;
				//if (y2==0 && y1!=0) System.out.println ("warning: SOLY2[y1][y4]="+SOLY2[y1][y4]+"    y1:"+y1+"   y4:"+y4);


				YUV[component][i]=y2;//YUV[component][i]+2*frame[component][i]-256;
				
				
				//YUV[component][i]=(4*frame[component][i]-512)/2+YUV[component][i];

				if (YUV[component][i]>255) YUV[component][i]=255;
				if (YUV[component][i]<=0) YUV[component][i]=1;
			}

		}


	}//for component	
	
	
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

public void computeY2fromDeltaY1pY1ppV2(int[][] delta_interpol,int[][] y1PP, int[][] llr)
{
	
	int component=0;
	{
		for (int i=0;i<width*height;i++)
		{
			//if (frame[component][i]!=128)
			{
				int y2=0;//creamos la variable
				
				int y1p=interpolated_YUV[component][i];
				int dy=delta_interpol[component][i];
				int y1pp=y1PP[component][i];//ya lleva sumada dy en down y ya esta interpolada
				
				
				int dy1=SOLY2[y1p][dy]-y1p;
				int dy2=SOLY2[y1pp][dy]-y1pp;
				int dy3=y1pp-llr[component][i];
				int dy4=SOLY2[llr[component][i]][dy]-llr[component][i];
				int dy5=SOLY2[y1pp][dy]-y1p;
				
				int dy6=dy-y1pp;//y1pp;//no cambio nada, ya esta bien
				
				//float l=(float)Math.abs(dy2);//esto es dy interpolada tambien, en teoria
				
				//esto es 100% correcto
				//la taza queda en 128. el humo se mantiene
				//dy=dy2;
				//float l=(float)Math.abs(dy6);//esto es dy interpolada tambien, en teoria
				//System.out.println("dy:"+dy);
				
				
				float l=(float)Math.abs(dy6);
				float k=(float)Math.abs(y1pp-y1p);
				
				
				float uk=4;
				float ul=0;
				float ul2=4;
				/*
				if (k<=uk) y2=dy;//resoulcion de y1p y y1pp similar
				else 
				{
				if (k>=l-ul2 && k<=l+ul2) y2=y1p+dy6; //misma resolucion, movimiento leve	
				else if (l<=ul) y2=y1p; //distinta resolucion pero cambio minimo 
				else if (l<=ul2){ //distinta resolucion y cambio medio
					float l2=l/ul2;
					y2=(int)(l2*(float)dy+(1-l2)*(float)(y1p));
					//y2=dy;
				    }
				else y2=dy;//distinta resolucion, gran cambio (movimiento)
				}
				*/
				
				
				float u=0;
				float u2=8;//4;
				if (l<=u)
				{
					y2=y1p;//+dy6;//no hay cambio  Y1pp tiene  baja resolucion muy probable
				}
				//si la diferencia de resolucion es peque�a (y1p==y1pp) usaremos y1pp+dy6
				//pero si es grande tendremos que usar otra cosa
				else if (l<=u2) //leve cambio, quizas aumento de brillo
					{
					float l2=(l-u)/(u2-u);
					//dy ya lleva ypp + dy
					y2=(int)(l2*(float)dy+(1-l2)*(float)(y1p));
					}
					
			    else y2=dy;//y1pp;//+dy;//y1pp tiene buena resolucion, se esta moviendo
				
				
			
				
				if (k<=0) {
					
					//vamos a corregir , puede que hayamos cogido comb lineal y no hace falta
					
					if (dy!=y1p+dy6) {
						//System.out.println("mierda");System.exit(0);
					}
					y2=dy;//haya cambio de brillo o movimiento, y1pp tiene la misma reslucion que y1p seguramente
					//y2=y1p+dy6;
				}
				else if (k<=4)
				{
					//float l2=(k)/(8);
					//dy ya lleva ypp + dy
					//y2=(int)(l2*(float)(y1p+dy6)+(1-l2)*(float)(dy));
					
					//y2=y1p+dy6;
					// float l2=(k-u)/(u2-u);
					//l2=k/4;
					//y2=(int)(l*(float)y1pp+(1-l)*(float)(y1p+dy6));
					//dy ya lleva ypp + dy
					//y2=(int)(l2*(float)dy+(1-l2)*(float)(y1p+dy6));
                    //y2=(int)(l2*(float)dy+(1-l2)*(float)(y1p+dy6));
					
				}
				//else y2=dy;
				
				//y2=(int)(l*(float)dy+(1-l)*(float)(y1p));
				//
				
				//y2=y1p+dy6;
				//y2=dy;
				
				//y2=(int)(l*(float)dy+(1-l)*(float)(y1p));
				//if (l<2) y2=y1p;
				//else y2=dy;
				//y2=dy;//y1pp;
				/*
				else 
				{
					y2=y1pp;//y1pp;//hay movimiento
				}
				*/
				/*
				if (l>u) 
					{
					//igualamos a la y1pp= down(y1p)+delta+up
					
					y2=y1pp;//SOLY2[y1pp][dy];//ya tiene sumada la delta
					//y2=(int)((float)y1pp+(float)y1p)/2;
					float  umbral_brillo=-1000;
					float  umbral_brillo2=8;
					float  umbral_brillo3=64;
					//l=l-u;// ESTO ES NUEVO. ASI PARA L=3 tenemos l=1/8 y 1-l=0
					if (l <=umbral_brillo)
					 {
						
						l=l/32;//umbral_brillo;
						if (l>1) l=1;
						dy2+=128;
						if (dy2<0) dy2=0;
						if (dy2>255) dy2=255;
						
						y2=(int)(l*(float)y1pp+(1-l)*(float)SOLY2[y1p][dy2]);
						y2=(y1pp+SOLY2[y1p][dy2])/2;
						//y2=SOLY2[y1p][dy2];//+y1pp)/2;
						y2=y1p;//(y1pp+y1p)/2;
						y2=(int)(l*(float)y1pp+(1-l)*(float)(y1p+dy3));
						y2=(int)(l*(float)y1pp+(1-l)*(float)(y1p));
						//y2=(int)((float)y1pp+(float)(y1p+dy3))/2;
						//y2=(int)((float)y1pp+(float)y1p)/2;
						//y2=y1p+dy3;
						//dy3+=128;
						//if (dy3<0) dy3=0;
						//if (dy3>255) dy3=255;
						//y2=(int)((l)*(float)y1pp+(1-l)*(float)SOLY2[y1p][dy3]);
						
						//y2=SOLY2[y1p][dy3];
						//y2=(y1pp+y1p)/2;
						//y2=y1pp;
					 }
					else if (l<umbral_brillo2)
					{
						l=l-2; //ya hecho
						if (l<0) l=0;
						l=l/umbral_brillo2;
						if (l>1) l=1;
					    //l=(float)Math.sqrt(l);
						y2=(int)(l*(float)y1pp+(1-l)*(float)(y1p));
						//y2=(int)((float)y1pp+(float)y1p)/2;
						//System.out.println("alerta");
						y2=y1p+dy3;
					}
					else if (l<-umbral_brillo3)
					{
						l=l-2; //ya hecho
						if (l<0) l=0;
						l=l/umbral_brillo3;
						if (l>1) l=1;
					    //l=(float)Math.sqrt(l);
						y2=(int)(l*(float)y1pp+(1-l)*(float)(y1p));
						//y2=(int)((float)y1pp+(float)y1p)/2;
						//System.out.println("alerta");
						//y2=y1p+dy3;
					}
					}
				
				
				else 
					
				{
					y2=y1p;//SOLY2[y1p][dy];//valdria y1p
				}
				//y2=(y1pp+y1p)/2;
				//dy no existe, no ha sido interpolada
				//y2=SOLY2[y1p][dy];
				//y2=y1p;
				
				if (y2<0 || y2>255) 
				{
					//System.out.println("computeY2fromY4:   y2 <0 con :    y1:"+y1+"  y4:"+y4+ "   ->y2"+y2);
					//System.out.println("y2 sale negativo o mayor que 255");
					//System.exit(0);
				}
			*/
				YUV[component][i]=y2;//YUV[component][i]+2*frame[component][i]-256;
				
			
				if (YUV[component][i]>255) YUV[component][i]=255;
				if (YUV[component][i]<=0) YUV[component][i]=1;
			}

		}


	}//for component	
	

}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void regeneraDownDelta(int[][] y1PPdown)
{
	int component=0;
	{
		for (int i=0;i<width*height;i++)
		{
			//if (frame[component][i]!=128)
			{
				
				int dy=downsampled_LHE_YUV[component][i];
				//if (dy==0) dy=1;
				if (dy==0) continue;//zona "muerta" entre bloques downsampleados
				int y1ppdown=y1PPdown[component][i];
				//System.out.println("y1pp:"+y1pp+ "   dy:"+dy);
				
				//int dy2=SOLY2[y1pp][dy]-y1pp;//
				
				int dy2=SOLY2[y1ppdown][dy];// nuevo y2, no es delta es y1pp con el delta sumado
				
				if (dy2<1) dy2=1;
				if (dy2>255) dy2=255;
				/*
				dy2=(dy2+255)/2;
				if (dy2<1) dy2=1;
				if (dy2>255) dy2=255;
				*/
				//System.out.println("dy:"+dy2);
				downsampled_LHE_YUV[component][i]=dy2;
				
				
				
			}
		}
    }
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

public void computeY2fromDeltaY1pY1ppV3(int[][] delta_interpol,int[][] y1PP)//, int[][] llr)
{
	//llr no se usa!
	
	int component=0;
	{
		for (int i=0;i<width*height;i++)
		{
			//if (frame[component][i]!=128)
			{
				int y2=0;//creamos la variable
				int y1p=interpolated_YUV[component][i];
				int y1pp=y1PP[component][i];//
				int dy=delta_interpol[component][i]-y1pp;// dy es referenciada a y1pp. es Y2''-y1''
				
				//buscando el bug
				//int y=i/width;
				//int x=i-y*width;
				//if (x==516 && y==374) System.out.println("                                           dy:"+dy);
				//-------
				
				float l=(float)Math.abs(dy);
				float k=(float)Math.abs(y1pp-y1p);
				
				
				float uk=1;//no se usa
				float ul1=1;
				//ul1=2;
				float ul2=6;//4
				
				//y1p y y1pp son la misma imagen a distinta resolucion
				//si y1p==y1pp es que la grid tiene alta resolucion ( la misma o mas que y1p)
				// en ese caso uso y1p+dy o y1pp+dy pero no una combinacion lineal de y1pp+dy + y1p
				
				
				//creo que este coeficiente me va a decir si la suma del dy puede ser destructiva
				//puede ser que la imagen aumente de brillo simplemente o no se mueva. en ese caso es destructiva
				//
				float coeficiente_de_destruccion=k/255f;//si es 1 es que ha pasado de blanco a negro o viceversa
				float cd=coeficiente_de_destruccion;
				//if (k<ul2 && l<ul2) l=ul1;
				if (y1pp> y1p+16) y1pp=y1p+16;//=(int)(y1pp*(1-cd)+ cd*y1p);
				if (y1pp< y1p-16) y1pp=y1p-16;	
				if (k==0) y2=y1p+dy;//igual a y1p+dy  AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
				
				//si k aumenta debe pesar mas y1p
				//else if (coeficiente_de_destruccion>0.25 && l<8) y2=y1p;
				//else //if (1<2) y2=(int)(pesoY1p*y1p+(1-pesoY1p)*(y1pp+dy));
					  //if (k>128) y2=y1p;
					
				//si y1p se parece a y1pp es que la grid tiene algo de menos resolucion. 
				//objeto parandose o incluso quieto o bien cambio de tono (brusco o suave)
				//else if (k<=uk && l<ul2) y2=y1p+dy;
				
				//si y1p es distinto de y1pp la resolucion es diferente. puede ser menor o mayor
				//aumento: si paso de 2 pixels a 3, el pixel medio tendra color mezcla y entonces y1p=y1pp
				//bajada: si paso de 3 pixeles a 2, los nuevos 2 pix tendran una mezcla de extremo y medio
				
				// o bien esta quieto, o bien hay cambio de brillo en zona detallada o cambio de tono importante en zona lisa
				else //y1 muy distinto de y1p, por resolucion o movimiento
				{
					//ul1=2;
					if (l<=ul1) y2=y1p;//no hay cambios y y1pp puede tener baja resolucion (quieto)
					//else if (coeficiente_de_destruccion>0.25f) y2=y1p;//es un cambio destructivo
					else 
					
					
						if (l<=ul2)
					{
						//hay un cambio peque�o. puede ser :
						//  aumento de brillo en zona detallada 
						//  cambio de tono suave en zona suave
						//  movimiento leve
						float l2=(l-ul1)/(ul2-ul1);
						
						 
						
						y2=(int)(l2*(float)(y1pp+dy)+(1-l2)*(float)(y1p));
						//y2=y1p;
					}
					else 
						//gran cambio, 
						// un cambio de tono significativo en zona suave. si uso y1p+dy dejo estelas
						// un movimiento, con aumento de resolucion
						{
						y2=y1pp+dy;
						//y2=delta_interpol[component][i];
						}
					
					
					//y2=y1p+dy;
				}
				//y2=y1p;
				YUV[component][i]=y2;//YUV[component][i]+2*frame[component][i]-256;
				
			
				if (YUV[component][i]>255) YUV[component][i]=255;
				if (YUV[component][i]<=0) YUV[component][i]=1;
			}

		}


	}//for component	
	

}
public void compute_dy(int[][] frame,  int[][] frame_y1, int[] countdown)
{
	//YUVtoBMP("./output_video"+"/Y1.bmp",frame_y1[0]);
	//result is (y-y'+255)/2 belongs to 0..255
	
	int component=0;
	//for (int component=0;component<3;component++)
	{
		for (int i=0;i<width*height;i++)
		{
			//System.out.println ("count:"+countdown[i]);
			//countdown[i]=-3;
			{
				int y1=frame_y1[component][i];
				int y1p=frame[component][i];
				int y2=	YUV[component][i];
				
				
				

				//countdown:
				//cada vez que algo se mueve o cambia, countdown vale lo maximo (N)
				// al quedarse quieto, tras N fotogramas pasa a cero y 
				//dejamos de enviar informacion de refinado. enviamos 128
				int max_countdown=0;//0;
				int min_countdown=-3;//3;//-20000;//-3; un -3 es casi igual que un -2
			    
				//valores de min_count
				// -1: genera estelas. un solo salto no es suficiente para restaurar el color de fondo
				// -2. aceptable, aunque se ve leve estela. quizas con vectores de mov ser�a ideal
				// -3: es mejor que -2 (estela no aparece) pero puede ocurrir que el tercer salto sea destructivo 
				//     debido al fuerte down. Solo es destructivo en zonas detalladas. en fondo liso es mejor
				// -4 : no es mejor que -3 y es algo mas destructivo
				
				//int refresh=-20;// evita errores. aunque es una chapuza 
				
				float z=0;
				//de lo contrario un incremento leve paulatino de brillo sobre
				//una zona quieta no seria percibido
				/*if (y2>y1-1 && y2<y1+1) //ojo miro y1 y no y1p
				{
					countdown[i]=min_countdown+1;// restart countdown 3...2...1
				}
				*/
				
				if (y2<y1-z || y2>y1+z) //ojo miro y1 y no y1p
				{
					//hay movimiento o cambio 
					countdown[i]=max_countdown;// restart countdown 3...2...1
					//if (y2>y1-1 && y2<y1+1) countdown[i]=min_countdown+1; //cambios muy leves solo un refinamiento
				}
				else 
				{//no hay movimiento ni cambio de tono pues y2=y1
					//refinamos un numero finito de veces, hasta que countdown llega al minimo
					int e=y2-y1p;
					if (e<0)e=-e;
					error[i][1]=error[i][0];
					error[i][0]=e;//NO HACEMOS NADA CON ESTO AL FINAL
					
					//NUEVO. dejar de destruir si aumenta el error. Con mario funciona en PSNR pero
					//se genera estela. es mejor seguir refinando. si el video cambia mucho (foreman y otros) es inocuo
					//lo malo es que lo dejamos mal:-(
					//if (e>=error[i][1]) countdown[i]=min_countdown;
					
					
					//probemos la regla inversa. esto genera parpadeos en los bordes que no se mueven
					//if (e>error[i][1]+2) countdown[i]=max_countdown;
					
					//nuevo
					//if (e>error[i][0] && countdown[i]<=min_countdown+1)countdown[i]=min_countdown+2;
					//if (e>4 && countdown[i]<=min_countdown+1)countdown[i]=min_countdown+2;
					//error[i][0]=e;
					//-----
					
					//la cuenta atras es para cada pixel
					if (countdown[i]>min_countdown) countdown[i]--;//min debe ser al menos -2, si es -1 llega ya
					//countdown[i]--;
				    //if (countdown[i]>refresh) countdown[i]--;
					//else  countdown[i]=-1;
				}
				//countdown[i]=-3;
				int cero=128;//con 129 queda peor. y ademas debe ser 128
				
				float y5=cero;
				//primer tramo
				//if (y1p<5 ) y1p=5;
				//if (y1p==255 ) y1p=254;
				
				float u=2f;
				
				
				
			    if (y2<y1p-z)//primer tramo
			    {
			    	//u=u+1;
			    	float alfa1=(128f-u)/(float)(y1p);
			    	if (alfa1>1) alfa1=1; 
			    	//if (alfa1*(y1p-y2)<0) y5=128;
			    	//else 
			    	y5=cero-u-alfa1*(y1p-y2);//NUEVO 16/11
			    
			         
			    	
			    }
			    else if (y2>y1p+z)//segundo tramo ( es decir tercer tramo, pues el segundo es vertical)
			    {
			    	
			    	
			    	
			    	float alfa2=(128f-u)/(float)(255-y1p);
			    	if (alfa2>1) alfa2=1;
			    	//if (alfa2*(float)(y2-y1p)<0) y5=128;
			    	//else 
			    	y5=cero+u+alfa2*(float)(y2-y1p);
			    	
			    }
			    else //y2==y1p
			    {
			    	
			    	y5=cero;
			    	// y de paso paro el countdown
			    	//countdown[i]=min_countdown; //no quiero que se estropee despues :esto no mejora
			    	
			    }
				//y5=128;
			    //y5p es dy, es decir y5
			    
				float y5p=y5;
				if (y5p<=0) y5p=1;
				if (y5p>255) y5p=255;

				
				if (countdown[i]>min_countdown)// 2,1 y ya esta
				{
					YUV[component][i]=(int)y5p;//refinamos
					
				}
				else //countdown=0;
				{
					//pongo 129 porque con 128 a veces se oscurece algo paulatinamente.
					//debe haber un microbug en cuanto a lo que es el h1/2 hacia abajo pero con 129 esta resuelto
					//no he descubierto por que pasa esto. conceptualmente no hay problema
					YUV[component][i]=129;
					//YUV[component][i]=128;//(int)y5p;//128;//no refinamos. Esto puede ser 128 pero asi es mas general
					//YUV[component][i]=(int)y5p;
				}


				if (YUV[component][i]<=0) YUV[component][i]=1;
				else if (YUV[component][i]>255) YUV[component][i]=255;
				
				

			}
		}


	}//for component
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
void initY3mejorada()
{


	SOLY2=new int[256][256];
	//ponemos todo a -1
	for (int y1=0;y1<=255;y1++)
	{
		for (int y4=0;y4<=255;y4++)
		{
			SOLY2[y1][y4]=-1;
		}
	}
	float u=2;
	//ahora rellenamos
	for (int y1=0;y1<=255;y1++)
	{
		
		//ecuaciones cuadraticas
		//----------------------
		/*
		float a1=0;
		if (y1>0) a1= ((float)y1-128f)/(float)(y1*y1);
		float b1=1f-2f*a1*(float)y1;
		
		float a2=100;
		if (y1<255) a2=((float)y1-128f)/(255f*255f-2f*(float)y1*255f+(float)(y1*y1));
		float b2=1f-2f*a2*(float)y1;
		float c2=128f+a2*(float)(y1*y1)-(float)y1;
		*/
		//ecuaciones lineales
		//------------------------
		float alfa1=(128f-u)/(float)y1;
		//float beta1=0;
		
		
		float alfa2=(128f-u)/(float)(255f-y1);
		
		
		if (alfa1>1) 
			{alfa1=1;
			//falta beta1
			//beta1=(128-u)-y1;
			}
		if (alfa2>1) 
			{alfa2=1;
			
			}
		
		//System.out.println("y1:"+y1+"   alfa1:"+alfa1+"   alfa2:"+alfa2);
		
		for (int y4=0;y4<=255;y4++)
		{

			//for each Y4 value, search the y2 value
			
			
			//1er tramo
			//==========
				if (y4<128-u) //esto no lo puedo hacer asi si voy a sumar
				{	
					
				//busco entre todos los valores posibles de y2 el correcto	
				//for (int y2=0;y2<=y1;y2++)
					for (int y2=y1;y2>=0;y2--)
				{
					
					int  y5=0;
					//if (y1>0) 
						//y5=(int)(0.5f+a1*y2*y2+b1*y2);
					
						//y5=(int) (alfa1*y2 +beta1);
						
						
						y5=(int)(128-u-alfa1*(y1-y2));//NUEVO 16/11
						//if (y5<0) y5=0;//EXPERIMENTO
						
						//metemos a y2
						//y5=(int) (1.0f*(alfa1*y2 +beta1)+0.0f*y2);
						
						
					//if (y2==y1) System.out.println("y1:"+y1+" y4:"+y5);
					
					//if ( (int)y5==y4 )
						if ( y5==y4 )
					{
						//cazado! 
						
						
						SOLY2[y1][y4]= y2;
						//SOLY2[y1][(int)(1.0f*y4+0.0f*y2)]=y2;	
						//if (SOLY2[y1][y4]<=0) SOLY2[y1][y4]=1;
						//else if (SOLY2[y1][y4]>255) SOLY2[y1][y4]=255;
						
						break;//salimos del for, solo se puede asignar un valor
					}
					
						
						
						
				}//for y2
				
				/*if (!found)
				{
				System.out.println("y1:"+y1+",y4:"+y4+"  anterior:"+anterior+"   posterior:"+posterior);
				if (!found && anterior!=-1) SOLY2[y1][y4]=anterior;
				else if (!found && posterior!=-1) SOLY2[y1][y4]=posterior;
				}
				*/
				}
				
				
				//2nd tramo
				//===================
				
				else if (y4>128+u)//y4>128 2nd tramo
				{
					for (int y2=y1;y2<=255;y2++)
					{
					int y5=255;
							
						//y5=(int)(0.5f+a2*y2*y2+b2*y2+c2);
					
						
							//y5=128;
						
						y5=(int)(128f+u+alfa2*(y2-y1));
						
						//if (y5>255) y5=255;//EXPERIMENTO
					
						//metemos a y2
						//y5=(int) (1.0f*(128f+alfa2*(y2-y1))+0.0f*y2);
						//System.out.println("y1:"+y1+" y2:"+y2+"  y5:"+y5);
					//if (1<2) System.exit(0);

					//if ( (int)y5==y4)
						if ( y5==y4)
					{
						SOLY2[y1][y4]=y2;
						//SOLY2[y1][(int)(1.0f*y4+0.0f*y2)]=y2;
						//if (SOLY2[y1][y4]<=0) SOLY2[y1][y4]=1;
						//else if (SOLY2[y1][y4]>255) SOLY2[y1][y4]=255;
						break;
					}
							
				   }//end for y2
				}//else
				else
				{
					//tramo recto desde 128-u hasta 128+u, extremos incluidos
					//no necesito recorrer y2.
					SOLY2[y1][y4]=y1;
					
					//System.out.println("puta   y4:"+y4);
					//System.exit(0);
					//metemos a y2, que es igual que y1
					//SOLY2[y1][y4]=(int) (0.5f*y1+0.5f*y1);
				}

			
		}//y4
	}//y1

	//check si todo esta relleno
	
	for (int y1=0;y1<=255;y1++)
	{
		int current=-1;
		
		//para cada y4 (=dy)establecemos el primer valor valido
		//---------------------------------------------------			
		for (int j=128;j<=255;j++)
		  {	if (SOLY2[y1][j]!=-1) 
		      {
			  current =SOLY2[y1][j];
			  //System.out.println(" current y4 es "+j+"   da y2="+current);
			  break;
		      }
		  }
		//current=0;
		//ya tenemos el primer valor valido. ahora recorremos todo y4
		//-----------------------------------------------------------
		for (int y4=128;y4<=255;y4++)
		{
		
			//set de correcciones por si acaso
			//if (y4==128) SOLY2[y1][y4]=y1;//correccion
			
			//if (y4>=128-u && y4<=128+u) SOLY2[y1][y4]=y1;//correccion
			
			//if (y1>=250 && y4>=128)	SOLY2[y1][y4]=y1;//correccion
			//if (y1<=10 && y4<=128)    SOLY2[y1][y4]=y1;
			
			
			//SOLY2[y1][y4]=y1;
			//if (y4==128)SOLY2[y1][y4]=y1;
			
				
			if (SOLY2[y1][y4]!=-1) current=SOLY2[y1][y4];
			//System.out.println(" y1:"+y1+"  y4:"+y4+"   >"+SOLY2[y1][y4]);
			
			if (SOLY2[y1][y4]==-1)
			{
			//System.out.println(" y1:"+y1+"  y4:"+y4+"   >"+SOLY2[y1][y4]);
			//System.exit(0);
			
				SOLY2[y1][y4]=current;
				//System.out.println(" y1:"+y1+"  y4:"+y4+"   >"+SOLY2[y1][y4]);
			}//for
		//if (y1==254)	System.out.println(" y1:"+y1+"  y4:"+y4+"   >"+SOLY2[y1][y4]);
			
			
			if (SOLY2[(int)y1][(int)y4]<=0) SOLY2[(int)y1][(int)y4]=1;
			else if (SOLY2[(int)y1][(int)y4]>255) SOLY2[(int)y1][(int)y4]=255;
		}
		
	}
	//y ahora el otro tramo en negativo
	
	for (int y1=0;y1<=255;y1++)
	{
		int current=-1;
		
		//para cada y4 (=dy)establecemos el primer valor valido
		//---------------------------------------------------			
		for (int j=128;j>=0;j--)
		  {	if (SOLY2[y1][j]!=-1) 
		      {
			  current =SOLY2[y1][j];
			  //System.out.println(" current y4 es "+j+"   da y2="+current);
			  break;
		      }
		  }
		//current=0;
		//ya tenemos el primer valor valido. ahora recorremos todo y4
		//-----------------------------------------------------------
		for (int y4=128;y4>=0;y4--)
		{
		
			//set de correcciones por si acaso
			//if (y4==128) SOLY2[y1][y4]=y1;//correccion
			
			//if (y4>=128-u && y4<=128+u) SOLY2[y1][y4]=y1;//correccion
			
			//if (y1>=250 && y4>=128)	SOLY2[y1][y4]=y1;//correccion
			//if (y1<=10 && y4<=128)    SOLY2[y1][y4]=y1;
			
			
			//SOLY2[y1][y4]=y1;
			//if (y4==128)SOLY2[y1][y4]=y1;
			
				
			if (SOLY2[y1][y4]!=-1) current=SOLY2[y1][y4];
			//System.out.println(" y1:"+y1+"  y4:"+y4+"   >"+SOLY2[y1][y4]);
			
			if (SOLY2[y1][y4]==-1)
			{
			//System.out.println(" y1:"+y1+"  y4:"+y4+"   >"+SOLY2[y1][y4]);
			//System.exit(0);
			
				SOLY2[y1][y4]=current;
				//System.out.println(" y1:"+y1+"  y4:"+y4+"   >"+SOLY2[y1][y4]);
			}//for
		//if (y1==254)	System.out.println(" y1:"+y1+"  y4:"+y4+"   >"+SOLY2[y1][y4]);
			
			
			if (SOLY2[(int)y1][(int)y4]<=0) SOLY2[(int)y1][(int)y4]=1;
			else if (SOLY2[(int)y1][(int)y4]>255) SOLY2[(int)y1][(int)y4]=255;
		}
		
	}
	//y esto siempre
	for (int y1=0;y1<=255;y1++)
	{
		SOLY2[y1][128]=y1;
	}
//if (1<2) System.exit(0);
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%	
public void setMinCountdown()
{
	for (int i=0;i< width*height;i++)
	{
		countdown[i]=-3;
	}
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void filtersoft(int [] pix, int []hops)
{
	System.out.println(" filtering soft...");
	//int[] tmp=new int[width*height];
	
	for (int y=1;y<height-1;y++)
		for (int x=1;x<width-1;x++)
		{
			//if (1<2) continue;
			//if (hops[y*width+x]>=1 && hops[y*width+x]<=7) {
			//if (hops[y*width+x]<=3 || hops[y*width+x]>=5) {
			//if (hops[y*width+x]!=4) {
			//if (hops[y*width+x]==4) {
			//if (hops[y*width+x]<=1 || hops[y*width+x]>=7) {
			
			//if (hops[y*width+x]<=2 || hops[y*width+x]>=6) {
				//razonamiento: asi el brillo del borde se mantiene y lo que se filtra es lo de alrededor
				
			
			//if (hops[y*width+x]==4 || hops[y*width+x+1]!=4) {
			//if (y%2==0)
			//{
			//if (hops[y*width+x]==4 ){//|| hops[y*width+x+1]!=4) {
				
			
			//if (
			//	(hops[y*width+x]==4 && hops[y*width+x+1]>=6) ||
			 //   (hops[y*width+x]==4 && hops[y*width+x+1]<=2)
			  //  )
			    //||    (hops[y*width+x]>=6) || (hops[y*width+x]<=2)) //no vale
				
			//con mario si filtro no consigo mejorar, sino empeorar
			//en caso de filtrar lo que era 4, no empeora aunque tampoco mejora.
			
			//if (hops[y*width+x]==4) // si el anterior era un 4, era zona lisa. con mario es lo mejor
			//if (hops[y*width+x]!=4) // si el anterior no es 4, hay que filtrarlo para eliminarlo mejor
			//en el video ice, filtrando cuando !=4 da 2653
			//en el video ice sin filtrar da 2826
			//en ice filtrando cuando =4 da 2653 tambien
				
			//if ((hops[y*width+x]!=4) || (hops[y*width+x+1]!=4))	
			//en ice filtrando cuando !=4 o el siguiente !=4 da 2653 tambien
				
			//conclusion:
			// como no tengo la info del frame actual, no puedo filtrar segun movimiento
			// por eso solo puedo filtrar indiscriminadamente
			// filtrar indiscriminadamente hace da�o en las partes que no se mueven
			// el da�p puede ser superior al beneficio. eso ocurre en mario, aunque no en los demas
				
			{
				//asi solo filtro los bordes
				
			//if (hops[y*width+x]==4 && hops[y*width+x+1]!=4) {	
				//continue;
			//}
			
			//if (y>410) continue;
			//int a=pix[y*width+x];
			//pix[y*width+x]=(pix[y*width+x-1]+pix[y*width+x+1]+pix[(y-1)*width+x]+pix[(y+1)*width+x]+pix[y*width+x])/5;
			
			pix[y*width+x]=(pix[y*width+x-1]+pix[y*width+x+1]+pix[(y-1)*width+x]+pix[(y+1)*width+x]+pix[y*width+x])/5;
			
			//pix[y*width+x]=(pix[(y)*width+x+1]+pix[y*width+x])/2;//+pix[(y-1)*width+x+1])/2;
			
			//pix[y*width+x]=(pix[(y+1)*width+x-1]+pix[(y+1)*width+x+1]+pix[(y-1)*width+x-1]+pix[(y-1)*width+x+1]+pix[y*width+x])/5;
			//pix[y*width+x]=(pix[(y+1)*width+x+1]+pix[y*width+x-1])/2;
			//pix[y*width+x]=(pix[y*width+x-1]+pix[y*width+x+1]+pix[(y-1)*width+x]+pix[(y+1)*width+x]+pix[y*width+x]+pix[(y+1)*width+x-1]+pix[(y+1)*width+x+1]+pix[(y-1)*width+x-1]+pix[(y-1)*width+x+1])/9;
			
			//if (Math.abs(a-pix[y*width+x])>12)pix[y*width+x]=128;
			
			//if (pix[y*width+x]<0) pix[y*width+x]=0;
			//if (pix[y*width+x]>255) pix[y*width+x]=255;
			
			//pix[y*width+x]=(pix[y*width+x-1]+pix[y*width+x])/2;
			//pix[y*width+x]=(pix[y*width+x-1]+pix[y*width+x]+pix[(y-1)*width+x])/3;
		}
		}//for
	/*
	for (int y=1;y<height-1;y++)
		for (int x=1;x<width-1;x++)
		{
			pix[y*width+x]=tmp[y*width+x];
		}
	*/
	
	
}


public void computedif(int[] orig, int[] degradada)
{
	int tramo1_ini=24;//24;//32;//32;//32;//48;//32;//32
	int tramo2_ini=186;//24;//186;//162;//160;//114;//160;//160
	
	int tramo1=tramo1_ini;
	int tramo2=tramo2_ini;
	
	dif=new int[width*height]; //entero positivo o negativo
	for (int y=0;y<height;y++)
	for (int x=0;x<width;x++)
	{	
		
		//esta division genera un ruido de cuantizacion visible
		//dif[y*width+x]=(orig[y*width+x]- degradada[y*width+x])/2+128;
		//if (1==1) continue;
		
		//esto es mejor. 3 tramos
		int valordif=orig[y*width+x]- degradada[y*width+x];
		
		
		
		/*
		int umbral_mov=10;
		if (Math.abs(valordif)>umbral_mov &&  y>0 && y<height-1 && x>0 && x<width-1) 
		{
			degradada[y*width+x]=(degradada[y*width+x]+degradada[y*width+x-1]+degradada[y*width+x+1])/3;//+degradada[(y-1)*width+x]+degradada[(y+1)*width+x])/5;
			valordif=orig[y*width+x]- degradada[y*width+x];
		}
		*/
		
		int signo=1;
		
		if (valordif<0) {signo=-1; valordif=-valordif;}
		//3 tramos
		//--------
		
		//no degradar mas de lo necesario
		tramo1=tramo1_ini;//32;
		tramo2=tramo2_ini;//160;
		
		
				
		
		//if (( degradada[y*width+x]>=128 && signo==1)) {tramo1=128;tramo2=255;} 
		//if (( degradada[y*width+x]<=128 && signo==-1)) {tramo1=128;tramo2=255;}
		
		//esto es una mejora opcional--------------------------
		//tramo1=(int)(128-(valordif/128f -1f)*96f);
		//tramo2=(int)(255-(valordif/128f -1f)*96f);
		
		{	
				
		  if (valordif<tramo1)//primeros 
		  {
			//dif[y*width+x]=valordif;
		  }
		  else if (valordif<tramo2) // con 64 cubre hasta 32+32*2= 96
		  {
			  valordif=valordif-tramo1;
			  valordif=(tramo1+valordif/2);
		  }
		  else 
		  {
			  valordif=valordif-tramo2;
		      valordif=(tramo1+(tramo2-tramo1)/2+valordif/3);
		  }
		  
		  valordif=signo*valordif+128;
		  if (valordif<0) valordif=0;
		  if (valordif>255) valordif=255;
		}
		
		  dif[y*width+x]=valordif;
		  
		  
	}//for
		
		
	
	
	
}

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void sumadif(int[] A, int[] B, int[] C)
{
	
	int tramo1_ini=24;//24;//32;//32;//32;//48;//32;//32
	int tramo2_ini=186;//186;//186;//162;//160;//114;//160;//160
	
	int tramo1=tramo1_ini;
	int tramo2=tramo2_ini;

	
	//supongo que estoy sumando algo de tipo diferencia
	for (int y=0;y<height;y++)
		for (int x=0;x<width;x++)
			{
			
			//esto produce ruido
			//C[y*width+x]=A[y*width+x]+2*(B[y*width+x]-128);
			//if (1==1) continue;
			
			//esto es mejor. 3 tramos
			//3 tramos
			//--------
			
			int valordif=	B[y*width+x]-128;
			int signo=1;
			if (valordif<0) {signo=-1; valordif=-valordif;}
			
			//no degradar mas de lo necesario
			tramo1=tramo1_ini;//32;
			tramo2=tramo2_ini;//160;
			//if (( A[y*width+x]>=128 && signo==1)) {tramo1=128;tramo2=255;}
			//if (( A[y*width+x]<=128 && signo==-1)) {tramo1=128;tramo2=255;}
			
			//mejora opcional
			//tramo1=(int)(128-(valordif/128f -1f)*96f);
			//tramo2=(int)(255-(valordif/128f -1f)*96f);
				
			{	
				
			  if (valordif<tramo1)//primeros 
			  {
				//nada que hacer
			  }
			  else if (valordif<tramo2) // con 64 cubre hasta 32+32*2= 96
			  {
				  valordif=valordif-tramo1;
				  valordif=(tramo1+valordif*2);
			  }
			  else 
			  {
				  //valordif=valordif-48;
			      valordif=valordif-tramo2;
				  valordif=(tramo1+(tramo2-tramo1)*2+valordif*3);
			  }
			}
			  valordif=signo*valordif;
			  
			  C[y*width+x]=A[y*width+x]+valordif;
			  
			  //C[y*width+x]=128+valordif;
			 
			
			if (C[y*width+x]<0) C[y*width+x]=0;
			if (C[y*width+x]>255) C[y*width+x]=255;
			}
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void copy(int[] orig, int[] des)
{
	for (int y=0;y<height;y++)
		for (int x=0;x<width;x++)
			{des[y*width+x]=orig[y*width+x];
			}
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


//%%%%%%%%%%%%%%%%%%%%%% COMPRESS DOMAIN IMAGE ANALYSIS %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public  void CDA_edgeDetection_v001(String pathImagen, int hop_threshold, boolean black) {

		img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
		for (int y=0;y<height;y++)  {
			for (int x=0;x<width;x++)  {

				//the set of formulas must be coherent with formulas used for RGB->YUV
				int i=x+(y)*width;
				/*
				int red=component[i];//+(1402*(V[i]-128))/1000;
				int green=component[i];//- (334*(U[i]-128)-714*(V[i]-128))/1000;
				int blue=component[i];//+(177*(U[i]-128))/1000;
				 */
				int k=30;// 30*4=120--> 128+120=248
				int offset=128;
				int hop=hops[0][i]-4;
				
				if (black) 
					{hop=Math.abs(hop);
				     k=60;
				     offset=0;
					}
				
				if (Math.abs(hop)<hop_threshold) hop=0;
				int red =offset+k*(hop);
				int green=red;
				int blue=red;
				
				int rgb=red+green*256+blue*65536;
				img.setRGB(x, y, rgb);

			}//x
		}//y
		
		//BufferedImage buff_c=intToImg(component);
		saveBufferedImage(pathImagen, img);
		
		//return img;
	}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public  void CDA_blur_v001(String pathImagen, int hop_threshold) {

			img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
			for (int y=0;y<height;y++)  {
				for (int x=0;x<width;x++)  {

					//the set of formulas must be coherent with formulas used for RGB->YUV
					int i=x+(y)*width;
					
					
					int lum =LHE_YUV[0][i];
					
					/*
					int red=LHE_YUV[0][i];//+(1402*(V[i]-128))/1000;
					int green=LHE_YUV[0][i];//- (334*(U[i]-128)-714*(V[i]-128))/1000;
					int blue=LHE_YUV[0][i];//+(177*(U[i]-128))/1000;
					 */
					
					
					int hop=hops[0][i]-4;
					hop=Math.abs(hop);
					if (Math.abs(hop)>=hop_threshold) 
						{
						if (x>0 && y>0)
							lum=(lum+LHE_YUV[0][i-1]+LHE_YUV[0][i-width])/3;
						
						}
					
					int red =lum;
					int green=lum;
					int blue=lum;
					
					int rgb=red+green*256+blue*65536;
					img.setRGB(x, y, rgb);

				}//x
			}//y
			
			//BufferedImage buff_c=intToImg(component);
			saveBufferedImage(pathImagen, img);			
			//return img;
 }	
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public void CDA_sharpen_v001(String pathImagen, int hop_threshold) {

		int counter=0;
		img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
		for (int y=0;y<height;y++)  {
			for (int x=0;x<width;x++)  {
				
				int i=x+(y)*width;
				
				
				int lum =LHE_YUV[0][i];
				
				
				int hop=hops[0][i]-4;
				//hop=Math.abs(hop);
				if (Math.abs(hop)>=hop_threshold) 
					{
					if (x>0 && y>0)
						//lum=(lum+LHE_YUV[0][i-1]+LHE_YUV[0][i-width])/3;
					{
						if (hop>0) {lum=lum+(int)(lum*0.4f);if (lum>255) lum=255;}
						if (hop<0) {lum=lum -(int)(lum*0.4f);if (lum<0) lum=0;}
						
						lum=(lum+ LHE_YUV[0][i])/2;
						counter++;
						
					}
					}
				
				int red =lum;
				int green=red;
				int blue=red;
				
				int rgb=red+green*256+blue*65536;
				img.setRGB(x, y, rgb);
				
				
			}//x
			}//y
		saveBufferedImage(pathImagen, img);
		System.out.println("counter: "+counter);
}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		public void CDA_identity_v001(String pathImagen) {

			int counter=0;
			img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
			for (int y=0;y<height;y++)  {
				for (int x=0;x<width;x++)  {
					
					int i=x+(y)*width;
					
					
					int lum =LHE_YUV[0][i];
					
					
					
					int red =lum;
					int green=red;
					int blue=red;
					
					int rgb=red+green*256+blue*65536;
					img.setRGB(x, y, rgb);
					
					
				}//x
				}//y
			saveBufferedImage(pathImagen, img);
			System.out.println("counter: "+counter);
	}
		
		
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void CDA_denoise_v001(String pathImagen, int hop_threshold) {

			int counter=0;
			
			int[] lum_buffer= new int[width*height];
			float[] kernel={1f,1f,1f,
							1f,1f,1f,
							1f,1f,1f};
					
			img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
			for (int y=0;y<height;y++)  {
				for (int x=0;x<width;x++)  {
					
					int i=x+(y)*width;
					
					lum_buffer[i]=LHE_YUV[0][i];
					//int lum =LHE_YUV[0][i];
					int lum=lum_buffer[i];
					
					
					
					if (x>0 && y>0 && y <height-1 && x<width-1)
					{	
						
					//int last_hop=hops[0][i-1]-4;
					int hop=hops[0][i]-4;
					//int next_hop=hops[0][i+1]-4;
					
					if (Math.abs(hop)>=4)
						{
							counter++;
							//lum =(lum_buffer[i-1]+lum_buffer[i-width+1])/2;
							lum =selective_Kernel(x,y,kernel,-3,3);
							
							
							lum_buffer[i]=lum;
							
						}
						
					
						
					}
					int red =lum;
					int green=red;
					int blue=red;
					
					int rgb=red+green*256+blue*65536;
					img.setRGB(x, y, rgb);
					
					
				}//x
				}//y
			saveBufferedImage(pathImagen, img);
			//System.out.println("counter: "+counter);
			System.out.println("denoise counter: "+counter+ "  --> "+100*counter/(width*height)+" %");
	}

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public int selective_Kernel(int xo, int yo, float kernel[], int hmin, int hmax)
{	
	int i=0;// kernel index
    int k=0;//counter of summation
    float resultf=0;
	for (int y=yo-1;y<=yo+1;y++)  {
		for (int x=xo-1;x<=xo+1;x++,i++)  {
			int pix=x+(y)*width;// pixel position
			int p=LHE_YUV[0][pix]; //pixel color
			int h=hops[0][pix]-4;//hop assotiated to pixel
			if (h>=hmin && h<=hmax )
			  {
			  resultf=resultf +(float)p*kernel[i];
			  k+=kernel[i];//ocunter of summation
			  }			
		  }//x
		}//y
	if (k>0) resultf=resultf/k;
	//else resultf=LHE_YUV[0][xo+(yo)*width];
	
	if (resultf>255) resultf=255;
	else if (resultf<=0 ) resultf=0;//255;
	
	return (int)resultf;
}

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public void CDA_sharpen_v002(String pathImagen, int hop_threshold) {

		int counter=0;
		float[] kernel=
			   {0f,-1f,0f,
				-1f,5f,-1f,
				0f,-1f,0f};
		
		img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
		for (int y=1;y<height-1;y++)  {
			for (int x=1;x<width-1;x++)  {
				
				int i=x+(y)*width;
				
				
				int lum =LHE_YUV[0][i];
				
				
				int hop=hops[0][i]-4;
				//hop=Math.abs(hop);
				if (Math.abs(hop)>=hop_threshold) 
					{
						lum =selective_Kernel(x,y,kernel,-4,4);
						counter++;
					}
				
				int red =lum;
				int green=red;
				int blue=red;
				
				int rgb=red+green*256+blue*65536;
				img.setRGB(x, y, rgb);
				
				
			}//x
			}//y
		saveBufferedImage(pathImagen, img);
		System.out.println("sharpen counter: "+counter+ "  --> "+100*counter/(width*height)+" %");
}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public  void CDA_blur_v002(String pathImagen, int hop_threshold) {

		
		int counter=0;
		float[] kernel=
			   {1f,1f,1f,
				1f,1f,1f,
				1f,1f,1f};
		
		img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
		for (int y=1;y<height-1;y++)  {
			for (int x=1;x<width-1;x++)  {

				//the set of formulas must be coherent with formulas used for RGB->YUV
				int i=x+(y)*width;
				
				
				int lum =LHE_YUV[0][i];
			
				
				
				int hop=hops[0][i]-4;
				hop=Math.abs(hop);
				if (Math.abs(hop)>=hop_threshold) 
					{
					if (x>0 && y>0)
						//lum=(lum+LHE_YUV[0][i-1]+LHE_YUV[0][i-width])/3;
					lum=selective_Kernel(x,y,kernel,-4,4);
					    counter++;
					}
				
				int red =lum;
				int green=lum;
				int blue=lum;
				
				int rgb=red+green*256+blue*65536;
				img.setRGB(x, y, rgb);

			}//x
		}//y
		
		//BufferedImage buff_c=intToImg(component);
		saveBufferedImage(pathImagen, img);			
		//return img;
		System.out.println("blur counter: "+counter+ "  --> "+100*counter/(width*height)+" %");
}	

	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public  void CDA_beauty_v001(String pathImagen, int hop_threshold) {

		
		int counter=0;
		float[] kernel=
			   {2f,1f,2f,
				1f, 0f,1f,
				2f,1f,2f};
		
		float[] kernelsharp=
			   {0f,-1f,0f,
				-1f,5f,-1f,
				0f,-1f,0f};
		img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
		for (int y=1;y<height-1;y++)  {
			for (int x=1;x<width-1;x++)  {

				//the set of formulas must be coherent with formulas used for RGB->YUV
				int i=x+(y)*width;
				
				
				int lum =LHE_YUV[0][i];
			
				
				
				int hop=hops[0][i]-4;
				hop=Math.abs(hop);
				if (Math.abs(hop)>=hop_threshold) 
					{
					//if (x>0 && y>0)
						//lum=(lum+LHE_YUV[0][i-1]+LHE_YUV[0][i-width])/3;
					//lum=selective_Kernel(x,y,kernel,-1,1);
					//selective_Kernel(x,y,kernelsharp,-4,4);
					    //counter++;
					}
				else {
					lum=selective_Kernel(x,y,kernel,-4,4);
					counter++;
				}
				
				int red =lum;
				int green=lum;
				int blue=lum;
				
				int rgb=red+green*256+blue*65536;
				img.setRGB(x, y, rgb);

			}//x
		}//y
		
		//BufferedImage buff_c=intToImg(component);
		saveBufferedImage(pathImagen, img);			
		//return img;
		System.out.println("clean counter: "+counter+ "  --> "+100*counter/(width*height)+" %");
}		
	
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public void CDA_emboss_v001(String pathImagen, int hop_threshold) {

		int counter=0;
		float[] /*kernel=
			   {0f,0f,0f,
				0f,5f,0f,
				0f,0f,-4f};
		*/
		 kernel=
			   { 0f,0f,0f,
				 0f,5f,0f,
				 0f,0f,-4f};
		
		
		img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
		for (int y=1;y<height-1;y++)  {
			for (int x=1;x<width-1;x++)  {
				
				int i=x+(y)*width;
				
				
				int lum =LHE_YUV[0][i];
				
				
				int hop=hops[0][i]-4;
				hop=Math.abs(hop);
				if (Math.abs(hop)>=hop_threshold) 
					{
						lum =selective_Kernel(x,y,kernel,-4,4);
						counter++;
					}
				
				
				int red =lum;
				int green=red;
				int blue=red;
				
				int rgb=red+green*256+blue*65536;
				img.setRGB(x, y, rgb);
				
				
			}//x
			}//y
		saveBufferedImage(pathImagen, img);
		System.out.println("emboss counter: "+counter+ "  --> "+100*counter/(width*height)+" %");
}
	
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%	
	public void CDA_emboss_v002(String pathImagen,int hop_threshold) {

		int counter=0;
		
		int inc=10;
		img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
		for (int y=1;y<height-1;y++)  {
			for (int x=1;x<width-1;x++)  {
				
				int i=x+(y)*width;
				
				
				int lum =LHE_YUV[0][i];
				
				
				int lum_ant=lum; 
				
				
				int hop=hops[0][i]-4;
				
				if (hop>=2) lum=255;
				//if (hop==1) lum=225;//128;//192;
				
				//if (hop==-1) lum=32;//64;
				
				lum=128 +hop*hop*10;
				if (Math.abs(hop)>0)
				lum=(lum+LHE_YUV[0][i])/2;
				else lum=LHE_YUV[0][i];
				
				if (Math.abs(hop)>=0) lum=LHE_YUV[0][i]+lum;
				else lum=LHE_YUV[0][i];
				/*
				if (hop>0)
				lum=-2*LHE_YUV[0][i]/(Math.abs(hop));
				else if (hop<0)
				lum=2*LHE_YUV[0][i]/(Math.abs(hop));
				else 
				*/	
				hop=hops[0][i]-4;
				lum=LHE_YUV[0][i];
				if (Math.abs(hop)>1)
				lum=(LHE_YUV[0][i]*hop)+(LHE_YUV[0][i]);	
				
				if (lum>255) lum=255;
				if (lum<0) lum=0;
				int red =lum;
				int green=red;
				int blue=red;
				
				int rgb=red+green*256+blue*65536;
				img.setRGB(x, y, rgb);
				
				
			}//x
			}//y
		
		
		saveBufferedImage(pathImagen, img);
		System.out.println("emboss2 counter: "+counter+ "  --> "+100*counter/(width*height)+" %");
		
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void interpol_edgedetection(String pathImagen)
{
	int counter=0;
	img= new BufferedImage (width*2,height*2,BufferedImage.TYPE_INT_RGB);
	for (int y=0;y<height*2;y++)  {
		for (int x=0;x<width*2;x++)  {
			
			int x2=x/2;
			int y2=y/2;
			int i2=x2+(y2)*width;
			
			int lum =0;//LHE_YUV[0][i2];
			int hop=0;//hops[0][i2]-4;
			
			if (x%2==0 && y%2==0)
			{
				lum =LHE_YUV[0][i2];
				hop=hops[0][i2]-4;
			}
			else
			
			if ( x/2>0 && x/2<width-1 && y/2>0 && y/2<height-1)
				{
			//diagonal	
				if ( x%2!=0 && y%2!=0){
			    int a=LHE_YUV[0][i2];
				int b=LHE_YUV[0][i2+1];
				int c=LHE_YUV[0][i2+width];
				int d=LHE_YUV[0][i2+1+width];
				
				int difad=Math.abs(a-d);
				int difbc=Math.abs(b-c);
				if (difad>=difbc) lum=(b+c)/2;
				else lum=(a+d)/2;
				//lum=0;
				
			  }
			  else
			  //	
			  {
				    int a=LHE_YUV[0][i2];
					int b=LHE_YUV[0][i2+1];
					int c=LHE_YUV[0][i2+width];
					int d=LHE_YUV[0][i2];
					
					int difac=Math.abs(a-c);
					int difbd=Math.abs(b-d);
					if (difac>=difbd) lum=(b+d)/2;
					else   lum=(a+c)/2;
					//else lum=(a+b)/2;
					lum=0;
			  }
				
				
			}//else interpol
			if (lum>255) lum=255;
			if (lum<0) lum=0;
			int red =lum;
			int green=red;
			int blue=red;
			
			int rgb=red+green*256+blue*65536;
			img.setRGB(x, y, rgb);	
		}//x
	}//Y
	
	for (int y=0;y<height*2;y++)  {
		for (int x=0;x<width*2;x++)  {
			
			
			

			int lum=getlum(img,x,y);
			
			if ( x>0 && x<width*2-1 && y>0 && y<height*2-1 && lum==0)
				
			  
			  //	
			  {
				    int a=getlum(img,x,y-1);
					int b=getlum(img,x+1,y);
					int c=getlum(img,x,y+1);
					int d=getlum(img,x-1,y);
					
					int difac=Math.abs(a-c);
					int difbd=Math.abs(b-d);
					if (difac>=difbd) lum=(b+d)/2;
					else   lum=(a+c)/2;
					//else lum=(a+b)/2;
					//lum=0;
			  //}
				
				
			//}//else interpol
			if (lum>255) lum=255;
			if (lum<0) lum=0;
			int red =lum;
			int green=red;
			int blue=red;
			
			int rgb=red+green*256+blue*65536;
			img.setRGB(x, y, rgb);
			  }
		}//x
	}//Y
	
	saveBufferedImage(pathImagen, img);
}
public int getlum(BufferedImage img,int x, int y)
{
	int c=img.getRGB(x, y);

	int lum=(c & 0x00ff0000) >> 16;
    //int green=(c & 0x0000ff00) >> 8;
    //int blue=(c & 0x000000ff);
return lum;	
	
	
}

public void CDVA_PRimage_v001(Grid grid, String pathImagen){
	img= new BufferedImage (33,33,BufferedImage.TYPE_INT_RGB);
	for (int y=0;y<33;y++)  {
		for (int x=0;x<33;x++)  {
	
			
			int k=25;// 30*4=120--> 128+120=248
			int offset=0;
			
			//PR varia entre 0.0 y 1, siendo los valores 0.0 - 0.125 - 0.25 - 0.5 - 1.0
			float pr=10*(grid.prbl[y][x].PRx+grid.prbl[y][x].PRy)/2.0f;
			
			//float pr=10*(Math.max(grid.prbl[y][x].PRx,grid.prbl[y][x].PRy));
			
			
			//System.out.println ("PR:"+grid.prbl[y][x].PRx);
			
			int red =offset+k*(int)pr;
			
			if (red>255) red=255;
			if (red<0) red=0;		
			
			int green=red;
			int blue=red;
			
			
			int rgb=red+green*256+blue*65536;
			img.setRGB(x, y, rgb);
			
		}
	}
	saveBufferedImage(pathImagen, img);
}


public void CDVA_PRdif_image_v001(String pathImagen, String pathReferenceImagen,String pathResultImagen){
	
	loadImageToBufferedImage(pathReferenceImagen);
	
	BufferedImage img_ref=null;
	BufferedImage img=null;
	try {	
		img_ref = ImageIO.read(new File(pathReferenceImagen));
	} catch (IOException e) {
		System.out.println("error loading image");
		System.exit(0);
	}
	
	
	try {	
		img = ImageIO.read(new File(pathImagen));
	} catch (IOException e) {
		System.out.println("error loading image");
		System.exit(0);
	}
	
	//imgToInt();//width & height calculado
	BufferedImage img_result= new BufferedImage (33,33,BufferedImage.TYPE_INT_RGB);
	
	for (int y=0;y<33;y++)  {
		for (int x=0;x<33;x++)  {
	
			
			int c=img_ref.getRGB(x, y);

			int red=(c & 0x00ff0000) >> 16;
	  	    int green=(c & 0x0000ff00) >> 8;
		    int blue=(c & 0x000000ff);
			

		    int c2=img.getRGB(x, y);

			int red2=(c2 & 0x00ff0000) >> 16;
	  	    int green2=(c2 & 0x0000ff00) >> 8;
		    int blue2=(c2 & 0x000000ff);

		    if (red2<red) red2=red;
		    
		    int red3= 128+(red2-red)/2;
		    int green3=red3;
			int blue3=red3;	
		    
		    
			
			
			int rgb=red3+green3*256+blue3*65536;
			img_result.setRGB(x, y, rgb);
			
		}
	}
	saveBufferedImage(pathResultImagen, img_result);
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void RGBtoYCD(String pathImagen)
{
	System.out.println("loading: "+pathImagen);
	loadImageToBufferedImage(pathImagen);
	
	System.out.println ("imagen cargada");
	
	imgToYCD();

	//for (int i=0;i<10;i++) System.out.println("Y[i]:"+Y[i]+" , U[i]:"+U[i]+" ,V[i]"+V[i] );

}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void RGBto8(String pathImagen)
{
	System.out.println("loading: "+pathImagen);
	loadImageToBufferedImage(pathImagen);
	
	System.out.println ("imagen cargada");
	
	imgTo8();
	//imgTo8v002();

	//for (int i=0;i<10;i++) System.out.println("Y[i]:"+Y[i]+" , U[i]:"+U[i]+" ,V[i]"+V[i] );

}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void RGBto8v002(String pathImagen)
{
	System.out.println("loading: "+pathImagen);
	loadImageToBufferedImage(pathImagen);
	
	System.out.println ("imagen cargada");
	
	//imgTo8();
	//imgTo8v002();
	//imgTo8v003();
	imgTo8v004();
	//imgTo8v005();
	//for (int i=0;i<10;i++) System.out.println("Y[i]:"+Y[i]+" , U[i]:"+U[i]+" ,V[i]"+V[i] );

}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%
private void imgToYCD() {
   
	System.out.println ("enter in img to YCD");
	width=img.getWidth(); 
	height=img.getHeight(); 

	YCD=new int[3][width*height];
	
	int r_old=0;
	int g_old=0;
	int b_old=0;
	
	//this bucle converts BufferedImage object ( which is "img") into YUV array (luminance and chrominance)
	int i=0;
	for (int y=0;y<height;y++)  {
		for (int x=0;x<width;x++)  {
		
			
		int c=img.getRGB(x, y);

		int r=(c & 0x00ff0000) >> 16;
		int g=(c & 0x0000ff00) >> 8;
		int b=(c & 0x000000ff);

		//identical formulas used in JPEG . model YCbCr (not pure YUV)
		YCD[0][i]=(r+g+b)/3; //lumminance [0..255]
		/*
		if (x%3==0)	{YCD[1][i]=128+(red - r_old)/2;r_old=r_old+(YCD[1][i]-128)/2;}//g_old=3*YCD[0][i]-r_old-b_old;b_old=3*YCD[0][i]-r_old-g_old;}
		if (x%3==1)	{YCD[1][i]=128+(green - g_old)/2;g_old=green;r_old=3*YCD[0][i]-g_old-b_old;g_old=3*YCD[0][i]-r_old-g_old;}
		if (x%3==2)	{YCD[1][i]=128+(blue - b_old)/2;b_old=blue;r_old=3*YCD[0][i]-g_old-b_old;g_old=3*YCD[0][i]-r_old-b_old;}
*/
		
		
		if (x%3==0)	{
			YCD[1][i]=(r-r_old)/2+128;
			r_old=(YCD[1][i]-128)*2+r_old;
			
			YCD[1][i]=r;
			r_old=r;
			int d=(3*YCD[0][i]-r_old-g_old-b_old ); //desviacion a repartir
			float f= (float)(g_old)/(float)(g_old+b_old+1);
			g_old=g_old+(int)((float)d*f);
			b_old=b_old+(int)((float)d*(1-f));
			//g_old=g_old+d/2; 
			//b_old=b_old+d/2;
		
		}
		if (x%3==1)	{
			YCD[1][i]=(g-g_old)/2+128;
			g_old=(YCD[1][i]-128)*2+g_old;
			
			YCD[1][i]=g;
			g_old=g;
		
		int d=(3*YCD[0][i]-r_old-g_old-b_old )/2; //desviacion a repartir
		r_old=r_old+d; 
		b_old=b_old+d;}
		
		if (x%3==2)	{
			YCD[1][i]=(b-b_old)/2+128;
			b_old=(YCD[1][i]-128)*2+b_old;
			
			YCD[1][i]=b;
			b_old=b;
		int d=(3*YCD[0][i]-r_old-g_old-b_old )/2; //desviacion a repartir
		
		g_old=g_old+d; 
		r_old=r_old+d;}
        
		
		if (YCD[0][i]>255) YCD[0][i]=255;
		if (YCD[1][i]>255)  YCD[1][i]=255;
		if (YCD[2][i]>255)  YCD[2][i]=255;

		if (YCD[0][i]<0) YCD[0][i]=0;
		if (YCD[1][i]<0)  YCD[1][i]=0;
		if (YCD[2][i]<0)  YCD[2][i]=0;
		
		if (r_old>255) r_old=255;
		if (g_old>255) g_old=255;
		if (b_old>255) b_old=255;
		
		if (r_old<0) r_old=0;
		if (g_old<0) g_old=0;
		if (b_old<0) b_old=0;
		
		//ahora la D (8bit)
		if (x%3==0)	{YCD[2][i]=r;}
		if (x%3==1)	{YCD[2][i]=g;}
		if (x%3==2)	{YCD[2][i]=b;}
		
		
		//r_old=r;
		//g_old=g;
		//b_old=b;
		
		//YCD[0][i]=red;
		//YCD[1][i]=green;
		//YCD[2][i]=blue;

		i++;	
		}
	}

	System.out.println ("convertida a YCD");
}	

//*******************************************************************************
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
private void imgTo8() {
 
	System.out.println ("enter in img to YCD");
	width=img.getWidth(); 
	height=img.getHeight(); 

	YCD=new int[3][width*height];
	
	int r_old=0;
	int g_old=0;
	int b_old=0;
	
	//this bucle converts BufferedImage object ( which is "img") into YUV array (luminance and chrominance)
	int i=0;
	for (int y=0;y<height;y++)  {
		for (int x=0;x<width;x++)  {
		
			
		int c=img.getRGB(x, y);

		int r=(c & 0x00ff0000) >> 16;
		int g=(c & 0x0000ff00) >> 8;
		int b=(c & 0x000000ff);

		//identical formulas used in JPEG . model YCbCr (not pure YUV)
		YCD[0][i]=(r+g+b)/3; //lumminance [0..255]
		
		//ahora la D (8bit)
		//if (y%3==0)
		 {
		 if (x%3==0)	{YCD[2][i]=r;}
		 if (x%3==1)	{YCD[2][i]=g;}
		 if (x%3==2)	{YCD[2][i]=b;}
		 }
		/* 
		if (y%3==1)
		 {
		 if (x%3==1)	{YCD[2][i]=r;}
		 if (x%3==2)	{YCD[2][i]=g;}
		 if (x%3==0)	{YCD[2][i]=b;}
		 }
		if (y%3==2)
		 {
		 if (x%3==2)	{YCD[2][i]=r;}
		 if (x%3==0)	{YCD[2][i]=g;}
		 if (x%3==1)	{YCD[2][i]=b;}
		 }
		*/
		
		//r_old=r;
		//g_old=g;
		//b_old=b;
		
		//YCD[0][i]=red;
		//YCD[1][i]=green;
		//YCD[2][i]=blue;

		i++;	
		}
	}

	System.out.println ("convertida a YCD");
}	

//*******************************************************************************
public void cache_sumas()
{
	//cache_sumas
		cs=new int[255];
		
		int r=0;
		int g=0;
		int b=0;
		
		int k=0;
		cs[0]=0;
		k++;
		for (r=2;r<65;r=r*2)
			for (g=2;g<65;g=g*2)
				for (b=2;b<65;b=b*2)
				
				{
					if (k>255) break;
					int rgb=b+g*256+r*65536;
					
					
					cs[k]=rgb; k++;
					System.out.println("cs["+k+"]="+r+","+g+","+b+ "    "+Math.log((double)r));
			
				}
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
private void imgTo8v002() {

	System.out.println ("enter in img to YCD v002");
	width=img.getWidth(); 
	height=img.getHeight(); 

	YCD=new int[3][width*height];
	
	
				
				
	int r_old=0;
	int g_old=0;
	int b_old=0;
	
	//this bucle converts BufferedImage object ( which is "img") into YUV array (luminance and chrominance)
	int i=0;
	for (int y=0;y<height;y++)  {
		for (int x=0;x<width;x++)  {
		
			
		int c=img.getRGB(x, y);

		 int r=(c & 0x00ff0000) >> 16;
		 int g=(c & 0x0000ff00) >> 8;
		 int b=(c & 0x000000ff);

		//identical formulas used in JPEG . model YCbCr (not pure YUV)
		//YCD[0][i]=(r+g+b)/3; //lumminance [0..255]
		
		//ahora la D (8bit)
		//if (y%3==0)
		 {
		 if (x%3==0)	{YCD[2][i]=(r+g)/2;}
		 if (x%3==1)	{YCD[2][i]=(g+b)/2;}
		 if (x%3==2)	{YCD[2][i]=(b+r)/2;}
		 }
		/* 
		if (y%3==1)
		 {
		 if (x%3==1)	{YCD[2][i]=r;}
		 if (x%3==2)	{YCD[2][i]=g;}
		 if (x%3==0)	{YCD[2][i]=b;}
		 }
		if (y%3==2)
		 {
		 if (x%3==2)	{YCD[2][i]=r;}
		 if (x%3==0)	{YCD[2][i]=g;}
		 if (x%3==1)	{YCD[2][i]=b;}
		 }
		*/
		
		//r_old=r;
		//g_old=g;
		//b_old=b;
		
		//YCD[0][i]=red;
		//YCD[1][i]=green;
		//YCD[2][i]=blue;

		i++;	
		}
	}

	System.out.println ("convertida a YCD");
}	

//*******************************************************************************


//*******************************************************************************

	public void YCDtoBMP(String pathImagen, int[][] ycd)
	{
		System.out.println ("enter in YCD to BMP");
		//save image component Only one YUV component) in BMP format

		BufferedImage buff_c=YCDtoImg(ycd);
		saveBufferedImage(pathImagen, buff_c);

	}
	
	//*******************************************************************************
		public void YCD8toBMP(String pathImagen, int[][] ycd)
		{
			System.out.println ("enter in YCD to BMP");
			//save image component Only one YUV component) in BMP format

			BufferedImage buff_c=YCD8toImg(ycd);
			saveBufferedImage(pathImagen, buff_c);

		}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		//*******************************************************************************
				public void YCD8toBMP_v002(String pathImagen, int[][] ycd)
				{
					System.out.println ("enter in YCD to BMP");
					//save image component Only one YUV component) in BMP format

					//BufferedImage buff_c=YCD8toImgv002(ycd);
					//BufferedImage buff_c=YCD8toImgv003(ycd);
					BufferedImage buff_c=YCD8toImgv004(ycd);
					//BufferedImage buff_c=YCD8toImgv005(ycd);
					saveBufferedImage(pathImagen, buff_c);

				}
			//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	private BufferedImage YCDtoImg(int[][] ycd) {

		System.out.println ("enter in YCD to img");
			int r=0,g=0,b=0,yp=0;
			int old_g=0,old_r=0,old_b=0;
			
			img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
			for (int y=0;y<height;y++)  {
				for (int x=0;x<width;x++)  {

					//the set of formulas must be coherent with formulas used for RGB->YUV
					int i=x+(y)*width;
					yp=ycd[0][i];
					if (x%3==0)	{
						//r=(ycd[1][i]-128)*2+r;
						
						
						r=ycd[1][i];
						int d=(3*yp-r-g-b ); //desviacion a repartir
						
						
						
						g=g+d/2; 
						b=b+d/2;
						//float f= (float)g/(float)(g+b+1);
						//g=g+(int)((float)d*f); 
						//b=b+(int)((float)d*(1-f));
						
						
					}
					
					if (x%3==1)	{
						//g=(ycd[1][i]-128)*2+g;
						g=ycd[1][i];
						int d=(3*yp-r-g-b ); //desviacion a repartir
						
						
						r=r+d/2; 
						b=b+d/2;
						
						
						
					}
					if (x%3==2)	{
						//b=(ycd[1][i]-128)*2+b;
						
						b=ycd[1][i];
						int d=(3*yp-r-g-b ); //desviacion a repartir
						
						g=g+d/2; 
						r=r+d/2;
						
						
					}
					if (r>255) r=255;
					if (g>255) g=255;
					if (b>255) b=255;
					if (r<0) r=0;
					if (g<0) g=0;
					if (b<0) b=0;
					old_r=r;
					old_g=g;
					old_b=b;
					
					//g=0;
					//b=0;
					//r=ycd[0][i];
					//g=ycd[1][i];
					//b=ycd[2][i];
					
					int rgb=b+g*256+r*65536;
					img.setRGB(x, y, rgb);

				}//x
			}//y
			
			System.out.println ("exit from  YCD to img");
			return img;
		}	
	
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		private BufferedImage YCD8toImg(int[][] ycd) {

			System.out.println ("enter in YCD to img");
				int r=0,g=0,b=0,yp=0;
				int old_g=0,old_r=0,old_b=0;
				int d=0;
				int old_d=0;
				img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
				for (int y=0;y<height;y++)  {
					for (int x=0;x<width;x++)  {

						//the set of formulas must be coherent with formulas used for RGB->YUV
						int i=x+(y)*width;
						yp=ycd[0][i];
						
						//if (y%3==0)
						 {
						 if (x%3==0)	{d=ycd[2][i]-r;r=	ycd[2][i];
						 				
						 				//d=d/2;
						 				float f=(float)g/(float)(g+b+1);
						 				g=g+(int)(f*d);
						 				b=b+(int)((1-f)*d);
						 				//g=g+3*d/4;
						 				//b=b+d/4;
						 				
						 				}
						 
						 if (x%3==1)	{d=ycd[2][i]-g;g=	ycd[2][i];
						 			
						 				float f=(float)r/(float)(r+b+1);
						 				r=r+(int)(f*d);
						 				b=b+(int)((1-f)*d);
						 				
						 				//r=r+3*d/6;
						 				//b=b+3*d/6;
			 							}
			 
						 
						 if (x%3==2)	{d=ycd[2][i]-b;b=	ycd[2][i];
						 				
						 				//d=d/2;
						 				float f=(float)r/(float)(r+g+1);
						 				r=r+(int)(f*d);
						 				g=g+(int)((1-f)*d);
						 				//r=r+d/3;
						 				//g=g+2*d/3;
											
						 				}
						 }	
					
						if (y%3==5)
						 {
						 if (x%3==1)	r=	ycd[2][i];
						 if (x%3==2)	g=	ycd[2][i];
						 if (x%3==0)	b=	ycd[2][i];
						 }	
						if (y%3==6)
						 {
						 if (x%3==2)	r=	ycd[2][i];
						 if (x%3==0)	g=	ycd[2][i];
						 if (x%3==1)	b=	ycd[2][i];
						 }	
						
						
						
						if (r>255) r=255;
						if (g>255) g=255;
						if (b>255) b=255;
						if (r<0) r=0;
						if (g<0) g=0;
						if (b<0) b=0;
						old_r=r;
						old_g=g;
						old_b=b;
						
						//g=0;
						//b=0;
						//r=ycd[0][i];
						//g=ycd[1][i];
						//b=ycd[2][i];
						yp=r+g+b;
						old_d=d;
						
						int rgb=b+g*256+r*65536;
						img.setRGB(x, y, rgb);

					}//x
				}//y
				
				System.out.println ("exit from  YCD to img");
				return img;
			}		
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
private BufferedImage YCD8toImgv002(int[][] ycd) {

					System.out.println ("enter in YCD to img 8 v002");
						int r=0,g=0,b=0,yp=0;
						int old_g=0,old_r=0,old_b=0;
						int d=0;
						int old_d=0;
						img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
						
						r=ycd[2][0];
						b=r;
						g=r;
						int lum=r+g+b;
						int gradb=0,gradr=0,gradg=0;		
						for (int y=0;y<height;y++)  {
							for (int x=0;x<width;x++)  {

								//the set of formulas must be coherent with formulas used for RGB->YUV
								int i=x+(y)*width;
								
								
								//if (y%3==0)
								 {
								 if (x%3==0)	{
								 				
								 				
									 			old_r=r;
									 			old_g=g;
									 			
									 			
									 			d=ycd[2][i]*2-(r+g);
									 			r=r+2*d/5;
									 			g=g+3*d/5;
									 			
									 			
									 			//float f=(float)r/(float)(g+r+1);
								 				//r=r+(int)(f*d);
								 				//g=g+(int)((1-f)*d);
									 			//b=b+d/5;
									 			/*
									 			r=r+2*d/5;
									 			g=g+3*d/5;
									 			b=b+d/5;
									 			*/
									 			if (x>=2)
									 			{
									 				 int c=img.getRGB(x-1, y);
									 				 int ra=(c & 0x00ff0000) >> 16;
									 				 int ga=(c & 0x0000ff00) >> 8;
									 				 int ba=(c & 0x000000ff);
									 				 ga=(g+old_g)/2;
									 				if (ga>255) ga=255;
													if (ga<0) ga=0;
													
									 				 int rgb=ba+ga*256+ra*65536;
													 img.setRGB(x-1, y, rgb);
									 				
									 			}
									 			
								 				}
								 
								 if (x%3==1)	{
									 			
									 			d=ycd[2][i]*2-(g+b);
									 			
									 			
												old_g=g;
												old_b=b;
									 			g=g+d/2;
									 			b=b+d/2;
									 			
									 			
									 			if (x>=2)
									 			{
									 				 int c=img.getRGB(x-1, y);
									 				 int ra=(c & 0x00ff0000) >> 16;
									 				 int ga=(c & 0x0000ff00) >> 8;
									 				 int ba=(c & 0x000000ff);
									 				 ba=(b+old_b)/2;
									 				if (ba>255) ba=255;
													if (ba<0) ba=0;
													
									 				 int rgb=ba+ga*256+ra*65536;
													 img.setRGB(x-1, y, rgb);
									 				
									 			}
									 			
									 			/*
									 			g=g+d/2;
									 			b=b+d/2;
									 			r=r+d/6;
									 			*/
									 			//r=r+d/5;
					 							}
					 
								 
								 if (x%3==2)	{
									 			d=ycd[2][i]*2-(r+b);
									 			
									 			old_r=r;
												
												old_b=b;
												
									 			r=r+3*d/5;
									 			b=b+2*d/5;
									 				
									 			/*
									 			r=r+3*d/5;
									 			b=b+2*d/5;
									 			g=g+d/2;
									 			*/
									 			
									 			//g=g+d/5;
									 			
									 			
									 			if (x>=2)
									 			{
									 				 int c=img.getRGB(x-1, y);
									 				 int ra=(c & 0x00ff0000) >> 16;
									 				 int ga=(c & 0x0000ff00) >> 8;
									 				 int ba=(c & 0x000000ff);
									 				 ra=(r+old_r)/2;
									 				if (ra>255) ra=255;
													if (ra<0) ra=0;
													 int rgb=ba+ga*256+ra*65536;
													 img.setRGB(x-1, y, rgb);
									 				
									 			}
									 			}
								 }	
							
								if (y%3==5)
								 {
								 if (x%3==1)	r=	ycd[2][i];
								 if (x%3==2)	g=	ycd[2][i];
								 if (x%3==0)	b=	ycd[2][i];
								 }	
								if (y%3==6)
								 {
								 if (x%3==2)	r=	ycd[2][i];
								 if (x%3==0)	g=	ycd[2][i];
								 if (x%3==1)	b=	ycd[2][i];
								 }	
								
								
								
								if (r>255) r=255;
								if (g>255) g=255;
								if (b>255) b=255;
								if (r<0) r=0;
								if (g<0) g=0;
								if (b<0) b=0;
								
								
								//g=0;
								//b=0;
								//r=ycd[0][i];
								//g=ycd[1][i];
								//b=ycd[2][i];
								//yp=r+g+b;
								//old_d=d;
								
								//lum=(3*r+6*g+2*b);
								
								int rgb=b+g*256+r*65536;
								img.setRGB(x, y, rgb);

							}//x
						}//y
						
						System.out.println ("exit from  YCD to img");
						return img;
					}		

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
private void imgTo8v003() {

  System.out.println ("enter in img to YCD v003");
  width=img.getWidth(); 
  height=img.getHeight(); 

  YCD=new int[3][width*height];
								
  int r_old=0;
  int g_old=0;
  int b_old=0;

  int v=0;
  int v_old=0;

  int i=0;
  int paleta=0;
  int paleta_old=0;
  int count=0;
  for (int y=0;y<height;y++)  {
    for (int x=0;x<width;x++)  {
	 
    	int c=img.getRGB(x, y);

		int r=(c & 0x00ff0000) >> 16;
		int g=(c & 0x0000ff00) >> 8;
		int b=(c & 0x000000ff);

		paleta= ((g & 128)+    ((r & 128)/2) + ((b & 128)/4)+
				  ((g & 64)/4)+ ((r & 64)/8) +  ((b & 64)/16) 
				 ); 
				/*
				 *255 +
				 
				(g & 32)*4+    ((r & 32)*2) + ((b & 32))+
				  ((g & 16))+ ((r & 16)/2) +  ((b & 16)/4) ;
				 */ 
				  
		
		if ((paleta!=paleta_old) || false)
		{
			
			
			//v=(r & 0b11000000)+ ((g & 0b11000000)>>2) + ((b & 0b11000000)>>4);
			
			v=128+(
			   (g & 128)+    ((r & 128)/2) + ((b & 128)/4)+
			  ((g & 64)/4)+ ((r & 64)/8) +  ((b & 64)/16)
			  )/2;
			  v=v+(g &32)/32;
		
			  count=0;
		}
		else if (count==0)//paleta se mantiene
		{
		
			count=1;
			
			
			v=
			   (g & 32)*2+ ((r & 32)) + ((b & 32)/2)+
			   (g & 16)/2+ ((r & 16)/4) + ((b & 16)/8)+
			   (g & 8)/8; 
			   //(b & 16)/32;
			
			//v=(r+g)/(4);
			
			
		}
		
		else if (count==1)
		{
			v=
					   (g & 8)*8+ ((r & 8)*4) + ((b & 8)*2)+
					   (g & 4)*2+ ((r & 4)) + ((b & 4)/2)+
					   (g & 2)/2; 
					
			//count=0;
			//v=(g+b)/(4);
			v=
					   (g & 32)*2+ ((r & 32)) + ((b & 32)/2)+
					   (g & 16)/2+ ((r & 16)/4) + ((b & 16)/8)+
					   (r & 8)/8; 
					
			count=0;
			
		}
		paleta_old=paleta;    
		//i=x+(y)*width;
		YCD[2][i]=v;
        v_old=v;
        i++;
		
		}
  }

  System.out.println ("convertida a YCD");
}	
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
private BufferedImage YCD8toImgv003(int[][] ycd) {

System.out.println ("enter in YCD to img 8 v003");
int r=0,g=0,b=0,yp=0;
int g_old=0,r_old=0,b_old=0;
int d=0;
int old_d=0;
img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
						
r=0;
b=r;
g=r;
int lum=r+g+b;
int gradb=0,gradr=0,gradg=0;

int v=0;
//int paleta_old=0;
int paleta=0;

int count=0;
int ap=0;
int bp=0;
int cp=0;

  for (int y=0;y<height;y++)  {
	  for (int x=0;x<width;x++)  {
		  
		  int i=x+(y)*width;
		  v= ycd[2][i];
    	  paleta=v & 128;
		  
		  if ((paleta==128) || false)
		  {
			//if (paleta!=paleta_old)
			r=0;
			g=0;
			b=0;
			  
			//    g=g & 0b00111111;
			//	r=r & 0b00111111;
			//	b=b & 0b00111111;
				
				//System.out.println("grb  "+g+"  "+r+"  "+b);
			
			
		    g=g+(v & 64)*2;   //128
		    r=r+(v & 32)*2*2;
		    b=b+(v & 16)*4*2;
			
		    //System.out.println("grb  "+g+"  "+r+"  "+b);
		    //System.exit(0);
		    
		    g=g+(v & 8)*4*2; //64
		    r=r+(v &  4)*8*2;
		    b=b+(v &  2)*16*2;
		    
		    g=g+(v & 1)*32;  //32
		    //r=r+(v &  1)*32; //32;
		    
		    r=r | 0b00011111;
		    g=g | 0b00001111;
		    b=b | 0b00011111;
		    
		    //r=(r+(r_old & 0b00111111));
		    //g=(g+(g_old & 0b00111111));
		    //b=(b+(b_old & 0b00111111));
		   count=0;
		   ap++;
		  }
		  else if (count==0)
		  {
			count =1;  
			  /*  
			
				int dif=(r+g)-v*2;
				r=r & 0b11000000;
				g=g & 0b11000000;
				
				r=r | dif/2;
				g=g | dif/2;
			*/
				
				g=g & 0b11000000;
				r=r & 0b11000000;
				b=b & 0b11000000;
			    
				g=g+(v & 64)/2; //32
			    r=r+(v & 32);
			    b=b+(v & 16)*2;
				
			    
			    g=g+(v & 8)*2; //16
			    r=r+(v &  4)*4;
			    b=b+(v &  2)*8;
			    
			    g=g+(v & 1)*8; //16
			        
			    

			    r=r | 0b00000111;
			    g=g | 0b00000011;
			    b=b | 0b00000111;
			    
			  bp++;
			  //r=0;
			   // g=0;
			   // b=0;
		  }
		  else if (count==1)
		  {
			/*  
			     int dif=(b+g)-v*2;
				b=b & 0b11000000;
				g=g & 0b11000000;
				
				b=b | dif/2;
				g=g | dif/2;
			*/
				g=g & 0b11000000;
				r=r & 0b11000000;
				b=b & 0b11000000;
			    
				g=g+(v & 64)/2; //32
			    r=r+(v & 32);
			    b=b+(v & 16)*2;
				
			    
			    g=g+(v & 8)*2; //16
			    r=r+(v &  4)*4;
			    b=b+(v &  2)*8;
			    
			    r=r+(v & 1)*8; //16
			        
			    

			    r=r | 0b00000011;
			    g=g | 0b00000011;
			    b=b | 0b00000111;
			    
			  

			  /*
			    g=g & 0b11110000;
				r=r & 0b11110000;
				b=b & 0b11110000;
			    
				g=g+(v & 64)/8; //8
			    r=r+(v & 32)/4;
			    b=b+(v & 16)/2;
				
			    
			    g=g+(v & 8)/2; //4
			    r=r+(v &  4);
			    b=b+(v &  2)*2;
			    
			    g=g+(v & 1)*2; //2
*/
			    
			    count=0;
			    cp++;
			    
			    //r=0;
			    //g=0;
			    //b=0;
			    
		  }
		  
			if (r>255) r=255;
			if (g>255) g=255;
			if (b>255) b=255;
			if (r<0) r=0;
			if (g<0) g=0;
			if (b<0) b=0;

		  int rgb=b+g*256+r*65536;
		  
		  img.setRGB(x, y, rgb);
		
//		  paleta_old =paleta_old;
		  r_old=r;
		  g_old=g;
		  b_old=b;
		  
		  
    }//x
  }//y
						
   System.out.println ("exit from  YCD to img");
   System.out.println ("a:"+ap+"  b:"+bp+"  c:"+cp);
   return img;
   
   
}		

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//*******************************************************************************



//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
private void imgTo8v004() {

System.out.println ("enter in img to YCD v003");
width=img.getWidth(); 
height=img.getHeight(); 

YCD=new int[3][width*height];
								
int r_old=0;
int g_old=0;
int b_old=0;
int r=0;
int g=0;
int b=0;
int rgb=0;
int rgb_old=0;
int rg=0;
int rg_old=0;
int rb=0;
int rb_old=0;
int gb=0;
int gb_old=0;

int v=0;
int v_old=0;

int i=0;
int paleta=0;
int paleta_old=0;
int count=0;
for (int y=0;y<height;y++)  {
  for (int x=0;x<width;x++)  {
	 
  	int c=img.getRGB(x, y);

		 r=(c & 0x00ff0000) >> 16;
		 g=(c & 0x0000ff00) >> 8;
		 b=(c & 0x000000ff);
		
		rgb=(r+g+b)/3;
		
		rg=(r+g)/2;
		rb=(r+b)/2;
		gb=(g+b)/2;
		
		int d_rgb=Math.abs(rgb-rgb_old);
		int d_r=Math.abs(r-r_old);
		int d_g=Math.abs(g-g_old);
		int d_b=Math.abs(b-b_old);
		int d_rg=Math.abs(rg-rg_old);
		int d_gb=Math.abs(gb-gb_old);
		int d_rb=Math.abs(rb-rb_old);
		
		
		
		//int m=Math.max(d_rgb, Math.max(d_rg, Math.max(d_gb, d_rb)));
		//int m=Math.max(d_rgb, Math.max(d_r, Math.max(d_g, d_b)));
		//int m=Math.min(d_rgb, Math.min(d_r, Math.min(d_g, d_b)));
		//if (m == d_rgb || x==0 || count==0)
		int k=rgb;
		int pot=0;
		int cte=0;
		int rbit=0;
		int gbit=0;
		int bbit=0;
		
		int m=32;
		
		if (r>=128) {cte=128; rbit=1;}
		if (g>=128) {cte=cte+64;gbit=1;}
		if (b>=128) {cte=cte+32;bbit=1;}
		
		//System.out.println("--- "+r+":"+rbit+","+g+":"+gbit+","+b+":"+bbit+"       ");
		//if (x==0) count=0;// 1+y %2;
		if (x==0) count= y%2;
		
		if (count==0){
			v=r;
			
			v=v & 0b01111111;
			v=(v>>2);
			v=cte+v;
			count=1;
			
		}
		else if (count==1){
			v=g;
			v=v & 0b01111111;
			v=(v>>2);
			v=cte+v;
			count=2;
		}
		else 
		{
			v=b;
			v=v & 0b01111111;
			v=(v>>2);
			v=cte+v;
			count=0;
		}
		
		/*
		while (k>1)
		{
			k= k >> 1;
			pot++;
		}
		k=(int)(Math.pow(2,pot));
		
		if (count==0) {v=(int)(((float)k/255f)*r)+k;count=1;}
		else if (count==1) {v=(int)(((float)k/255f)*g)+k;count=2;}
		else if (count==2) {v=(int)(((float)k/255f)*b)+k;count=0;}
		*/
		//System.out.println(" rgb:"+rgb+" k:"+k+"    v:"+v+ "   pot:"+pot);		
		YCD[2][i]=v;
      v_old=v;
      i++;
		
		}
}

System.out.println ("convertida a YCD");
}	
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
private BufferedImage YCD8toImgv004(int[][] ycd) {

System.out.println ("enter in YCD to img 8 v003");
int r=0,g=0,b=0,yp=0;
int g_old=0,r_old=0,b_old=0;
int d=0;
int old_d=0;
img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
						
r=0;
b=r;
g=r;
int lum=r+g+b;
int gradb=0,gradr=0,gradg=0;

int v=0;
//int paleta_old=0;
int paleta=0;

int count=0;
int ap=0;
int bp=0;
int cp=0;


int r_yant=0;
int g_yant=0;
int b_yant=0;

r=128;
g=128;
b=128;

int r_bit=0;
int g_bit=0;
int b_bit=0;

int r_bit_old=0;
int g_bit_old=0;
int b_bit_old=0;

int lum_old=r+g+b;
int ar=0;
int ag=0;
int ab=0;
for (int y=0;y<height;y++)  {
	  for (int x=0;x<width;x++)  {
		  
		  int i=x+(y)*width;
		  v= ycd[2][i];
  	  
		  //v=v & 0b11100000;
		  r_bit= (v & 0b10000000)/128;
		  g_bit= (v & 0b01000000)/64;
		  b_bit= (v & 0b00100000)/32;
		  
		  //System.out.println ("--- "+r_bit+","+g_bit+","+b_bit);
		  
		  
		  //if (x>0 && y>0 && x<width-1){
			  //int c=img.getRGB(x-1, y);
			  	//prediccion
				 //r=(c & 0x00ff0000) >> 16;
				 //g=(c & 0x0000ff00) >> 8;
				 //b=(c & 0x000000ff);
		  //}
			  //r=r+ar/4;
			  //g=g+ag/4;
			  //b=b+ab/4;
		  //System.out.println("ar:"+ar);
		  
		  
		  
		  //r=0;
		  //g=0;
		  //b=0;
		  int brusco=0;
		  //int semibrusco=1;
		  if ((r_bit!=r_bit_old) && (g_bit!=g_bit_old) && (b_bit!=b_bit_old)) brusco=1;
		  
		  
		  
		  int cte=0;
		  int m=4;
		  
		  if (r_bit!=r_bit_old) {if (r_bit==1){r=128+m;}else {r=128-m;}}
		  if (g_bit!=g_bit_old) {if (g_bit==1){g=128+m;}else {g=128-m;}}
		  if (b_bit!=b_bit_old) {if (b_bit==1){b=128+m;}else {b=128-m;}}
		  
		  int max=255;
		  int min=0;

		  
		  
		  //if (r_bit!=r_bit_old && brusco==1 ) {if (r_bit==1){r=max;}else {r=min;}}
		  //if (g_bit!=g_bit_old && brusco==1 ) {if (g_bit==1){g=max;}else {g=min;}}
		  //if (b_bit!=b_bit_old && brusco==1 ) {if (b_bit==1){b=max;}else {b=min;}}
		  
		  //lum=(r+b+g)/3;
		  //if (r_bit!=r_bit_old) {r=128;}
		  //if (g_bit!=g_bit_old) {g=128;}
		  //if (b_bit!=b_bit_old) {b=128;}
		  
		  
		  v= ycd[2][i];
		  v=v & 0b00011111;;
		  v=v << 2;
		  
		  v=v+1;
		  //System.out.println ("v:"+v);
		  
		  if (x==0) count= (y)%2;
		  
		  if (count==0){
			  if (r_bit==1){r=128+v;} else r=v; 
			  count=1;
			  if ( brusco==1 ) {g=r;b=r;}
			  
			//ajuste semibrusco
			  d=r-r_old;
			  if (r_bit==g_bit && r_bit_old==g_bit_old && r_bit !=r_bit_old){g=r;};
			  if (r_bit==b_bit && r_bit_old==b_bit_old && r_bit !=r_bit_old){b=r;};
		  }
		  else if (count==1){
			  if (g_bit==1){g=128+v;} else g=v; 
			  count=2;
			  if ( brusco==1 ) {r=g;b=g;}
			  
			//ajuste semibrusco
			  d=g-g_old;
			  if (r_bit==g_bit && r_bit_old==g_bit_old && g_bit !=g_bit_old){r=g;};
			  if (g_bit==b_bit && g_bit_old==b_bit_old && b_bit !=b_bit_old){b=g;};
		  }
		  else 
		  {
			  if (b_bit==1){b=128+v;} else b=v; 
			  count=0;
			  if ( brusco==1 ) {r=b;g=b;}
			  d=b-b_old;
			  //ajuste semibrusco
			  if (b_bit==r_bit && b_bit_old==r_bit_old && r_bit !=r_bit_old){r=b;};
			  if (b_bit==g_bit && g_bit_old==b_bit_old && b_bit !=b_bit_old){g=b;};
		  }
		 //System.out.println ("r:"+r+"g:"+g+"b:"+b);
		  
		  //lum=(r+g+b)/3;
		  //if (count !=1 && r_bit!=r_bit_old) {if (r_bit==1){r=128;}else {r=128;}}
		  //if (count !=2 && g_bit!=g_bit_old) {if (g_bit==1){g=128+m;}else {g=128-m;}}
		  //if (count !=0 && b_bit!=b_bit_old) {if (b_bit==1){b=128+m;}else {b=128-m;}}
		  
		  
		  r_bit_old=r_bit;
		  g_bit_old=g_bit;
		  b_bit_old=b_bit;
		  
		  
		  
		  
		if (r>255) r=255;
		if (g>255) g=255;
		if (b>255) b=255;
		if (r<0) r=0;
		if (g<0) g=0;
		if (b<0) b=0;
	
		  
		  int rgb=b+g*256+r*65536;
		  
		  img.setRGB(x, y, rgb);
		
		  ar=r-r_old;
		  ag=g-g_old;
		  ab=b-b_old;
		  
//		  paleta_old =paleta_old;
		  
		  r_old=r;
		  g_old=g;
		  b_old=b;
		  
		  
		  lum_old=lum;//r+g+b;
		  
  }//x
}//y
						
 System.out.println ("exit from  YCD to img");
 System.out.println ("a:"+ap+"  b:"+bp+"  c:"+cp);
 return img;
 
 
}		

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//*******************************************************************************

public int bit(int n, int dato)
{
	
	int res=dato & (int) (( Math.pow(2, n)));
	res=res / (int)Math.pow(2, n);
	return res;
	
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
private void imgTo8v005() {

System.out.println ("enter in img to YCD v003");
width=img.getWidth(); 
height=img.getHeight(); 

YCD=new int[3][width*height];
								
int r_old=0;
int g_old=0;
int b_old=0;
int r=0;
int g=0;
int b=0;
int rgb=0;
int rgb_old=0;
int rg=0;
int rg_old=0;
int rb=0;
int rb_old=0;
int gb=0;
int gb_old=0;

int v=0;
int v_old=0;

int i=0;
int paleta=0;
int paleta_old=0;
int count=0;

int t[]=new int[6];
int t_old[]=new int[6];


for (int y=0;y<height;y++)  {
for (int x=0;x<width;x++)  {
	 
	int color=img.getRGB(x, y);

		 r=(color & 0x00ff0000) >> 16;
		 g=(color & 0x0000ff00) >> 8;
		 b=(color & 0x000000ff);
		
		rgb=(r+g+b)/3;
		
		t[0]= 4*bit(7,r)+2*bit(7,g)+bit(7,b);
		t[1]= 4*bit(6,r)+2*bit(6,g)+bit(6,b);
		t[2]= 4*bit(5,r)+2*bit(5,g)+bit(5,b);
		t[3]= 4*bit(4,r)+2*bit(4,g)+bit(4,b);
		t[4]= 4*bit(3,r)+2*bit(3,g)+bit(3,b);
		t[5]= 4*bit(2,r)+2*bit(2,g)+bit(2,b);
		
		if   ( (t[0]!=t_old[0]) ||  (t[1]!=t_old[1]) || x==0 
			||
			 (t[2]!=t_old[2]) ||  (t[3]!=t_old[3]) )
			 	
			
		{
			v=128+t[0]*16+t[1]*2;
			
			count=1;
			
		}
		else if (count==1 )
		{
			v=0+t[2]*16+t[3]*2;
			count=2;
		}
		else 
		{
			v=0+t[4]*16+t[5]*2;
			count=2;
		}
		
		for (int j=0;j<6;j++) t_old[j]=t[j];
			
		YCD[2][i]=v;
		v_old=v;
		i++;
		
		}
}

System.out.println ("convertida a YCD");
}	
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
private BufferedImage YCD8toImgv005(int[][] ycd) {

System.out.println ("enter in YCD to img 8 v003");
int r=0,g=0,b=0,yp=0;
int g_old=0,r_old=0,b_old=0;


img= new BufferedImage (width,height,BufferedImage.TYPE_INT_RGB);
						
r=0;
b=r;
g=r;

int v=0;

int t[]=new int[6];
int t_old[]=new int[6];

int tes=0b111;
System.out.println ("tes:"+bit(2,tes)+"  "+bit(1,tes)+"  "+bit(0,tes));
//if (2>1) System.exit(0);


int count=0;

for (int j=0;j<6;j++) {t_old[j]=0;t[j]=0;};
int m=0;
for (int y=0;y<height;y++)  {
	  for (int x=0;x<width;x++)  {
		  
		  int i=x+(y)*width;
		  v= ycd[2][i];
	  
		  if (v>128 || x==0 )
		  {
			 v=v-128;
			 count=1;
			 //for (int j=0;j<6;j++) {t_old[j]=0;t[j]=1;};
			 t[0]= (v & 0b01110000)>>4;
			 t[1]= (v & 0b00001110)>>1;
		    


		
		  }
		  else if (count==1 )
		  {
			  count=2;
				 t[2]= (v & 0b01110000)>>4;
				 t[3]= (v & 0b00001110)>>1;
		
				
		  }
		  else if (count==2)
		  {
			  count=2;
				 t[4]= (v & 0b01110000)>>4;
				 t[5]= (v & 0b00001110)>>1;
			
			count=2;
		  }
		  
		  r=bit(2,t[0])*128+bit(2,t[1])*64+bit(2,t[2])*32+bit(2,t[3])*16+bit(2,t[4])*8+bit(2,t[5])*4;
		  g=bit(1,t[0])*128+bit(1,t[1])*64+bit(1,t[2])*32+bit(1,t[3])*16+bit(1,t[4])*8+bit(1,t[5])*4;
		  b=bit(0,t[0])*128+bit(0,t[1])*64+bit(0,t[2])*32+bit(0,t[3])*16+bit(0,t[4])*8+bit(0,t[5])*4;
		  
		  //r=(r+r_old)/2;
		  //g=(g+g_old)/2;
		  //b=(b+b_old)/2;
		  
		  /*
		if (r>255) r=255;
		if (g>255) g=255;
		if (b>255) b=255;
		if (r<0) r=0;
		if (g<0) g=0;
		if (b<0) b=0;
		*/
		  int rgb=b+g*256+r*65536;
		  
		  img.setRGB(x, y, rgb);
		
		  
//		  paleta_old =paleta_old;
		  r_old=r;
		  g_old=g;
		  b_old=b;
		 
		  
}//x
}//y
						
System.out.println ("exit from  YCD to img");

return img;


}		

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

}// end class


