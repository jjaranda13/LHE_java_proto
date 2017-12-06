package LHE;

public class Block {

	int HMIN=1;
	int HMAX=7;
	int SALTO=8;
	//boolean bilineal=true;//experimento
	
	//pixels of margin for interpolation between blocks
	public static float margin_max=0;
	
	//original shape. It is not a square. block shape Depends on image shape
	public int xini;
	public int yini;
	public int xfin;
	public int yfin;

	//length of sides of original block. lx=xfin-xini+1;
	public int lx;
	public int ly;

	//downsampled shape (it is forced to be a rectangle)
	public int downsampled_xfin;
	public int downsampled_yfin;


	public float lx_sc;//length of downsampled lx. Downsampled block is a rectangle
	public float ly_sc;//length of downsampled ly. Downsampled block is a rectangle


	public String downmode="NORMAL"; //normal, SPSoneshot

	// Pixels Per Pixel (real pixels per downsampled pixel)
	// ppp is "pixels per pixel" array. 4 corners 0,1,2,3 and 4 sides a,b,c,d
	// ppp[0] is coordinate "x", ppp[1] is coordinate "y"
	/*
	 *  sides & corners labeling
	 *         c
	 *    0+-------+1    
	 *     |       |
	 *   a |       | b
	 *     |       |
	 *    2+-------+3
	 *         d
	 *  
	 *  side a: from 0 to 2
	 *  side b: from 1 to 3
	 *  side c: from 0 to 1
	 *  side d: from 2 to 3
	 */
	public float ppp[][]; 
	//examples:
	//ppp[0][0] is ppp_x_0 
	//ppp[1][3] is ppp_y_3

	///constant(final)  max value of any PPP. any instance of block share this maximum value (static)
	// a maximum PPP=20 means that maximum spatial compression is 1/(20x20) =0.0025  ( 0.25 % of original size)
	
	//OJO PPPmax se ajusta en fromPRtoPPP(), alli se machaca el valor que se ponga aqui
	public static float MAX_PPP=8;//20;//8;//4; //20;//20;//8;//16;//16;
	

	//the same image for all blocks (static)
	public static ImgUtil img;
	
	//public FrameStorage frame;
    
    
	public float PRavg;
	public float PRavgx;
	public float PRavgy;
	//ELIMINAR ESTO
	//   public int kinint=36;


	//dos constantes
	private final float ajuste=0.5f;//0.5f;//0.5f;//0.5f;
	private final float lateral=0.25f;
	
	//**************************************************************
	public Block()
	{
		//memory for PPP (Pixels Per Pixel array)

		ppp=new float[2][4];


	}
	//**********************************************************
	public void computeDownsampledLengths()
	{
		//for use at decoder, not at encoder. 
		//At encoder, the variables downsampled_xfin and downsampled_yfin are computed at pppToRectangleShape() function
		//invoke this function just AFTER PPP computation

		//System.out.println("  antes  lx_sc:"+lx_sc);

		lx_sc=(int)(0.5f+(2.0f*lx)/(ppp[0][1]+ppp[0][0]));
		//System.out.println(" despues lx_sc:"+lx_sc);

		ly_sc=(int)(0.5f+(2.0f*ly)/(ppp[1][0]+ppp[1][2]));
		downsampled_xfin=xini+(int)lx_sc-1;
		downsampled_yfin=yini+(int)ly_sc-1;

		//System.out.println(" lx_sc:"+lx_sc);

	}
	//**********************************************************
	/**
	 * This function downsample a block 
	 * @param b : block to downsample
	 * @param bilineal: to choose between simple pixel selection or bilineal interpolation
	 * 
	 * 
	 * color mode: 422 or 420 must be specified as input
	 * color_mode: 0 (b/n)
	 * color_mode: 1 (444)
	 * color mode: 2 (422)
	 * color_mode: 3 (420)
	 * 
	 *  
	 *  method ( to read and understand slowly):
	 *    calculate legnth of downsampled side, then round to integer.
	 *    Then re-calculate ppp gradient, taking into account the new downsampled side lenght
	 *  
	 *  ppp: pixels per pixel (original pixels per downsampled pixel)
	 *  
	 *  divide and win:
	 *    Downsample Horizontal and then downsample vertical 
	 *  
	 *  rectangular condition
	 *    final downsampled block must have rectangular shape, in order to 
	 *    encode it using LHE, which is an scanline (pixel by pixel) algorithm
	 *  	   
	 */

	public void downsampleBlock( boolean bilineal)
	{
		
	
		
		if (downmode.equals("NORMAL"))
		{
		//System	
	
		downsampleH_FIX(img.YUV,img.intermediate_downsampled_YUV);
		downsampleV_FIX(img.intermediate_downsampled_YUV,img.downsampled_YUV);
		}
		else
		{
			
		downsampleSPSOneShot(img.YUV,img.downsampled_YUV);
		}

	}
	//*********************************************************************************	
	/**
	 * 
	 * this function interpolates a downsampled block to reach its original size
	 * 
	 */

	public void interpolateBlock(boolean bilineal)
	{

		if (bilineal)
		{
			interpolateBilinealV(img.downsampled_YUV,img.intermediate_interpolated_YUV);
			interpolateBilinealH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
		}
		else //neighbour
		{
			interpolateNeighbourV(img.downsampled_YUV,img.intermediate_interpolated_YUV);
			interpolateNeighbourH(img.intermediate_interpolated_YUV,img.interpolated_YUV);
		}

	}
	
	
	//*******************************************************************
	public void interpolateGapH( Block b_left)
	{
		//System.out.println("************HEHEHEHEHE***************************");
		//int margin=(int)(MAX_PPP/2f -0.5f);//+0.5f);
		
		//int margin=(int)(MAX_PPP/2f +0.5f);// 0.5 per each side
		
		//margin=(int)(MAX_PPP/2f);// +1f);
		
		
		
		//margin=12;//(int)(MAX_PPP/2f +1f);
		//esto no es generico, solo vale para luminancia
		/*
		if (b_left==null)
		margin=(int)(0.5f+Math.max(ppp[0][0]/2f,ppp[0][2]/2f));
		else 
		margin=(int)(0.5f+Math.max(ppp[0][0]/2f,Math.max(ppp[0][2]/2f,Math.max(b_left.ppp[0][3]/2f,b_left.ppp[0][1]/2f))));
		*/
		
		//selection of maximum PPP to choose the minimum H margin value for interpolation
		//-------------------------------------------------------------------------------
		//Math.max is heavier than this strategy
		float Hmarginf=ppp[0][0];
		if (ppp[0][2]>Hmarginf) Hmarginf=ppp[0][2];
		if (b_left!=null)
		{
			if (b_left.ppp[0][3]>Hmarginf) Hmarginf=b_left.ppp[0][3];
			if (b_left.ppp[0][1]>Hmarginf) Hmarginf=b_left.ppp[0][1];
		}
		int margin=(int)(0.5f+Hmarginf/2f);
		//System.out.println(" interpolateGapH:  b_left:"+b_left);
		//-------------blocks located at left side of the image------------ 
		if (b_left==null)
		{

			int xend=margin;//Math.max(ppp[0][0]/2, ppp[0][2]/2)////(int)(ppp[0][0]+1);///xini is zero
			for (int y=yini;y<=yfin;y++)
			{
				for (int x=xend;x>=0;x--)
				{
					//System.out.println("yini"+yini+" yend"+yfin+" y:"+y+" x:"+x);
					if (img.interpolated_YUV[0][y*img.width+x]==0)
					{
						img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][(y)*img.width+x+1];

						//img.mask[y*img.width+x]=1;
					}

				}
				//caso de 1 solo bloque
				if (xfin==img.width-1)
				{
					// margin must change
					margin=(int)(MAX_PPP/2f +0.5f);

					for (int x=xfin-margin;x<=xfin;x++)
					{
						if (img.interpolated_YUV[0][y*img.width+x]!=0) continue;
						img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][y*img.width+x-1];
						//img.mask[y*img.width+x]=1;
					}

				}
			}
			return;
		}

		//System.out.println("aqui estamos");
		//-------------Horizontal interpolation--------------------------------

		//if (xini==0) return;//sobra, debido al return de arriba
		
		//ESTO DE MARGIN NO DEBIA ESTAR AQUI?
		//margin=(int)(0.5f+Math.max(b_left.ppp[0][1]/2f, Math.max(margin,b_left.ppp[0][3]/2f)));

		//margin=(int)(MAX_PPP/2f +0.5f);//NUEVA CORRECCION
		
		
		//int ystart=Math.max(yini+(int)(+0.5f+ppp[1][0]/2f-1f), b_left.yini+(int)(+0.5f+b_left.ppp[1][1]/2f-1f));

		//int ystart=Math.max(yini+(int)(-0.5f+ppp[1][0]/2f-1f), b_left.yini+(int)(-0.5f +b_left.ppp[1][1]/2f-1f));
		//int ystart=Math.max(yini+(int)(ppp[1][0]/2f), b_left.yini+(int)(b_left.ppp[1][1]/2f));

		float Vmarginf=ppp[1][0];
		if (ppp[1][1]>Vmarginf) Vmarginf=ppp[1][1];
		int ystart=yini+ (int)(-0.5f+Vmarginf/2f);
		
		
		
		
		
		//ystart=yini+(int)(ppp[1][0]/2f-1-0.5f);
		if (yini==0) ystart=0;
		//int yend=Math.min(yfin-(int)(ppp[1][2]/2f+0.5f),
		//	b_left.yfin-(int)(b_left.ppp[1][3]/2f+0.5f));
		int yend=0;
		if (yfin==img.height-1) yend=yfin;
		//else yend=Math.min(yfin-(int)(ppp[1][2]/2f-0.5f),b_left.yfin-(int)(b_left.ppp[1][3]/2f-0.5f));
		//else yend=Math.min(yfin-(int)(ppp[1][2]/2f+0.5f),b_left.yfin-(int)(b_left.ppp[1][3]/2f+0.5f));
		else 
			{
			
			//yend=(int)Math.min(yfin-(ppp[1][2]/2f)+0.5f+1,b_left.yfin-(b_left.ppp[1][3]/2f)+0.5f+1);
			Vmarginf=ppp[1][2];
			if (ppp[1][3]>Vmarginf) Vmarginf=ppp[1][3];
			yend=yfin-(int)(-0.5f+Vmarginf/2f);
			}
		//PARCHE PARA ENCONTRAR EL BUG
		//ystart=yini;
		//yend=yfin;
		//margin=lx-2;
		//System.out.println("yini="+yini+"  yfin="+yend);
		//	yend=yfin;
		//System.out.println("b_left.xfin:"+b_left.xfin+ "  b.xini:"+xini+" b.xfin:"+xfin+" b.lx"+lx);
		for (int y=ystart;y<=yend;y++)
		{
			int xstart=b_left.xfin;
			int xend=xini;
			//this bucle identifies xstart
			for (xstart=b_left.xfin;xstart>=b_left.xfin-margin; xstart--)
			{

				if (xstart==0) break;
				if (img.interpolated_YUV[0][y*img.width+xstart]!=0) break ;
			}
			//this bucle identifies xend
			for (xend=xini;xend<=xini+margin; xend++)
			{
				if (xend==img.width-1) break;
				if (img.interpolated_YUV[0][y*img.width+xend]!=0) break;
			}
			
			//check good values
			//img.interpolated_YUV[0][y*img.width+b.xini]=255;
			if ((img.interpolated_YUV[0][(y)*img.width+xend]==0) ||
					(img.interpolated_YUV[0][y*img.width+xstart]==0)) 
			{
				//int colorini=img.interpolated_YUV[0][y*img.width+xstart];
				//int colorfin=img.interpolated_YUV[0][y*img.width+xend];
				//System.out.println("bad:    color_ini:"+colorini+" color_fin:"+colorfin);//+ "  betax:"+betax);
				//System.out.println("       bad" );
				/*for (int x=xstart+1;x<xend;x++)
							{
								if (x>=xini)img.interpolated_YUV[0][y*img.width+x]=255;
								   else img.interpolated_YUV[0][y*img.width+x]=128;	
							}*/
				//this scanline must not be interpolated horizontally
				continue;
			}

			//System.out.println("       ok");
			//both pixels have value
			//float betax=(float)(img.interpolated_YUV[0][(y)*img.width+xend]-img.interpolated_YUV[0][y*img.width+xstart])/(float)(xend-xstart);
			float betax=(float)(img.interpolated_YUV[0][(y)*img.width+xend]-img.interpolated_YUV[0][y*img.width+xstart])/(float)(xend-xstart);
			
			
			
			
			//float betax=(float)(img.interpolated_YUV[0][(y)*img.width+xend]-img.interpolated_YUV[0][y*img.width+xstart])/(float)(xend-xstart);
			/*
			float num=(float)(img.interpolated_YUV[0][(y)*img.width+xend]-img.interpolated_YUV[0][y*img.width+xstart]);
			float denum=(float)(xend-xstart);
			float betax= num/denum;
			*/
			//float betax=(float)(img.interpolated_YUV[0][(y)*img.width+xend]-img.interpolated_YUV[0][y*img.width+xstart])/(float)(xend-xstart);
			
			//betax=0;
			
			//if(1>2)
			/*for (int x=xend-1;x>xstart;x--)
			{
				if (img.interpolated_YUV[0][y*img.width+x]!=0) System.out.println(" WARNING");	
				//img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][y*img.width+x+1];
			}
			*/
			//if (1>2)
			int colorini=img.interpolated_YUV[0][y*img.width+xstart];
			int colorfin=img.interpolated_YUV[0][y*img.width+xend];
			//System.out.println("color_ini:"+colorini+" color_fin:"+colorfin+ "  betax:"+betax);
			for (int x=xstart+1;x<xend;x++)
			//for (int x=xstart;x<=xend;x++)
			{
			
				
				//este es el que tenia y que supuestamente esta bien
				//img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][y*img.width+xstart]+(int)(0.5f+(float)((x-xstart)*betax));
				
				//este creo q es mejor, aunque en esencia es lo mismo
				///if (img.interpolated_YUV[0][y*img.width+x]==0)
				
				
				//CREO QUE ESTO SE PUEDE HACER MAS RAPIDO SUMANDO CADA VEZ betax en lugar de multiplicar todo el rato
				
				//img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][y*img.width+xstart]+(int)(0.5f+(float)(x-xstart)*betax);
				//img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][y*img.width+xstart]+(int)((float)(x-xstart)*betax);
				
				img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][y*img.width+xstart]+(int)(ajuste+(float)(x-xstart)*betax);
				//cambio 31/10/2014
				
				/*
				int brillo=img.interpolated_YUV[0][y*img.width+xstart]+(int)((float)(x-xstart)*betax);
				if (brillo<=0) {brillo=1;}
				else if (brillo>255){ brillo=255;}
				img.interpolated_YUV[0][y*img.width+x]=brillo;
				*/
				
				//img.mask[y*img.width+x]=1;
				/*
				neighbour style 
				if (x<xini)
				img.interpolated_YUV[0][y*img.width+x]=colorini;
				else img.interpolated_YUV[0][y*img.width+x]=colorfin;
				*/
				//img.interpolated_YUV[0][y*img.width+x]=(img.interpolated_YUV[0][(y)*img.width+xend]+img.interpolated_YUV[0][y*img.width+xstart])/2;
				
				//if (img.interpolated_YUV[0][y*img.width+x]<=0) img.interpolated_YUV[0][y*img.width+x]=1;
				//else if (img.interpolated_YUV[0][y*img.width+x]>=255) img.interpolated_YUV[0][y*img.width+x]=255;
				//img.interpolated_YUV[0][y*img.width+b.xini]=255;
				//img.interpolated_YUV[0][y*img.width+xstart]=255;
			}

			// interpolate blocks located at the right side of the image
			if (xfin==img.width-1)
			{
				// margin must change
				margin=(int)(MAX_PPP/2f +0.5f);

				for (int x=xfin-margin;x<=xfin;x++)
				{
					if (img.interpolated_YUV[0][y*img.width+x]!=0) continue;
					img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][y*img.width+x-1];
					//img.mask[y*img.width+x]=1;
				}

			}
			

		}//y
	}
	//****************************************************************************************
	public void interpolateGapV( Block b_up)
	{

		//this margin is greater than needed, but simple and fast to set :-)
		int margin=(int)(MAX_PPP/2f+0.5f); 
		
		//margin=(int)(MAX_PPP/2f+5); 
		
		//margin=(int)(MAX_PPP/2f+1f); 
		
		//margin=(int)(MAX_PPP/2f+2f); 
		//System.out.println("MAX:"+MAX_PPP);
		
		
		//int margin=(int)ly/2;
		//esto no es generico, solo vale para luminancia
		//margin=(int)(0.5f+Math.max(ppp[1][0]/2f,ppp[1][1]/2f));

		//-------------blocks located at top side of the image--------------------------
		if (b_up==null)
		{
			//	if (preferred_dimension=='V') return;//already filled

			int yend=(int)(ppp[1][0]+1);///yini is zero

			yend=margin;// because we must interpolate areas inter blocks ( ppp?) 
			for (int x=xini;x<=xfin;x++)
			{
				for (int y=yend;y>=0;y--)
				{
					//System.out.println("yini"+yini+" yend"+yend+" y:"+y+" x:"+x);
					if (img.interpolated_YUV[0][y*img.width+x]==0)
					{
						img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][(y+1)*img.width+x];
						//img.mask[y*img.width+x]=1;
					}

				}
				//caso de 1 solo bloque:
				if (yfin==img.height-1)
				{

					for (int y=yfin-margin;y<=yfin;y++)
					{
						if (img.interpolated_YUV[0][y*img.width+x]!=0) continue;
						img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][(y-1)*img.width+x];
						//img.mask[y*img.width+x]=1;
					}


				}
			}
			return;
		}
		//-------------vertical interpolation--------------------------------

		//System.out.println("xfin"+b.xfin+"  yfin:"+b.yfin+"  bupyfin:"+b_up.yfin);

		//	bucle_x:
		for (int x=xini;x<=xfin;x++)
		{

			//int last_ystart=b_up.yfin-margin;
			//int last_yend=b_up.yfin-margin;

			int ystart=b_up.yfin-margin-1;;
			int yend=0;

			while (true)
			{
				
				//System.out.println("img.interpolated_YUV[0][(ystart)*img.width+x] = "+img.interpolated_YUV[0][(ystart)*img.width+x]);
				while (img.interpolated_YUV[0][(ystart)*img.width+x]!=0)
				{ystart++;
				if (ystart>=yini+margin) break;//continue bucle_x;
				}
				if (ystart>=yini+margin) break;//

				yend=ystart+1;
				while (img.interpolated_YUV[0][(yend)*img.width+x]==0)
				{yend++; 
				if (yend==img.height)
				{
					System.out.println("x:"+x+"  ystart:"+ystart+"  xini:"+xini+"   xfin:"+xfin+"   yini:"+yini+"   yfin"+yfin);
					System.exit(0);	
				}
				}

				int color_ini=img.interpolated_YUV[0][(ystart-1)*img.width+x];
				int color_fin=img.interpolated_YUV[0][(yend)*img.width+x];
				if (color_ini==0 || color_fin==0) 
				{System.out.println("interpolateGapV: warning dos negros    colorini:"+color_ini+" cfin:"+color_fin);
				img.interpolated_YUV[0][(ystart-1)*img.width+x]=255;
				img.YUVtoBMP("./output_debug/gaph.bmp",img.interpolated_YUV[0]);
				System.out.println("x:"+x+"  ystart:"+ystart+"    yend:"+yend+"  xini:"+xini+"   xfin:"+xfin+"   yini:"+yini+"   yfin"+yfin);
				System.exit(0);
				//break;
				}

				float betay=(float)(color_fin-color_ini)/(float)(yend-(ystart-1));//*
				
				//betay=(float)(color_fin-color_ini)/(float)(yend-ystart);
				//System.out.println(" ystart:"+ystart+" yend:"+yend);
				for (int y=ystart;y<yend;y++)//*
			//	for (int y=ystart+1;y<yend;y++)
				{
					// if (img.interpolated_YUV[0][y*img.width+x]==0)
					//if (x==xini || x==xini) 
					//img.interpolated_YUV[0][y*img.width+x]=255;//color_ini+(int)(0.5f+(float)((y-(ystart-1))*betay));
					//else 
					
					//img.interpolated_YUV[0][y*img.width+x]=color_ini+(int)(0.5f+(float)((y-(ystart-1))*betay));//*
					
					//PARECE QUE ESTO VA MEJOR
					//img.interpolated_YUV[0][y*img.width+x]=color_ini+(int)(0.5f+(float)(y-(ystart-1))*betay);
					
					img.interpolated_YUV[0][y*img.width+x]=color_ini+(int)(ajuste+(float)(y-(ystart-1))*betay);
					/*
					 int brillo=color_ini+(int)(0.0f+(float)(y-(ystart-1))*betay);
					 
					if (brillo<=0) {brillo=1;}
					else if (brillo>255){ brillo=255;}
					img.interpolated_YUV[0][y*img.width+x]=brillo;
					*/
					
					//img.mask[y*img.width+x]=1;
					/* Neighbour style
					  
					 if (y<yini)
						img.interpolated_YUV[0][y*img.width+x]=color_ini;
						else img.interpolated_YUV[0][y*img.width+x]=color_fin;
					*/
					
					//img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][(y-1)*img.width+x];
					//if (y>=yini)img.interpolated_YUV[0][y*img.width+x]=255;
					//else img.interpolated_YUV[0][y*img.width+x]=128;
				}
				ystart=yend;//prepare ystart for next vertical sub-segment to interpolate
			}//true

			//System.out.println("ok");
			//ahora comprobamos si hay que rellenar lo de abajo
			if (yfin==img.height-1)
			{

				for (int y=yfin-margin;y<=yfin;y++)
				{
					if (img.interpolated_YUV[0][y*img.width+x]!=0) continue;
					img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][(y-1)*img.width+x];
					//img.mask[y*img.width+x]=1;
				}


			}
		}//x

	}
	//****************************************************************************************
	/**
	 * 
	 * interpolate boundaries for next blocks, using nearest neighbour
	 * 
	 * result should be stored at ImgUtil.boundaries_YUV 
	 * 
	 * 
	 * two interpolations must be done: vertical and horizontal. 
	 * both interpolations are done in one shot invoking this function
	 * 
	 *     +---+--------v
	 *     |   |        v
	 *     +---+        v
	 *     |            v
	 *     |            v
	 *     hhhhhhhhhhhhhh
	 * 
	 * after interpolation, downsampleBoundaries must be invoked.
	 * 
	 * 
	 * @param result_YUV
	 * @param src_YUV
	 */
	public void interpolateBoundaries(int[][] result_YUV, int [][] src_YUV)
	{
		//interpolate boundaries for next blocks, using nearest neighbour
		//output is writen into boundaries_YUV array
		//------------------------------- VERTICAL INTERPOLATION----------------
		//	float lenx=lx_sc;

		//if block is located at the right image edge, there is no need for interpolation
		if (xfin!=img.width-1)
		{

			//initialization for the scanline
			float ppp_yc=ppp[1][1];
			float ppp_yd=ppp[1][3];

			// pppx initialized to ppp_yc
			float pppy=ppp_yc;

			float gry_sc=0;
			if (ppp_yc!=ppp_yd) gry_sc=(2*ly-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);

			// interpolated y coordinate in Nearest Neighbour mode
			float  yf=yini+pppy-1;
			//float  yf=yini+pppy;// despues se suma 0.5
			int y=0;
			//previous interpolated y coordinate
			int y_anti=(int)(yf-pppy+0.5f);

            //vertical scanline over the downsampled block (only one scanline)
			int x=downsampled_xfin;//x possition of vertical scanline
			for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
			{
				if ( y_sc==yini+ly_sc-1) yf=yfin;

				//integer interpolated y coordinate 
				y=(int) (yf+0.5f);
				if (y>yfin) y=yfin;

				if (y_sc==yini) 
				{

					//nearest neighbour mode:
					//copy sample into ppp pixels
					for (int i=yini;i<=y;i++)
					{
						result_YUV[0][i*img.width+xfin]=src_YUV[0][yini*img.width+x];
					}	
				}
				// y_sc is not yini  
				else 
				{
					//nearest neighbour
                    //copy sample into ppp pixels
					for (int i=y_anti+1;i<=y;i++)
					{
						result_YUV[0][i*img.width+xfin]=src_YUV[0][y_sc*img.width+x];
					}
				}

				y_anti=y;
				pppy+=gry_sc;
				yf+=pppy;//ok
			}//y
		}// if block is not at the right image edge
		//------------------------------- HORIZONTAL INTERPOLATION----------------
		//if the block is located at the bottom, interpolation is not needed
		if (yfin==img.height-1) return;

		//inicialization of PPP for the scanline
		float ppp_xa=ppp[0][2];
		float ppp_xb=ppp[0][3];
		// pppx initialized to ppp_xa
		float pppx=ppp_xa;
		
		int x=0;
		int x_anti=0;

		float grx_sc=0;
	    if (ppp_xa!=ppp_xb) grx_sc=(2*lx +ppp_xa-ppp_xb-2*ppp_xa*lx_sc)/((lx_sc-1)*lx_sc);

		pppx=ppp_xa;
		// sample 
		float xf=xini+pppx-1;
		x_anti=(int)(xf-pppx+0.5f);

		// bucle for horizontal scanline 
		int y=downsampled_yfin; //position of horizontal scanline
		for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
		{
			//ONLY FOR NEAREST NEIGHBOUR
			if ( x_sc==xini+lx_sc-1) xf=xfin;

			x=(int) (xf+0.5f);
			if (x>xfin) x=xfin;

			if (x_sc==xini) 
			{
				//nearest neighbour: copy sample into ppp/2 pixels
				for (int i=xini;i<=x;i++)
				{
					result_YUV[0][yfin*img.width+i]=src_YUV[0][y*img.width+x_sc];
				}
			}// if x_sc==xini
			else  // x_sc is not xini
			{
				for (int i=x_anti+1;i<=x;i++)
				{
					result_YUV[0][yfin*img.width+i]=src_YUV[0][y*img.width+x_sc];
				}
			}
			x_anti=x;
			pppx+=grx_sc;
			xf+=pppx;///2;//ok
		}//x
	}
	//****************************************************************************************
	/**
	 * 
	 * this function is invoked after invoking interpolateBoundaries()
	 * 
	 *  
	 * given these four blocks
	 * 
	 * KL
	 * MN
	 * 
	 * what we must downsample is 
	 * the interpolated K H-boundary using the PPP of block M
	 * the interpolated K V-boundary using the PPP of block L
	 * etc
	 * 
	 * the result shold be stored at img.boundaries_YUV
	 * bi.interpolateBoundaries(img.boundaries_YUV,img.downsampled_YUV);
	 * bi.downsampleBoundaries(true,img.boundaries_YUV,img.boundaries_YUV);
	 * 
	 * 
	 * @param bilineal
	 * @param result_YUV
	 * @param src_YUV
	 */
	public void downsampleBoundaries_OLD(boolean bilineal, int[][] result_YUV, int[][] src_YUV)
	{
		//elastic downsampling of  the previous interpolated boundaries, using mixing 
		//the array "boundaries_YUV" is rewritten with new values	

		//----------------------HORIZONTAL DOWNSAMPLING-----------------------------------------
		//initialization of ppp at side a and ppp at side b
		float ppp_xa=ppp[0][0];
		float ppp_xb=ppp[0][1];

		// initialization of pppx to ppp_xa
		float pppx=ppp_xa;
		float grx_sc=0;
		
		int y=yini-1;// we will scan the boundary that belongs to the upper block

		if (y>=0)
		{

			if (ppp_xa!=ppp_xb)	grx_sc=(2*lx-2*ppp_xa*(lx_sc))/((lx_sc-1)*lx_sc);

			//if (ppp_xa!=ppp_xb) grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1);

			//initialization of pppx at start of scanline
			pppx=ppp_xa;

			//first set of pixels to conver in one pix begins at xini and ends at xini+pppx
			//xf is the float coord x 
			float xf=xini+pppx-1f;// -1 beacuse pixel "0" exists
			int xant=xini-1;//previous integer x. first time is not relevant 

			// bucle for horizontal scanline 
			for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
			{
				//x is integer value of xf
				int x=(int) (xf+0.5f);
				if (x>xfin) x=xfin;
				//System.out.println(""+(x-xant));
				if (!bilineal) // single pixel selection, at the middle of pppx
				{	
					result_YUV[0][y*img.width+x_sc]=src_YUV[0][y*img.width+(int) (xf-pppx/2+0.5f)];
				}
				else //mix some pixels in one (linear interpolation)
				{	
					int brillo=src_YUV[0][y*img.width+x];
					int counter=1;
					for (int i=xant+1;i<=x;i++)  {brillo+=src_YUV[0][y*img.width+i];counter++;}	
					brillo=brillo/counter;
					if (brillo==0) brillo=1;
					result_YUV[0][y*img.width+x_sc]=brillo;

				}
				//xf+=pppx/2;
				pppx+=grx_sc;
				xant=x;
				xf+=pppx;//ok
			}//x

			//fill the rest of the line. Not needed 
			for (int i=downsampled_xfin+1;i<xfin;i++)result_YUV[0][y*img.width+i]=0;

		}//y>0		
		//----------------------VERTICAL DOWNSAMPLING-----------------------------------------
		float ppp_yc=ppp[1][0];
		float ppp_yd=ppp[1][2];
		float pppy=ppp_yc;


		//	float lenx=lx_sc;


		float gry_sc=0;
		//ly_sc is the lenght of scaled vertical scanline. round to the closest integer

		
		//these following two lines discard the blocks located at the left side of the image
		int x=xini-1;
		if (x<0) return;


		//calculation of gradient for this vertical row
		//each row may have a different gradient although lx_sc is constant.
		if (ppp_yc!=ppp_yd) gry_sc=(2*ly-2*ppp_yc*(ly_sc))/((ly_sc-1)*ly_sc);
		
		pppy=ppp_yc; 

		float yf=yini+pppy-1; 
		int yant=yini-1;

		for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
		{
			y=(int) (yf+0.5f);
			if (y>yfin) y=yfin;
			
			
			//origin image is boundary. it is not downsampled
			if (!bilineal) // single pixel selection, at the middle of pppy
			{
				result_YUV[0][y_sc*img.width+x]=src_YUV[0][(int)(yf-pppy/2+0.5f)*img.width+x];
			}
			else //mix some pixels in one (linear interpolation)
			{	
				int brillo=src_YUV[0][y*img.width+x];;
				int counter=1;
				for (int i=yant+1;i<y;i++)  {
					brillo+=src_YUV[0][i*img.width+x];counter++;
				}	
				brillo=brillo/counter;
				if (brillo==0) brillo=1;
				result_YUV[0][y_sc*img.width+x]=brillo;
			}

			//yf+=pppy/2;
			pppy+=gry_sc;
			yant=y;
			yf+=pppy;
		}//ysc
		
		//fill  the rest of the line. Not needed 
		for (int i=downsampled_yfin+1;i<yfin;i++)	result_YUV[0][i*img.width+x]=0;

	}
	//**************************************************************************************************
	public void downsampleH_BUG( int[][] src_YUV, int[][] result_YUV)
	{
		//we assume that the block has already rectangle shape, 
		// during this "Horizontal" processing we fill  the "horizontal_downsampled_YUV" array
		//gradient PPPx side a


		float leny=ly;

		//gradient side a
		float gryax_sc=(ppp[0][2]-ppp[0][0])/(leny-1);


		//gradient PPPx side b
		float grybx_sc=(ppp[0][3]-ppp[0][1])/(leny-1);

		//initialization of ppp at side a and ppp at side b
		float ppp_xa=ppp[0][0];
		float ppp_xb=ppp[0][1];

		// initialization of pppx to ppp_xa
		float pppx=ppp_xa;

		float grx_sc=0;
		//all scanlines have the same width (=lenght of scaled scanline)
		//lx_sc is the lenght of scaled horizontal scanline. It is integer

		//already computed, but , to be sure 
		//if (lx_sc==0) lx_sc=(int)(0.5f+(2.0f*lx)/(ppp_xb+ppp_xa));
		int y_end=yfin;

		for (int y=yini;y<=y_end;y++)
		{
			//calculation of gradient for this row. 
			//each row may have a different gradient although lx_sc is constant.

			if (ppp_xa!=ppp_xb) grx_sc=(2*lx-2*ppp_xa*(lx_sc))/((lx_sc-1)*lx_sc);
			else grx_sc=0;

			//grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1f); //lx_sc -1 steps



			//System.out.println("lx_sc:"+lx_sc+" lx:"+lx+" lx*ppp:"+(lx_sc*ppp_xa));

			//initialization of pppx at start of scanline
			pppx=ppp_xa;

			//first set of pixels to conver in one pix begins at xini and ends at xini+pppx
			//xf is the float x coord  
			//
			//    xfant-----------xf

			float xf=xini+pppx;// 
			float xfant=xini;//xf-pppx;

			int xant=xini;

			// bucle for horizontal scanline
			for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
			{
			

				int x=(int) (xf);
				if (x>xfin) 
				{x=xfin;
				//System.out.println(" ERROR:"+(xf-xfin-1));

				
				//xf=xfin;
				xf=xfin+1;
				pppx=xf-xfant;
				}
				
				//este control lo podemos quitar. puede haber un error pero es de orden 0.001
				/*
				  if (x_sc==xini+lx_sc-1)
					if ((xf-1-xfin)>0.001) System.out.println("error  es:"+(xf-1-xfin) +  "    x_sc:"+x_sc+"  y:"+y+" lx_sc:"+lx_sc+" lx:"+lx);
                */

				float color=0;
				float porcent=(1-(xfant-xant));
				
				//if (porcent<0) System.out.println(" down h me cago en todo");
				
				//xf deberia cubrir todo el porcentaje siempre, ya que ppp minimo es 1
				color+=porcent*src_YUV[0][y*img.width+xant];
				//System.out.println("colorini:"+color+" porcent:"+porcent+" xant:"+xant+"  xantf="+xfant+"   pppx:"+pppx+" x:"+x+"  xf:"+xf+" xfin:"+xfin);
				for (int i=xant+1;i<x ;i++)
				{
					color+=src_YUV[0][y*img.width+i];
				}
				//si estamos en xfin, xf deberia valer xfin
				//if (x_sc==xini+lx_sc-1) System.out.println("xf:"+xf+"   xfin:"+xfin +"   xf-xfin:"+(xf-xfin)+" pppxb:"+ppp_xb+"   pppx:"+pppx+" grx_sc:"+grx_sc);

				//con ppp=0.9999 puede ocurrir que xant y x sean lo mismo y por tanto ya ha sido sumado
				if (xant<x)	color+=(xf-x)*src_YUV[0][y*img.width+x];

				//if (xant>=x) color=255*pppx;
				//else color=255*pppx;
				color=color/pppx;
				int brillo=(int)(color+0.5f);
				if (brillo==0) brillo=1;
				else if (brillo>255) brillo=255;
				//if (x_sc==xini+lx_sc-1) brillo=255;
				result_YUV[0][y*img.width+x_sc]=brillo;


				pppx+=grx_sc;
				xant=x;
				xfant=xf;
				xf+=pppx;//ok

			}//x

			//if (y==yini+ly-1 ) System.out.println("y:"+y+"   ppp_xa:"+ppp_xa+"   ppp[0][2]"+ppp[0][2]);
			ppp_xa+=gryax_sc;
			ppp_xb+=grybx_sc;

		}//y
		downsampled_xfin=(int)lx_sc+xini-1;

	}

	//****************************************************************************************

	public void downsampleV_BUG(int[][] src_YUV, int[][] result_YUV)
	{
		//System.out.println("enter in  downsampleVfloat");
		//-------------------------------------------------------------------------
		//during this vertical processing, we fill "downsampled_YUV" array
		float ppp_yc=ppp[1][0];
		float ppp_yd=ppp[1][2];
		float pppy=ppp_yc;


		float lenx=lx_sc;

		//gradient side c
		float grycy_sc=(ppp[1][1]-ppp[1][0])/(lenx-1);

		//gradient side d
		float grydy_sc=(ppp[1][3]-ppp[1][2])/(lenx-1);


		//already computed
		//if (ly_sc==0)ly_sc=(int)(0.5f+(2.0f*ly)/(ppp_yd+ppp_yc));

		int x_end=xini+(int)lx_sc-1;//downsampled_xfin;

		//System.out.println("xend:"+x_end);
		for (int x=xini;x<=x_end;x++)
		{



			float gry_sc=0;
			if (ppp_yc!=ppp_yd) gry_sc=(2*ly-2*ppp_yc*(ly_sc))/((ly_sc-1)*ly_sc);


			//float gry_sc=(ppp_yd-ppp_yc)/(ly_sc-1);
			//else  gry_sc=0;
			//System.out.println(""+gry_sc);
			pppy=ppp_yc; 

			//System.out.println("pppyc="+ppp_yc+ "    pppyd:"+ppp_yd+" pppy:"+pppy);
			if (pppy<0.9) {
				System.out.println("pppy:"+pppy);
				System.exit(0);
			}
			// yfant --------yf
			float yf=yini+pppy; 
			float yfant=yini;
			int yant=yini;


			//System.out.println("Y:");
			//System.out.println("x:"+x+ " ly_sc:"+ly_sc);
			for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
			{
				//System.out.println("y_sc:"+y_sc);

				int y=(int) yf;
				if (y>yfin) {
					y=yfin;
					
					
					yf=yfin+1;
					pppy=yf-yfant;
					
				}

				if (y_sc==yini+ly_sc-1)
					if ((yf-1-yfin)>0.01) System.out.println("error  es:"+(yf-1-yfin) +  "    y_sc:"+y_sc+"  y:"+y+" ly_sc:"+ly_sc+" ly:"+ly);

				float color=0;
				float porcent=(1-(yfant-yant));

				//if (porcent<0) System.out.println(" down v me cago en todo");
				
				color+=porcent*=src_YUV[0][(yant)*img.width+x];
				for (int i=yant+1;i<y;i++)
				{
					color+=src_YUV[0][(i)*img.width+x];
				}
				//System.out.println("y:"+y+" yf:"+yf+"  x:"+x);
				if (yant<y) color+=(yf-y)*src_YUV[0][(y)*img.width+x];
				else color=src_YUV[0][y*img.width+x]*pppy;
				color=color/(yf-yfant);//pppy;
				int brillo=(int)(color+0.5f);
				if (brillo==0)brillo=1;
				else if (brillo>255) brillo=255;

				//System.out.println("brillo:"+brillo);

				result_YUV[0][y_sc*img.width+x]=brillo;



				//yf+=pppy/2;
				pppy+=gry_sc;
				yant=y;
				yfant=yf;
				yf+=pppy;
			}//ysc
			ppp_yc+=grycy_sc;
			ppp_yd+=grydy_sc;

		}//x
		// finally, we fill the attributes containing the last downsampled coordinates, 
		// which are useful for later interpolation.

		downsampled_yfin=(int)ly_sc+yini-1; //NOT USED
		//System.out.println("out from   downsampleVfloat");
	}
	
	//*********************************************************************************
	/**
	 * this function transform PPP values at corners in order to generate a rectangle when
	 * the block is downsampled.
	 * 
	 * However, at interpolation, this function does not assure that the block takes a rectangular shape at interpolation
	 * A rectangular downsampled block, after interpolation, generates a poligonal shape (not parallelepiped)
	 * 
	 *                                                                   
	 *   original          down             interpolated               
	 *  +-------+         +----+                    +
	 *  |       |   ----> |    |   ---->     +             
	 *  |       |         +----+                                    
	 *  |       |        rectangle                 +             
	 *  +-------+                          +  
	 *                                       any shape
	 *                                      
	 *                                       
	 */
	public void pppToRectangleShape()
	{
		/*
System.out.println("ENTER IN  pppToRectangleShapeFloat");
				//transformation of corners' bpp into rectangular shape
				//-----------------------------------------------------
				 * 
				 
//if (xini==80 && yini==208)
//{
	System.out.println("");
				System.out.println(" ppp_x1="+ppp[0][0]+"   ppp_x2="+ppp[0][1]);
				System.out.println(" ppp_x3="+ppp[0][2]+"   ppp_x4="+ppp[0][3]);
				System.out.println(" ppp_y1="+ppp[1][0]+"   ppp_y2="+ppp[1][1]);
				System.out.println(" ppp_y3="+ppp[1][2]+"   ppp_y4="+ppp[1][3]);
				System.out.println("-----------------------------------");
//}
	*/	 
		float side_c=ppp[0][0]+ppp[0][1];
		float side_d=ppp[0][2]+ppp[0][3];

		//weight to the larger side
		float weight=2;
		//weight=2;
		
		float side_average=side_c;
		// ajustment of horizontal sides
		if (side_c!=side_d)
		{
			//horizontal sides adjustment	
			//---------------------------
			//side_min is the side whose ppp summation is bigger ( seems a contradiction but it is correct)
			//side max is the side whose resolution is bigger and ppp summation is lower
			float side_min=side_c;
			float side_max=side_d;
			if (side_min<side_max) {side_min=side_d;side_max=side_c;}
			side_average=(side_min+side_max*weight)/(1f+weight);


			/*
					 System.out.println("  ppp_x1="+ppp[0][0]+"   ppp_x2="+ppp[0][1]);
						System.out.println("  ppp_x3="+ppp[0][2]+"   ppp_x4="+ppp[0][3]);
						System.out.println("  ppp_y1="+ppp[1][0]+"   ppp_y2="+ppp[1][1]);
						System.out.println("  ppp_y3="+ppp[1][2]+"   ppp_y4="+ppp[1][3]);
					System.out.println("side_min:"+side_min+"   side_max"+side_max+"  avg:"+side_average);
			 */

			//adjust side_average
			//----------------------
			//the fastest solution in order to avoid exceed MAX_PPP 
			//side_average=side_max; 
            System.out.println ("side_average="+side_average+ "  sidec:"+side_c+ "  sided:"+side_d);
			//slower solution but allows better average ajustment
			
            
			if (side_c<side_d)
			{
				float min_factor= Math.min (MAX_PPP/ppp[0][0],MAX_PPP/ppp[0][1]);
				side_average=Math.min(min_factor*side_c,side_average);
			}

			else
			{
				float min_factor= Math.min (MAX_PPP/ppp[0][2],MAX_PPP/ppp[0][3]);
				side_average=Math.min(min_factor*side_d,side_average);
			}
			
			
			
			//side_average=side_max;//PRUEBA 2016
		}
		
		 
		

		lx_sc=(int) (0.5f+ 2f*lx/side_average);

		//rejuste fino de p1+p2 de modo que se cumpla la ecuacion
		
		side_average=2*lx/lx_sc;
		
		//System.out.println("ajuste de suma:"+side_average+" side d:"+side_d);
		//ahora hay que ajustar los ppp con este nuevo lx_sc
		//incluso aunque sean iguales, ya que lx_sc*ppp no suma lx, debido al redondeo




		//System.out.println("side average H="+side_average+"   lx_sc:"+lx_sc);
		//si ya tenemos s podemos recalcular el nuevo lado
		//pero debe ser un numero entero


		//podemos cumplir ese valor de lx_sc?
		//si, pero si garantizamos la suma. topar en 1 y en max no es garantizarlo.

       // System.out.println(" side c,d average (P1+P2):"+side_average);

		//adjust side c
		//--------------
		if (ppp[0][0]<=ppp[0][1])
		{	
			ppp[0][0]=side_average*ppp[0][0]/side_c;

			//float add1=0;
			//if (ppp[0][0]<1) {add1=1-ppp[0][0]; ppp[0][0]=1;}
			if (ppp[0][0]<1) {ppp[0][0]=1;}//PPPmin is 1 a PPP value <1 is not possible

			float add0=0;
			ppp[0][1]=side_average-ppp[0][0];//+add1;
			if (ppp[0][1]>MAX_PPP) {add0=ppp[0][1]-MAX_PPP; ppp[0][1]=MAX_PPP;}

			ppp[0][0]+=add0;
		}
		else
		{
			//System.out.println("01 es menor que 00");
			ppp[0][1]=side_average*ppp[0][1]/side_c;

			//float add0=0;
			//if (ppp[0][1]<1) {add0=1-ppp[0][1]; ppp[0][1]=1;}
			if (ppp[0][1]<1) { ppp[0][1]=1;}//PPPmin is 1 a PPP value <1 is not possible
			
			float add1=0;
			ppp[0][0]=side_average-ppp[0][1];//+add0;
			if (ppp[0][0]>MAX_PPP) {add1=ppp[0][0]-MAX_PPP; ppp[0][0]=MAX_PPP;}

			ppp[0][1]+=add1;



		}

		//adjust side d
		if (ppp[0][2]<=ppp[0][3])
		{	
			ppp[0][2]=side_average*ppp[0][2]/side_d;

			//System.out.println(" p02 inicial:"+ppp[0][2]);
			//float add3=0;
			//if (ppp[0][2]<1) {add3=1-ppp[0][2]; ppp[0][2]=1;}
			if (ppp[0][2]<1) {ppp[0][2]=1;}// PPP can not be <1
			
			float add2=0;
			ppp[0][3]=side_average-ppp[0][2];//+add3;
			if (ppp[0][3]>MAX_PPP) {add2=ppp[0][3]-MAX_PPP; ppp[0][3]=MAX_PPP;}

			ppp[0][2]+=add2;

			//el problema es que podemos habernos pasado
		}
		else
		{
			ppp[0][3]=side_average*ppp[0][3]/side_d;

			//System.out.println(" p03 inicial:"+ppp[0][3]);
			//float add2=0;
			//if (ppp[0][3]<1) {add2=1-ppp[0][3]; ppp[0][3]=1;}
			if (ppp[0][3]<1) {ppp[0][3]=1;}//ppp can not be <1

			float add3=0;
			ppp[0][2]=side_average-ppp[0][3];//+add2;
			//System.out.println(" p02 inicial:"+ppp[0][2]);
			if (ppp[0][2]>MAX_PPP) {add3=ppp[0][2]-MAX_PPP; ppp[0][2]=MAX_PPP;}
			//System.out.println(" add3:"+add3);
			ppp[0][3]+=add3;

		}

		//}//end H adjustment

		//vertical sides adjustment
		//-------------------------
		float side_a=ppp[1][0]+ppp[1][2];
		float side_b=ppp[1][1]+ppp[1][3];


		side_average=side_a;
		//System.out.println(" side a :"+side_a+" side_average:"+side_average+" side_b:"+side_b);
		if (side_a!=side_b)
		{
			//System.out.println(" adjusting vertical sides...");
			//side_min is the side whose ppp summation is bigger ( seems a contradiction but it is correct)
			float side_min=side_a;
			float side_max=side_b;
			if (side_min<side_max) {side_min=side_b;side_max=side_a;}
			side_average=(side_min+side_max*weight)/(1f+weight);


			//adjust side_average
			//----------------------
			//the fastest solution in order to avoid exceed MAX_PPP 
			//side_average=side_max; 

			//slower solution but allows better average ajustment
			
			if (side_a<side_b)
			{
				float min_factor= Math.min (MAX_PPP/ppp[1][0],MAX_PPP/ppp[1][2]);
				side_average=Math.min(min_factor*side_a,side_average);
			}

			else
			{
				float min_factor= Math.min (MAX_PPP/ppp[1][1],MAX_PPP/ppp[1][3]);
				side_average=Math.min(min_factor*side_b,side_average);
			}
			
			//side_average=side_max;//PRUEBA 2016
			
			
		}
		//System.out.println("side average:"+side_average);
		ly_sc=(int) (0.5f+ 2f*ly/side_average);
		side_average=2*ly/ly_sc;



		//adjust side a
		if (ppp[1][0]<=ppp[1][2])
		{	
			ppp[1][0]=side_average*ppp[1][0]/side_a;

			//float add2=0;
			//if (ppp[1][0]<1) {add2=1-ppp[1][0]; ppp[1][0]=1;}
			if (ppp[1][0]<1) { ppp[1][0]=1;}
			
			float add0=0;
			ppp[1][2]=side_average-ppp[1][0];//+add2;
			if (ppp[1][2]>MAX_PPP) {add0=ppp[1][2]-MAX_PPP; ppp[1][2]=MAX_PPP;}

			ppp[1][0]+=add0;
		}
		else
		{
			ppp[1][2]=side_average*ppp[1][2]/side_a;

			//float add0=0;
			//if (ppp[1][2]<1) {add0=1-ppp[1][2]; ppp[1][2]=1;}
			if (ppp[1][2]<1) { ppp[1][2]=1;}
			
			float add2=0;
			ppp[1][0]=side_average-ppp[1][2];//+add0;
			if (ppp[1][0]>MAX_PPP) {add2=ppp[1][0]-MAX_PPP; ppp[1][0]=MAX_PPP;}

			ppp[1][2]+=add2;

		}

		//adjust side b
		if (ppp[1][1]<=ppp[1][3])
		{	
			ppp[1][1]=side_average*ppp[1][1]/side_b;

			//float add3=0;
			//if (ppp[1][1]<1) {add3=1-ppp[1][1]; ppp[1][1]=1;}
			if (ppp[1][1]<1) { ppp[1][1]=1;}

			float add1=0;
			ppp[1][3]=side_average-ppp[1][1];//+add3;
			if (ppp[1][3]>MAX_PPP) {add1=ppp[1][3]-MAX_PPP; ppp[1][3]=MAX_PPP;}

			ppp[1][1]+=add1;
		}
		else
		{
			ppp[1][3]=side_average*ppp[1][3]/side_b;

			//float add1=0;
			//if (ppp[1][3]<1) {add1=1-ppp[1][3]; ppp[1][3]=1;}
			if (ppp[1][3]<1) { ppp[1][3]=1;}
			
			float add3=0;
			ppp[1][1]=side_average-ppp[1][3];//+add1;
			if (ppp[1][1]>MAX_PPP) {add3=ppp[1][1]-MAX_PPP; ppp[1][1]=MAX_PPP;}

			ppp[1][3]+=add3;

		}
		downsampled_xfin=xini+(int)lx_sc-1;
		downsampled_yfin=yini+(int)ly_sc-1;


		///por ultimo


		//results:
			/* 
	//		  if (xini==80 && yini==208)
//{System.out.println("");
				  System.out.println (" Rectangle shape  lx_sc:"+lx_sc+" ly_sc:"+ly_sc+"  MAX_PPP:"+MAX_PPP );
			        System.out.println("  ppp_x1="+ppp[0][0]+"   ppp_x2="+ppp[0][1]);
					System.out.println("  ppp_x3="+ppp[0][2]+"   ppp_x4="+ppp[0][3]);
					System.out.println("  ppp_y1="+ppp[1][0]+"   ppp_y2="+ppp[1][1]);
					System.out.println("  ppp_y3="+ppp[1][2]+"   ppp_y4="+ppp[1][3]);
					System.out.println("-----------------------------------");
}
			  
				System.out.println("OUT FROM  pppToRectangleShapeFloat");
		 */
	}

	//*******************************************************************
	
	public void interpolateAdaptH( int[][] src_YUV, int[][] result_YUV)
	{
		//interpolateBilinealH(src_YUV,result_YUV);
		float leny=ly;
		//leny=ly_sc;
				//gradient x scaled of side a
				float gryax_sc=0;
				//gradient x of side a
				if (ppp[0][2]!=ppp[0][0])	gryax_sc=(ppp[0][2]-ppp[0][0])/(leny-1);//lya_sc;

				float grybx_sc=0;
				if (ppp[0][3]!=ppp[0][1])  grybx_sc=(ppp[0][3]-ppp[0][1])/(leny-1);//b_sc;

				//initial ppp values for sides "a" and "b"
				float ppp_xa=ppp[0][0];
				float ppp_xb=ppp[0][1];

				//extrapolate ppp to the top,because the vertical interpolated block is shifted (in bilineal case)
				ppp_xa=ppp_xa-gryax_sc*ppp[1][0]/2; //extrapolation
				ppp_xb=ppp_xb-grybx_sc*ppp[1][1]/2;//extrapolation

				// pppx is initialized to ppp_xa
				float pppx=ppp_xa;


				for (int y=yini;y<yini+leny;y++)
				{
					//starts at side c ( which is y==yini)
					float grx_sc=0;
					if (ppp_xa!=ppp_xb) grx_sc=(2*lx +ppp_xa-ppp_xb-2*ppp_xa*lx_sc)/((lx_sc-1)*lx_sc);

					//grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1);
					pppx=ppp_xa;
					// xf is the x coord of end of the segment to interpolate
					// x_antif is the c coord of the begining of the segment to interpolate
					float xf=xini+pppx/2;
					float x_antif=xf-pppx;

					// bucle for horizontal scanline. It scans the downsampled image, pixel by pixel
					int cfin_ant=0;
					//int coordy=y*img.width;// to avoid multiplications
					for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
					{

						//coord "x" is the last pixel influenced by the segment x_sc-1 <----> x_sc
						int x=(int) (xf);//+0.5f); 

						int cini=0;//initial color
						//int cfin=src_YUV[0][y*img.width+x_sc];//final color
						int cfin=src_YUV[0][y*img.width+x_sc];//final color

						if (cfin<=0) 
							{//System.out.println("interpolateBilinealH: cfin es cero:"+cfin);
							//System.exit(0);
							
							}
						if (x_sc==xini) //begining of downsampled block. there is no segment
						{
							//downsampled pix only cover part of this pixel 
							//at least must cover a 30% of the pixel to be filled here. otherwise it will be
							//interpolated between blocks, but interpolation between blocks is roughly because
							//we will not consider this residual percentage.
							//IF yf< y+0.5 then this pixel is painted in the next step
							//
							//LA BUENA
							
							//if (xf>=x+0.5f && xf<=x+0.75f) result_YUV[0][y*img.width+x]=cfin;
							//System.out.println ("dato:" + (xf-x));
							//if (2>1) System.exit(0);
							//if (xf<=x+0.75f) result_YUV[0][y*img.width+x]=cfin;
							
							//LA NUEVA
							if (xf>=x+0.5f   && xf<=x+0.75f ) result_YUV[0][y*img.width+x]=cfin;
							//MEJOR SIEMPRE
						
							if (xini==0)
							if (xf>=x+0.5f) result_YUV[0][y*img.width+x]=cfin;
							
							// finally is better to fill here the pixel, even if the % <30%
							//if (xf>=x+0.5f ) result_YUV[0][y*img.width+x]=cfin;
						}
						else  if (x_sc!=xini && cfin!=0 )// There is segment
						{
							cini=cfin_ant;//src_YUV[0][y*img.width+x_sc-1];

							if (cini!=0) //interpolation is possible
							{

								//VOY A DETERMINAR SI USO BILINEAL O VECINO
								int hop_ant=img.hops[0][(y)*img.width+x_sc-1];
								int hop_new=img.hops[0][(y)*img.width+x_sc];
								//012345678
								//    |
								//   null
								int salto=Math.abs(cfin-cini);
								int hop_min=HMIN;
								int hop_max=HMAX;
								//if (hop_ant<hop_min || hop_ant>hop_max || hop_new<hop_min || hop_new>hop_max)
								if (salto>SALTO)
								{
									//no se hace bilineal, se hace vecino
									cini=cfin;//esto da pendiente cero.
									//lo ideal es pintar la mitad de cini y la otra mitad de cfin
								}
								
								
								
								//interpolation gradient x
								float  igradx=(float)(cfin-cini)/pppx;
								
								//float  igradx=(float)(cfin-cini)/(xf-x_antif);
								
								// colour of each pixel x=i inside segment is the corresponding  x = i.5
								//=======================================================================
								//colour initial segment
								//float cis=cini+(int)(0.5f+((((int)(x_antif+0.5f)+0.5f)-x_antif)*igradx));
								
								
								float cis=cini+(((int)(x_antif+0.5f)+0.5f)-x_antif)*igradx;
								
								//float cis=cini+(((int)(x_antif+0.5f)+0.5f)-(int)x_antif)*igradx;
								
								
								for (int i=(int)(x_antif+0.5f);i<=(int)(xf-0.5f);i++)
								{

									//	int color=cini+(int)(0.5f+((((float)i+0.5f)-x_antif)*igradx));
									//	result_YUV[0][y*img.width+i]=color;
									
									//result_YUV[0][y*img.width+i]=(int)(cis+0.5f);
									
									//modificacion para corregir que nunca pintemos cero
									//al ser float debo chequear <1 y no <=0
									float cis2=cis+ajuste;
									if (cis2<1) cis2=1f;
									else if (cis2>255) cis2=255f;
									
									result_YUV[0][y*img.width+i]=(int)(cis2);//por coherencia con gap, que fue mejor asi
									
									//20141118
									//if (result_YUV[0][y*img.width+i]<=0) result_YUV[0][y*img.width+i]=1;
									//else if (result_YUV[0][y*img.width+i]>255) result_YUV[0][y*img.width+i]=255;
									
									
									cis+=igradx;
								}

								//if (x_sc==xini+lx_sc-1) 
								if (x_sc==downsampled_xfin)
								{
									//if xf>=x+0.5f then the pixel is already painted
									//if (xf>=x+0.5f && result_YUV[0][y*img.width+x]==0) System.out.println("WARNING");
									// a 30% must be covered to be filled here. otherwise it will be interpolated between blocks
									
									
									//ESTA ES LA BUENA
									//float dif=((float)x+0.5f)-xf;
									//cfin=(int)(cfin+igradx*dif+0.5f);
									if (xf<(float)x+0.5f && xf>=x+0.25f ) result_YUV[0][y*img.width+x]=cfin;
									//mejor siempre
									//if (pppx>2)
									//siempre si xfin es width
									if (xfin==img.width)//|| pppx<1.2f)
									if (xf<(float)x+0.5f) result_YUV[0][y*img.width+x]=cfin;
									
									//ESTA ES LA NUEVA
									//if (xf<(float)x+0.5f)result_YUV[0][y*img.width+x]=cfin;
									
									//me gusta mas asi porque no se forma efecto cuadricula por no haber interpolado.
									//el cambio es introducir xfin
									//IF xf>=x+0.5 then this pixel is already painted
									//if (xf<(float)x+0.5f && xf>=x+0.25f && x!=xfin) result_YUV[0][y*img.width+x]=cfin;
									
									// finally is better to fill here the pixel, even if the % <30%
									//if (xf<(float)x+0.5f) result_YUV[0][y*img.width+x]=cfin;
								}

							}

						}//else not xini

						cfin_ant=cfin;
						x_antif=xf;
						pppx+=grx_sc;
						xf+=pppx;
					}//x
					ppp_xa+=gryax_sc;
					ppp_xb+=grybx_sc;

				}//y
	}

	
	
	//*******************************************************************
	/**
	 * horizontal bilineal interpolation
	 * 
	 * the interpolation process is: first vertical then horizontal
	 * @param src_YUV
	 * @param result_YUV
	 */
	public void interpolateBilinealH( int[][] src_YUV, int[][] result_YUV)
	{

		float leny=ly;
//leny=ly_sc;
		//gradient x scaled of side a
		float gryax_sc=0;
		//gradient x of side a
		if (ppp[0][2]!=ppp[0][0])	gryax_sc=(ppp[0][2]-ppp[0][0])/(leny-1);//lya_sc;

		float grybx_sc=0;
		if (ppp[0][3]!=ppp[0][1])  grybx_sc=(ppp[0][3]-ppp[0][1])/(leny-1);//b_sc;

		//initial ppp values for sides "a" and "b"
		float ppp_xa=ppp[0][0];
		float ppp_xb=ppp[0][1];

		//extrapolate ppp to the top,because the vertical interpolated block is shifted (in bilineal case)
		ppp_xa=ppp_xa-gryax_sc*ppp[1][0]/2; //extrapolation
		ppp_xb=ppp_xb-grybx_sc*ppp[1][1]/2;//extrapolation

		// pppx is initialized to ppp_xa
		float pppx=ppp_xa;


		for (int y=yini;y<yini+leny;y++)
		{
			//starts at side c ( which is y==yini)
			float grx_sc=0;
			if (ppp_xa!=ppp_xb) grx_sc=(2*lx +ppp_xa-ppp_xb-2*ppp_xa*lx_sc)/((lx_sc-1)*lx_sc);

			//grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1);
			pppx=ppp_xa;
			// xf is the x coord of end of the segment to interpolate
			// x_antif is the c coord of the begining of the segment to interpolate
			float xf=xini+pppx/2;
			float x_antif=xf-pppx;

			// bucle for horizontal scanline. It scans the downsampled image, pixel by pixel
			int cfin_ant=0;
			//int coordy=y*img.width;// to avoid multiplications
			for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
			{

				//coord "x" is the last pixel influenced by the segment x_sc-1 <----> x_sc
				int x=(int) (xf);//+0.5f); 

				int cini=0;//initial color
				//int cfin=src_YUV[0][y*img.width+x_sc];//final color
				int cfin=src_YUV[0][y*img.width+x_sc];//final color

				if (cfin<=0) 
					{//System.out.println("interpolateBilinealH: cfin es cero:"+cfin);
					//System.exit(0);
					
					}
				if (x_sc==xini) //begining of downsampled block. there is no segment
				{
					//downsampled pix only cover part of this pixel 
					//at least must cover a 30% of the pixel to be filled here. otherwise it will be
					//interpolated between blocks, but interpolation between blocks is roughly because
					//we will not consider this residual percentage.
					//IF yf< y+0.5 then this pixel is painted in the next step
					//
					//LA BUENA
					
					//if (xf>=x+0.5f && xf<=x+0.75f) result_YUV[0][y*img.width+x]=cfin;
					//System.out.println ("dato:" + (xf-x));
					//if (2>1) System.exit(0);
					//if (xf<=x+0.75f) result_YUV[0][y*img.width+x]=cfin;
					
					//LA NUEVA
					if (xf>=x+0.5f   && xf<=x+0.75f ) result_YUV[0][y*img.width+x]=cfin;
					//MEJOR SIEMPRE
				
					if (xini==0)
					if (xf>=x+0.5f) result_YUV[0][y*img.width+x]=cfin;
					
					// finally is better to fill here the pixel, even if the % <30%
					//if (xf>=x+0.5f ) result_YUV[0][y*img.width+x]=cfin;
				}
				else  if (x_sc!=xini && cfin!=0 )// There is segment
				{
					cini=cfin_ant;//src_YUV[0][y*img.width+x_sc-1];

					if (cini!=0) //interpolation is possible
					{

						//interpolation gradient x
						float  igradx=(float)(cfin-cini)/pppx;
						
						//float  igradx=(float)(cfin-cini)/(xf-x_antif);
						
						// colour of each pixel x=i inside segment is the corresponding  x = i.5
						//=======================================================================
						//colour initial segment
						//float cis=cini+(int)(0.5f+((((int)(x_antif+0.5f)+0.5f)-x_antif)*igradx));
						
						
						float cis=cini+(((int)(x_antif+0.5f)+0.5f)-x_antif)*igradx;
						
						//float cis=cini+(((int)(x_antif+0.5f)+0.5f)-(int)x_antif)*igradx;
						
						
						for (int i=(int)(x_antif+0.5f);i<=(int)(xf-0.5f);i++)
						{

							//	int color=cini+(int)(0.5f+((((float)i+0.5f)-x_antif)*igradx));
							//	result_YUV[0][y*img.width+i]=color;
							
							//result_YUV[0][y*img.width+i]=(int)(cis+0.5f);
							
							//modificacion para corregir que nunca pintemos cero
							//al ser float debo chequear <1 y no <=0
							float cis2=cis+ajuste;
							if (cis2<1) cis2=1f;
							else if (cis2>255) cis2=255f;
							
							result_YUV[0][y*img.width+i]=(int)(cis2);//por coherencia con gap, que fue mejor asi
							
							//20141118
							//if (result_YUV[0][y*img.width+i]<=0) result_YUV[0][y*img.width+i]=1;
							//else if (result_YUV[0][y*img.width+i]>255) result_YUV[0][y*img.width+i]=255;
							
							
							cis+=igradx;
						}

						//if (x_sc==xini+lx_sc-1) 
						if (x_sc==downsampled_xfin)
						{
							//if xf>=x+0.5f then the pixel is already painted
							//if (xf>=x+0.5f && result_YUV[0][y*img.width+x]==0) System.out.println("WARNING");
							// a 30% must be covered to be filled here. otherwise it will be interpolated between blocks
							
							
							//ESTA ES LA BUENA
							//float dif=((float)x+0.5f)-xf;
							//cfin=(int)(cfin+igradx*dif+0.5f);
							if (xf<(float)x+0.5f && xf>=x+0.25f ) result_YUV[0][y*img.width+x]=cfin;
							//mejor siempre
							//if (pppx>2)
							//siempre si xfin es width
							if (xfin==img.width)//|| pppx<1.2f)
							if (xf<(float)x+0.5f) result_YUV[0][y*img.width+x]=cfin;
							
							//ESTA ES LA NUEVA
							//if (xf<(float)x+0.5f)result_YUV[0][y*img.width+x]=cfin;
							
							//me gusta mas asi porque no se forma efecto cuadricula por no haber interpolado.
							//el cambio es introducir xfin
							//IF xf>=x+0.5 then this pixel is already painted
							//if (xf<(float)x+0.5f && xf>=x+0.25f && x!=xfin) result_YUV[0][y*img.width+x]=cfin;
							
							// finally is better to fill here the pixel, even if the % <30%
							//if (xf<(float)x+0.5f) result_YUV[0][y*img.width+x]=cfin;
						}

					}

				}//else not xini

				cfin_ant=cfin;
				x_antif=xf;
				pppx+=grx_sc;
				xf+=pppx;
			}//x
			ppp_xa+=gryax_sc;
			ppp_xb+=grybx_sc;

		}//y
	}	
	
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
	public void interpolateBilinealH_false( int[][] src_YUV, int[][] result_YUV)
	{

		float leny=ly;
//leny=ly_sc;
		//gradient x scaled of side a
		float gryax_sc=0;
		//gradient x of side a
		if (ppp[0][2]!=ppp[0][0])	gryax_sc=(ppp[0][2]-ppp[0][0])/(leny-1);//lya_sc;

		float grybx_sc=0;
		if (ppp[0][3]!=ppp[0][1])  grybx_sc=(ppp[0][3]-ppp[0][1])/(leny-1);//b_sc;

		//initial ppp values for sides "a" and "b"
		float ppp_xa=ppp[0][0];
		float ppp_xb=ppp[0][1];

		//extrapolate ppp to the top,because the vertical interpolated block is shifted (in bilineal case)
		ppp_xa=ppp_xa-gryax_sc*ppp[1][0]/2; //extrapolation
		ppp_xb=ppp_xb-grybx_sc*ppp[1][1]/2;//extrapolation

		// pppx is initialized to ppp_xa
		float pppx=ppp_xa;


		for (int y=yini;y<yini+leny;y++) //lega hasta el final
		{
			//starts at side c ( which is y==yini)
			float grx_sc=0;
			if (ppp_xa!=ppp_xb) grx_sc=(2*lx +ppp_xa-ppp_xb-2*ppp_xa*lx_sc)/((lx_sc-1)*lx_sc);

			//grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1);
			pppx=ppp_xa;
			// xf is the x coord of end of the segment to interpolate
			// x_antif is the c coord of the begining of the segment to interpolate
			float xf=xini;//+pppx/2;
			float x_antif=xf;//-pppx;

			// bucle for horizontal scanline. It scans the downsampled image, pixel by pixel
			int cfin_ant=src_YUV[0][yini*img.width+xini];
			int cini=0;
			int cfin=0;		
			//int coordy=y*img.width;// to avoid multiplications
			for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
			{

				//coord "x" is the last pixel influenced by the segment x_sc-1 <----> x_sc
				int x=(int) (xf+0.5f); 

				cini=cfin_ant;//initial color
				cfin=src_YUV[0][y*img.width+x_sc];//final color
				//interpolation gradient x
				float  igradx=(float)(cfin-cini)/pppx;
						
				float cis=cini+(((int)(x_antif+0.5f))-x_antif)*igradx;
						
						
				for (int i=(int)(x_antif);i<=(int)(xf);i++)
				  {
					float cis2=cis;
					if (cis2<1) cis2=1f;
					else if (cis2>255) cis2=255f;
							
					result_YUV[0][y*img.width+i]=(int)(cis2);//por coherencia con gap, que fue mejor asi
					cis+=igradx;
				 }
 
				 if (x_sc==downsampled_xfin)
					{
						//if xf>=x+0.5f then the pixel is already painted
					 //for (int i=(int)xf;i<xfin;ixx)
						//if (xf<(float)x+0.5f) result_YUV[0][y*img.width+x]=cfin;
						for (int i=(int)xf;i<=xfin;i++)
							result_YUV[0][y*img.width+i]=cfin;
					}

					

				

				cfin_ant=cfin;
				x_antif=xf;
				//pppx+=grx_sc;
				xf+=pppx;
				pppx+=grx_sc;
			}//x
			ppp_xa+=gryax_sc;
			ppp_xb+=grybx_sc;

		}//y
	}	
	
	//******************************************************************
	public void interpolateAdaptV( int[][] src_YUV, int[][] result_YUV)
	{
		//interpolateBilinealV(src_YUV,result_YUV);
		//------------------------------- VERTICAL INTERPOLATION----------------
		float lenx=lx_sc;
//lenx=lx;
		//gradient pppy of side c
		float grycy_sc=(ppp[1][1]-ppp[1][0])/(lenx-1);//lya_sc;
		//gradient PPPy side d
		float grydy_sc=(ppp[1][3]-ppp[1][2])/(lenx-1);

		//en la vertical no hace falta porque el bloque esta comprimido y no desplazado
		//vertical interpolation always is executed before H interpolation
		//therefore source is a block not shifted.  No need for extrapolation
		float ppp_yc=ppp[1][0];
		float ppp_yd=ppp[1][2];

		// pppx initialized to ppp_yc
		float pppy=ppp_yc;

		int y=0;

		//dont scan lx but lenx, which is lx_sc. 
		for (int x=xini;x<xini+lenx;x++)
		{

			float gry_sc=0;
			if (ppp_yc!=ppp_yd) 
				gry_sc=(2*ly-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);


			//float gry_sc=(ppp_yd-ppp_yc)/(ly_sc-1);

			//ppp_yc is updated at each iteration
			pppy=ppp_yc;//-grycy_sc;


			// yf is the end of the segment
			float yf=yini+pppy/2f;
			
			//NUEVO
			//float yf=yini+pppy/2f-0.5f;
			
			float y_antif=yf-pppy;

			// bucle for horizontal scanline 
			// scans the downsampled image, pixel by pixel
			int cfin_ant=0;
			//int coordy=y*img.width;
			for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
			{

				//coord "y" is the last pixel influenced by the segment y_sc-1 <----> y_sc
				y=(int) (yf); 

				int cini=0;
				int cfin=src_YUV[0][(y_sc)*img.width+x];

				if (y_sc==yini)//no se puede interpolar
				{
					//int cfin=src_YUV[0][(y_sc)*img.width+x];
					
					//IF yf< y+0.5 then this pixel is painted in the next step
					//LA BUENA
					if (yf>=y+0.5f && yf<=y+0.75f) result_YUV[0][y*img.width+x]=cfin;
					//mejor siempre
					//siempre si yini es cero
					if (yini==0 )//|| pppy<1.2f)
					if (yf>=y+0.5f) result_YUV[0][y*img.width+x]=cfin;
					
					//LA NUEVA
					//if (yf>=y+0.5f && x!=xini) result_YUV[0][y*img.width+x]=cfin;
					//if (yf>=y+0.5f) result_YUV[0][y*img.width+x]=cfin;
					
					//if (yf>=y+0.5f && yf<=y+0.75f && y!=yini) result_YUV[0][y*img.width+x]=cfin;
					
					//if ( yf<=y+0.75f) result_YUV[0][y*img.width+x]=cfin;
					// finally is better to fill here the pixel, even if the % <30%
					//if (yf>=y+0.5f ) result_YUV[0][y*img.width+x]=cfin;
				}

				else if (y_sc!=yini  && cfin!=0)	
				{
					cini=cfin_ant;//src_YUV[0][(y_sc-1)*img.width+x];

					if (cini!=0) //interpolation is possible
					{
						//int cfin=src_YUV[0][(y_sc)*img.width+x];;

						
						//VOY A DETERMINAR SI USO BILINEAL O VECINO
						int hop_ant=img.hops[0][(y_sc-1)*img.width+x];
						int hop_new=img.hops[0][(y_sc)*img.width+x];
						//012345678
						//    |
						//   null
						int hop_min=HMIN;
						int hop_max=HMAX;
						
						System.out.println(" "+hop_ant+" ," +hop_new);
						int salto=Math.abs(cfin-cini);
						if (salto>SALTO && cfin!=0)
						//if (hop_ant<hop_min || hop_ant>hop_max || hop_new<hop_min || hop_new>hop_max)
						{
							//no se hace bilineal, se hace vecino
							cini=cfin;//=cfin;//esto da pendiente cero, aunque es chapucero
							//lo ideal es pintar la mitad de cini y la otra mitad de cfin
							
							
							
						}
						
						//interpolation gradient y
						float  igrady=(float)(cfin-cini)/pppy;
						// colour of each pixel y=i inside segment is the corresponding  y = i.5
						//=======================================================================

						//colour initial segment
						//float cis=cini+(int)(0.5f+((((int)(y_antif+0.5f)+0.5f)-y_antif)*igrady));
						float cis=cini+(((int)(y_antif+0.5f)+0.5f)-y_antif)*igrady;
						for (int i=(int)(y_antif+0.5f);i<=(int)(yf-0.5f);i++)
						{
							//int color=cini+(int)(0.5f+((((float)i+0.5f)-y_antif)*igrady));
							//result_YUV[0][i*img.width+x]=color;
							
							//result_YUV[0][i*img.width+x]=(int)(cis+0.5f);
							
							//al ser float debo chequear <1 y no <=0
							//if (cis<1) cis=1f;
							//else if (cis>255) cis=255f;
							
							float cis2=cis+ajuste;
							if (cis2<1) cis2=1f;
							else if (cis2>255) cis2=255f;
							
							result_YUV[0][i*img.width+x]=(int)(cis2);//por coherencia con gap
							
							
							//20141118
							//if (result_YUV[0][i*img.width+x]<=0)result_YUV[0][i*img.width+x]=1;
							//else if (result_YUV[0][i*img.width+x]>255) result_YUV[0][i*img.width+x]=255;
							
							
							cis+=igrady;
						}
						//comprobamos s no se ha podido pintar el ultmo pix

						//si el ultimo pix no se puede pintar, es mejor pintarlo a pesar de ello?
						//if (y_sc==yini+ly_sc-1)
						if (y_sc==downsampled_yfin)
						{

							//if yf>=y+0.5f then the pixel is already painted
							//if (yf>=y+0.5f && result_YUV[0][y*img.width+x]==0) System.out.println("WARNING");
							// a 30% must be covered to be filled here. otherwise it will be interpolated between blocks

							//LA BUENA
							//float dif=((float)y+0.5f)-yf;
							//cfin=(int)(cfin+igrady*dif+0.5f);
						    if (yf<(float)y+0.5f && yf>=y+0.25f ) result_YUV[0][y*img.width+x]=cfin;
							//mejor siempre
						    //siempre si yfin es height
						    if (yfin==img.height )//|| pppy<1.2f)
							if (yf<(float)y+0.5f) result_YUV[0][y*img.width+x]=cfin;
							
							
							//LA NUEVA
							//if (yf<(float)y+0.5f && y!=yfin) result_YUV[0][y*img.width+x]=cfin;
							//if (yf<(float)y+0.5f )	result_YUV[0][y*img.width+x]=cfin;
							
							
							//if (yf<(float)y+0.5f && yf>=y+0.25f && y!=yfin) result_YUV[0][y*img.width+x]=cfin;
							// finally is better to fill here the pixel, even if the % <30%
							//if (yf<(float)x+0.5f)	result_YUV[0][y*img.width+x]=cfin;

						}
					}


				}

				cfin_ant=cfin;

				//y_anti=y;
				y_antif=yf;
				//yf+=pppy/2f;
				pppy+=gry_sc;

				yf+=pppy;//ok


			}//y
			ppp_yc+=grycy_sc;
			ppp_yd+=grydy_sc;

		}//x
	}
	
	
	//*******************************************************************
	/**
	 * vertical bilineal interpolation
	 * 
	 * the interpolation process is: first vertical then horizontal
	 * @param src_YUV
	 * @param result_YUV
	 */
	public void interpolateBilinealV( int[][] src_YUV, int[][] result_YUV)
	{
		//------------------------------- VERTICAL INTERPOLATION----------------
		float lenx=lx_sc;
//lenx=lx;
		//gradient pppy of side c
		float grycy_sc=(ppp[1][1]-ppp[1][0])/(lenx-1);//lya_sc;
		//gradient PPPy side d
		float grydy_sc=(ppp[1][3]-ppp[1][2])/(lenx-1);

		//en la vertical no hace falta porque el bloque esta comprimido y no desplazado
		//vertical interpolation always is executed before H interpolation
		//therefore source is a block not shifted.  No need for extrapolation
		float ppp_yc=ppp[1][0];
		float ppp_yd=ppp[1][2];

		// pppx initialized to ppp_yc
		float pppy=ppp_yc;

		int y=0;

		//dont scan lx but lenx, which is lx_sc. 
		for (int x=xini;x<xini+lenx;x++)
		{

			float gry_sc=0;
			if (ppp_yc!=ppp_yd) 
				gry_sc=(2*ly-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);


			//float gry_sc=(ppp_yd-ppp_yc)/(ly_sc-1);

			//ppp_yc is updated at each iteration
			pppy=ppp_yc;//-grycy_sc;


			// yf is the end of the segment
			float yf=yini+pppy/2f;
			
			//NUEVO
			//float yf=yini+pppy/2f-0.5f;
			
			float y_antif=yf-pppy;

			// bucle for horizontal scanline 
			// scans the downsampled image, pixel by pixel
			int cfin_ant=0;
			//int coordy=y*img.width;
			for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
			{

				//coord "y" is the last pixel influenced by the segment y_sc-1 <----> y_sc
				y=(int) (yf); 

				int cini=0;
				int cfin=src_YUV[0][(y_sc)*img.width+x];

				if (y_sc==yini)//no se puede interpolar
				{
					//int cfin=src_YUV[0][(y_sc)*img.width+x];
					
					//IF yf< y+0.5 then this pixel is painted in the next step
					//LA BUENA
					if (yf>=y+0.5f && yf<=y+0.75f) result_YUV[0][y*img.width+x]=cfin;
					//mejor siempre
					//siempre si yini es cero
					if (yini==0 )//|| pppy<1.2f)
					if (yf>=y+0.5f) result_YUV[0][y*img.width+x]=cfin;
					
					//LA NUEVA
					//if (yf>=y+0.5f && x!=xini) result_YUV[0][y*img.width+x]=cfin;
					//if (yf>=y+0.5f) result_YUV[0][y*img.width+x]=cfin;
					
					//if (yf>=y+0.5f && yf<=y+0.75f && y!=yini) result_YUV[0][y*img.width+x]=cfin;
					
					//if ( yf<=y+0.75f) result_YUV[0][y*img.width+x]=cfin;
					// finally is better to fill here the pixel, even if the % <30%
					//if (yf>=y+0.5f ) result_YUV[0][y*img.width+x]=cfin;
				}

				else if (y_sc!=yini  && cfin!=0)	
				{
					cini=cfin_ant;//src_YUV[0][(y_sc-1)*img.width+x];

					if (cini!=0) //interpolation is possible
					{
						//int cfin=src_YUV[0][(y_sc)*img.width+x];;

						//interpolation gradient y
						float  igrady=(float)(cfin-cini)/pppy;
						// colour of each pixel y=i inside segment is the corresponding  y = i.5
						//=======================================================================

						//colour initial segment
						//float cis=cini+(int)(0.5f+((((int)(y_antif+0.5f)+0.5f)-y_antif)*igrady));
						float cis=cini+(((int)(y_antif+0.5f)+0.5f)-y_antif)*igrady;
						for (int i=(int)(y_antif+0.5f);i<=(int)(yf-0.5f);i++)
						{
							//int color=cini+(int)(0.5f+((((float)i+0.5f)-y_antif)*igrady));
							//result_YUV[0][i*img.width+x]=color;
							
							//result_YUV[0][i*img.width+x]=(int)(cis+0.5f);
							
							//al ser float debo chequear <1 y no <=0
							//if (cis<1) cis=1f;
							//else if (cis>255) cis=255f;
							
							float cis2=cis+ajuste;
							if (cis2<1) cis2=1f;
							else if (cis2>255) cis2=255f;
							
							result_YUV[0][i*img.width+x]=(int)(cis2);//por coherencia con gap
							
							
							//20141118
							//if (result_YUV[0][i*img.width+x]<=0)result_YUV[0][i*img.width+x]=1;
							//else if (result_YUV[0][i*img.width+x]>255) result_YUV[0][i*img.width+x]=255;
							
							
							cis+=igrady;
						}
						//comprobamos s no se ha podido pintar el ultmo pix

						//si el ultimo pix no se puede pintar, es mejor pintarlo a pesar de ello?
						//if (y_sc==yini+ly_sc-1)
						if (y_sc==downsampled_yfin)
						{

							//if yf>=y+0.5f then the pixel is already painted
							//if (yf>=y+0.5f && result_YUV[0][y*img.width+x]==0) System.out.println("WARNING");
							// a 30% must be covered to be filled here. otherwise it will be interpolated between blocks

							//LA BUENA
							//float dif=((float)y+0.5f)-yf;
							//cfin=(int)(cfin+igrady*dif+0.5f);
						    if (yf<(float)y+0.5f && yf>=y+0.25f ) result_YUV[0][y*img.width+x]=cfin;
							//mejor siempre
						    //siempre si yfin es height
						    if (yfin==img.height )//|| pppy<1.2f)
							if (yf<(float)y+0.5f) result_YUV[0][y*img.width+x]=cfin;
							
							
							//LA NUEVA
							//if (yf<(float)y+0.5f && y!=yfin) result_YUV[0][y*img.width+x]=cfin;
							//if (yf<(float)y+0.5f )	result_YUV[0][y*img.width+x]=cfin;
							
							
							//if (yf<(float)y+0.5f && yf>=y+0.25f && y!=yfin) result_YUV[0][y*img.width+x]=cfin;
							// finally is better to fill here the pixel, even if the % <30%
							//if (yf<(float)x+0.5f)	result_YUV[0][y*img.width+x]=cfin;

						}
					}


				}

				cfin_ant=cfin;

				//y_anti=y;
				y_antif=yf;
				//yf+=pppy/2f;
				pppy+=gry_sc;

				yf+=pppy;//ok


			}//y
			ppp_yc+=grycy_sc;
			ppp_yd+=grydy_sc;

		}//x
	}	

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%	
	public void interpolateBilinealV_false( int[][] src_YUV, int[][] result_YUV)
	{
		//------------------------------- VERTICAL INTERPOLATION----------------
		float lenx=lx_sc;
//lenx=lx;
		//gradient pppy of side c
		float grycy_sc=(ppp[1][1]-ppp[1][0])/(lenx-1);//lya_sc;
		//gradient PPPy side d
		float grydy_sc=(ppp[1][3]-ppp[1][2])/(lenx-1);

		//en la vertical no hace falta porque el bloque esta comprimido y no desplazado
		//vertical interpolation always is executed before H interpolation
		//therefore source is a block not shifted.  No need for extrapolation
		float ppp_yc=ppp[1][0];
		float ppp_yd=ppp[1][2];

		// pppx initialized to ppp_yc
		float pppy=ppp_yc;

		int y=0;

		//dont scan lx but lenx, which is lx_sc. 
		for (int x=xini;x<xini+lenx;x++)
		{

			float gry_sc=0;
			if (ppp_yc!=ppp_yd) 
				gry_sc=(2*ly-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);


			//float gry_sc=(ppp_yd-ppp_yc)/(ly_sc-1);

			//ppp_yc is updated at each iteration
			pppy=ppp_yc;//-grycy_sc;


			// yf is the end of the segment
			float yf=yini;//+pppy/2f;
			
			
			float y_antif=yini; //yf-pppy;

			// bucle for horizontal scanline 
			// scans the downsampled image, pixel by pixel
			
			int cfin=src_YUV[0][(yini)*img.width+xini];
			int cfin_ant=cfin;
			int cini=cfin;
			//int coordy=y*img.width;
			for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
			{

				//coord "y" is the last pixel influenced by the segment y_sc-1 <----> y_sc
				y=(int) (yf); 

				cini=cfin_ant;
				cfin=src_YUV[0][(y_sc)*img.width+x];
								
				
				//interpolation gradient y
				float  igrady=(float)(cfin-cini)/pppy;
				// colour of each pixel y=i inside segment is the corresponding  y = i.5
				//=======================================================================
				//colour initial segment
				float cis=cini+(((int)(y_antif+0.5f))-y_antif)*igrady;
				for (int i=(int)(y_antif+0.5f);i<=(int)(yf);i++)
				  {
						float cis2=cis;
						if (cis2<1) cis2=1f;
						else if (cis2>255) cis2=255f;
							
						result_YUV[0][i*img.width+x]=(int)(cis2);//por coherencia con gap
						cis+=igrady;
					}
					
				//comprobamos s no se ha podido pintar el ultmo pix

				if (y_sc==downsampled_yfin)
						{
							for (int i=(int)(y_antif+0.5f);i<=yfin;i++)
							result_YUV[0][i*img.width+x]=cfin;
							
						}
					
					
				


				

				cfin_ant=cfin;

				//y_anti=y;
				y_antif=yf;
				//yf+=pppy/2f;
				//pppy+=gry_sc;

				yf+=pppy;//ok
				pppy+=gry_sc;

			}//y
			ppp_yc+=grycy_sc;
			ppp_yd+=grydy_sc;

		}//x
	}	

	//*******************************************************************
	//*******************************************************************
	/**
	 * vertical interpolation in neighbour mode
	 *  
	 *  the interpolation process is: first vertical then horizontal
	 * @param src_YUV
	 * @param result_YUV
	 */
	public void interpolateNeighbourV( int[][] src_YUV, int[][] result_YUV)
	{
		float lenx=lx_sc;
//lenx=lx;
		//gradient PPPy side c
		float grycy_sc=(ppp[1][1]-ppp[1][0])/(lenx-1);//lya_sc;
		//gradient PPPy side d
		float grydy_sc=(ppp[1][3]-ppp[1][2])/(lenx-1);

		//en la vertical no hace falta porque el bloque esta comprimido y no desplazado
		float ppp_yc=ppp[1][0];//-grycy_sc*(ppp[0][0]/2)*((lx_sc)/(lx));
		float ppp_yd=ppp[1][2];//-grydy_sc*(ppp[0][2]/2)*((lx_sc)/(lx));

		// pppx initialized to ppp_yc
		float pppy=ppp_yc;

		//input image is downsampled_YUV. block width is lx_sc
		int y=0;

		//dont scan lx but lenx. ( lenx value is lx or lx_sc)
		for (int x=xini;x<xini+lenx;x++)
		{
			float gry_sc=0;
			//if (ppp_yc!=ppp_yd) gry_sc=(2*ly-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);

			gry_sc=(ppp_yd-ppp_yc)/(ly_sc-1);
			
			//ppp_yc is updated at each iteration
			pppy=ppp_yc;//-grycy_sc;

			float yf=yini+pppy-1f;//1;

			//previous interpolated y coordinate
			int y_anti=(int)(yf-pppy+0.5f);

			// bucle for horizontal scanline 
			// scans the downsampled image, pixel by pixel
			for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
			{
				//end of block in nearest neighbour mode
				//if ( y_sc==yini+ly_sc-1) yf=yfin;

				//integer interpolated y coordinate 
				y=(int) (yf+0.5f);
				if (y>yfin) y=yfin;//esta protección no hace falta

				if (y_sc==yini) 
				{
					//nearest neighbour mode: copy sample into ppp/2 pixels
					for (int i=yini;i<=y;i++)
					{
						result_YUV[0][i*img.width+x]=src_YUV[0][yini*img.width+x];
					}	
				}
				// y_sc is not yini  
				else 
				{
					for (int i=y_anti+1;i<=y;i++)
					{
						result_YUV[0][i*img.width+x]=src_YUV[0][y_sc*img.width+x];
					}
				}
				y_anti=y;
				pppy+=gry_sc;
				yf+=pppy;//ok
			}//y
			ppp_yc+=grycy_sc;
			ppp_yd+=grydy_sc;
		}//x
	}	
	//*******************************************************************
	/**
	 * horizontal interpolation in neighbour mode
	 * 
	 *  the interpolation process is: first vertical then horizontal
	 * @param src_YUV
	 * @param result_YUV
	 */
	public void interpolateNeighbourH( int[][] src_YUV, int[][] result_YUV)
	{

		float leny=ly;
//leny=ly_sc;
		float gryax_sc=0;
		//gradient x of side a
		if (ppp[0][2]!=ppp[0][0])
			gryax_sc=(ppp[0][2]-ppp[0][0])/(leny-1);//lya_sc;
		float grybx_sc=0;
		if (ppp[0][3]!=ppp[0][1])
			grybx_sc=(ppp[0][3]-ppp[0][1])/(leny-1);//b_sc;

		//initial ppp values of sides "a" and "b"
		float ppp_xa=ppp[0][0];
		float ppp_xb=ppp[0][1];


		// pppx is initialized to ppp_xa
		float pppx=ppp_xa;


		int x=0;
		//float grx_sc=(2*lx +ppp_xa-ppp_xb-2*ppp_xa*lx_sc)/((lx_sc-1)*lx_sc);
		int x_anti=0;


		for (int y=yini;y<yini+leny;y++)
		{
			//starts at side c ( which is y==yini)
			float grx_sc=0;

			//if (ppp_xa!=ppp_xb) grx_sc=(2*lx +ppp_xa-ppp_xb-2*ppp_xa*lx_sc)/((lx_sc-1)*lx_sc);				
			grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1);
			
			pppx=ppp_xa;
			// sample 

			float xf=xini+pppx-1;// -1 because xini exists

			x_anti=(int)(xf-pppx+0.5f);

			// bucle for horizontal scanline 
			for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
			{
				//if ( x_sc==xini+lx_sc-1) xf=xfin;

				x=(int) (xf+0.5f);
				if (x>xfin) x=xfin;

				if (x_sc==xini) 
				{
					//nearest neighbour:copy sample into ppp/2 pixels
					for (int i=xini;i<=x;i++)
					{
						result_YUV[0][y*img.width+i]=src_YUV[0][y*img.width+x_sc];
					}
				}// if x_sc==xini

				else  // x_sc is not xini
				{
					for (int i=x_anti+1;i<=x;i++)
					{
						result_YUV[0][y*img.width+i]=src_YUV[0][y*img.width+x_sc];
						//if (y==b.yini+leny-1) result_YUV[0][y*img.width+i]=255;
					}
				}

				x_anti=x;
				pppx+=grx_sc;
				xf+=pppx;///2;//ok
			}//x
			ppp_xa+=gryax_sc;
			ppp_xb+=grybx_sc;
		}//y
	}	
	//*******************************************************************
	public void interpolateBoundariesVlinear(int[][] result_YUV, int [][] src_YUV)
	{
		//if block is located at the right image edge, there is no need for interpolation
		if (xfin==img.width-1) return;
		
		//initialization for the scanline. side b of the block
		float ppp_yc=ppp[1][1];
		float ppp_yd=ppp[1][3];

		// pppy initialized to ppp_yc
		float pppy=ppp_yc;

		float gry_sc=0;
		if (ppp_yc!=ppp_yd) gry_sc=(2*ly-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);
		// yf is the end of the segment
		float yf=yini+pppy/2f;
		float y_antif=yini;//yf-pppy;

		// bucle for horizontal scanline 
		// scans the downsampled image, pixel by pixel
		int cfin_ant=0;
		//int y=yini;
		//int coordy=y*img.width;
		for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
		{

			//coord "y" is the last pixel influenced by the segment y_sc-1 <----> y_sc
			//y=(int) (yf); 
			//System.out.println("hola");
			//result_YUV[0][y_sc*img.width+xfin]=255;
			
			int cini=0;
			int cfin=src_YUV[0][(y_sc)*img.width+downsampled_xfin];

			if (y_sc==yini)//no se puede interpolar
			{
				
				for (int i=yini;i<=(int)(yf-0.5f);i++)
				{
					result_YUV[0][i*img.width+xfin]=cfin;
				}
				
			}
			else if (y_sc!=yini  && cfin!=0)	
			{
				cini=cfin_ant;//src_YUV[0][(y_sc-1)*img.width+x];

				if (cini!=0) //interpolation is possible
				{
					//int cfin=src_YUV[0][(y_sc)*img.width+x];;

					//interpolation gradient y
					float  igrady=(float)(cfin-cini)/pppy;
					// colour of each pixel y=i inside segment is the corresponding  y = i.5
					//=======================================================================

					//colour initial segment
					//float cis=cini+(int)(0.5f+((((int)(y_antif+0.5f)+0.5f)-y_antif)*igrady));
					float cis=cini+(((int)(y_antif+0.5f)+0.5f)-y_antif)*igrady;
					for (int i=(int)(y_antif+0.5f);i<=(int)(yf-0.5f);i++)
					{
						//int color=cini+(int)(0.5f+((((float)i+0.5f)-y_antif)*igrady));
						//result_YUV[0][i*img.width+x]=color;
						result_YUV[0][i*img.width+xfin]=(int)(cis);//+0.5f);
						cis+=igrady;
					}
					//ultimos ppp/2 pixeles
					if (y_sc==yini+ly_sc-1)
					{
						for (int i=(int)(yf+0.5f);i<=yfin;i++)
						{
							result_YUV[0][i*img.width+xfin]=cfin;
						}					
					}
				}
			}
			//result_YUV[0][y_sc*img.width+xfin]=255;
			cfin_ant=cfin;
			y_antif=yf;
			pppy+=gry_sc;
			yf+=pppy;//ok
		}//y
	}
	//**********************************************************************************
	public void interpolateBoundariesHlinear(int[][] result_YUV, int [][] src_YUV)
	{
		//if block is located at the bootom image edge, there is no need for interpolation
		if (yfin==img.height-1) return;
		
		//initialization for the scanline. side d of the block
		float ppp_xa=ppp[0][2];
		float ppp_xb=ppp[0][3];

		// pppx initialized to ppp_ya
		float pppx=ppp_xa;

		float grx_sc=0;
		if (ppp_xa!=ppp_xb) grx_sc=(2*lx-2*ppp_xa*(lx_sc)+ppp_xa-ppp_xb)/((lx_sc-1)*lx_sc);
		// xf is the end of the segment
		float xf=xini+pppx/2f;
		float x_antif=xini;//xf-pppx;

		
		
		
		// bucle for horizontal scanline 
		// scans the downsampled image, pixel by pixel
		int cfin_ant=0;
		//int y=yini;
		//int coordy=y*img.width;
		for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
		{

			int cini=0;
			int cfin=src_YUV[0][(downsampled_yfin)*img.width+x_sc];

			//inicio
			
			if (x_sc==xini)//no se puede interpolar desde xini hasta pppx/2
			{
				//result_YUV[0][yfin*img.width+xini]=cfin;	
				for (int i=xini;i<=(int)(xf-0.5f);i++)
				{
					result_YUV[0][yfin*img.width+i]=cfin;
				}
				
			}
			else if (x_sc!=xini  && cfin!=0)	
			{
				cini=cfin_ant;//src_YUV[0][(y_sc-1)*img.width+x];

				if (cini!=0) //interpolation is possible
				{
					//int cfin=src_YUV[0][(y_sc)*img.width+x];;

					//interpolation gradient y
					float  igradx=(float)(cfin-cini)/pppx;
					// colour of each pixel y=i inside segment is the corresponding  y = i.5
					//=======================================================================

					//colour initial segment
					//float cis=cini+(int)(0.5f+((((int)(y_antif+0.5f)+0.5f)-y_antif)*igrady));
					float cis=cini+(((int)(x_antif+0.5f)+0.5f)-x_antif)*igradx;
					for (int i=(int)(x_antif+0.5f);i<=(int)(xf-0.5f);i++)
					{
						//int color=cini+(int)(0.5f+((((float)i+0.5f)-y_antif)*igrady));
						//result_YUV[0][i*img.width+x]=color;
						
						result_YUV[0][yfin*img.width+i]=(int)(cis);//+0.5f);
						cis+=igradx;
					}
					//ultimos ppp/2 pixeles
					if (x_sc==xini+lx_sc-1)
					{
						for (int i=(int)(xf+0.5f);i<=xfin;i++)
						{
							result_YUV[0][yfin*img.width+i]=cfin;
						}					
					}
				}
			}
			//result_YUV[0][y_sc*img.width+xfin]=255;
			cfin_ant=cfin;
			x_antif=xf;
			pppx+=grx_sc;
			xf+=pppx;//ok
		}//y
	}	
	//*************************************************************************************
	public void downsampleBoundariesH(int[][] result_YUV, int [][] src_YUV)
	{
		
		if (yini==0) return;

		//initialization of ppp at side a and ppp at side b
		float ppp_xa=ppp[0][2];
		float ppp_xb=ppp[0][3];

		// initialization of pppx to ppp_xa
		float pppx=ppp_xa;

		float grx_sc=0;
		//all scanlines have the same width (=lenght of scaled scanline)
		//lx_sc is the lenght of scaled horizontal scanline. It is integer

		
		
			//calculation of gradient for this row. 
			//each row may have a different gradient although lx_sc is constant.

			if (ppp_xa!=ppp_xb) grx_sc=(2*lx-2*ppp_xa*(lx_sc))/((lx_sc-1)*lx_sc);
			//else grx_sc=0;

			

			//first set of pixels to conver in one pix begins at xini and ends at xini+pppx
			//xf is the float x coord  
			//
			//    xfant-----------xf

			float xf=xini+pppx;// 
			float xfant=xini;//xf-pppx;

			int xant=xini;

			// bucle for horizontal scanline
			//for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
			for (int x_sc=xini;x_sc<=downsampled_xfin;x_sc++)
			{
			

				int x=(int) (xf);
				
				if (x>xfin) 
				{x=xfin;
				//System.out.println(" ERROR:"+(xf-xfin-1));

				xf=xfin+1;
				pppx=xf-xfant;
				}
				
				/*
				if (x_sc==downsampled_xfin) // "y" can overtake yfin in 0.0000001. we must take care of it
				{
					x=xfin;
					xf=xfin+1;
					pppx=xf-xfant;
				}*/
				
				//este control lo podemos quitar. puede haber un error pero es de orden 0.001
				/*
				  if (x_sc==xini+lx_sc-1)
					if ((xf-1-xfin)>0.001) System.out.println("error  es:"+(xf-1-xfin) +  "    x_sc:"+x_sc+"  y:"+y+" lx_sc:"+lx_sc+" lx:"+lx);
                */

				float color=0;
				float porcent=(1-(xfant-xant));
				
				//if (porcent<0) porcent=0;
				
				//xf deberia cubrir todo el porcentaje siempre, ya que ppp minimo es 1
				//color+=porcent*src_YUV[0][yfin*img.width+xant];
				//if (x>xant)
					color+=porcent*src_YUV[0][(yini-1)*img.width+xant];
				//System.out.println("colorini:"+color+" porcent:"+porcent+" xant:"+xant+"  xantf="+xfant+"   pppx:"+pppx+" x:"+x+"  xf:"+xf+" xfin:"+xfin);
				for (int i=xant+1;i<x ;i++)
				{
					//color+=src_YUV[0][yfin*img.width+i];
					color+=src_YUV[0][(yini-1)*img.width+i];
				}
				//si estamos en xfin, xf deberia valer xfin
				//if (x_sc==xini+lx_sc-1) System.out.println("xf:"+xf+"   xfin:"+xfin +"   xf-xfin:"+(xf-xfin)+" pppxb:"+ppp_xb+"   pppx:"+pppx+" grx_sc:"+grx_sc);

				//con ppp=0.9999 puede ocurrir que xant y x sean lo mismo y por tanto ya ha sido sumado
				//if (xant<x)	color+=(xf-x)*src_YUV[0][yfin*img.width+x];
				
				//x es xant en el ultimo pix, ya que lo forzamos
				// en caso de ser iguales, ya hemos sumado en porcent?
				
				if (xant<x)	color+=(xf-x)*src_YUV[0][(yini-1)*img.width+x];
				//else color+=src_YUV[0][(yini-1)*img.width+x];
				//if (x=xfin)	color+=(xf-x)*src_YUV[0][(yini-1)*img.width+x];
				//if (xant>=x) color=255*pppx;
				//else color=255*pppx;
				color=color/pppx;
				int brillo=(int)(color+0.5f);
				if (brillo==0) brillo=1;
				else if (brillo>255) brillo=255;
				//if (brillo<0) System.out.println(" fallo en down boundaries H. brillo <0");
				if (brillo<0) System.out.println(" fallo en down boundaries H. brillo <0     valor:"+ (xf-x)+"  ppx:"+pppx+ " color:"+color);
				
				//if (x_sc==xini+lx_sc-1) brillo=255;
				//result_YUV[0][yfin*img.width+x_sc]=brillo;
				
				result_YUV[0][(yini-1)*img.width+x_sc]=brillo;


				pppx+=grx_sc;
				xant=x;
				xfant=xf;
				xf+=pppx;//ok

			}//x
			//fill the rest of the line. Not needed 
			//for (int i=downsampled_xfin+1;i<xfin;i++)result_YUV[0][yfin*img.width+i]=0;
	}
//*********************************************************************************	
	public void downsampleBoundariesV(int[][] result_YUV, int [][] src_YUV)
	{
	
        if (xini==0) return;
        
		//initialization of ppp at side c and ppp at side d
		float ppp_yc=ppp[1][1];
		float ppp_yd=ppp[1][3];

		// initialization of pppx to ppp_xa
		float pppy=ppp_yc;

		float gry_sc=0;
		//all scanlines have the same width (=lenght of scaled scanline)
		//lx_sc is the lenght of scaled horizontal scanline. It is integer

		

		
			//calculation of gradient for this row. 
			//each row may have a different gradient although lx_sc is constant.

			if (ppp_yc!=ppp_yd) gry_sc=(2*ly-2*ppp_yc*(ly_sc))/((ly_sc-1)*ly_sc);
			//else grx_sc=0;

			

			//first set of pixels to conver in one pix begins at xini and ends at xini+pppx
			//xf is the float x coord  
			//
			//    xfant-----------xf

			float yf=yini+pppy;// 
			float yfant=yini;//xf-pppx;

			int yant=yini;

			// bucle for horizontal scanline
			//for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
			for (int y_sc=yini;y_sc<=downsampled_yfin;y_sc++)
			{
			
				
				
				int y=(int) (yf);
				
				if (y>=yfin) 
				{y=yfin;
				
				
				//NO PUEDO HACER ESTO, O SALDRA UN PUNTEADO
				//yf=yfin+1f;
				//pppy=yf-yfant;
				//System.out.println(" ERROR:"+(xf-xfin-1));

				}
				/*
				if (y_sc==downsampled_yfin) // "y" can overtake yfin in 0.0000001. we must take care of it
				{
					y=yfin;
					yf=yfin+1;
					pppy=yf-yfant;
				}*/
				
				//este control lo podemos quitar. puede haber un error pero es de orden 0.001
				/*
				  if (x_sc==xini+lx_sc-1)
					if ((xf-1-xfin)>0.001) System.out.println("error  es:"+(xf-1-xfin) +  "    x_sc:"+x_sc+"  y:"+y+" lx_sc:"+lx_sc+" lx:"+lx);
                */

				float color=0;
				float porcent=(1-(yfant-yant));
				
				//if (porcent<0) porcent=0;
				//nuevo
				//if (y_sc==yini) porcent=0;
				
				//xf deberia cubrir todo el porcentaje siempre, ya que ppp minimo es 1
				//color+=porcent*src_YUV[0][yant*img.width+xfin];
				
				//esto es el trozo de pixel que hay que sumar del segmento anterior
				//if (porcent>0)
				//if (y>yant)
					color+=porcent*src_YUV[0][yant*img.width+xini-1];
				//if (xini==144 && y_sc>=80 && y_sc<90) System.out.println("porcent:"+porcent+"  color:"+color+"  xini:"+xini+" y_sc:"+y_sc+ "  ydownfin:"+downsampled_yfin+" yf:"+yf+" y:"+y+" yfant:"+yfant+ "    yfin:"+yfin+"  ------------------------------------------------------");
				//if (color<0) System.out.println(" warning  "+ porcent+ "  yfant:"+yfant+"  yant:"+yant+ " yini:"+yini+ " yfin:"+yfin);
				
				//System.out.println("colorini:"+color+" porcent:"+porcent+" xant:"+xant+"  xantf="+xfant+"   pppx:"+pppx+" x:"+x+"  xf:"+xf+" xfin:"+xfin);
				//en este bucle sumamos pixeles completos. no trozos
				for (int i=yant+1;i<y ;i++)
				{
					//color+=src_YUV[0][i*img.width+xfin];
					color+=src_YUV[0][i*img.width+xini-1];
					
				//	if (color<0) System.out.println(" warning");
				}
				
				//si estamos en xfin, xf deberia valer xfin
				//if (x_sc==xini+lx_sc-1) System.out.println("xf:"+xf+"   xfin:"+xfin +"   xf-xfin:"+(xf-xfin)+" pppxb:"+ppp_xb+"   pppx:"+pppx+" grx_sc:"+grx_sc);

				//con ppp=0.9999 puede ocurrir que xant y x sean lo mismo y por tanto ya ha sido sumado
				//if (yant<y)	color+=(yf-y)*src_YUV[0][y*img.width+xfin];
				
				//este es el trozo final de este segmento
				if (yant<y)	color+=(yf-y)*src_YUV[0][y*img.width+xini-1];
				//else color+=src_YUV[0][y*img.width+xini-1];
				//if (xant>=x) color=255*pppx;
				//else color=255*pppx;
				
				color=color/pppy;
				//color=color/(yf-yfant);
				
				
				
				
				
				int brillo=(int)(color+0.5f);
				//if (xini==144 && y_sc>=80 && y_sc<90) System.out.println("brillo:"+brillo+ " ysc:"+y_sc);
				if (brillo==0) brillo=1;
				else if (brillo>255) brillo=255;
				
				
				if (brillo<0) System.out.println(" fallo en down boundaries V. brillo <0     valor:"+ (yf-y)+"  ppy:"+pppy+ "color:"+color);
				//if (x_sc==xini+lx_sc-1) brillo=255;
				//result_YUV[0][y_sc*img.width+xfin]=brillo;
				result_YUV[0][y_sc*img.width+xini-1]=brillo;

				pppy+=gry_sc;
				yant=y;
				yfant=yf;
				yf+=pppy;//ok

			}//x
			//if (xini==144 && yini=73) System.exit(0);
			//fill the rest of the line. Not needed 
			//for (int i=downsampled_yfin+1;i<yfin;i++)result_YUV[0][i*img.width+xfin]=0;	
		
		
		
	}
	
	//****************************************************************************
	public void interpolateBoundariesVneighbour( int[][] src_YUV, int[][] result_YUV)
	{
		//reflexiones apoyadas en experimentos: 
		//como el down ya contiene pixels mixed, aqui no mezclamos de nuevo. tan solo
		//pintamos los pixeles finales de un color o del siguiente
		//ademas despues el resultado va a ser nuevamente downsampled y ahi se van a mezclar de nuevo los 
		//pixeles por lo que no gano calidad mezclandolos aqui.
		//tampoco gano calidad haciendo una interpolacion lineal, esto esta comprobado
		//es mas, se pierde calidad con la lineal, quizas debido a que se desvirtua el valor de los pixeles reales
		
		if (xfin==img.width-1) return;
		
		//en la vertical no hace falta porque el bloque esta comprimido y no desplazado
		float ppp_yc=ppp[1][1];//-grycy_sc*(ppp[0][0]/2)*((lx_sc)/(lx));
		float ppp_yd=ppp[1][3];//-grydy_sc*(ppp[0][2]/2)*((lx_sc)/(lx));

		
		//input image is downsampled_YUV. block width is lx_sc
		int y=0;

		//dont scan ly but ly_sc.
		   float gry_sc=0;
			//if (ppp_yc!=ppp_yd) gry_sc=(2*ly-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);
		   //gry_sc=(2*(ly-1)-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);
			gry_sc=(ppp_yd-ppp_yc)/(ly_sc-1);
			
			float pppy=ppp_yc;//-grycy_sc;

			float yf=yini+pppy-1;

			//previous interpolated y coordinate
			int y_anti=(int)(yf-pppy+0.5f);

			
			// scans the downsampled image, pixel by pixel
			for (int y_sc=yini;y_sc<=downsampled_yfin;y_sc++)
			{
				//end of block in nearest neighbour mode
				//if ( y_sc==downsampled_yfin) yf=yfin;
				

				//integer interpolated y coordinate 
				y=(int) (yf+0.5f);
				if (y>yfin) y=yfin;//esta protección no hace falta SI HACE FALTA!!!

				if (y_sc==yini) 
				{
					//nearest neighbour mode: copy sample into ppp/2 pixels
					for (int i=yini;i<=y;i++)
					{
						result_YUV[0][i*img.width+xfin]=src_YUV[0][yini*img.width+downsampled_xfin];
					}	
				}
				// y_sc is not yini  
				else 
				{
					for (int i=y_anti+1;i<=y;i++)
					{
						//int a= src_YUV[0][y_sc*img.width+downsampled_xfin];
						//try{
						result_YUV[0][i*img.width+xfin]=src_YUV[0][y_sc*img.width+downsampled_xfin];
						//}catch (Exception e)
					//	{
						//	System.out.println("i:"+i+" xini:"+xini+"  yini:"+yini+"  xfin:"+xfin+"  yfin:"+yfin+" yf:"+yf);
							//System.exit(0);
						//}
						
					}
				}
				y_anti=y;
				pppy+=gry_sc;
				yf+=pppy;//ok
			}//y
			
	}	
	//*******************************************************************
	
		public void interpolateBoundariesHneighbour( int[][] src_YUV, int[][] result_YUV)
		{
			//reflexiones apoyadas en experimentos: 
			//como el down ya contiene pixels mixed, aqui no mezclamos de nuevo. tan solo
			//pintamos los pixeles finales de un color o del siguiente
			//ademas despues el resultado va a ser nuevamente downsampled y ahi se van a mezclar de nuevo los 
			//pixeles por lo que no gano calidad mezclandolos aqui.
			//tampoco gano calidad haciendo una interpolacion lineal, esto esta comprobado
			//es mas, se pierde calidad con la lineal, quizas debido a que se desvirtua el valor de los pixeles reales
			
			
			
			if (yfin==img.height-1) return;
			
			//en la vertical no hace falta porque el bloque esta comprimido y no desplazado
			float ppp_xa=ppp[0][2];//-grycy_sc*(ppp[0][0]/2)*((lx_sc)/(lx));
			float ppp_xb=ppp[0][3];//-grydy_sc*(ppp[0][2]/2)*((lx_sc)/(lx));

			
			//input image is downsampled_YUV. block width is lx_sc
			int x=0;

			//dont scan ly but ly_sc.
			   //float grx_sc=0;
			//	if (ppp_xa!=ppp_xb) grx_sc=(2*lx-2*ppp_xa*(lx_sc)+ppp_xa-ppp_xb)/((lx_sc-1)*lx_sc);
			//float grx_sc=(2*(lx-1)-2*ppp_xa*(lx_sc)+ppp_xa-ppp_xb)/((lx_sc-1)*lx_sc);
				float grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1);
				//hola
				float pppx=ppp_xa;//-grycy_sc;

				float xf=xini+pppx-1;

				//previous interpolated y coordinate
				int x_anti=(int)(xf-pppx+0.5f);

				
				// scans the downsampled image, pixel by pixel
				for (int x_sc=xini;x_sc<=downsampled_xfin;x_sc++)
				{
					//end of block in nearest neighbour mode
					//if ( x_sc==downsampled_xfin) xf=xfin;

					//integer interpolated y coordinate 
					x=(int) (xf+0.5f);
					if (x>xfin) x=xfin;//esta protección no hace falta

					if (x_sc==xini) 
					{
						//nearest neighbour mode: copy sample into ppp/2 pixels
						for (int i=xini;i<=x;i++)
						{
							result_YUV[0][yfin*img.width+i]=src_YUV[0][downsampled_yfin*img.width+x_sc];
						}	
					}
					// y_sc is not yini  
					else 
					{
						for (int i=x_anti+1;i<=x;i++)
						{
							result_YUV[0][yfin*img.width+i]=src_YUV[0][downsampled_yfin*img.width+x_sc];
						}
					}
					x_anti=x;
					pppx+=grx_sc;
					xf+=pppx;//ok
				}//y
				
		}	
		//*******************************************************************
		//**************************************************************************************************
		public void downsampleH_FIX( int[][] src_YUV, int[][] result_YUV)
		{
			//we assume that the block has already rectangle shape, 
			// during this "Horizontal" processing we fill  the "horizontal_downsampled_YUV" array
			//gradient PPPx side a


			float leny=ly;

			//gradient side a
			float gryax_sc=(ppp[0][2]-ppp[0][0])/(leny-1);
			

			//gradient PPPx side b
			float grybx_sc=(ppp[0][3]-ppp[0][1])/(leny-1);

			//initialization of ppp at side a and ppp at side b
			float ppp_xa=ppp[0][0];
			float ppp_xb=ppp[0][1];

			// initialization of pppx to ppp_xa
			float pppx=ppp_xa;

			float grx_sc=0;
			//all scanlines have the same width (=lenght of scaled scanline)
			//lx_sc is the lenght of scaled horizontal scanline. It is integer

			//already computed, but , to be sure 
			//if (lx_sc==0) lx_sc=(int)(0.5f+(2.0f*lx)/(ppp_xb+ppp_xa));
			

			for (int y=yini;y<=yfin;y++)
			{
				//calculation of gradient for this row. 
				//each row may have a different gradient although lx_sc is constant.

				//if (ppp_xa!=ppp_xb)
				//grx_sc=(2*lx-2*ppp_xa*(lx_sc))/((lx_sc-1)*lx_sc);
				
				
				//grx_sc=(2*(xfin-xini)-2*ppp_xa*(lx_sc))/((lx_sc-1)*lx_sc);
				//else grx_sc=0;

				grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1f); //lx_sc -1 steps
				
				//System.out.println("  "+(grx_sc-grx_sc2));



				//System.out.println("lx_sc:"+lx_sc+" lx:"+lx+" lx*ppp:"+(lx_sc*ppp_xa));

				//initialization of pppx at start of scanline
				pppx=ppp_xa;

				//first set of pixels to conver in one pix begins at xini and ends at xini+pppx
				//xf is the float x coord  
				//
				//    xfant-----------xf

				float xf=xini+pppx;// esto ya se coloca por delante en 1 lo cual esta bien 
				
				float xfant=xini;//xf-pppx;

				int xant=xini;

				// bucle for horizontal scanline
				//for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
				//System.out.println("Scanline:"+xf);
				
				for (int x_sc=xini;x_sc<=downsampled_xfin;x_sc++)
				{
				/*	
				int maxc=-1000;
                int minc=1000;   
                int inic=-1000;
                int finc=-1000;
                int cant=-1000;
                boolean rulemax=true;
                int statemax=0;
                boolean rulemin=true;
                int statemin=0;
                */
				//	System.out.println("xf:"+xf);
                    if (x_sc==downsampled_xfin) {xf=xfin+1f;pppx=xf-xfant;}
                   
					//if (xfant>=xf) {System.out.println(" error en downH");System.exit(0);}
                    
                    int x=(int) (xf);
					
					/*
					
					if (x>xfin) 
					{x=xfin;
					//System.out.println(" ERROR:"+(xf-xfin-1));

					
					//xf=xfin;
					xf=xfin+1;
					pppx=xf-xfant;
					}
					*/
					//este control lo podemos quitar. puede haber un error pero es de orden 0.001
					/*
					  if (x_sc==xini+lx_sc-1)
						if ((xf-1-xfin)>0.001) System.out.println("error  es:"+(xf-1-xfin) +  "    x_sc:"+x_sc+"  y:"+y+" lx_sc:"+lx_sc+" lx:"+lx);
	                */

					float color=0;
					float porcent=(1-(xfant-xant));
					
					//if (porcent<0) System.out.println(" down h me cago en todo");
					
					//float centro=xfant+(xf-xfant)/2f;
					//float centromax=-1000;
					//float centromin=-1000;
					//float lenpix=pppx/2f;
					//float peso=lenpix-Math.abs(centro-xant+0.5f);
					//float tot_peso=peso*porcent;
					
					
					
					//xf deberia cubrir todo el porcentaje siempre, ya que ppp minimo es 1
					//color+=porcent*src_YUV[0][y*img.width+xant]*peso;
					color+=porcent*src_YUV[0][y*img.width+xant];
					//float tot_peso=porcent;
					/*
					inic=src_YUV[0][y*img.width+xant];
					cant=inic;
					maxc=cant;
					minc=cant;
					finc=cant;
					*/
					//System.out.println("colorini:"+color+" porcent:"+porcent+" xant:"+xant+"  xantf="+xfant+"   pppx:"+pppx+" x:"+x+"  xf:"+xf+" xfin:"+xfin);
					for (int i=xant+1;i<x ;i++)
					{
						//peso=lenpix-Math.abs(centro-i+0.5f);
						//tot_peso+=peso;
						//color+=src_YUV[0][y*img.width+i]*peso;
						color+=src_YUV[0][y*img.width+i];
						//tot_peso+=1;
						/*
						if (cant>src_YUV[0][y*img.width+i] && statemax==0) statemax=1;
						else if (cant<src_YUV[0][y*img.width+i] && statemax==1) rulemax=false;
						
						if (cant<src_YUV[0][y*img.width+i] && statemin==0) statemin=1;
						else if (cant>src_YUV[0][y*img.width+i] && statemin==1) rulemin=false;
						
						if (maxc<src_YUV[0][y*img.width+i]) {maxc=src_YUV[0][y*img.width+i];centromax=Math.abs(centro-i-0.5f);}
						if (minc>src_YUV[0][y*img.width+i]) {minc=src_YUV[0][y*img.width+i];centromin=Math.abs(centro-i-0.5f);}
						if (inic==-1000) inic=src_YUV[0][y*img.width+i];
						finc=src_YUV[0][y*img.width+i];
						cant=src_YUV[0][y*img.width+i];
						*/
					}
					//si estamos en xfin, xf deberia valer xfin
					//if (x_sc==xini+lx_sc-1) System.out.println("xf:"+xf+"   xfin:"+xfin +"   xf-xfin:"+(xf-xfin)+" pppxb:"+ppp_xb+"   pppx:"+pppx+" grx_sc:"+grx_sc);

					//con ppp=0.9999 puede ocurrir que xant y x sean lo mismo y por tanto ya ha sido sumado
					//if (xant<x)
					//System.out.println("xf:"+xf+"   x:"+x+"    xfin:"+xfin+"  downxfin:"+downsampled_xfin+"  xsc:"+x_sc);
					//if (xf>x) {peso=lenpix-Math.abs(centro-x+0.5f);tot_peso+=peso*(xf-x);}
						
					//if (xf>x) color+=(xf-x)*src_YUV[0][y*img.width+x]*peso;
					if (xf>x) {color+=(xf-x)*src_YUV[0][y*img.width+x];}

						//if (xf>=x+0.5f) finc=src_YUV[0][y*img.width+x];
						
					//if (xant>=x) color=255*pppx;
					//else color=255*pppx;
					color=color/(pppx);
					//color=color/(xf-xfant);//deberia dar lo mismo que con pppx, pero sale infinitesimalmente mejor asi
					int brillo=(int)(color+0.5f);
					
					//if (brillo>128) brillo =(int)((float)brillo*1.05f);
					//if (brillo<128) brillo =(int)((float)brillo*0.95f);
					
					if (brillo<=0) brillo=1;
					else if (brillo>255) brillo=255;
					
					//if (rulemax==true && centromax!=-1000) brillo=(int)((float)maxc*(pppx-2*centromax)/pppx+(float)brillo*(2*centromax)/pppx);
					//else if (rulemin==true && centromin!=-1000) brillo=(int)((float)minc*(pppx-2*centromin)/pppx+(float)brillo*(2*centromin)/pppx);;
					//if (inic<maxc && maxc>finc && inic!=-1000 && finc!=-1000 && maxc!=1000 && maxc>minc)brillo=maxc;
					//else if (inic>minc && minc<finc && inic!=-1000 && finc!=-1000 && minc!=-1000 && minc<maxc)brillo=minc;
					
					//if (brillo>cant && cant!=-1000 && maxc>-1000 && maxc>0) brillo=maxc;
					//else if (brillo<cant && cant!=-1000 && minc<1000 && minc<550) brillo=minc;
					//cant=brillo;
					
					if (brillo<0) 
						{System.out.println(" brillo<0 imposible en down H "+"   xf:"+xf+"   xfant:"+xfant+"   x:"+x+"  brillo:"+brillo+"   ppx:"+pppx);
						System.out.println("lx_sc:"+lx_sc+"  downxfin:"+downsampled_xfin+" xsc:"+x_sc+"   pppxa:"+ppp_xa+"  pppx_b:"+ppp_xb+"   xini:"+xini+"   xfin:"+xfin);
						System.out.println("yini:"+yini+" yfin:"+yfin+" y:"+y+"  ppp0:"+ppp[0][0]+"   ppp2:"+ppp[0][2]);
						System.out.println("grx_sc:"+grx_sc);
						System.out.println("y:"+y+"   yini:"+yini+"   yfin:"+yfin);
						System.exit(0);
						}
					//if (x_sc==xini+lx_sc-1) brillo=255;
					
					
					result_YUV[0][y*img.width+x_sc]=brillo;

					//pppx=xf-xfant;
					pppx+=grx_sc;
					xant=x;
					xfant=xf;
					xf+=pppx;//ok

				}//x

				//if (y==yini+ly-1 ) System.out.println("y:"+y+"   ppp_xa:"+ppp_xa+"   ppp[0][2]"+ppp[0][2]);
				ppp_xa+=gryax_sc;
				ppp_xb+=grybx_sc;

			}//y
		//	downsampled_xfin=(int)lx_sc+xini-1;

		}

		//****************************************************************************************

		public void downsampleV_FIX(int[][] src_YUV, int[][] result_YUV)
		{
			//System.out.println("enter in  downsampleVfloat");
			//-------------------------------------------------------------------------
			//during this vertical processing, we fill "downsampled_YUV" array
			float ppp_yc=ppp[1][0];
			float ppp_yd=ppp[1][2];
			float pppy=ppp_yc;


			float lenx=lx_sc;

			//gradient side c
			float grycy_sc=(ppp[1][1]-ppp[1][0])/(lenx-1);
			
			//gradient side d
			float grydy_sc=(ppp[1][3]-ppp[1][2])/(lenx-1);


			//already computed
			//if (ly_sc==0)ly_sc=(int)(0.5f+(2.0f*ly)/(ppp_yd+ppp_yc));

			int x_end=xini+(int)lx_sc-1;//downsampled_xfin;

			//System.out.println("xend:"+x_end);
			for (int x=xini;x<=x_end;x++)
			{



				//float gry_sc=(2*ly-2*ppp_yc*(ly_sc))/((ly_sc-1)*ly_sc);
				//float gry_sc=(2*(ly-1)-2*ppp_yc*(ly_sc))/((ly_sc-1)*ly_sc);


				float gry_sc=(ppp_yd-ppp_yc)/(ly_sc-1);
				//else  gry_sc=0;
				//System.out.println(""+gry_sc);
				pppy=ppp_yc; 

				//System.out.println("pppyc="+ppp_yc+ "    pppyd:"+ppp_yd+" pppy:"+pppy);
				if (pppy<0.9) {
					System.out.println("pppy:"+pppy);
					System.exit(0);
				}
				// yfant --------yf
				float yf=yini+pppy; 
				float yfant=yini;
				int yant=yini;


				//System.out.println("Y:");
				//System.out.println("x:"+x+ " ly_sc:"+ly_sc);
				//int cant=-1000;
				for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
				{
					/*int maxc=-1000;
	                int minc=1000;
	                int inic=-1000;
	                int finc=-1000;
	                */
					//System.out.println("y_sc:"+y_sc);

					if (y_sc==downsampled_yfin) {yf=yfin+1;pppy=yf-yfant;}
					
					
					int y=(int) yf;
					
					/*
					if (y>yfin) {
						y=yfin;
						
						
						yf=yfin+1;
						pppy=yf-yfant;
						
					}

					if (y_sc==yini+ly_sc-1)
						if ((yf-1-yfin)>0.01) System.out.println("error  es:"+(yf-1-yfin) +  "    y_sc:"+y_sc+"  y:"+y+" ly_sc:"+ly_sc+" ly:"+ly);
*/
					float color=0;
					float porcent=(1-(yfant-yant));

					//float tot_peso=porcent;
					//if (porcent<0) System.out.println(" down v me cago en todo");
					
					//color+=porcent*=src_YUV[0][(yant)*img.width+x];
					color+=porcent*src_YUV[0][(yant)*img.width+x];
					//if (inic==-1000 && porcent>=0.5f) inic=src_YUV[0][(yant)*img.width+x];
					
					for (int i=yant+1;i<y;i++)
					{
						color+=src_YUV[0][(i)*img.width+x];
						//tot_peso+=1;
						/*if (maxc>src_YUV[0][(i)*img.width+x]) maxc=src_YUV[0][(i)*img.width+x];
						if (minc<src_YUV[0][(i)*img.width+x]) minc=src_YUV[0][(i)*img.width+x];
						finc=src_YUV[0][(i)*img.width+x];
						if (inic==-1000) inic=src_YUV[0][(i)*img.width+x];
						*/
					}
					//System.out.println("y:"+y+" yf:"+yf+"  x:"+x);
					
					
					//if (yant<y) color+=(yf-y)*src_YUV[0][(y)*img.width+x];
					//else color=src_YUV[0][y*img.width+x]*pppy;
					
					if (yf>y) {color+=(yf-y)*src_YUV[0][(y)*img.width+x];}
					//if (yf>=y+0.5f)finc=src_YUV[0][(y)*img.width+x];
					
					color=color/pppy;
					//color=color/(yf-yfant);//tot_peso;
					int brillo=(int)(color+0.5f);
					if (brillo<=0)brillo=1;
					else if (brillo>255) brillo=255;

					//if (inic<maxc && maxc>finc && inic!=-1000 && finc!=-1000 && maxc!=1000 && maxc>minc)brillo=maxc;
					//else if (inic>minc && minc<finc && inic!=-1000 && finc!=-1000 && minc!=-1000 && minc<maxc)brillo=minc;
					
					//if (brillo>cant && cant!=-1000 && maxc>-1000 && maxc>0) brillo=maxc;
					//else if (brillo<cant && cant!=-1000 && minc<1000 && minc<550) brillo=minc;
					//cant=brillo;
					//System.out.println("brillo:"+brillo);

					result_YUV[0][y_sc*img.width+x]=brillo;



					//yf+=pppy/2;
					pppy+=gry_sc;
					yant=y;
					yfant=yf;
					yf+=pppy;
				}//ysc
				ppp_yc+=grycy_sc;
				ppp_yd+=grydy_sc;

			}//x
			// finally, we fill the attributes containing the last downsampled coordinates, 
			// which are useful for later interpolation.

			downsampled_yfin=(int)ly_sc+yini-1; //NOT USED
			//System.out.println("out from   downsampleVfloat");
		}
			
		//*************************************************************************************
		public void downsampleBoundariesH_FIX(int[][] result_YUV, int [][] src_YUV)
		{
			
			if (yini==0) return;

			//initialization of ppp at side a and ppp at side b
			float ppp_xa=ppp[0][2];
			float ppp_xb=ppp[0][3];

			// initialization of pppx to ppp_xa
			float pppx=ppp_xa;

			//float grx_sc=0;
			//all scanlines have the same width (=lenght of scaled scanline)
			//lx_sc is the lenght of scaled horizontal scanline. It is integer

			
			
				//calculation of gradient for this row. 
				//each row may have a different gradient although lx_sc is constant.

				//float grx_sc=(2*lx-2*ppp_xa*(lx_sc))/((lx_sc-1)*lx_sc);
				//float grx_sc=(2*(lx-1)-2*ppp_xa*(lx_sc))/((lx_sc-1)*lx_sc);
				//else grx_sc=0;

				float grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1f);

				//first set of pixels to conver in one pix begins at xini and ends at xini+pppx
				//xf is the float x coord  
				//
				//    xfant-----------xf

				float xf=xini+pppx;// 
				float xfant=xini;//xf-pppx;

				int xant=xini;

				// bucle for horizontal scanline
				//for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
				for (int x_sc=xini;x_sc<=downsampled_xfin;x_sc++)
				{
				

					if (x_sc==downsampled_xfin) {xf=xfin+1f;pppx=xf-xfant;}
					
					int x=(int) (xf);
					
					/*
					if (x>xfin) 
					{x=xfin;
					//System.out.println(" ERROR:"+(xf-xfin-1));

					xf=xfin+1;
					pppx=xf-xfant;
					}
					*/
					/*
					if (x_sc==downsampled_xfin) // "y" can overtake yfin in 0.0000001. we must take care of it
					{
						x=xfin;
						xf=xfin+1;
						pppx=xf-xfant;
					}*/
					
					//este control lo podemos quitar. puede haber un error pero es de orden 0.001
					/*
					  if (x_sc==xini+lx_sc-1)
						if ((xf-1-xfin)>0.001) System.out.println("error  es:"+(xf-1-xfin) +  "    x_sc:"+x_sc+"  y:"+y+" lx_sc:"+lx_sc+" lx:"+lx);
	                */

					float color=0;
					float porcent=(1-(xfant-xant));
					
					//if (porcent<0) porcent=0;
					
					//xf deberia cubrir todo el porcentaje siempre, ya que ppp minimo es 1
					//color+=porcent*src_YUV[0][yfin*img.width+xant];
					//if (x>xant)
						color+=porcent*src_YUV[0][(yini-1)*img.width+xant];
					//System.out.println("colorini:"+color+" porcent:"+porcent+" xant:"+xant+"  xantf="+xfant+"   pppx:"+pppx+" x:"+x+"  xf:"+xf+" xfin:"+xfin);
					for (int i=xant+1;i<x ;i++)
					{
						//color+=src_YUV[0][yfin*img.width+i];
						color+=src_YUV[0][(yini-1)*img.width+i];
					}
					//si estamos en xfin, xf deberia valer xfin
					//if (x_sc==xini+lx_sc-1) System.out.println("xf:"+xf+"   xfin:"+xfin +"   xf-xfin:"+(xf-xfin)+" pppxb:"+ppp_xb+"   pppx:"+pppx+" grx_sc:"+grx_sc);

					//con ppp=0.9999 puede ocurrir que xant y x sean lo mismo y por tanto ya ha sido sumado
					//if (xant<x)	color+=(xf-x)*src_YUV[0][yfin*img.width+x];
					
					//x es xant en el ultimo pix, ya que lo forzamos
					// en caso de ser iguales, ya hemos sumado en porcent?
					
					//if (xant<x)	color+=(xf-x)*src_YUV[0][(yini-1)*img.width+x];
					if (xf>x)	color+=(xf-x)*src_YUV[0][(yini-1)*img.width+x];
					//else color+=src_YUV[0][(yini-1)*img.width+x];
					//if (x=xfin)	color+=(xf-x)*src_YUV[0][(yini-1)*img.width+x];
					//if (xant>=x) color=255*pppx;
					//else color=255*pppx;
					color=color/pppx;
					int brillo=(int)(color+0.5f);
					if (brillo==0) brillo=1;
					else if (brillo>255) brillo=255;
					//if (brillo<0) System.out.println(" fallo en down boundaries H. brillo <0");
					if (brillo<0) System.out.println(" fallo en down boundaries H. brillo <0     valor:"+ (xf-x)+"  ppx:"+pppx+ " color:"+color);
					
					//if (x_sc==xini+lx_sc-1) brillo=255;
					//result_YUV[0][yfin*img.width+x_sc]=brillo;
					
					result_YUV[0][(yini-1)*img.width+x_sc]=brillo;


					pppx+=grx_sc;
					xant=x;
					xfant=xf;
					xf+=pppx;//ok

				}//x
				//fill the rest of the line. Not needed 
				//for (int i=downsampled_xfin+1;i<xfin;i++)result_YUV[0][yfin*img.width+i]=0;
		}
	//*********************************************************************************			
		//*********************************************************************************	
		public void downsampleBoundariesV_FIX(int[][] result_YUV, int [][] src_YUV)
		{
		
	        if (xini==0) return;
	        
			//initialization of ppp at side c and ppp at side d
			float ppp_yc=ppp[1][1];
			float ppp_yd=ppp[1][3];

			// initialization of pppx to ppp_xa
			float pppy=ppp_yc;

			//float gry_sc=0;
			//all scanlines have the same width (=lenght of scaled scanline)
			//lx_sc is the lenght of scaled horizontal scanline. It is integer

			

			
				//calculation of gradient for this row. 
				//each row may have a different gradient although lx_sc is constant.

				//float gry_sc=(2*ly-2*ppp_yc*(ly_sc))/((ly_sc-1)*ly_sc);
				//float gry_sc=(2*(ly-1)-2*ppp_yc*(ly_sc))/((ly_sc-1)*ly_sc);
				//else grx_sc=0;

				float gry_sc=(ppp_yd-ppp_yc)/(ly_sc-1);

				//first set of pixels to conver in one pix begins at xini and ends at xini+pppx
				//xf is the float x coord  
				//
				//    xfant-----------xf

				float yf=yini+pppy;// 
				float yfant=yini;//xf-pppx;

				int yant=yini;

				// bucle for horizontal scanline
				//for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
				for (int y_sc=yini;y_sc<=downsampled_yfin;y_sc++)
				{
				
					
					if (y_sc==downsampled_yfin) {yf=yfin+1;pppy=yf-yfant;}
					int y=(int) (yf);
					
					/*
					
					if (y>=yfin) 
					{y=yfin;
					
					
					//NO PUEDO HACER ESTO, O SALDRA UN PUNTEADO
					//yf=yfin+1f;
					//pppy=yf-yfant;
					//System.out.println(" ERROR:"+(xf-xfin-1));

					}
					*/
					/*
					if (y_sc==downsampled_yfin) // "y" can overtake yfin in 0.0000001. we must take care of it
					{
						y=yfin;
						yf=yfin+1;
						pppy=yf-yfant;
					}*/
					
					//este control lo podemos quitar. puede haber un error pero es de orden 0.001
					/*
					  if (x_sc==xini+lx_sc-1)
						if ((xf-1-xfin)>0.001) System.out.println("error  es:"+(xf-1-xfin) +  "    x_sc:"+x_sc+"  y:"+y+" lx_sc:"+lx_sc+" lx:"+lx);
	                */

					float color=0;
					float porcent=(1-(yfant-yant));
					
					//if (porcent<0) porcent=0;
					//nuevo
					//if (y_sc==yini) porcent=0;
					
					//xf deberia cubrir todo el porcentaje siempre, ya que ppp minimo es 1
					//color+=porcent*src_YUV[0][yant*img.width+xfin];
					
					//esto es el trozo de pixel que hay que sumar del segmento anterior
					//if (porcent>0)
					//if (y>yant)
						color+=porcent*src_YUV[0][yant*img.width+xini-1];
					//if (xini==144 && y_sc>=80 && y_sc<90) System.out.println("porcent:"+porcent+"  color:"+color+"  xini:"+xini+" y_sc:"+y_sc+ "  ydownfin:"+downsampled_yfin+" yf:"+yf+" y:"+y+" yfant:"+yfant+ "    yfin:"+yfin+"  ------------------------------------------------------");
					//if (color<0) System.out.println(" warning  "+ porcent+ "  yfant:"+yfant+"  yant:"+yant+ " yini:"+yini+ " yfin:"+yfin);
					
					//System.out.println("colorini:"+color+" porcent:"+porcent+" xant:"+xant+"  xantf="+xfant+"   pppx:"+pppx+" x:"+x+"  xf:"+xf+" xfin:"+xfin);
					//en este bucle sumamos pixeles completos. no trozos
					for (int i=yant+1;i<y ;i++)
					{
						//color+=src_YUV[0][i*img.width+xfin];
						color+=src_YUV[0][i*img.width+xini-1];
						
					//	if (color<0) System.out.println(" warning");
					}
					
					//si estamos en xfin, xf deberia valer xfin
					//if (x_sc==xini+lx_sc-1) System.out.println("xf:"+xf+"   xfin:"+xfin +"   xf-xfin:"+(xf-xfin)+" pppxb:"+ppp_xb+"   pppx:"+pppx+" grx_sc:"+grx_sc);

					//con ppp=0.9999 puede ocurrir que xant y x sean lo mismo y por tanto ya ha sido sumado
					//if (yant<y)	color+=(yf-y)*src_YUV[0][y*img.width+xfin];
					
					//este es el trozo final de este segmento
					//if (yant<y)	color+=(yf-y)*src_YUV[0][y*img.width+xini-1];
					if (yf>y)	color+=(yf-y)*src_YUV[0][y*img.width+xini-1];
					//else color+=src_YUV[0][y*img.width+xini-1];
					//if (xant>=x) color=255*pppx;
					//else color=255*pppx;
					
					color=color/pppy;
					//color=color/(yf-yfant);
					
					
					
					
					
					int brillo=(int)(color+0.5f);
					//if (xini==144 && y_sc>=80 && y_sc<90) System.out.println("brillo:"+brillo+ " ysc:"+y_sc);
					if (brillo==0) brillo=1;
					else if (brillo>255) brillo=255;
					
					
					if (brillo<0) System.out.println(" fallo en down boundaries V. brillo <0     valor:"+ (yf-y)+"  ppy:"+pppy+ "color:"+color);
					//if (x_sc==xini+lx_sc-1) brillo=255;
					//result_YUV[0][y_sc*img.width+xfin]=brillo;
					result_YUV[0][y_sc*img.width+xini-1]=brillo;

					pppy+=gry_sc;
					yant=y;
					yfant=yf;
					yf+=pppy;//ok

				}//x
				//if (xini==144 && yini=73) System.exit(0);
				//fill the rest of the line. Not needed 
				//for (int i=downsampled_yfin+1;i<yfin;i++)result_YUV[0][i*img.width+xfin]=0;	
			
			
			
		}
		//***********************************************
		public void watermarkFilter(ImgUtil copia)
		{
			//suponemos que en img hay una copia de down, que ademas inlcuye boundaries
			//copia 
			
		}
		//*******************************************************************
		public void interpolateGapHNeighbour( Block b_left)
		{
			//System.out.println("************HEHEHEHEHE***************************");
			//int margin=(int)(MAX_PPP/2f -0.5f);//+0.5f);
			int margin=(int)(MAX_PPP/2f +0.5f);// 0.5 per each side

			//esto no es generico, solo vale para luminancia
			margin=(int)(0.5f+Math.max(ppp[0][0]/2f,ppp[0][2]/2f));

			//-------------blocks located at left side of the image------------ 
			if (b_left==null)
			{


				int xend=margin;//Math.max(ppp[0][0]/2, ppp[0][2]/2)////(int)(ppp[0][0]+1);///xini is zero
				for (int y=yini;y<=yfin;y++)
					for (int x=xend;x>=0;x--)
					{
						//System.out.println("yini"+yini+" yend"+yend+" y:"+y+" x:"+x);
						if (img.interpolated_YUV[0][y*img.width+x]==0)
						{
							img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][(y)*img.width+x+1];

						}

					}
				return;
			}


			//-------------Horizontal interpolation--------------------------------

			//if (xini==0) return;//sobra, debido al return de arriba
			margin=(int)(0.5f+Math.max(b_left.ppp[0][1]/2f, Math.max(margin,b_left.ppp[0][3]/2f)));

			//int ystart=Math.max(yini+(int)(+0.5f+ppp[1][0]/2f-1f), b_left.yini+(int)(+0.5f+b_left.ppp[1][1]/2f-1f));

			//int ystart=Math.max(yini+(int)(-0.5f+ppp[1][0]/2f-1f), b_left.yini+(int)(-0.5f +b_left.ppp[1][1]/2f-1f));
			int ystart=Math.max(yini+(int)(ppp[1][0]/2f), b_left.yini+(int)(b_left.ppp[1][1]/2f));

			//ystart=yini+(int)(ppp[1][0]/2f-1-0.5f);
			if (yini==0) ystart=0;
			//int yend=Math.min(yfin-(int)(ppp[1][2]/2f+0.5f),
			//	b_left.yfin-(int)(b_left.ppp[1][3]/2f+0.5f));
			int yend=0;
			if (yfin==img.height-1) yend=yfin;
			//else yend=Math.min(yfin-(int)(ppp[1][2]/2f-0.5f),b_left.yfin-(int)(b_left.ppp[1][3]/2f-0.5f));
			//else yend=Math.min(yfin-(int)(ppp[1][2]/2f+0.5f),b_left.yfin-(int)(b_left.ppp[1][3]/2f+0.5f));
			else yend=(int)Math.min(yfin-(ppp[1][2]/2f)+0.5f+1,b_left.yfin-(b_left.ppp[1][3]/2f)+0.5f+1);

			//	yend=yfin;
			//System.out.println("b_left.xfin:"+b_left.xfin+ "  b.xini:"+b.xini+" b.xfin:"+b.xfin+" b.lx"+b.lx);
			for (int y=ystart;y<=yend;y++)
			{
				int xstart=b_left.xfin;
				int xend=xini;
				for (xstart=b_left.xfin;xstart>=b_left.xfin-margin; xstart--)
				{

					if (xstart==0) break;
					if (img.interpolated_YUV[0][y*img.width+xstart]!=0) break ;
				}

				for (xend=xini;xend<=xini+margin; xend++)
				{
					if (xend==img.width-1) break;
					if (img.interpolated_YUV[0][y*img.width+xend]!=0) break;
				}
				//img.interpolated_YUV[0][y*img.width+b.xini]=255;
				if ((img.interpolated_YUV[0][(y)*img.width+xend]==0) ||
						(img.interpolated_YUV[0][y*img.width+xstart]==0)) 
				{
					//System.out.println("       bad");
					/*for (int x=xstart+1;x<xend;x++)
								{
									if (x>=xini)img.interpolated_YUV[0][y*img.width+x]=255;
									   else img.interpolated_YUV[0][y*img.width+x]=128;	
								}*/
					//this scanline must not be interpolated horizontally
					continue;
				}


				//both pixels have value
				//float betax=(float)(img.interpolated_YUV[0][(y)*img.width+xend]-img.interpolated_YUV[0][y*img.width+xstart])/(float)(xend-xstart);
				float betax=(float)(img.interpolated_YUV[0][(y)*img.width+xend]-img.interpolated_YUV[0][y*img.width+xstart])/(float)(xend-xstart);
				//betax=0;
				
				//if(1>2)
				/*for (int x=xend-1;x>xstart;x--)
				{
					if (img.interpolated_YUV[0][y*img.width+x]!=0) System.out.println(" WARNING");	
					//img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][y*img.width+x+1];
				}
				*/
				//if (1>2)
				int colorini=img.interpolated_YUV[0][y*img.width+xstart];
				int colorfin=img.interpolated_YUV[0][y*img.width+xend];

				for (int x=xstart+1;x<xini;x++)
				{
					img.interpolated_YUV[0][y*img.width+x]=colorini;	
				}
				for (int x=xend-1;x>=xini;x--)
				{
					img.interpolated_YUV[0][y*img.width+x]=colorfin;
				}
			
				// interpolate blocks located at the right side of the image
				if (xfin==img.width-1)
				{
					// margin must change
					margin=(int)(MAX_PPP/2f +0.5f);

					for (int x=xfin-margin;x<=xfin;x++)
					{
						if (img.interpolated_YUV[0][y*img.width+x]!=0) continue;
						img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][y*img.width+x-1];
					}

				}
			}//y
		}
		//****************************************************************************************
		public void interpolateGapVNeighbour( Block b_up)
		{

			//this margin is greater than needed, but simple and fast to set :-)
			int margin=(int)(MAX_PPP/2f+0.5f); 


			//int margin=(int)ly/2;


			//-------------blocks located at top side of the image--------------------------
			if (b_up==null)
			{
				//	if (preferred_dimension=='V') return;//already filled

				int yend=(int)(ppp[1][0]+1);///yini is zero

				yend=margin;// because we must interpolate areas inter blocks ( ppp?) 
				for (int x=xini;x<=xfin;x++)
					for (int y=yend;y>=0;y--)
					{
						//System.out.println("yini"+yini+" yend"+yend+" y:"+y+" x:"+x);
						if (img.interpolated_YUV[0][y*img.width+x]==0)
						{
							img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][(y+1)*img.width+x];

						}

					}
				return;
			}
			//-------------vertical interpolation--------------------------------

			//System.out.println("xfin"+b.xfin+"  yfin:"+b.yfin+"  bupyfin:"+b_up.yfin);

			//	bucle_x:
			for (int x=xini;x<=xfin;x++)
			{

				//int last_ystart=b_up.yfin-margin;
				//int last_yend=b_up.yfin-margin;

				int ystart=b_up.yfin-margin-1;;
				int yend=0;

				while (true)
				{
					
					//System.out.println("img.interpolated_YUV[0][(ystart)*img.width+x] = "+img.interpolated_YUV[0][(ystart)*img.width+x]);
					while (img.interpolated_YUV[0][(ystart)*img.width+x]!=0)
					{ystart++;
					if (ystart>=yini+margin) break;//continue bucle_x;
					}
					if (ystart>=yini+margin) break;//

					yend=ystart+1;
					while (img.interpolated_YUV[0][(yend)*img.width+x]==0)
					{yend++; 
					if (yend==img.height)
					{
						System.out.println("x:"+x+"  ystart:"+ystart+"  xini:"+xini+"   xfin:"+xfin+"   yini:"+yini+"   yfin"+yfin);
						System.exit(0);	
					}
					}

					int color_ini=img.interpolated_YUV[0][(ystart-1)*img.width+x];
					int color_fin=img.interpolated_YUV[0][(yend)*img.width+x];
					if (color_ini==0 || color_fin==0) 
					{System.out.println("interpolate gapV: warning dos negros ");

					System.out.println("x:"+x+"  ystart:"+ystart+"  xini:"+xini+"   xfin:"+xfin+"   yini:"+yini+"   yfin"+yfin);
					System.exit(0);
					//break;
					}

					float betay=(float)(color_fin-color_ini)/(float)(yend-(ystart-1));//*
					
					//betay=(float)(color_fin-color_ini)/(float)(yend-ystart);
					//System.out.println(" ystart:"+ystart+" yend:"+yend);
					for (int y=ystart;y<yend;y++)//*
				//	for (int y=ystart+1;y<yend;y++)
					{
						// if (img.interpolated_YUV[0][y*img.width+x]==0)
						//if (x==xini || x==xini) 
						//img.interpolated_YUV[0][y*img.width+x]=255;//color_ini+(int)(0.5f+(float)((y-(ystart-1))*betay));
						//else 
						
						//img.interpolated_YUV[0][y*img.width+x]=color_ini+(int)(0.5f+(float)((y-(ystart-1))*betay));//*
						
						//PARECE QUE ESTO VA MEJOR
						//img.interpolated_YUV[0][y*img.width+x]=color_ini+(int)(0.5f+(float)(y-(ystart-1))*betay);
						
						
						// Neighbour style
						  
						 if (y<yini)
							img.interpolated_YUV[0][y*img.width+x]=color_ini;
							else img.interpolated_YUV[0][y*img.width+x]=color_fin;
						
						
						//img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][(y-1)*img.width+x];
						//if (y>=yini)img.interpolated_YUV[0][y*img.width+x]=255;
						//else img.interpolated_YUV[0][y*img.width+x]=128;
					}
					ystart=yend;//prepare ystart for next vertical sub-segment to interpolate
				}//true

				//System.out.println("ok");
				//ahora comprobamos si hay que rellenar lo de abajo
				if (yfin==img.height-1)
				{

					for (int y=yfin-margin;y<=yfin;y++)
					{
						if (img.interpolated_YUV[0][y*img.width+x]!=0) continue;
						img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][(y-1)*img.width+x];
					}


				}
			}//x

		}
		//*******************************************************************
		/**
		 * vertical bicubic interpolation
		 * 
		 * the interpolation process is: first vertical then horizontal
		 * @param src_YUV
		 * @param result_YUV
		 */
public void interpolateBicubicV( int[][] src_YUV, int[][] result_YUV)
		{
			
	
	//------------------------------- VERTICAL INTERPOLATION----------------
			float lenx=lx_sc;

			//gradient pppy of side c
			float grycy_sc=(ppp[1][1]-ppp[1][0])/(lenx-1);//lya_sc;
			//gradient PPPy side d
			float grydy_sc=(ppp[1][3]-ppp[1][2])/(lenx-1);

			//en la vertical no hace falta porque el bloque esta comprimido y no desplazado
			//vertical interpolation always is executed before H interpolation
			//therefore source is a block not shifted.  No need for extrapolation
			float ppp_yc=ppp[1][0];
			float ppp_yd=ppp[1][2];

			// pppx initialized to ppp_yc
			float pppy=ppp_yc;

			int y=0;

			//new
			float pppxc=ppp[0][0];
			float pppxd=ppp[0][2];
			float gradxc=(ppp[0][1]-ppp[0][0])/lx_sc-1;
			float gradxd=(ppp[0][3]-ppp[0][2])/lx_sc-1;
			float xc=pppxc/2;
			float xd=pppxd/2;
			
			
			//dont scan lx but lenx, which is lx_sc. 
			for (int x=xini;x<xini+lenx;x++)
			{

				float gry_sc=0;
				if (ppp_yc!=ppp_yd) 
					gry_sc=(2*ly-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);


				//float gry_sc=(ppp_yd-ppp_yc)/(ly_sc-1);

				//ppp_yc is updated at each iteration
				pppy=ppp_yc;//-grycy_sc;


				// yf is the end of the segment
				float yf=yini+pppy/2f;
				
				//NUEVO
				//float yf=yini+pppy/2f-0.5f;
				
				float y_antif=yf-pppy;

				// bucle for horizontal scanline 
				// scans the downsampled image, pixel by pixel
				int cfin_ant=0;
				//int coordy=y*img.width;
				
				int cini1=0;
				int cini2=0;
				int cfin1=0;
				int cfin2=0;
				
				for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
				{

					//coord "y" is the last pixel influenced by the segment y_sc-1 <----> y_sc
					y=(int) (yf); 

					
					int cini=0;
					int cfin=src_YUV[0][(y_sc)*img.width+x];
                    
					cfin1=cfin;
					cfin2=0;
					if (y_sc+1<yini+ly_sc) cfin2=src_YUV[0][(y_sc+1)*img.width+x];
					//else cfin2=cfin;
					
					if (y_sc==yini)//no se puede interpolar
					{
						//int cfin=src_YUV[0][(y_sc)*img.width+x];
						
						//IF yf< y+0.5 then this pixel is painted in the next step
						//LA BUENA
						//if (yf>=y+0.5f && yf<=y+0.75f) result_YUV[0][y*img.width+x]=cfin;
						if (yf>=y+0.5f && yf<=y+(1f-lateral) )result_YUV[0][y*img.width+x]=cfin;
						//if (yf>=y+0.5f ) 	result_YUV[0][y*img.width+x]=cfin;
						if (yini==0 )//|| pppy<1.2f)
						if (yf>=y+0.5f) result_YUV[0][y*img.width+x]=cfin;
						
						//UAAAACA
						
					}

					else if (y_sc!=yini  && cfin!=0)	
					{
						
						cini=cfin_ant;//src_YUV[0][(y_sc-1)*img.width+x];
						
						//if (cini==0) System.exit(0);
						
						//introduccion bicubic
						//if (cini2==0){cini2=cini;System.exit(0);}
						
						//if (cini1==0) cini1=cini2;//primer segmento interpolable. queda mejor si
						//if (cfin2==0 ) cfin2=cfin1;
						if (cini1==0 && yini>0) 
							{
							
							//boundaries y frontier en este caso es lo mismo
							//cini1=img.boundaries_YUV[0][(yini-1)*img.width+x];// OJO cambiado 20150410 por la sig linea
							cini1=img.frontierDownH_YUV[0][(yini-1)*img.width+x];
							//System.out.println("cini1:"+cini1);
							}
						if (cfin2==0 && y_sc==yini+lx_sc-1 && yfin!=img.height-1) {
							//cfin2=cfin1+(cini2-cini1);//con esto queda peor. es mejor aplicar bilineal en este caso
							//if (cfin2<0)cfin2=0;
							//if (cfin2>255) cfin2=255;
							
							//esto tiene que estar mal aunque funcione bien, 
							//no lo entiendo
							//cfin2=img.boundaries_ini_interH_YUV[0][(yfin+1)*img.width+x];
							
							//esto es correcto pero no queda mejor, queda igual. aun asi es lo correcto
							//podria ahorrarme interpolar, es decir haber evitado el calculo de boundaries_ini
							//y consultar directamente la muestra. NO es cierto
							//los PPP no son iguales en un bloque y en otro y por lo tanto no pueden alinearse
							//no es un problema de longitudes simplemente
							
							//aqui xx esta mal calculado. supone que todos los ppp son iguales!!!
							//usar x tambien esta mal pues x es dominio down
							
							//int xx=xini+(int)((((float)(x-xini))/(lx_sc-1f))*(lx-1));
							
							//int xx=(int)(xd+0.5f);
							//xd+=gradxd;
							//OJO QUE ESTO NO VA BIEN
							int xx=x;
							
							//ESTO ES NUEVO
							
							//cfin2=img.boundaries_ini_interH_YUV[0][(yfin+1)*img.width+xx];
							cfin2=img.frontierDownH_YUV[0][(yfin+1)*img.width+xx];
							
							//cfin2=img.boundaries_inter_YUV[0][(yfin+1)*img.width+xx];
							//System.out.println("xx:"+xx+" lx:"+lx);
							
							//esto es lo mas simple y da lo mismo que el xx
							//cfin2=cfin1;
							//System.out.println("cfin2:"+cfin2);
						}
						
						//if (cfin2==0) cfin2=cfin1;
						/*
						if (cini1==0) {
							
							cini1=cini-(cfin-cini);
							if (cini1<0) cini1=0;
							else if (cini1>255) cini1=255;
						}
						if (cfin2==0) {
							cfin2=cfin+(cfin-cini);
							if (cfin2<0) cfin2=0;
							else if (cfin2>255) cfin2=255;
						}
						*/
						if (cini1!=0 && cini2!=0 && cfin1!=0 && cfin2!=0)
						{
							for (int i=(int)(y_antif+0.5f);i<=(int)(yf-0.5f);i++)
							{
								double p[]=new double[4];
								
								p[0]=cini1;
								p[1]=cini2;
								p[2]=cfin1;
								p[3]=cfin2;
								double coord=((double)i+0.5f-y_antif)/pppy;
								if (coord>1) coord=1;
								else if (coord<0) coord=0;
								//System.out.println(" "+cini1+"  "+cini2+"  "+cfin1+"  "+cfin2+"   corrd:"+coord);
								int color=(int)(0.5f+BicubicInterpolator.getValue(p,coord));
								if (color>255) color=255;
								else if (color<1) color=1;
								
								result_YUV[0][i*img.width+x]=(int)(color);
								//System.out.println(" BICUBIC");
							}
						}
						
						else
						if (cini!=0) //interpolation is possible
						{
							//int cfin=src_YUV[0][(y_sc)*img.width+x];;

							//interpolation gradient y
							float  igrady=(float)(cfin-cini)/pppy;
							// colour of each pixel y=i inside segment is the corresponding  y = i.5
							//=======================================================================

							//colour initial segment
							//float cis=cini+(int)(0.5f+((((int)(y_antif+0.5f)+0.5f)-y_antif)*igrady));
							float cis=cini+(((int)(y_antif+0.5f)+0.5f)-y_antif)*igrady;
							for (int i=(int)(y_antif+0.5f);i<=(int)(yf-0.5f);i++)
							{
								
								
								float cis2=cis+ajuste;
								if (cis2<1) cis2=1f;
								else if (cis2>255) cis2=255f;
								
								result_YUV[0][i*img.width+x]=(int)(cis2);//por coherencia con gap
								
								
								cis+=igrady;
							}
							//comprobamos si no se ha podido pintar el ultmo pix

							//si el ultimo pix no se puede pintar, es mejor pintarlo a pesar de ello?
							//if (y_sc==yini+ly_sc-1)
							if (y_sc==downsampled_yfin)

							{

						
								//LA BUENA
								//if (yf<(float)y+0.5f && yf>=y+0.25f ) result_YUV[0][y*img.width+x]=cfin;
								if (yf<(float)y+0.5f && yf>=y+lateral ) result_YUV[0][y*img.width+x]=cfin;
								//if (yf<=(float)y+0.5f ) result_YUV[0][y*img.width+x]=cfin;
								if (yfin==img.height )//|| pppy<1.2f)
								if (yf<(float)y+0.5f) result_YUV[0][y*img.width+x]=cfin;
								
								//else result_YUV[0][y*img.width+x]=0;//NUEVO
						
							}
						}
						
						//NUEVO!!
						//if (y_sc==yini+ly_sc-1)
						if (y_sc==downsampled_yfin)
  						  {
                        	if (yf<(float)y+0.5f && yf>=y+0.25f ) result_YUV[0][y*img.width+x]=cfin;
							//if (yf<(float)y+0.5f )result_YUV[0][y*img.width+x]=cfin;
                        	if (yfin==img.height )//|| pppy<1.2f)
							if (yf<(float)y+0.5f) result_YUV[0][y*img.width+x]=cfin;
					
						  }

					}

					cini2=cfin;
					cini1=cini;
					cfin_ant=cfin;

					//y_anti=y;
					y_antif=yf;
					//yf+=pppy/2f;
					pppy+=gry_sc;

					yf+=pppy;//ok


				}//y
				ppp_yc+=grycy_sc;
				ppp_yd+=grydy_sc;

			}//x
		}	

		//*******************************************************************
//*******************************************************************
	/**
	 * horizontal bilineal interpolation
	 * 
	 * the interpolation process is: first vertical then horizontal
	 * @param src_YUV
	 * @param result_YUV
	 */
	public void interpolateBicubicH( int[][] src_YUV, int[][] result_YUV)
	{

		float leny=ly;

		//gradient x scaled of side a
		float gryax_sc=0;
		//gradient x of side a
		if (ppp[0][2]!=ppp[0][0])	gryax_sc=(ppp[0][2]-ppp[0][0])/(leny-1);//lya_sc;

		float grybx_sc=0;
		if (ppp[0][3]!=ppp[0][1])  grybx_sc=(ppp[0][3]-ppp[0][1])/(leny-1);//b_sc;

		//initial ppp values for sides "a" and "b"
		float ppp_xa=ppp[0][0];
		float ppp_xb=ppp[0][1];

		//extrapolate ppp to the top,because the vertical interpolated block is shifted (in bilineal case)
		ppp_xa=ppp_xa-gryax_sc*ppp[1][0]/2; //extrapolation
		ppp_xb=ppp_xb-grybx_sc*ppp[1][1]/2;//extrapolation

		// pppx is initialized to ppp_xa
		float pppx=ppp_xa;


		for (int y=yini;y<yini+leny;y++)
		{
			//starts at side c ( which is y==yini)
			float grx_sc=0;
			if (ppp_xa!=ppp_xb) grx_sc=(2*lx +ppp_xa-ppp_xb-2*ppp_xa*lx_sc)/((lx_sc-1)*lx_sc);

			//grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1);
			pppx=ppp_xa;
			// xf is the x coord of end of the segment to interpolate
			// x_antif is the c coord of the begining of the segment to interpolate
			float xf=xini+pppx/2;
			float x_antif=xf-pppx;

			// bucle for horizontal scanline. It scans the downsampled image, pixel by pixel
			int cfin_ant=0;
			//int coordy=y*img.width;// to avoid multiplications
			
			int cini1=0;
			int cini2=0;
			int cfin1=0;
			int cfin2=0;
			
			
			for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
			{

				//coord "x" is the last pixel influenced by the segment x_sc-1 <----> x_sc
				int x=(int) (xf);//+0.5f); 

				int cini=0;//initial color
				//int cfin=src_YUV[0][y*img.width+x_sc];//final color
				int cfin=src_YUV[0][y*img.width+x_sc];//final color

				
				cfin1=cfin;
				cfin2=0;
				if (x_sc+1<xini+lx_sc) cfin2=src_YUV[0][y*img.width+x_sc+1];
				
				
				if (cfin<=0) 
					{//System.out.println("interpolateBilinealH: cfin es cero:"+cfin);
					//System.exit(0);
					
					}
				if (x_sc==xini) //begining of downsampled block. there is no segment
				{
					//downsampled pix only cover part of this pixel 
					//at least must cover a 30% of the pixel to be filled here. otherwise it will be
					//interpolated between blocks, but interpolation between blocks is roughly because
					//we will not consider this residual percentage.
					//IF yf< y+0.5 then this pixel is painted in the next step
					//
					//LA BUENA
					//if (xf>=x+0.5f && xf<=x+0.75f) result_YUV[0][y*img.width+x]=cfin;
					if (xf>=x+0.5f && xf<=x+(1f-lateral)) result_YUV[0][y*img.width+x]=cfin;
					if (xini==0)
					if (xf>=x+0.5f) result_YUV[0][y*img.width+x]=cfin;
					//if (xf>=0.5f) result_YUV[0][y*img.width+x]=cfin;
					//result_YUV[0][y*img.width+x]=cfin;
					
					//LA NUEVA
					//if (xf>=x+0.5f )	result_YUV[0][y*img.width+x]=cfin;
					
					//if (xf>=x+0.5f   && xf<=x+0.75f && x!=xini) result_YUV[0][y*img.width+x]=cfin;
					
					//if (xf<=x+0.75f) result_YUV[0][y*img.width+x]=cfin;
					// finally is better to fill here the pixel, even if the % <30%
					//if (xf>=x+0.5f ) result_YUV[0][y*img.width+x]=cfin;
				}
				else  if (x_sc!=xini && cfin!=0 )// There is segment
				{
					cini=cfin_ant;//src_YUV[0][y*img.width+x_sc-1];

					
					
					//introduccion bicubic
					//if (cini2==0)cini2=cini;
					//if (cini1==0) cini1=cini2;
					if (cini1==0 && xini>0) {
						//cini1=img.boundaries_inter_YUV[0][(y)*img.width+xini-1];//ESTO ESTA MAL
						cini1=img.boundaries_ini_interV_YUV[0][(y)*img.width+xini-1];//esto esta bien 20150410
						//System.out.println("cini1:"+cini1);
						//for (int z=0;z<512;z++)System.out.print(img.boundaries_inter_YUV[0][z]+",");
					}
					if (cfin2==0 && x_sc==xini+lx_sc-1 && xfin!=img.width-1) {
					//if (cfin2==0 && xfin!=img.width-1) {
						//cfin2=cfin1;//con esto queda peor. es mejor aplicar bilineal en este caso
						//System.out.println("cfin2:"+cfin2);
						//
						
						cfin2=img.boundaries_ini_interV_YUV[0][y*img.width+xfin+1];
						//System.out.println("cfin2:"+cfin2);
					}
					//
					
					//if (x_sc==xini+lx_sc-1 && cfin2==0 && cfin1>0) cfin2=cfin1;
					//if (cfin2==0 && xf<(float)x+0.5f && xf>=x+0.25f ) cfin2=cfin1;
					//if (cini1==0 && x_sc>0) cini1=img.boundaries_inter_YUV[0][(y)*img.width+x_sc-1];
					//if (cfin2==0) cfin2=cfin1;
					
					/*if (cini1==0) {
						
						cini1=cini-(cfin-cini);
					}
					if (cfin2==0) {
						cfin2=cfin+(cfin-cini);
					}
					*/
					if (cini1!=0 && cini2!=0 && cfin1!=0 && cfin2!=0)
					{
						for (int i=(int)(x_antif+0.5f);i<=(int)(xf-0.5f);i++)
						{
							double p[]=new double[4];
							
							p[0]=cini1;
							p[1]=cini2;
							p[2]=cfin1;
							p[3]=cfin2;
							double coord=((double)i+0.5f-x_antif)/pppx;
							if (coord>1) coord=1;
							else if (coord<0) coord=0;
							//System.out.println(" "+cini1+"  "+cini2+"  "+cfin1+"  "+cfin2+"   corrd:"+coord);
							int color=(int)(0.5f+BicubicInterpolator.getValue(p,coord));
							if (color>255) color=255;
							else if (color<1) color=1;
							
							result_YUV[0][y*img.width+i]=(int)(color);
							//System.out.println(" BICUBIC");
						}
					}
					
					else
					
					
					if (cini!=0) //interpolation is possible
					{

						//interpolation gradient x
						float  igradx=(float)(cfin-cini)/pppx;
						
						//float  igradx=(float)(cfin-cini)/(xf-x_antif);
						
						// colour of each pixel x=i inside segment is the corresponding  x = i.5
						//=======================================================================
						//colour initial segment
						//float cis=cini+(int)(0.5f+((((int)(x_antif+0.5f)+0.5f)-x_antif)*igradx));
						
						
						float cis=cini+(((int)(x_antif+0.5f)+0.5f)-x_antif)*igradx;
						
						//float cis=cini+(((int)(x_antif+0.5f)+0.5f)-(int)x_antif)*igradx;
						
						
						for (int i=(int)(x_antif+0.5f);i<=(int)(xf-0.5f);i++)
						{

							//	int color=cini+(int)(0.5f+((((float)i+0.5f)-x_antif)*igradx));
							//	result_YUV[0][y*img.width+i]=color;
							
							//result_YUV[0][y*img.width+i]=(int)(cis+0.5f);
							
							//modificacion para corregir que nunca pintemos cero
							//al ser float debo chequear <1 y no <=0
							float cis2=cis+ajuste;
							if (cis2<1) cis2=1f;
							else if (cis2>255) cis2=255f;
							
							result_YUV[0][y*img.width+i]=(int)(cis2);//por coherencia con gap, que fue mejor asi
							
							//20141118
							//if (result_YUV[0][y*img.width+i]<=0) result_YUV[0][y*img.width+i]=1;
							//else if (result_YUV[0][y*img.width+i]>255) result_YUV[0][y*img.width+i]=255;
							
							
							cis+=igradx;
						}

						
						if (x_sc==xini+lx_sc-1) 
						{
							//if xf>=x+0.5f then the pixel is already painted
							//if (xf>=x+0.5f && result_YUV[0][y*img.width+x]==0) System.out.println("WARNING");
							// a 30% must be covered to be filled here. otherwise it will be interpolated between blocks
							
							
							//ESTA ES LA BUENA
							//if (xf<(float)x+0.5f && xf>=x+0.25f ) result_YUV[0][y*img.width+x]=cfin;
							if (xf<(float)x+0.5f && xf>=x+lateral ) result_YUV[0][y*img.width+x]=cfin;
							//if (xf<=(float)x+0.5f  ) result_YUV[0][y*img.width+x]=cfin;
							if (xfin==img.width)//|| pppx<1.2f)
								if (xf<(float)x+0.5f) result_YUV[0][y*img.width+x]=cfin;
								
							
							
							//result_YUV[0][y*img.width+x]=cfin;
							//ESTA ES LA NUEVA
							//if (xf<(float)x+0.5f)result_YUV[0][y*img.width+x]=cfin;
							
							//me gusta mas asi porque no se forma efecto cuadricula por no haber interpolado.
							//el cambio es introducir xfin
							//IF xf>=x+0.5 then this pixel is already painted
							//if (xf<(float)x+0.5f && xf>=x+0.25f && x!=xfin) result_YUV[0][y*img.width+x]=cfin;
							
							// finally is better to fill here the pixel, even if the % <30%
							//if (xf<(float)x+0.5f) result_YUV[0][y*img.width+x]=cfin;
						}

					}
					
					//NUEVO!
					if (x_sc==xini+lx_sc-1) 
					{
						if (xf<(float)x+0.5f && xf>=x+0.25f ) result_YUV[0][y*img.width+x]=cfin;
						//if (xf<(float)x+0.5f  ) result_YUV[0][y*img.width+x]=cfin;
						if (xfin==img.width)//|| pppx<1.2f)
						if (xf<(float)x+0.5f) result_YUV[0][y*img.width+x]=cfin;
							
						
					}

				}//else not xini

				cini2=cfin;
				cini1=cini;
				
				
				cfin_ant=cfin;
				x_antif=xf;
				pppx+=grx_sc;
				xf+=pppx;
			}//x
			ppp_xa+=gryax_sc;
			ppp_xb+=grybx_sc;

		}//y
	}	
	//*******************************************************************
	//*******************************************************************
		public void interpolateGapHBicubic( Block b_left)
		{
			//System.out.println("************HEHEHEHEHE***************************");
			//int margin=(int)(MAX_PPP/2f -0.5f);//+0.5f);
			
			//int margin=(int)(MAX_PPP/2f +0.5f);// 0.5 per each side
			
			//margin=(int)(MAX_PPP/2f);// +1f);
			
			
			
			//margin=12;//(int)(MAX_PPP/2f +1f);
			//esto no es generico, solo vale para luminancia
			/*
			if (b_left==null)
			margin=(int)(0.5f+Math.max(ppp[0][0]/2f,ppp[0][2]/2f));
			else 
			margin=(int)(0.5f+Math.max(ppp[0][0]/2f,Math.max(ppp[0][2]/2f,Math.max(b_left.ppp[0][3]/2f,b_left.ppp[0][1]/2f))));
			*/
			
			//selection of maximum PPP to choose the minimum H margin value for interpolation
			//-------------------------------------------------------------------------------
			//Math.max is heavier than this strategy
			float Hmarginf=ppp[0][0];
			if (ppp[0][2]>Hmarginf) Hmarginf=ppp[0][2];
			if (b_left!=null)
			{
				if (b_left.ppp[0][3]>Hmarginf) Hmarginf=b_left.ppp[0][3];
				if (b_left.ppp[0][1]>Hmarginf) Hmarginf=b_left.ppp[0][1];
			}
			int margin=(int)(0.5f+Hmarginf/2f);
			//System.out.println(" interpolateGapH:  b_left:"+b_left);
			//-------------blocks located at left side of the image------------ 
			if (b_left==null)
			{

				int xend=margin;//Math.max(ppp[0][0]/2, ppp[0][2]/2)////(int)(ppp[0][0]+1);///xini is zero
				for (int y=yini;y<=yfin;y++)
				{
					for (int x=xend;x>=0;x--)
					{
						//System.out.println("yini"+yini+" yend"+yfin+" y:"+y+" x:"+x);
						if (img.interpolated_YUV[0][y*img.width+x]==0)
						{
							img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][(y)*img.width+x+1];

							//img.mask[y*img.width+x]=1;
						}

						
						
					}
				//ahora contemplo el caso de 1 solo bloque:
				 if (xfin==img.width-1)
				 {
					// margin must change
					margin=(int)(MAX_PPP/2f +0.5f);

					for (int x=xfin-margin;x<=xfin;x++)
					{
						if (img.interpolated_YUV[0][y*img.width+x]!=0) continue;
						img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][y*img.width+x-1];
						//img.mask[y*img.width+x]=1;
					}

				 }
				}
				//si hay un return aqui no podemos hacer que sea un solo bloque la imagen.
				//no debemos hacer return
				
				return;
			}

			//System.out.println("aqui estamos");
			//-------------Horizontal interpolation--------------------------------

			
			float Vmarginf=ppp[1][0];
			if (ppp[1][1]>Vmarginf) Vmarginf=ppp[1][1];
			int ystart=yini+ (int)(-0.5f+Vmarginf/2f);
			
			if (yini==0) ystart=0;
			int yend=0;
			if (yfin==img.height-1) yend=yfin;
			//else yend=Math.min(yfin-(int)(ppp[1][2]/2f-0.5f),b_left.yfin-(int)(b_left.ppp[1][3]/2f-0.5f));
			//else yend=Math.min(yfin-(int)(ppp[1][2]/2f+0.5f),b_left.yfin-(int)(b_left.ppp[1][3]/2f+0.5f));
			else 
				{
				
				//yend=(int)Math.min(yfin-(ppp[1][2]/2f)+0.5f+1,b_left.yfin-(b_left.ppp[1][3]/2f)+0.5f+1);
				Vmarginf=ppp[1][2];
				if (ppp[1][3]>Vmarginf) Vmarginf=ppp[1][3];
				yend=yfin-(int)(-0.5f+Vmarginf/2f);
				}
			
			for (int y=ystart;y<=yend;y++)
			{
				int xstart=b_left.xfin;
				int xend=xini;
				//this bucle identifies xstart
				for (xstart=b_left.xfin;xstart>=b_left.xfin-margin; xstart--)
				{

					if (xstart==0) break;
					if (img.interpolated_YUV[0][y*img.width+xstart]!=0) break ;
				}
				//this bucle identifies xend
				for (xend=xini;xend<=xini+margin; xend++)
				{
					if (xend==img.width-1) break;
					if (img.interpolated_YUV[0][y*img.width+xend]!=0) break;
				}
				
				//check good values
				//img.interpolated_YUV[0][y*img.width+b.xini]=255;
				if ((img.interpolated_YUV[0][(y)*img.width+xend]==0) ||
						(img.interpolated_YUV[0][y*img.width+xstart]==0)) 
				{
					//int colorini=img.interpolated_YUV[0][y*img.width+xstart];
					//int colorfin=img.interpolated_YUV[0][y*img.width+xend];
					//System.out.println("bad:    color_ini:"+colorini+" color_fin:"+colorfin);//+ "  betax:"+betax);
					//System.out.println("       bad" );
					/*for (int x=xstart+1;x<xend;x++)
								{
									if (x>=xini)img.interpolated_YUV[0][y*img.width+x]=255;
									   else img.interpolated_YUV[0][y*img.width+x]=128;	
								}*/
					//this scanline must not be interpolated horizontally
					continue;
				}

				//System.out.println("       ok");
				//both pixels have value
				//float betax=(float)(img.interpolated_YUV[0][(y)*img.width+xend]-img.interpolated_YUV[0][y*img.width+xstart])/(float)(xend-xstart);
				float betax=(float)(img.interpolated_YUV[0][(y)*img.width+xend]-img.interpolated_YUV[0][y*img.width+xstart])/(float)(xend-xstart);
				
				
				
				
				//float betax=(float)(img.interpolated_YUV[0][(y)*img.width+xend]-img.interpolated_YUV[0][y*img.width+xstart])/(float)(xend-xstart);
				/*
				float num=(float)(img.interpolated_YUV[0][(y)*img.width+xend]-img.interpolated_YUV[0][y*img.width+xstart]);
				float denum=(float)(xend-xstart);
				float betax= num/denum;
				*/
				//float betax=(float)(img.interpolated_YUV[0][(y)*img.width+xend]-img.interpolated_YUV[0][y*img.width+xstart])/(float)(xend-xstart);
				
				//betax=0;
				
				//if(1>2)
				/*for (int x=xend-1;x>xstart;x--)
				{
					if (img.interpolated_YUV[0][y*img.width+x]!=0) System.out.println(" WARNING");	
					//img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][y*img.width+x+1];
				}
				*/
				//if (1>2)
				int colorini=img.interpolated_YUV[0][y*img.width+xstart];
				int colorfin=img.interpolated_YUV[0][y*img.width+xend];
				
				
				//introduccion de bicubic
				//for (int i=(int)(x_antif+0.5f);i<=(int)(xf-0.5f);i++)
				
				//esto es una bicubica rara pues entre cini1 y cini2 hay distancia cero
				//y entre cini2 y cfin1 hay hasta 8 pixels. aun asi queda mejor que con bilineal
				//NO , ESTO ESTA CORREGIDO CON el ini_interV
				
				int cini1=img.interpolated_YUV[0][y*img.width+xstart-1];
				int cini2=img.interpolated_YUV[0][y*img.width+xstart];
				
				cini1=img.boundaries_ini_interV_YUV[0][y*img.width+b_left.downsampled_xfin-1];
				//System.out.println(cini1);
				int cfin1=img.interpolated_YUV[0][y*img.width+xend];
				int cfin2=img.interpolated_YUV[0][y*img.width+xend+1];
				
				cfin2=img.boundaries_ini_interV_YUV[0][y*img.width+xini+1];//mas adecuado que el pixel interpolado normal
				//System.out.println(cfin2);
				for (int x=xstart+1;x<xend;x++)
				{
					double p[]=new double[4];
					
					p[0]=cini1;
					p[1]=cini2;
					p[2]=cfin1;
					p[3]=cfin2;
					double coord=((double)x+0.5f-xstart)/(xend-xstart+0.5f);
					if (coord>1) coord=1;
					else if (coord<0) coord=0;
					//System.out.println(" "+cini1+"  "+cini2+"  "+cfin1+"  "+cfin2+"   corrd:"+coord);
					int color=(int)(0.5f+BicubicInterpolator.getValue(p,coord));
					if (color>255) color=255;
					else if (color<1) color=1;
					
					img.interpolated_YUV[0][y*img.width+x]=(int)(color);
					//System.out.println(" BICUBIC");
				}
				
				//comentamos bilineal
				/*
				
				//System.out.println("color_ini:"+colorini+" color_fin:"+colorfin+ "  betax:"+betax);
				for (int x=xstart+1;x<xend;x++)
				//for (int x=xstart;x<=xend;x++)
				{
					
					
					//CREO QUE ESTO SE PUEDE HACER MAS RAPIDO SUMANDO CADA VEZ betax en lugar de multiplicar todo el rato
					
					img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][y*img.width+xstart]+(int)(ajuste+(float)(x-xstart)*betax);
					
					img.mask[y*img.width+x]=1;
				}
				*/

				// interpolate blocks located at the right side of the image
				//System.out.println("xfin:"+xfin);
				
				if (xfin==img.width-1)
				{
					// margin must change
					margin=(int)(MAX_PPP/2f +0.5f);

					for (int x=xfin-margin;x<=xfin;x++)
					{
						if (img.interpolated_YUV[0][y*img.width+x]!=0) continue;
						img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][y*img.width+x-1];
						//img.mask[y*img.width+x]=1;
					}

				}
				

			}//y
		}
		//****************************************************************************************
		//****************************************************************************************
		public void interpolateGapVBicubic( Block b_up)
		{

			//this margin is greater than needed, but simple and fast to set :-)
			int margin=(int)(MAX_PPP/2f+0.5f); 
			
			//margin=(int)(MAX_PPP/2f+5); 
			
			//margin=(int)(MAX_PPP/2f+1f); 
			
			//margin=(int)(MAX_PPP/2f+2f); 
			//System.out.println("MAX:"+MAX_PPP);
			
			
			//int margin=(int)ly/2;
			//esto no es generico, solo vale para luminancia
			//margin=(int)(0.5f+Math.max(ppp[1][0]/2f,ppp[1][1]/2f));

			//-------------blocks located at top side of the image--------------------------
			if (b_up==null)
			{
				//	if (preferred_dimension=='V') return;//already filled

				int yend=(int)(ppp[1][0]+1);///yini is zero

				yend=margin;// because we must interpolate areas inter blocks ( ppp?) 
				for (int x=xini;x<=xfin;x++)
				{
					for (int y=yend;y>=0;y--)
					{
						//System.out.println("yini"+yini+" yend"+yend+" y:"+y+" x:"+x);
						if (img.interpolated_YUV[0][y*img.width+x]==0)
						{
							img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][(y+1)*img.width+x];
							//img.mask[y*img.width+x]=1;
						}

					}
					//caso de 1 solo bloque
					if (yfin==img.height-1)
					{

						for (int y=yfin-margin;y<=yfin;y++)
						{
							if (img.interpolated_YUV[0][y*img.width+x]!=0) continue;
							img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][(y-1)*img.width+x];
							//img.mask[y*img.width+x]=1;
						}


					}
				}
				return;
			}
			//-------------vertical interpolation--------------------------------

			//System.out.println("xfin"+b.xfin+"  yfin:"+b.yfin+"  bupyfin:"+b_up.yfin);

			
			//	bucle_x:
			for (int x=xini;x<=xfin;x++)
			{

				//int last_ystart=b_up.yfin-margin;
				//int last_yend=b_up.yfin-margin;

				int ystart=b_up.yfin-margin-1;;
				int yend=0;

				
				
				while (true)
				{
					
					//System.out.println("img.interpolated_YUV[0][(ystart)*img.width+x] = "+img.interpolated_YUV[0][(ystart)*img.width+x]);
					while (img.interpolated_YUV[0][(ystart)*img.width+x]!=0)
					{ystart++;
					if (ystart>=yini+margin) break;//continue bucle_x;
					}
					if (ystart>=yini+margin) break;//

					yend=ystart+1;
					while (img.interpolated_YUV[0][(yend)*img.width+x]==0)
					{yend++; 
					if (yend==img.height)
					{
						System.out.println("x:"+x+"  ystart:"+ystart+"  xini:"+xini+"   xfin:"+xfin+"   yini:"+yini+"   yfin"+yfin);
						System.exit(0);	
					}
					}

					int color_ini=img.interpolated_YUV[0][(ystart-1)*img.width+x];
					int color_fin=img.interpolated_YUV[0][(yend)*img.width+x];
					if (color_ini==0 || color_fin==0) 
					{System.out.println("interpolateGapV: warning dos negros    colorini:"+color_ini+" cfin:"+color_fin);
					img.interpolated_YUV[0][(ystart-1)*img.width+x]=255;
					img.YUVtoBMP("./output_debug/gaph.bmp",img.interpolated_YUV[0]);
					System.out.println("x:"+x+"  ystart:"+ystart+"    yend:"+yend+"  xini:"+xini+"   xfin:"+xfin+"   yini:"+yini+"   yfin"+yfin);
					System.exit(0);
					//break;
					}

					float betay=(float)(color_fin-color_ini)/(float)(yend-(ystart-1));//*
					
					//betay=(float)(color_fin-color_ini)/(float)(yend-ystart);
					//System.out.println(" ystart:"+ystart+" yend:"+yend);
					
					
					int cini1=img.interpolated_YUV[0][(ystart-2)*img.width+x];
					int cini2=img.interpolated_YUV[0][(ystart-1)*img.width+x];
					int cfin1=img.interpolated_YUV[0][(yend)*img.width+x];
					int cfin2=img.interpolated_YUV[0][(yend+1)*img.width+x];
					
					
					//correccion bicubica
					if (yend>=yini && ystart<yini && b_up!=null)
					{
						//estamos entre dos bloques
						//cini1=img.boundaries_ini_interH_YUV[0][(b_up.downsampled_yfin-1)*img.width+x];
						
						cini1=img.frontierInterH_YUV[0][(b_up.downsampled_yfin-1)*img.width+x];
						
						
						//cfin2=img.boundaries_ini_interH_YUV[0][(yini+1)*img.width+x];
						cfin2=img.frontierInterH_YUV[0][(yini+1)*img.width+x];
					}
					
					
					//cfin2=img.boundaries_ini_interH_YUV[0][y*img.width+xini+1];//mas adecuado que el pixel interpolado normal
					
					for (int y=ystart;y<yend;y++)//*
					{
						double p[]=new double[4];
						
						p[0]=cini1;
						p[1]=cini2;
						p[2]=cfin1;
						p[3]=cfin2;
						double coord=((double)y+0.5f-ystart)/(yend-ystart+0.5f);
						if (coord>1) coord=1;
						else if (coord<0) coord=0;
						//System.out.println(" "+cini1+"  "+cini2+"  "+cfin1+"  "+cfin2+"   corrd:"+coord);
						int color=(int)(0.5f+BicubicInterpolator.getValue(p,coord));
						if (color>255) color=255;
						else if (color<1) color=1;
						
						img.interpolated_YUV[0][y*img.width+x]=(int)(color);
						//System.out.println(" BICUBICAAAA");
					}
					
					
					
					
					
					
					/*
					for (int y=ystart;y<yend;y++)
					{
					
						img.interpolated_YUV[0][y*img.width+x]=color_ini+(int)(ajuste+(float)(y-(ystart-1))*betay);
						
						img.mask[y*img.width+x]=1;
					}
					*/
					
					ystart=yend;//prepare ystart for next vertical sub-segment to interpolate
				}//true

				//System.out.println("ok");
				//ahora comprobamos si hay que rellenar lo de abajo
				if (yfin==img.height-1)
				{

					for (int y=yfin-margin;y<=yfin;y++)
					{
						if (img.interpolated_YUV[0][y*img.width+x]!=0) continue;
						img.interpolated_YUV[0][y*img.width+x]=img.interpolated_YUV[0][(y-1)*img.width+x];
						//img.mask[y*img.width+x]=1;
					}


				}
			}//x

		}
		//****************************************************************************************
		
		
		//****************************************************************************
		public void interpolateBoundariesIniVneighbour( int[][] src_YUV, int[][] result_YUV)
		{
			
			//if (xfin==img.width-1) return;
			//if (xini==0) return;//CAMBIO
			
			float lenx=lx_sc;

			//gradient PPPy side c
			float grycy_sc=(ppp[1][1]-ppp[1][0])/(lenx-1);//lya_sc;
			//gradient PPPy side d
			float grydy_sc=(ppp[1][3]-ppp[1][2])/(lenx-1);
			
			
			//en la vertical no hace falta porque el bloque esta comprimido y no desplazado
			//float ppp_yc=ppp[1][1];
			float ppp_yc=ppp[1][0];//CAMBIO
			//float ppp_yd=ppp[1][3];
			float ppp_yd=ppp[1][2];//CAMBIO

			//for (int x=xini;x<xini+2;x++)
			for (int x=xini;x<xini+lenx;x++)
			{
			//input image is downsampled_YUV. block width is lx_sc
			int y=0;

			//dont scan ly but ly_sc.
			   float gry_sc=0;
				//if (ppp_yc!=ppp_yd) gry_sc=(2*ly-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);
			   //gry_sc=(2*(ly-1)-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);
				gry_sc=(ppp_yd-ppp_yc)/(ly_sc-1);
				
				float pppy=ppp_yc;//-grycy_sc;

				float yf=yini+pppy-1;

				//previous interpolated y coordinate
				int y_anti=(int)(yf-pppy+0.5f);

				
				// scans the downsampled image, pixel by pixel
				for (int y_sc=yini;y_sc<=downsampled_yfin;y_sc++)
				{
					//end of block in nearest neighbour mode
					//if ( y_sc==downsampled_yfin) yf=yfin;
					

					//integer interpolated y coordinate 
					y=(int) (yf+0.5f);
					if (y>yfin) y=yfin;//esta protección no hace falta SI HACE FALTA!!!

					if (y_sc==yini) 
					{
						//nearest neighbour mode: copy sample into ppp/2 pixels
						for (int i=yini;i<=y;i++)
						{
							//result_YUV[0][i*img.width+xfin]=src_YUV[0][yini*img.width+downsampled_xfin];
							//System.out.println("x:"+x+"   width:"+img.width+" point:"+result_YUV[0]);
							result_YUV[0][i*img.width+x]=src_YUV[0][yini*img.width+x];//CAMBIO
						}	
					}
					// y_sc is not yini  
					else 
					{
						for (int i=y_anti+1;i<=y;i++)
						{
							//int a= src_YUV[0][y_sc*img.width+downsampled_xfin];
							//try{
							//result_YUV[0][i*img.width+xfin]=src_YUV[0][y_sc*img.width+downsampled_xfin];
							result_YUV[0][i*img.width+x]=src_YUV[0][y_sc*img.width+x];//CAMBIO
							
							
							//}catch (Exception e)
						//	{
							//	System.out.println("i:"+i+" xini:"+xini+"  yini:"+yini+"  xfin:"+xfin+"  yfin:"+yfin+" yf:"+yf);
								//System.exit(0);
							//}
							
						}
					}
					y_anti=y;
					pppy+=gry_sc;
					yf+=pppy;//ok
				}//y
				ppp_yc+=grycy_sc;
				ppp_yd+=grydy_sc;
			}//x
			
			//experimento
			//copio la linea para usar boundaries_ini y no boundaries en playFrame()
			for (int y=yini;y<=yfin;y++)
			{
				result_YUV[0][y*img.width+xfin]=result_YUV[0][y*img.width+downsampled_xfin];
			}
			
			
		}	
		//*******************************************************************
		//*******************************************************************
		
			public void interpolateBoundariesIniHneighbour( int[][] src_YUV, int[][] result_YUV)
			{
				//esta funcion interpola todo el bloque en horizontal por vecino
				//el objetivo es unicamente quedarse con  la frontera horizontal pero
				//como asi es mas facil pues lo interpolo todo, me da igual.
				//el resultado son tantas scanlines como altura tenga el bloque downsampled
				//porque no me quedo solo con lo que necesito sino que interpolo todo
				
				
				//if (yfin==img.height-1) return;//
				//if (yini==0) return;//CAMBIO
				
				//vamos a partir del bloque sin interpolar. esto es distinto a la interpol de bloque por vecino
				//en interpol bloque vecino, el bloque ya esta interpolado en V y leny es ly. aqui no
				float leny=ly_sc;
				float gryax_sc=0;
				if (ppp[0][2]!=ppp[0][0])
					gryax_sc=(ppp[0][2]-ppp[0][0])/(leny-1);//lya_sc;
				float grybx_sc=0;
				if (ppp[0][3]!=ppp[0][1])
					grybx_sc=(ppp[0][3]-ppp[0][1])/(leny-1);//b_sc;
				
				//en la vertical no hace falta porque el bloque esta comprimido y no desplazado
				//float ppp_xa=ppp[0][2];//-grycy_sc*(ppp[0][0]/2)*((lx_sc)/(lx));
				float ppp_xa=ppp[0][0];//CAMBIO
				//float ppp_xb=ppp[0][3];//-grydy_sc*(ppp[0][2]/2)*((lx_sc)/(lx));
				float ppp_xb=ppp[0][1];//CAMBIO

			
				//in this case y and y_sc is the same
				//for (int y=yini;y<yini+2;y++)
				for (int y=yini;y<yini+leny;y++)
				{
				//input image is downsampled_YUV. block width is lx_sc
				int x=0;

				//dont scan ly but ly_sc.
				   //float grx_sc=0;
				//	if (ppp_xa!=ppp_xb) grx_sc=(2*lx-2*ppp_xa*(lx_sc)+ppp_xa-ppp_xb)/((lx_sc-1)*lx_sc);
				//float grx_sc=(2*(lx-1)-2*ppp_xa*(lx_sc)+ppp_xa-ppp_xb)/((lx_sc-1)*lx_sc);
					float grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1);
					//hola
					float pppx=ppp_xa;//-grycy_sc;

					float xf=xini+pppx-1;

					//previous interpolated y coordinate
					int x_anti=(int)(xf-pppx+0.5f);

					
					// scans the downsampled image, pixel by pixel
					for (int x_sc=xini;x_sc<=downsampled_xfin;x_sc++)
					{
						//end of block in nearest neighbour mode
						//if ( x_sc==downsampled_xfin) xf=xfin;

						//integer interpolated y coordinate 
						x=(int) (xf+0.5f);
						if (x>xfin) x=xfin;//esta protección no hace falta

						if (x_sc==xini) 
						{
							//nearest neighbour mode: copy sample into ppp/2 pixels
							for (int i=xini;i<=x;i++)
							{
								//result_YUV[0][yfin*img.width+i]=src_YUV[0][downsampled_yfin*img.width+x_sc];
								//result_YUV[0][yini*img.width+i]=src_YUV[0][yini*img.width+x_sc];//CAMBIO
								result_YUV[0][y*img.width+i]=src_YUV[0][y*img.width+x_sc];//CAMBIO
							}	
						}
						// y_sc is not yini  
						else 
						{
							for (int i=x_anti+1;i<=x;i++)
							{
								//result_YUV[0][yfin*img.width+i]=src_YUV[0][downsampled_yfin*img.width+x_sc];
								result_YUV[0][y*img.width+i]=src_YUV[0][y*img.width+x_sc];//CAMBIO
							}
						}
						x_anti=x;
						pppx+=grx_sc;
						xf+=pppx;//ok
					}// for x
					ppp_xa+=gryax_sc;
					ppp_xb+=grybx_sc;
				}//y
			}	
			//*******************************************************************
			//*******************************************************************
			/**
			 * vertical bilineal interpolation
			 * 
			 * the interpolation process is: first vertical then horizontal
			 * @param src_YUV
			 * @param result_YUV
			 */
			public void interpolateBilinealVF( int[][] src_YUV, int[][] result_YUV)
			{
				//------------------------------- VERTICAL INTERPOLATION----------------
				float lenx=lx_sc;
		//lenx=lx;
				//gradient pppy of side c
				float grycy_sc=(ppp[1][1]-ppp[1][0])/(lenx-1);//lya_sc;
				//gradient PPPy side d
				float grydy_sc=(ppp[1][3]-ppp[1][2])/(lenx-1);

				//en la vertical no hace falta porque el bloque esta comprimido y no desplazado
				//vertical interpolation always is executed before H interpolation
				//therefore source is a block not shifted.  No need for extrapolation
				float ppp_yc=ppp[1][0];
				float ppp_yd=ppp[1][2];

				// pppx initialized to ppp_yc
				float pppy=ppp_yc;

				int y=0;

				//dont scan lx but lenx, which is lx_sc. 
				for (int x=xini;x<xini+lenx;x++)
				{

					float gry_sc=0;
					if (ppp_yc!=ppp_yd) 
						gry_sc=(2*ly-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);


					//float gry_sc=(ppp_yd-ppp_yc)/(ly_sc-1);

					//ppp_yc is updated at each iteration
					pppy=ppp_yc;//-grycy_sc;


					// yf is the end of the segment
					float yf=yini+pppy/2f;
					
					//NUEVO
					//float yf=yini+pppy/2f-0.5f;
					
					float y_antif=yf-pppy;

					// bucle for horizontal scanline 
					// scans the downsampled image, pixel by pixel
					int cfin_ant=0;
					//int coordy=y*img.width;
					
					for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
					{
						
						//coord "y" is the last pixel influenced by the segment y_sc-1 <----> y_sc
						y=(int) (yf); 

						int cini=0;
						int cfin=src_YUV[0][(y_sc)*img.width+x];
						
						int color=cfin;
						
						if (y_sc==yini)//no se puede interpolar
						{
							
							//LA BUENA
							//if (yf>=y+0.5f && yf<=y+0.75f) result_YUV[0][y*img.width+x]=cfin;
							
							
							if (yini==0)//simplemente replicamos
							{
								for (int i=0;i<y;i++) result_YUV[0][i*img.width+x]=cfin;
								if (yf>=y+0.5f ) 
									result_YUV[0][y*img.width+x]=cfin;
							}
							else
							{
							//debo interpolar con la fronteraH
							int xx=xini+(int)(0.5f+(((float)(x-xini))/(lx_sc-1))*(lx-1));
							cini=img.frontierH_YUV[0][(yini-1)*img.width+xx];
							cini=(cfin+cini)/2;
							//System.out.println("cis at xx:"+cini+"  at "+xx);
							float  igrady=(float)(cfin-cini)/(yf-yini-0.5f);
							float cis=cini+(int)(0.5f*igrady);
							
							//ahora pinto
							int yend=y-1;
							//if (yf>=y+0.5f) yend=y;
							//if (yend>yini)
							for (int i=yini;i<=yend;i++)
							  {
								float cis2=cis+ajuste;
								if (cis2>255) cis2=255;
								if (cis2<1) cis2=1;
								
								result_YUV[0][i*img.width+x]=(int)cis2;
								
								cis+=igrady;
							  }
							//result_YUV[0][yini*img.width+x]=255;
							if (yf>=y+0.5f) result_YUV[0][y*img.width+x]=cfin;
							}
						}

						else if (y_sc!=yini  )	
						{
							cini=cfin_ant;//src_YUV[0][(y_sc-1)*img.width+x];

							if (cini!=0) //interpolation is possible
							{
								//int cfin=src_YUV[0][(y_sc)*img.width+x];;

								//interpolation gradient y
								float  igrady=(float)(cfin-cini)/pppy;
								// colour of each pixel y=i inside segment is the corresponding  y = i.5
								//=======================================================================

								//colour initial segment
								//float cis=cini+(int)(0.5f+((((int)(y_antif+0.5f)+0.5f)-y_antif)*igrady));
								float cis=cini+(((int)(y_antif+0.5f)+0.5f)-y_antif)*igrady;
								for (int i=(int)(y_antif+0.5f);i<=(int)(yf-0.5f);i++)
								{
									//int color=cini+(int)(0.5f+((((float)i+0.5f)-y_antif)*igrady));
									//result_YUV[0][i*img.width+x]=color;
									
									//result_YUV[0][i*img.width+x]=(int)(cis+0.5f);
									
									//al ser float debo chequear <1 y no <=0
									//if (cis<1) cis=1f;
									//else if (cis>255) cis=255f;
									
									float cis2=cis+ajuste;
									if (cis2<1) cis2=1f;
									else if (cis2>255) cis2=255f;
									color=(int)cis2;
									result_YUV[0][i*img.width+x]=(int)(cis2);//por coherencia con gap
									
									
									//20141118
									//if (result_YUV[0][i*img.width+x]<=0)result_YUV[0][i*img.width+x]=1;
									//else if (result_YUV[0][i*img.width+x]>255) result_YUV[0][i*img.width+x]=255;
									
									
									cis+=igrady;
									
								}
								//comprobamos s no se ha podido pintar el ultmo pix
								
								
								
								//NUEVO------2015
								if (y_sc==downsampled_yfin )	
								{
								cini=color;	
								//lo nuevo
								//System.out.println ("hola");
								if (yfin==img.height-1)//simplemente replicamos
								{
									cfin=cini;//cini;	
									for (int i=y+1;i<=yfin;i++) result_YUV[0][i*img.width+x]=cfin;
									//if (yf>=y+0.5f ) 
										result_YUV[0][y*img.width+x]=cfin;
								}
								else
								{
								//debo interpolar con la fronteraH
								int xx=xini+(int)((((float)(x-xini))/(lx_sc-1))*(lx-1));
								//cini=cfin_ant;
								cfin=img.frontierH_YUV[0][(yfin+1)*img.width+xx];
								cfin=(cfin+cini)/2;
								//System.out.println("cis at xx:"+cini+"  at "+xx);
								//cini=result_YUV[0][(y-1)*img.width+x];
								 igrady=(float)(cfin-cini)/(yfin+0.5f-yf);
								 cis=cini;
								
								//ahora pinto
								 int ystart=y;
								 if (yf>=y+0.5) ystart=y+1;
								for (int i=ystart;i<=yfin;i++)
								  {
									
									float cis2=cis+ajuste;
									result_YUV[0][i*img.width+x]=(int)cis2;
									cis+=igrady;
								  }
								//result_YUV[0][yfin*img.width+x]=255;//cfin;
								 //if (yf>=y+0.5) ystart=y+1;
								}
								
								}//nueva else
								
							}


						}
						//no es else porque ya se metio. Esto machaca lo que hubiese mal pintado
						/*
						if (y_sc==downsampled_yfin )	
						{
						cini=color;	
						//lo nuevo
						//System.out.println ("hola");
						if (yfin==img.height-1)//simplemente replicamos
						{
							cfin=cini;//cini;	
							for (int i=y;i<=yfin;i++) result_YUV[0][i*img.width+x]=cfin;
							//if (yf>=y+0.5f ) 
								result_YUV[0][y*img.width+x]=cfin;
						}
						else
						{
						//debo interpolar con la fronteraH
						int xx=xini+(int)(0.5f+(((float)(x-xini))/(lx_sc-1f))*(lx-1));
						//cini=cfin_ant;
						cfin=img.frontierH_YUV[0][(yfin+1)*img.width+xx];
						//System.out.println("cis at xx:"+cini+"  at "+xx);
						cini=result_YUV[0][(y-1)*img.width+x];
						float  igrady=(float)(cfin-cini)/(pppy);
						float cis=cini;
						
						//ahora pinto
						for (int i=y;i<=yfin;i++)
						  {
							float cis2=cis+ajuste;
							result_YUV[0][i*img.width+x]=(int)cis2;
							cis+=igrady;
						  }
						}
						
						}//nueva else
						*/
						cfin_ant=cfin;

						//y_anti=y;
						y_antif=yf;
						//yf+=pppy/2f;
						pppy+=gry_sc;

						yf+=pppy;//ok


					}//y
					ppp_yc+=grycy_sc;
					ppp_yd+=grydy_sc;

				}//x
				
				
			}	

			//*******************************************************************
			public void fillFrontierH()
			{
				for (int xi=xini;xi<=xfin;xi++)
		           {
		        	img.frontierH_YUV[0][yfin*img.width+xi]=img.frontierH_YUV[0][downsampled_yfin*img.width+xi];
		        	img.frontierH_YUV[0][(yfin-1)*img.width+xi]=img.frontierH_YUV[0][(downsampled_yfin-1)*img.width+xi];
		           }
			}
			//*******************************************************************
public void fillFrontierV(int[][]src)
{
	for (int yi=yini;yi<=yfin;yi++)
    {
 	img.frontierInterV_YUV[0][yi*img.width+xfin]=src[0][yi*img.width+downsampled_xfin];
 	img.frontierInterV_YUV[0][yi*img.width+xfin-1]=src[0][yi*img.width+downsampled_xfin-1];
 	
    }
}
	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//*******************************************************************
	/**
	 * horizontal bilineal interpolation
	 * 
	 * the interpolation process is: first vertical then horizontal
	 * @param src_YUV
	 * @param result_YUV
	 */
	public void interpolateBilinealHF( int[][] src_YUV, int[][] result_YUV)
	{

		float leny=ly;
//leny=ly_sc;
		//gradient x scaled of side a
		float gryax_sc=0;
		//gradient x of side a
		if (ppp[0][2]!=ppp[0][0])	gryax_sc=(ppp[0][2]-ppp[0][0])/(leny-1);//lya_sc;

		float grybx_sc=0;
		if (ppp[0][3]!=ppp[0][1])  grybx_sc=(ppp[0][3]-ppp[0][1])/(leny-1);//b_sc;

		//initial ppp values for sides "a" and "b"
		float ppp_xa=ppp[0][0];
		float ppp_xb=ppp[0][1];

		//extrapolate ppp to the top,because the vertical interpolated block is shifted (in bilineal case)
		ppp_xa=ppp_xa-gryax_sc*ppp[1][0]/2; //extrapolation
		ppp_xb=ppp_xb-grybx_sc*ppp[1][1]/2;//extrapolation

		// pppx is initialized to ppp_xa
		float pppx=ppp_xa;


		for (int y=yini;y<yini+leny;y++)
		{
			//starts at side c ( which is y==yini)
			float grx_sc=0;
			if (ppp_xa!=ppp_xb) grx_sc=(2*lx +ppp_xa-ppp_xb-2*ppp_xa*lx_sc)/((lx_sc-1)*lx_sc);

			//grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1);
			pppx=ppp_xa;
			// xf is the x coord of end of the segment to interpolate
			// x_antif is the c coord of the begining of the segment to interpolate
			float xf=xini+pppx/2;
			float x_antif=xf-pppx;

			// bucle for horizontal scanline. It scans the downsampled image, pixel by pixel
			int cfin_ant=0;
			//int coordy=y*img.width;// to avoid multiplications
			for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
			{

				//coord "x" is the last pixel influenced by the segment x_sc-1 <----> x_sc
				int x=(int) (xf);//+0.5f); 

				int cini=0;//initial color
				//int cfin=src_YUV[0][y*img.width+x_sc];//final color
				int cfin=src_YUV[0][y*img.width+x_sc];//final color

				
				if (x_sc==xini) //begining of downsampled block. there is no segment
				{
					
					
					//LA NUEVA
					//if (xf>=x+0.5f   && xf<=x+0.75f && x!=xini) result_YUV[0][y*img.width+x]=cfin;
					
					if (xini==0)//simplemente replicamos
					{
						for (int i=0;i<x;i++) result_YUV[0][y*img.width+i]=cfin;
						if (xf>=x+0.5f ) 
							result_YUV[0][y*img.width+x]=cfin;
					}
					else
					{
						//debo interpolar con la fronteraV
						//int yy=y;//xini+(int)((((float)(x-xini))/(lx_sc-1f))*(lx-1));
						cini=img.frontierInterV_YUV[0][y*img.width+(xini-1)];
						cini=(cfin+cini)/2;
						System.out.println("cis at xx:"+cini+"  at "+x);
						//float  igradx=(float)(cfin-cini)/(pppx/2f);
						float  igradx=(float)(cfin-cini)/(xf-xini-0.5f);
						float cis=cini+(0.5f)*igradx;
						
						//ahora pinto
						int xend=x-1;
						
						for (int i=xini;i<=xend;i++)
						  {
							float cis2=cis+ajuste;
							result_YUV[0][y*img.width+i]=(int)cis2;
							cis+=igradx;
						  }
						if (xf>=x+0.5f) result_YUV[0][y*img.width+x]=cfin;
						
					}
					
				
				}
					
					
					
					
				
				else  if (x_sc!=xini && cfin!=0 )// There is segment
				{
					cini=cfin_ant;//src_YUV[0][y*img.width+x_sc-1];
                    int color=cini;
					if (cini!=0) //interpolation is possible
					{

						//interpolation gradient x
						float  igradx=(float)(cfin-cini)/pppx;
						
						//float  igradx=(float)(cfin-cini)/(xf-x_antif);
						
						// colour of each pixel x=i inside segment is the corresponding  x = i.5
						//=======================================================================
						//colour initial segment
						//float cis=cini+(int)(0.5f+((((int)(x_antif+0.5f)+0.5f)-x_antif)*igradx));
						
						
						float cis=cini+(((int)(x_antif+0.5f)+0.5f)-x_antif)*igradx;
						
						//float cis=cini+(((int)(x_antif+0.5f)+0.5f)-(int)x_antif)*igradx;
						
						
						for (int i=(int)(x_antif+0.5f);i<=(int)(xf-0.5f);i++)
						{

							//	int color=cini+(int)(0.5f+((((float)i+0.5f)-x_antif)*igradx));
							//	result_YUV[0][y*img.width+i]=color;
							
							//result_YUV[0][y*img.width+i]=(int)(cis+0.5f);
							
							//modificacion para corregir que nunca pintemos cero
							//al ser float debo chequear <1 y no <=0
							float cis2=cis+ajuste;
							if (cis2<1) cis2=1f;
							else if (cis2>255) cis2=255f;
							color=(int)cis;
							result_YUV[0][y*img.width+i]=(int)(cis2);//por coherencia con gap, que fue mejor asi
							
							//20141118
							//if (result_YUV[0][y*img.width+i]<=0) result_YUV[0][y*img.width+i]=1;
							//else if (result_YUV[0][y*img.width+i]>255) result_YUV[0][y*img.width+i]=255;
							
							
							cis+=igradx;
						}

						if (x_sc==downsampled_xfin) 
						{
							//if xf>=x+0.5f then the pixel is already painted
							//if (xf>=x+0.5f && result_YUV[0][y*img.width+x]==0) System.out.println("WARNING");
							// a 30% must be covered to be filled here. otherwise it will be interpolated between blocks
							
							
							//ESTA ES LA BUENA
							//if (xf<(float)x+0.5f && xf>=x+0.25f ) result_YUV[0][y*img.width+x]=cfin;
							
							//NUEVO------2015
							
							cini=cfin;	
							//lo nuevo
							//System.out.println ("hola");
							if (xfin==img.width-1)//simplemente replicamos
							{
								cfin=cini;//cini;	
								for (int i=x+1;i<=xfin;i++) result_YUV[0][y*img.width+i]=cfin;
								//if (yf>=y+0.5f ) 
									result_YUV[0][y*img.width+x]=cfin;
							}
							else
							{
							//debo interpolar con la fronterav
							//int xx=xini+(int)((((float)(x-xini))/(lx_sc-1))*(lx-1));
							//cini=cfin_ant;
							//cfin=img.frontierV_YUV[0][x*img.width+xfin+1];
								cfin=src_YUV[0][y*img.width+xfin+1];
							//System.out.println("cis at xx:"+cini+"  at "+xx);
							//cini=result_YUV[0][(y-1)*img.width+x];
							 
								
								cfin=(cfin+cini)/2;
								//igradx=(float)(cfin-cini)/(pppx);
								igradx=(float)(cfin-cini)/(pppx/2);
							 cis=cini;
							
							//ahora pinto
							 int xstart=x;
							 if (xf>=x+0.5) xstart=x+1;
							for (int i=xstart;i<=xfin;i++)
							  {
								float cis2=cis+ajuste;
								result_YUV[0][y*img.width+i]=(int)cis2;
								cis+=igradx;
							  }
							}
							
							
													}

					}

				}//else not xini

				cfin_ant=cfin;
				x_antif=xf;
				pppx+=grx_sc;
				xf+=pppx;
			}//x
			ppp_xa+=gryax_sc;
			ppp_xb+=grybx_sc;

		}//y
	}	
	//*******************************************************************
	//*******************************************************************
	/**
	 * vertical bilineal interpolation
	 * 
	 * the interpolation process is: first vertical then horizontal
	 * @param src_YUV
	 * @param result_YUV
	 */
	public void interpolateBilinealVF2( int[][] src_YUV, int[][] result_YUV, Block up, Block down)
	{
		//------------------------------- VERTICAL INTERPOLATION----------------
		float lenx=lx_sc;
//lenx=lx;
		//gradient pppy of side c
		float grycy_sc=(ppp[1][1]-ppp[1][0])/(lenx-1);//lya_sc;
		//gradient PPPy side d
		float grydy_sc=(ppp[1][3]-ppp[1][2])/(lenx-1);

		//en la vertical no hace falta porque el bloque esta comprimido y no desplazado
		//vertical interpolation always is executed before H interpolation
		//therefore source is a block not shifted.  No need for extrapolation
		float ppp_yc=ppp[1][0];
		float ppp_yd=ppp[1][2];

		// pppx initialized to ppp_yc
		float pppy=ppp_yc;

		int y=0;

		//dont scan lx but lenx, which is lx_sc. 
		for (int x=xini;x<xini+lenx;x++)
		{

			float gry_sc=0;
			if (ppp_yc!=ppp_yd) 
				gry_sc=(2*ly-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);


			//float gry_sc=(ppp_yd-ppp_yc)/(ly_sc-1);

			//ppp_yc is updated at each iteration
			pppy=ppp_yc;//-grycy_sc;


			// yf is the end of the segment
			float yf=yini+pppy/2f;
			
			//NUEVO
			//float yf=yini+pppy/2f-0.5f;
			
			float y_antif=yf-pppy;

			// bucle for horizontal scanline 
			// scans the downsampled image, pixel by pixel
			int cfin_ant=0;
			//int coordy=y*img.width;
			
			for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
			{
				
				//coord "y" is the last pixel influenced by the segment y_sc-1 <----> y_sc
				y=(int) (yf); 

				int cini=0;
				int cfin=src_YUV[0][(y_sc)*img.width+x];
				
				int color=cfin;
				
				if (y_sc==yini)//no se puede interpolar
				{
					
					//LA BUENA
					//if (yf>=y+0.5f && yf<=y+0.75f) result_YUV[0][y*img.width+x]=cfin;
					
					
					if (yini==0)//simplemente replicamos
					{
						for (int i=0;i<y;i++) result_YUV[0][i*img.width+x]=cfin;
						if (yf>=y+0.5f ) 
							result_YUV[0][y*img.width+x]=cfin;
					}
					else
					{
					//debo interpolar con la fronteraH
						
					int xx=xini+(int)(0.5f+(((float)(x-xini))/(lx_sc-1))*(lx-1));
					float gradpppup=(up.ppp[1][3]-up.ppp[1][2]);//normalizado a 1 en eje x
					float pppup=up.ppp[1][2]+gradpppup*((float)(x-xini))/(lx_sc-1);
					xx=x;
					//System.out.println("Hola");
					cini=img.frontierDownH_YUV[0][(yini-1)*img.width+xx];
					//cini=(cfin+cini)/2;
					//System.out.println("cis at xx:"+cini+"  at "+xx);
					float  igrady=(float)(cfin-cini)/(pppy/2+pppup/2);
					float cis=cini+(pppup/2)*igrady;
					
					//ahora pinto
					int yend=y-1;
					//if (yf>=y+0.5f) yend=y;
					//if (yend>yini)
					for (int i=yini;i<=yend;i++)
					  {
						float cis2=cis+ajuste;
						if (cis2>255) cis2=255;
						if (cis2<1) cis2=1;
						
						result_YUV[0][i*img.width+x]=(int)cis2;
						
						cis+=igrady;
					  }
					//result_YUV[0][yini*img.width+x]=255;
					if (yf>=y+0.5f) result_YUV[0][y*img.width+x]=cfin;
					}
				}

				else if (y_sc!=yini  )	
				{
					cini=cfin_ant;//src_YUV[0][(y_sc-1)*img.width+x];

					if (cini!=0) //interpolation is possible
					{
						//int cfin=src_YUV[0][(y_sc)*img.width+x];;

						//interpolation gradient y
						float  igrady=(float)(cfin-cini)/pppy;
						// colour of each pixel y=i inside segment is the corresponding  y = i.5
						//=======================================================================

						//colour initial segment
						//float cis=cini+(int)(0.5f+((((int)(y_antif+0.5f)+0.5f)-y_antif)*igrady));
						float cis=cini+(((int)(y_antif+0.5f)+0.5f)-y_antif)*igrady;
						for (int i=(int)(y_antif+0.5f);i<=(int)(yf-0.5f);i++)
						{
							//int color=cini+(int)(0.5f+((((float)i+0.5f)-y_antif)*igrady));
							//result_YUV[0][i*img.width+x]=color;
							
							//result_YUV[0][i*img.width+x]=(int)(cis+0.5f);
							
							//al ser float debo chequear <1 y no <=0
							//if (cis<1) cis=1f;
							//else if (cis>255) cis=255f;
							
							float cis2=cis+ajuste;
							if (cis2<1) cis2=1f;
							else if (cis2>255) cis2=255f;
							color=(int)cis2;
							result_YUV[0][i*img.width+x]=(int)(cis2);//por coherencia con gap
							
							
							//20141118
							//if (result_YUV[0][i*img.width+x]<=0)result_YUV[0][i*img.width+x]=1;
							//else if (result_YUV[0][i*img.width+x]>255) result_YUV[0][i*img.width+x]=255;
							
							
							cis+=igrady;
							
						}
						//comprobamos s no se ha podido pintar el ultmo pix
						
						
						
						//NUEVO------2015
						if (y_sc==downsampled_yfin )	
						{
						cini=color;	
						//lo nuevo
						//System.out.println ("hola");
						  if (yfin==img.height-1)//simplemente replicamos
						  {
							cfin=cini;//cini;	
							for (int i=y+1;i<=yfin;i++) result_YUV[0][i*img.width+x]=cfin;
							//if (yf>=y+0.5f ) 
								result_YUV[0][y*img.width+x]=cfin;
						  }
						  else
						  {
						//debo interpolar con la fronteraH
						//int xx=xini+(int)((((float)(x-xini))/(lx_sc-1))*(lx-1));
						//cini=cfin_ant;
						int xx=x;
						float gradpppdown=(down.ppp[1][1]-down.ppp[1][0]);//normalizado a 1 en eje x
						float pppdown=down.ppp[1][0]+gradpppdown*((float)(x-xini))/(lx_sc-1);
						cfin=img.frontierDownH_YUV[0][(yfin+1)*img.width+xx];
						//cfin=(cfin+cini)/2;
						//System.out.println("cis at xx:"+cini+"  at "+xx);
						//cini=result_YUV[0][(y-1)*img.width+x];
						 igrady=(float)(cfin-cini)/(pppy/2+pppdown/2);
						 cis=cini;
						
						//ahora pinto
						 int ystart=y;
						 if (yf>=y+0.5) ystart=y+1;
						for (int i=ystart;i<=yfin;i++)
						  {
							
							float cis2=cis+ajuste;
							result_YUV[0][i*img.width+x]=(int)cis2;
							cis+=igrady;
						  }
						//result_YUV[0][yfin*img.width+x]=255;//cfin;
						 //if (yf>=y+0.5) ystart=y+1;
						}
						
						}//nueva else
						
					}


				}
				//no es else porque ya se metio. Esto machaca lo que hubiese mal pintado
				/*
				if (y_sc==downsampled_yfin )	
				{
				cini=color;	
				//lo nuevo
				//System.out.println ("hola");
				if (yfin==img.height-1)//simplemente replicamos
				{
					cfin=cini;//cini;	
					for (int i=y;i<=yfin;i++) result_YUV[0][i*img.width+x]=cfin;
					//if (yf>=y+0.5f ) 
						result_YUV[0][y*img.width+x]=cfin;
				}
				else
				{
				//debo interpolar con la fronteraH
				int xx=xini+(int)(0.5f+(((float)(x-xini))/(lx_sc-1f))*(lx-1));
				//cini=cfin_ant;
				cfin=img.frontierH_YUV[0][(yfin+1)*img.width+xx];
				//System.out.println("cis at xx:"+cini+"  at "+xx);
				cini=result_YUV[0][(y-1)*img.width+x];
				float  igrady=(float)(cfin-cini)/(pppy);
				float cis=cini;
				
				//ahora pinto
				for (int i=y;i<=yfin;i++)
				  {
					float cis2=cis+ajuste;
					result_YUV[0][i*img.width+x]=(int)cis2;
					cis+=igrady;
				  }
				}
				
				}//nueva else
				*/
				cfin_ant=cfin;

				//y_anti=y;
				y_antif=yf;
				//yf+=pppy/2f;
				pppy+=gry_sc;

				yf+=pppy;//ok


			}//y
			ppp_yc+=grycy_sc;
			ppp_yd+=grydy_sc;

		}//x
		
		
	}	

	//*******************************************************************
	//*************************************************************************************
	/**
	 * esta funcion hace down de una row de un bloque YA INTERPOLADO en HORIZONTAL (NO EN VERTICAL)
	 * con los datos pppx que le digamos pa---pb y la longitud que digamos (lendown)
	 * solo hace down de la linea que digamos
	 * 
	 * @param result_YUV
	 * @param src_YUV
	 * @param b
	 * @param line
	 */
	
	public void downsampleRow(int[][] result_YUV, int [][] src_YUV, float pa, float pb, int line, float lendown)
			{
				
				//System.out.println("lendown:"+lendown);
				//line=yini+line;
				//initialization of ppp at side a and ppp at side b
				float ppp_xa=pa;//ppp[0][2];
				float ppp_xb=pb;//ppp[0][3];

				// initialization of pppx to ppp_xa
				float pppx=ppp_xa;

				

				//	float grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1f);
				float grx_sc=(ppp_xb-ppp_xa)/(lendown-1f);

					//first set of pixels to conver in one pix begins at xini and ends at xini+pppx
					//xf is the float x coord  
					//
					//    xfant-----------xf

					float xf=xini+pppx;// 
					float xfant=xini;//xf-pppx;

					int xant=xini;

					// bucle for horizontal scanline
					//for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
					//for (int x_sc=xini;x_sc<=downsampled_xfin;x_sc++)
					
					int downxfin=(int)lendown-xini-1;
					for (int x_sc=xini;x_sc<=xini+(int)lendown-1;x_sc++)
					{
					

						//if (x_sc==downsampled_xfin) {xf=xfin+1f;pppx=xf-xfant;}
						if (x_sc==downxfin) {xf=xfin+1f;pppx=xf-xfant;}
						int x=(int) (xf);
					
						float color=0;
						float porcent=(1-(xfant-xant));
						
						//if (porcent<0) porcent=0;
						
						//xf deberia cubrir todo el porcentaje siempre, ya que ppp minimo es 1
						//color+=porcent*src_YUV[0][yfin*img.width+xant];
						//if (x>xant)
						//System.out.println("Line:"+line);
						color+=porcent*src_YUV[0][(line)*img.width+xant];
						//System.out.println("colorini:"+color+" porcent:"+porcent+" xant:"+xant+"  xantf="+xfant+"   pppx:"+pppx+" x:"+x+"  xf:"+xf+" xfin:"+xfin);
						for (int i=xant+1;i<x ;i++)
						{
							//color+=src_YUV[0][yfin*img.width+i];
							color+=src_YUV[0][(line)*img.width+i];
						}
						//si estamos en xfin, xf deberia valer xfin
						//if (x_sc==xini+lx_sc-1) System.out.println("xf:"+xf+"   xfin:"+xfin +"   xf-xfin:"+(xf-xfin)+" pppxb:"+ppp_xb+"   pppx:"+pppx+" grx_sc:"+grx_sc);

						//con ppp=0.9999 puede ocurrir que xant y x sean lo mismo y por tanto ya ha sido sumado
						//if (xant<x)	color+=(xf-x)*src_YUV[0][yfin*img.width+x];
						
						//x es xant en el ultimo pix, ya que lo forzamos
						// en caso de ser iguales, ya hemos sumado en porcent?
						
						//if (xant<x)	color+=(xf-x)*src_YUV[0][(yini-1)*img.width+x];
						if (xf>x)	color+=(xf-x)*src_YUV[0][(line)*img.width+x];
						//else color+=src_YUV[0][(yini-1)*img.width+x];
						//if (x=xfin)	color+=(xf-x)*src_YUV[0][(yini-1)*img.width+x];
						//if (xant>=x) color=255*pppx;
						//else color=255*pppx;
						color=color/pppx;
						int brillo=(int)(color+0.5f);
						if (brillo==0) brillo=1;
						else if (brillo>255) brillo=255;
						//if (brillo<0) System.out.println(" fallo en down boundaries H. brillo <0");
						if (brillo<0) System.out.println(" fallo en down boundaries H. brillo <0     valor:"+ (xf-x)+"  ppx:"+pppx+ " color:"+color);
						
						//if (x_sc==xini+lx_sc-1) brillo=255;
						//result_YUV[0][yfin*img.width+x_sc]=brillo;
						
						result_YUV[0][(line)*img.width+x_sc]=brillo;


						pppx+=grx_sc;
						xant=x;
						xfant=xf;
						xf+=pppx;//ok

					}//x
					//fill the rest of the line. Not needed 
					
			}
//*********************************************************************************	
//
//  NEW SUPERRESOLUTION INTERPOLATION FUNCTIONS
//
//*********************************************************************************
	//*******************************************************************
		/**
		 * vertical interpolation in neighbour mode + lineal
		 *   NEAREST NEIGHBOUR LINEAR (NNL)
		 *  the interpolation process is: first vertical then horizontal
		 * @param src_YUV
		 * @param result_YUV
		 */
		public void interpolateNNLV( int[][] src_YUV, int[][] result_YUV)
		{
			float lenx=lx_sc;
	//lenx=lx;
			//gradient PPPy side c
			float grycy_sc=(ppp[1][1]-ppp[1][0])/(lenx-1);//lya_sc;
			//gradient PPPy side d
			float grydy_sc=(ppp[1][3]-ppp[1][2])/(lenx-1);

			//en la vertical no hace falta porque el bloque esta comprimido y no desplazado
			float ppp_yc=ppp[1][0];//-grycy_sc*(ppp[0][0]/2)*((lx_sc)/(lx));
			float ppp_yd=ppp[1][2];//-grydy_sc*(ppp[0][2]/2)*((lx_sc)/(lx));

			// pppy initialized to ppp_yc
			float pppy=ppp_yc; // only for gradient calculation

			//input image is downsampled_YUV. block width is lx_sc
			int y=0;

			//dont scan lx but lenx. ( lenx value is lx or lx_sc)
			for (int x=xini;x<xini+lenx;x++)
			{
				float gry_sc=0;
				//if (ppp_yc!=ppp_yd) gry_sc=(2*ly-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);

				gry_sc=(ppp_yd-ppp_yc)/(ly_sc-1);
				
				//ppp_yc is updated at each iteration
				pppy=ppp_yc;
				//pppy=ppp_yc/2f+0.5f;//first segment ends at the middle ***
				int c1=0; // previous color not exist
				int c2=0;
				
				//float tot_ppp=0f;
				
				//aqui pppy ya tiene el valor inicial
				//float yf=yini+pppy-1;//si pongo -1 entonces tendre que pintar de c
				//float yf=yini+pppy-1;//si pongo -1 entonces tendre que pintar de c
				
				//float yf=yini+pppy-1;//si pongo -1 entonces tendre que pintar de c
				float yf=yini+pppy/2-1;//si pongo -1 entonces tendre que pintar de c
				if (yf<yini) yf=yini;
				
				//previous interpolated y coordinate
				int y_anti=yini;//(int)(yf-pppy+0.5f);
				int y_anti2=0;
				
				// bucle for horizontal scanline 
				// scans the downsampled image, pixel by pixel
				for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
				{
					//end of block in nearest neighbour mode
					//if ( y_sc==yini+ly_sc-1) yf=yfin;

					//integer interpolated y coordinate 
					y=(int) (yf+0.5f);
					//y=(int) (yf+1f);//esto queda mejor asi. no se pq
					
					if (y>yfin) y=yfin;//esta protección no hace falta

					c2=src_YUV[0][y_sc*img.width+x];//inicialmente y_sc=yini
					
					if (y_sc==yini) 
					{
						c1=c2;
						//nearest neighbour mode: copy sample into ppp/2 pixels. this generates blocking effect
						
						//si comentamos esta parte y ponemos costuras quedara bien
						
						for (int i=yini;i<=y;i++)
						{
							result_YUV[0][i*img.width+x]=c1;//src_YUV[0][yini*img.width+x];
						}	
						
					}
					// y_sc is not yini  
					else //if (y_sc<=yini+ly_sc-1); //intermediate or final segment . copy ppp pixels
					{
						
						
						//float alfa=(float) (c2-c1)/tot_ppp;
						float alfa=(float) (c2-c1)/(y-y_anti);
						//float alfa=(float) (c2-c1)/(y_anti+1-y_anti2);
								
						float j=1f;//yanti fue pintado de c1, ahora toca pintar de otro color..no? 
						
						for (int i=y_anti+1;i<=y;i++)
						//for (int i=y_anti2;i<=y_anti+1;i++)
						{
						//	result_YUV[0][i*img.width+x]=src_YUV[0][y_sc*img.width+x];
							result_YUV[0][i*img.width+x]=c1+(int)(alfa*j);
							j++;
						}
						
						
					
					  if (y_sc==yini+ly_sc-1) //final segment. copy ppp/2 pixels. this generates blocking effect
					    {
						  for (int i=y+1;i<yini+ly;i++)
						  //for (int i=y_anti+1;i<yini+ly;i++)
					  	  {
							//result_YUV[0][i*img.width+x]=src_YUV[0][y_sc*img.width+x];
							  result_YUV[0][i*img.width+x]=c2;
						  }
					    }
					
				    }
					c1=c2;
					y_anti2=y_anti;
					y_anti=y;
					//tot_ppp=pppy/2f;
					//yf+=pppy/2f; ///from last ppp
					pppy+=gry_sc;
					//yf+=pppy/2;///2f;//from new ppp
					//tot_ppp+=pppy/2f;
					yf+=pppy;
				}//y
				ppp_yc+=grycy_sc;
				ppp_yd+=grydy_sc;
			}//x
		}	
		//*******************************************************************
		/**
		 * horizontal interpolation in neighbour+ LINEAR mode
		 * 
		 *  the interpolation process is: first vertical then horizontal
		 * @param src_YUV
		 * @param result_YUV
		 */
		public void interpolateNNLH( int[][] src_YUV, int[][] result_YUV)
		{

			float leny=ly;
	//leny=ly_sc;
			float gryax_sc=0;
			//gradient x of side a
			if (ppp[0][2]!=ppp[0][0])
				gryax_sc=(ppp[0][2]-ppp[0][0])/(leny-1);//lya_sc;
			float grybx_sc=0;
			if (ppp[0][3]!=ppp[0][1])
				grybx_sc=(ppp[0][3]-ppp[0][1])/(leny-1);//b_sc;

			//initial ppp values of sides "a" and "b"
			float ppp_xa=ppp[0][0];
			float ppp_xb=ppp[0][1];


			// pppx is initialized to ppp_xa
			float pppx=ppp_xa; // only for gradient calculation
			

			int x=0;
			//float grx_sc=(2*lx +ppp_xa-ppp_xb-2*ppp_xa*lx_sc)/((lx_sc-1)*lx_sc);
			int x_anti=0;


			for (int y=yini;y<yini+leny;y++)
			{
				//starts at side c ( which is y==yini)
				float grx_sc=0;

				//if (ppp_xa!=ppp_xb) grx_sc=(2*lx +ppp_xa-ppp_xb-2*ppp_xa*lx_sc)/((lx_sc-1)*lx_sc);				
				grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1);
				
				pppx=ppp_xa;
				//pppx=ppp_xa/2f;//first segment ends at the middle ***
				
				int c1=0; // previous color not exist. updated after each iteration
				int c2=0; //new color updated at each iteration

				
				// sample 

				//float xf=xini+pppx-1;// -1 because xini exists
				float xf=xini+pppx/2-1;// -1 because xini exists
				if (xf<xini)xf=xini;
				
				x_anti=xini;//(int)(xf-pppx+0.5f);

				// bucle for horizontal scanline 
				for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
				{
					//if ( x_sc==xini+lx_sc-1) xf=xfin;

					x=(int) (xf+0.5f);
					//x=(int) (xf+1f);//queda mejor
					if (x>xfin) x=xfin;

					c2=src_YUV[0][y*img.width+x_sc];
					
					if (x_sc==xini) 
					{
						c1=c2;
						//nearest neighbour:copy sample into ppp/2 pixels
						for (int i=xini;i<=x;i++)
						{
							//result_YUV[0][y*img.width+i]=src_YUV[0][y*img.width+x_sc];
							result_YUV[0][y*img.width+i]=c1;
						}
					}// if x_sc==xini

					else  // x_sc is not xini
					{
						
						float alfa=(float) (c2-c1)/(x-x_anti);
						float j=1f;
						
						for (int i=x_anti+1;i<=x;i++)
						{
							//result_YUV[0][y*img.width+i]=src_YUV[0][y*img.width+x_sc];
							result_YUV[0][y*img.width+i]=c1+(int)(alfa*j);
							j++;
						}
						 if (x_sc==xini+lx_sc-1) //final segment. copy ppp/2 pixels
						    {
							  for (int i=x+1;i<xini+lx;i++)
						  	  {
								//result_YUV[0][i*img.width+x]=src_YUV[0][y_sc*img.width+x];
								  result_YUV[0][y*img.width+i]=c2;
							  }
						    }
					}
					c1=c2;
					x_anti=x;
					xf+=pppx/2f;//from last ppp
					pppx+=grx_sc;
					//xf+=pppx;///2;//ok
					xf+=pppx/2f;//from new ppp
				}//x
				ppp_xa+=gryax_sc;
				ppp_xb+=grybx_sc;
			}//y
		}	
		//*******************************************************************
//*********************************************************************************	
//
//  NEW SUPERRESOLUTION INTERPOLATION FUNCTIONS NNSR: nearest reighbour super RES
//
//*********************************************************************************
			//*******************************************************************
				/**
				 * vertical interpolation in neighbour mode + lineal
				 *   NEAREST NEIGHBOUR LINEAR (NNL)
				 *  the interpolation process is: first vertical then horizontal
				 * @param src_YUV
				 * @param result_YUV
				 */
				public void interpolateNNSRV_001( int[][] src_YUV, int[][] result_YUV)
				{
					float lenx=lx_sc;
			//lenx=lx;
					//gradient PPPy side c
					float grycy_sc=(ppp[1][1]-ppp[1][0])/(lenx-1);//lya_sc;
					//gradient PPPy side d
					float grydy_sc=(ppp[1][3]-ppp[1][2])/(lenx-1);

					//en la vertical no hace falta porque el bloque esta comprimido y no desplazado
					float ppp_yc=ppp[1][0];//-grycy_sc*(ppp[0][0]/2)*((lx_sc)/(lx));
					float ppp_yd=ppp[1][2];//-grydy_sc*(ppp[0][2]/2)*((lx_sc)/(lx));

					// pppy initialized to ppp_yc
					float pppy=ppp_yc; // only for gradient calculation

					//input image is downsampled_YUV. block width is lx_sc
					int y=0;

					//dont scan lx but lenx. ( lenx value is lx or lx_sc)
					for (int x=xini;x<xini+lenx;x++)
					{
						float gry_sc=0;
						//if (ppp_yc!=ppp_yd) gry_sc=(2*ly-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);

						gry_sc=(ppp_yd-ppp_yc)/(ly_sc-1);
						
						//ppp_yc is updated at each iteration
						pppy=ppp_yc;
						//pppy=ppp_yc/2f+0.5f;//first segment ends at the middle ***
						int c0=0; // previous color. no needed 
						int c1=0; // pixel color
						int c2=0; //next pixel
						
						
						//float tot_ppp=0f;
						
						//aqui pppy ya tiene el valor inicial
						//float yf=yini+pppy-1;//si pongo -1 entonces tendre que pintar de c
						//float yf=yini+pppy-1;//si pongo -1 entonces tendre que pintar de c
						
						//float yf=yini+pppy-1;//si pongo -1 entonces tendre que pintar de c
						//float yf=yini+pppy/2-1;//si pongo -1 entonces tendre que pintar de c
						float yf=yini+pppy-1;//si pongo -1 entonces tendre que pintar de c
						if (yf<yini) yf=yini;
						
						//previous interpolated y coordinate
						int y_anti=yini-1;//(int)(yf-pppy+0.5f);
						
						
						// bucle for vertical scanline 
						// scans the downsampled image, pixel by pixel
						
						//coordenadas de los otros pixeles para superres
						//---------------------------------------------------
						int xprev=x-1;
						if (xprev<xini) xprev=xini;
						int xnext=x+1;
						if (xnext>=xini+lenx) xnext=(int)(xini+lenx-1);
						
						
						float ultimo=1f;//ultimo label
						for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
						{
							
							// aqui en cada iteracion debo identificar el tipo de microbloque en el que estoy
							//--------------------------------------------------------------------------------
							
							//coordenadas de cada uno de los 3 pixeles C0, C1, C2
							//----------------------------------------------------
							int y_c1=y_sc;
							
							int y_c0=y_c1-1;
							if (y_c0<yini) y_c0=(int)(yini);
							
							int y_c2=y_c1+1;
							if (y_c2>=yini+ly_sc) y_c2=(int)(yini+ly_sc-1);
							
							
							
							
							//colores
							//-------
							c0=src_YUV[0][y_c0*img.width+x];
							c1=src_YUV[0][y_c1*img.width+x];
							c2=src_YUV[0][y_c2*img.width+x];
							
							//hops [0..8], siendo el 4 el nulo
							//-----
							
							//hops que definen el microtipo (son 4)
							int h4=img.hops[0][y_c1*img.width+x];
							if (h4<4) h4=4+(4-h4);//lo paso a positivo
							int h5=img.hops[0][y_c1*img.width+xnext];
							if (h5<4) h5=4+(4-h5);//lo paso a positivo
							int h7=img.hops[0][y_c2*img.width+x];
							if (h7<4) h7=4+(4-h7);//lo paso a positivo
							int h8=img.hops[0][y_c2*img.width+xnext];
							if (h8<4) h8=4+(4-h8);//lo paso a positivo
							
							System.out.println ("h4:"+h4+"  h5:"+h5+"  h7:"+h7+"   h8:"+h8);
							
							//hops fronterizos (no se usan)
							int h0=img.hops[0][y_c0*img.width+xprev];
							int h1=img.hops[0][y_c0*img.width+x];
							int h2=img.hops[0][y_c0*img.width+xnext];
							int h3=img.hops[0][y_c1*img.width+xprev];
							int h6=img.hops[0][y_c2*img.width+xprev];
							
							//labels fronterizos
							float l0=img.label_YUV[0][y_c0*img.width+xprev];
							float l1=img.label_YUV[0][y_c0*img.width+x];
							
							if (y_c0==yini) l1=1f;
							
							float l2=img.label_YUV[0][y_c0*img.width+xnext];
							float l3=img.label_YUV[0][y_c1*img.width+xprev];
							float l6=img.label_YUV[0][y_c2*img.width+xprev];
							
									
							
							
							//tipo de microbloque (hay 4 tipos + 1)
							//------------------------------------------------
							//cada microtipo tiene una forma diferente de colorear en esta interpolacion V
							//y una forma distinta de etiquetar
							//los pixeles los he denotado asi:
							
							
							int microtype=0;// bloque nulo
							int umbral=6;
							
							//valores default para microtype nulo
							float labelini=ultimo;//l1;//labelini debe ser el label del pixel superior
							float labelfin=1f;
							microtype=0;
							
							//diagonales
							if (h4<umbral && h5>=umbral &&  h7>=umbral && h8<umbral)	{microtype=32; labelfin=0.0f;}
							else if (h4>=umbral  && h5<umbral && h7<umbral && h8>=umbral)	{microtype=33;labelfin=1f;}
							
							//microdiagonales
							else if (h4>=umbral  && h5<umbral && h7<umbral && h8<umbral)	{microtype=41;labelfin=0f;}
							else if (h4<umbral  && h5>=umbral && h7<umbral && h8<umbral)	{microtype=42;labelfin=1f;}
							else if (h4<umbral  && h5<umbral && h7>=umbral && h8<umbral)	{microtype=43;labelfin=1f;}
							
							//labelfin=1f;//0.5f;
							System.out.println ("micro:"+microtype);
							
							System.out.println ("itera:");
							
							//integer interpolated y coordinate 
							y=(int) (yf+0.5f);
							if (y>yfin) y=yfin;//esta protección no hace falta

							//pintado de color
							
							//nulo
							if (microtype==0) 
							{
							for (int i=y_anti+1;i<=y;i++)
								{
								
									result_YUV[0][i*img.width+x]=c1;//src_YUV[0][yini*img.width+x];
								
									
								}	
							}
							else if (microtype==33) 
							{
							for (int i=y_anti+1;i<=y;i++)
								{
								
									result_YUV[0][i*img.width+x]=c1;//src_YUV[0][yini*img.width+x];
								
									
								}	
							}
							//diagonal "/"
							else
							{
								for (int i=y_anti+1;i<=y;i++)
								{
								
									result_YUV[0][i*img.width+x]=c1;//src_YUV[0][yini*img.width+x];
								
									
								}	
							}
							
							result_YUV[0][(y_anti+1)*img.width+x]=255;
							for (int i=y_anti+1;i<=y;i++)
							{
							
								result_YUV[0][i*img.width+x]=c1;//src_YUV[0][yini*img.width+x];
							
								
							}	
							
							/*
							else if (microtype!=3) 
							{
							for (int i=y_anti+1;i<=y;i++)
								{
								
									result_YUV[0][i*img.width+x]=c1;//src_YUV[0][yini*img.width+x];
								
									
								}	
							}
							else //borde H
							{
								int y1=y_anti+(y-y_anti)/2;
								for (int i=y_anti+1;i<=y1;i++)
									{
									
										result_YUV[0][i*img.width+x]=c0;//src_YUV[0][yini*img.width+x];
									
										
									}	
								for (int i=y1+1;i<=y;i++)
								{
								
									result_YUV[0][i*img.width+x]=c1;//src_YUV[0][yini*img.width+x];
								
									
								}
							}
							*/
							//calculo de etiquetas
							//--------------------
							//alfa se calcula igual para todos los bloques
							float alfa =(labelfin-labelini)/pppy;//(y-y_anti+1);
							
							//ahora relleno la etiqueta
							int j=0;//al ser cero, el primer pixel queda con labelini, que es lo correcto para
							//garantizar la continuidad
							
							//System.out.println ("------labeling------");
							for (int i=y_anti+1;i<=y;i++)
							{
								
								
								img.label_YUV[0][i*img.width+x]=labelini+j*alfa;//src_YUV[0][yini*img.width+x];
								j++;
								//System.out.println("type:"+microtype+"  ,y="+i+", x="+x+" , label="+ img.label_YUV[0][i*img.width+x]);
								ultimo=img.label_YUV[0][i*img.width+x];
							}	
							
							
							
							
							c1=c2;
							y_anti=y;
							//tot_ppp=pppy/2f;
							//yf+=pppy/2f; ///from last ppp
							pppy+=gry_sc;
							//yf+=pppy/2;///2f;//from new ppp
							//tot_ppp+=pppy/2f;
							yf+=pppy;
						}//y
						ppp_yc+=grycy_sc;
						ppp_yd+=grydy_sc;
					}//x
				}	
				//*******************************************************************
				/**
				 * horizontal interpolation in neighbour+ LINEAR mode
				 * 
				 *  the interpolation process is: first vertical then horizontal
				 * @param src_YUV
				 * @param result_YUV
				 */
				public void interpolateNNSRH_001( int[][] src_YUV, int[][] result_YUV)
				{

					float leny=ly;
			//leny=ly_sc;
					float gryax_sc=0;
					//gradient x of side a
					if (ppp[0][2]!=ppp[0][0])
						gryax_sc=(ppp[0][2]-ppp[0][0])/(leny-1);//lya_sc;
					float grybx_sc=0;
					if (ppp[0][3]!=ppp[0][1])
						grybx_sc=(ppp[0][3]-ppp[0][1])/(leny-1);//b_sc;

					//initial ppp values of sides "a" and "b"
					float ppp_xa=ppp[0][0];
					float ppp_xb=ppp[0][1];


					// pppx is initialized to ppp_xa
					float pppx=ppp_xa; // only for gradient calculation
					

					int x=0;
					//float grx_sc=(2*lx +ppp_xa-ppp_xb-2*ppp_xa*lx_sc)/((lx_sc-1)*lx_sc);
					int x_anti=0;


					for (int y=yini;y<yini+leny;y++)
					{
						//starts at side c ( which is y==yini)
						float grx_sc=0;

						//if (ppp_xa!=ppp_xb) grx_sc=(2*lx +ppp_xa-ppp_xb-2*ppp_xa*lx_sc)/((lx_sc-1)*lx_sc);				
						grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1);
						
						pppx=ppp_xa;
						//pppx=ppp_xa/2f;//first segment ends at the middle ***
						
						int ca=0; // previous color not exist. updated after each iteration
						int cb=0; //new color updated at each iteration

						
						// sample 

						float xf=xini+pppx-1;// -1 because xini exists
						//float xf=xini+pppx/2-1;// -1 because xini exists
						if (xf<xini)xf=xini;
						
						x_anti=xini-1;//(int)(xf-pppx+0.5f);

						
						
						
						// bucle for horizontal scanline 
						for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
						{
							//if ( x_sc==xini+lx_sc-1) xf=xfin;

							x=(int) (xf+0.5f);
							//x=(int) (xf+1f);//queda mejor
							if (x>xfin) x=xfin;

							//int x0=x_sc-1;
							int xa=x_sc;
							int xb=x_sc+1;
							if (xb==xini+lx_sc) xb=(int)(xini+lx_sc-1);
							
							 ca=src_YUV[0][y*img.width+xa];//color de la muestra A
							
							 cb=src_YUV[0][y*img.width+xb];//color de la muestra B
							
							//ahora voy a coger el valor donde finaliza ca y comienza cb, que es la etiqueta						
							int num_pix=(int)((x_anti+1)+pppx*img.label_YUV[0][y*img.width+x_sc]+0.5f);
							//int num_pix=(int)((x_anti+1)+pppx*img.label_YUV[0][y*img.width+x_sc]);
							System.out.println ("num_pix:"+num_pix);
							//tramo de ca
							for (int i=x_anti+1;i<num_pix;i++)
							  {
								result_YUV[0][y*img.width+i]=ca;//0;
							 }
							//tramo de cb
							for (int i=num_pix;i<=x;i++)
							  {
									result_YUV[0][y*img.width+i]=cb;
							  }
								
							
							cb=ca;
							x_anti=x;
							//xf+=pppx/2f;//from last ppp
							pppx+=grx_sc;
							xf+=pppx;///2;//ok
							//xf+=pppx/2f;//from new ppp
						}//x
						ppp_xa+=gryax_sc;
						ppp_xb+=grybx_sc;
					}//y
				}	
				//*******************************************************************	
				
				
//0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
				//*******************************************************************
				/**
				 * vertical interpolation in neighbour mode + lineal
				 *   NEAREST NEIGHBOUR LINEAR (NNL)
				 *  the interpolation process is: first vertical then horizontal
				 * @param src_YUV
				 * @param result_YUV
				 */
				public void interpolateNNSRV( int[][] src_YUV, int[][] result_YUV)
				{
					float lenx=lx_sc;
			//lenx=lx;
					//gradient PPPy side c
					float grycy_sc=(ppp[1][1]-ppp[1][0])/(lenx-1);//lya_sc;
					//gradient PPPy side d
					float grydy_sc=(ppp[1][3]-ppp[1][2])/(lenx-1);

					//en la vertical no hace falta porque el bloque esta comprimido y no desplazado
					float ppp_yc=ppp[1][0];//-grycy_sc*(ppp[0][0]/2)*((lx_sc)/(lx));
					float ppp_yd=ppp[1][2];//-grydy_sc*(ppp[0][2]/2)*((lx_sc)/(lx));

					// pppy initialized to ppp_yc
					float pppy=ppp_yc; // only for gradient calculation

					//input image is downsampled_YUV. block width is lx_sc
					int y=0;

					//dont scan lx but lenx. ( lenx value is lx or lx_sc)
					for (int x=xini;x<xini+lenx;x++)
					{
						float gry_sc=0;
						//if (ppp_yc!=ppp_yd) gry_sc=(2*ly-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);

						gry_sc=(ppp_yd-ppp_yc)/(ly_sc-1);
						
						//ppp_yc is updated at each iteration
						pppy=ppp_yc;
						//pppy=ppp_yc/2f+0.5f;//first segment ends at the middle ***
						int c0=0; // previous color 
						int c1=0; // pixel color
						int c2=0; //next pixel
						
						
						//float tot_ppp=0f;
						
						//aqui pppy ya tiene el valor inicial
						//float yf=yini+pppy-1;//si pongo -1 entonces tendre que pintar de c
						//float yf=yini+pppy-1;//si pongo -1 entonces tendre que pintar de c
						
						//float yf=yini+pppy-1;//si pongo -1 entonces tendre que pintar de c
						//float yf=yini+pppy/2-1;//si pongo -1 entonces tendre que pintar de c
						float yf=yini+pppy-1;//si pongo -1 entonces tendre que pintar de c
						if (yf<yini) yf=yini;
						
						//previous interpolated y coordinate
						int y_anti=yini-1;//(int)(yf-pppy+0.5f);
						
						
						// bucle for vertical scanline 
						// scans the downsampled image, pixel by pixel
						
						//coordenadas de los otros pixeles para superres
						//---------------------------------------------------
						int xprev=x-1;
						if (xprev<xini) xprev=xini;
						int xnext=x+1;
						if (xnext>=xini+lenx) xnext=(int)(xini+lenx-1);
						
						for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
						{
							
							// aqui en cada iteracion debo identificar el tipo de microbloque en el que estoy
							//--------------------------------------------------------------------------------
							
							//coordenadas de cada uno de los 3 pixeles C0, C1, C2
							//----------------------------------------------------
							int y_c1=y_sc;
							
							int y_c0=y_c1-1;
							if (y_c0<yini) y_c0=(int)(yini);
							
							int y_c2=y_c1+1;
							if (y_c2>=yini+ly_sc) y_c2=(int)(yini+ly_sc-1);
							
							
							
							
							//colores
							//-------
							c0=src_YUV[0][y_c0*img.width+x];
							c1=src_YUV[0][y_c1*img.width+x];
							c2=src_YUV[0][y_c2*img.width+x];
							
							//hops [0..8], siendo el 4 el nulo
							//-----
							int h0=img.hops[0][y_c0*img.width+x];
							int h1=img.hops[0][y_c1*img.width+x];
							int h2=img.hops[0][y_c2*img.width+x];
							
							//tipo de microbloque (hay 4 tipos + 1)
							//------------------------------------------------
							//cada microtipo tiene una forma diferente de colorear en esta interpolacion V
							//y una forma distinta de etiquetar
							//los pixeles los he denotado asi:
							//306
							//718
							//524
							
							int microtype=0;// hop nulo o ausencia de borde
							int umbral=0;
							if (h1!=4) microtype=-1;
							System.out.println("h1="+h1);
							//if (h2>4+umbral || h2<4-umbral) microtype=-1;
							
							if (microtype==-1)
							{
								//estamos aqui porque hay un hop en el pixel que vamos a interpolar
								//tipo 1
								//  * 
								// *
								int h5=img.hops[0][y_c2*img.width+xprev];
								int h6=img.hops[0][y_c0*img.width+xnext];
								
								int h3=img.hops[0][y_c0*img.width+xprev];
								int h4=img.hops[0][y_c2*img.width+xnext];
								
								int h7=img.hops[0][y_c1*img.width+xprev];
								int h8=img.hops[0][y_c1*img.width+xnext];
								
								System.out.println(""+h3+";"+h0+";"+h6);
								System.out.println(""+h7+";"+h1+";"+h8);
								System.out.println(""+h5+";"+h2+";"+h4);
								
								//h5=4;h6=4;h3=4;h4=4;
								
									if ((h0>4+umbral || h0<4-umbral) && 
										(h2>4+umbral || h2<4-umbral) )
										{microtype=4;}
									
										
								 	
								 	 if ((h5>4+umbral || h5<4-umbral) && 
											(h6>4+umbral || h6<4-umbral) )
											{microtype=1; }
								 	else if ((h7>4+umbral || h7<4-umbral) && 
											(h8>4+umbral || h8<4-umbral) )
											{microtype=3;}
								 	else if  ((h3>4+umbral || h3<4-umbral) && 
											(h4>4+umbral || h4<4-umbral) )
											{microtype=2;}
								 	else if ((h0>4+umbral || h0<4-umbral) && 
											(h2>4+umbral || h2<4-umbral) )
											{microtype=4;}
										
									
								//si no se ha detectado ningun tipo, se considera el tipo cero
								else
									microtype=0;	
								
								
								
							}
							
							System.out.println ("itera:");
							
							//integer interpolated y coordinate 
							y=(int) (yf+0.5f);
							if (y>yfin) y=yfin;//esta protección no hace falta

							//pintado de color
							if (microtype!=3) 
							{
							for (int i=y_anti+1;i<=y;i++)
								{
								
									result_YUV[0][i*img.width+x]=c1;//src_YUV[0][yini*img.width+x];
								
									
								}	
							}
							else //borde H
							{
								int y1=y_anti+(y-y_anti)/2;
								for (int i=y_anti+1;i<=y1;i++)
									{
									
										result_YUV[0][i*img.width+x]=c0;//src_YUV[0][yini*img.width+x];
									
										
									}	
								for (int i=y1+1;i<=y;i++)
								{
								
									result_YUV[0][i*img.width+x]=c1;//src_YUV[0][yini*img.width+x];
								
									
								}
							}
							
							//calculo de etiquetas
							//--------------------
							//ya puedo calcular alfa para las etiquetas
							float alfa=0;
							float labelini=0;
							
							switch (microtype)
							{
							
							case 0: //  x
								alfa =0f;
								labelini=0;
								break;
							
							case 1: //  /
								alfa =-1f/pppy;
								labelini=1;
								break;
							case 2:// \
								alfa =1f/pppy;
								labelini=0;
								break;
							case 3:// |
								alfa =0f;
								labelini=1;//0.5f;
								break;
							case 4:	// -
								alfa =0f;
								labelini=0;
								break;
								
							}
							//ahora relleno la etiqueta
							int j=0;
							System.out.println ("------labeling------");
							for (int i=y_anti+1;i<=y;i++)
							{
								
								
								img.label_YUV[0][i*img.width+x]=labelini+j*alfa;//src_YUV[0][yini*img.width+x];
								j++;
								System.out.println("type:"+microtype+"  ,y="+i+", x="+x+" , label="+ img.label_YUV[0][i*img.width+x]);
								
							}	
							
							
							
							
							c1=c2;
							y_anti=y;
							//tot_ppp=pppy/2f;
							//yf+=pppy/2f; ///from last ppp
							pppy+=gry_sc;
							//yf+=pppy/2;///2f;//from new ppp
							//tot_ppp+=pppy/2f;
							yf+=pppy;
						}//y
						ppp_yc+=grycy_sc;
						ppp_yd+=grydy_sc;
					}//x
				}	
				//*******************************************************************
				/**
				 * horizontal interpolation in neighbour+ LINEAR mode
				 * 
				 *  the interpolation process is: first vertical then horizontal
				 * @param src_YUV
				 * @param result_YUV
				 */
				public void interpolateNNSRH( int[][] src_YUV, int[][] result_YUV)
				{

					float leny=ly;
			//leny=ly_sc;
					float gryax_sc=0;
					//gradient x of side a
					if (ppp[0][2]!=ppp[0][0])
						gryax_sc=(ppp[0][2]-ppp[0][0])/(leny-1);//lya_sc;
					float grybx_sc=0;
					if (ppp[0][3]!=ppp[0][1])
						grybx_sc=(ppp[0][3]-ppp[0][1])/(leny-1);//b_sc;

					//initial ppp values of sides "a" and "b"
					float ppp_xa=ppp[0][0];
					float ppp_xb=ppp[0][1];


					// pppx is initialized to ppp_xa
					float pppx=ppp_xa; // only for gradient calculation
					

					int x=0;
					//float grx_sc=(2*lx +ppp_xa-ppp_xb-2*ppp_xa*lx_sc)/((lx_sc-1)*lx_sc);
					int x_anti=0;


					for (int y=yini;y<yini+leny;y++)
					{
						//starts at side c ( which is y==yini)
						float grx_sc=0;

						//if (ppp_xa!=ppp_xb) grx_sc=(2*lx +ppp_xa-ppp_xb-2*ppp_xa*lx_sc)/((lx_sc-1)*lx_sc);				
						grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1);
						
						pppx=ppp_xa;
						//pppx=ppp_xa/2f;//first segment ends at the middle ***
						
						int c0=0; // previous color not exist. updated after each iteration
						int c1=0; //new color updated at each iteration

						
						// sample 

						float xf=xini+pppx-1;// -1 because xini exists
						//float xf=xini+pppx/2-1;// -1 because xini exists
						if (xf<xini)xf=xini;
						
						x_anti=xini-1;//(int)(xf-pppx+0.5f);

						
						
						
						// bucle for horizontal scanline 
						for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
						{
							//if ( x_sc==xini+lx_sc-1) xf=xfin;

							x=(int) (xf+0.5f);
							//x=(int) (xf+1f);//queda mejor
							if (x>xfin) x=xfin;

							int x0=x_sc-1;
							if (x0==xini-1) x0=xini;
							
							c0=src_YUV[0][y*img.width+x0];
							
							c1=src_YUV[0][y*img.width+x_sc];
							
							//ahora voy a coger el valor donde finaliza c0 y comienza c1, que es la etiqueta						
							int num_pix=(int)((x_anti+1)+pppx*img.label_YUV[0][y*img.width+x_sc]+0.5f);
							
							System.out.println ("num_pix:"+num_pix);
							//tramo de c0
							for (int i=x_anti+1;i<num_pix;i++)
							  {
								result_YUV[0][y*img.width+i]=c0;//0;
							 }
							//tramo de c1
							for (int i=num_pix;i<=x;i++)
							  {
									result_YUV[0][y*img.width+i]=c1;
							  }
								
							
							c0=c1;
							x_anti=x;
							//xf+=pppx/2f;//from last ppp
							pppx+=grx_sc;
							xf+=pppx;///2;//ok
							//xf+=pppx/2f;//from new ppp
						}//x
						ppp_xa+=gryax_sc;
						ppp_xb+=grybx_sc;
					}//y
				}	
				//*******************************************************************	
				//*******************************************************************
				/**
				 * vertical interpolation in EPX mode
				 *  
				 *  the interpolation process is: first vertical then horizontal
				 * @param src_YUV
				 * @param result_YUV
				 */
				public void interpolateEPXV_001( int[][] src_YUV, int[][] result_YUV)
				{
					float lenx=lx_sc;
			//lenx=lx;
					//gradient PPPy side c
					float grycy_sc=(ppp[1][1]-ppp[1][0])/(lenx-1);//lya_sc;
					//gradient PPPy side d
					float grydy_sc=(ppp[1][3]-ppp[1][2])/(lenx-1);

					//en la vertical no hace falta porque el bloque esta comprimido y no desplazado
					float ppp_yc=ppp[1][0];//-grycy_sc*(ppp[0][0]/2)*((lx_sc)/(lx));
					float ppp_yd=ppp[1][2];//-grydy_sc*(ppp[0][2]/2)*((lx_sc)/(lx));

					// pppx initialized to ppp_yc
					float pppy=ppp_yc;

					//input image is downsampled_YUV. block width is lx_sc
					int y=0;

					//dont scan lx but lenx. ( lenx value is lx or lx_sc)
					for (int x=xini;x<xini+lenx;x++)
					{
						float gry_sc=0;
						//if (ppp_yc!=ppp_yd) gry_sc=(2*ly-2*ppp_yc*(ly_sc)+ppp_yc-ppp_yd)/((ly_sc-1)*ly_sc);

						gry_sc=(ppp_yd-ppp_yc)/(ly_sc-1);
						
						//ppp_yc is updated at each iteration
						pppy=ppp_yc;//-grycy_sc;

						float yf=yini+pppy-1f;//1;

						//previous interpolated y coordinate
						int y_anti=(int)(yf-pppy+0.5f);

						// bucle for horizontal scanline 
						// scans the downsampled image, pixel by pixel
						for (int y_sc=yini;y_sc<yini+ly_sc;y_sc++)
						{
							//end of block in nearest neighbour mode
							//if ( y_sc==yini+ly_sc-1) yf=yfin;

							//integer interpolated y coordinate 
							y=(int) (yf+0.5f);
							if (y>yfin) y=yfin;//esta protección no hace falta

							if (y_sc==yini) 
							{
								//nearest neighbour mode: copy sample into ppp/2 pixels
								for (int i=yini;i<=y;i++)
								{
									result_YUV[0][i*img.width+x]=src_YUV[0][yini*img.width+x];
								}	
							}
							// y_sc is not yini  
							else 
							{
								for (int i=y_anti+1;i<=y;i++)
								{
									result_YUV[0][i*img.width+x]=src_YUV[0][y_sc*img.width+x];
								}
							}
							y_anti=y;
							pppy+=gry_sc;
							yf+=pppy;//ok
						}//y
						ppp_yc+=grycy_sc;
						ppp_yd+=grydy_sc;
					}//x
				}	
				//*******************************************************************
				/**
				 * horizontal interpolation in EPX mode
				 * 
				 *  the interpolation process is: first vertical then horizontal
				 * @param src_YUV
				 * @param result_YUV
				 */
				public void interpolateEPXH_001( int[][] src_YUV, int[][] result_YUV)
				{
						// ESTO ES UN VECINO CERCANO, NO VEO DIFERENCIA
					// NO RECUERDO PORQUE HICE ESTA FUNCION
					
					float leny=ly;
			//leny=ly_sc;
					float gryax_sc=0;
					//gradient x of side a
					if (ppp[0][2]!=ppp[0][0])
						gryax_sc=(ppp[0][2]-ppp[0][0])/(leny-1);//lya_sc;
					float grybx_sc=0;
					if (ppp[0][3]!=ppp[0][1])
						grybx_sc=(ppp[0][3]-ppp[0][1])/(leny-1);//b_sc;

					//initial ppp values of sides "a" and "b"
					float ppp_xa=ppp[0][0];
					float ppp_xb=ppp[0][1];


					// pppx is initialized to ppp_xa
					float pppx=ppp_xa;


					int x=0;
					//float grx_sc=(2*lx +ppp_xa-ppp_xb-2*ppp_xa*lx_sc)/((lx_sc-1)*lx_sc);
					int x_anti=0;


					for (int y=yini;y<yini+leny;y++)
					{
						//starts at side c ( which is y==yini)
						float grx_sc=0;

						//if (ppp_xa!=ppp_xb) grx_sc=(2*lx +ppp_xa-ppp_xb-2*ppp_xa*lx_sc)/((lx_sc-1)*lx_sc);				
						grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1);
						
						pppx=ppp_xa;
						// sample 

						float xf=xini+pppx-1;// -1 because xini exists

						x_anti=(int)(xf-pppx+0.5f);

						// bucle for horizontal scanline 
						for (int x_sc=xini;x_sc<xini+lx_sc;x_sc++)
						{
							//if ( x_sc==xini+lx_sc-1) xf=xfin;

							x=(int) (xf+0.5f);
							if (x>xfin) x=xfin;

							if (x_sc==xini) 
							{
								//nearest neighbour:copy sample into ppp/2 pixels
								for (int i=xini;i<=x;i++)
								{
									result_YUV[0][y*img.width+i]=src_YUV[0][y*img.width+x_sc];
								}
							}// if x_sc==xini

							else  // x_sc is not xini
							{
								for (int i=x_anti+1;i<=x;i++)
								{
									result_YUV[0][y*img.width+i]=src_YUV[0][y*img.width+x_sc];
									//if (y==b.yini+leny-1) result_YUV[0][y*img.width+i]=255;
								}
							}

							x_anti=x;
							pppx+=grx_sc;
							xf+=pppx;///2;//ok
						}//x
						ppp_xa+=gryax_sc;
						ppp_xb+=grybx_sc;
					}//y
				}	
				//*******************************************************************
//***********************************************************************************
// esta funcion implementa el algoritmo EPX y retorna 4 pixeles nuevos a partir del pixel
// de color O (original)
//  A
// COB
//  D				
public int[] getEPX(int a, int b, int c, int d, int o)
{
	int[] res=new int[4];
	
	
	return res;
}
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void downsampleSPSOneShot_mal( int[][] src_YUV, int[][] result_YUV)
{
	//we assume that the block has already rectangle shape, 
	// during this "Horizontal" processing we fill  the "horizontal_downsampled_YUV" array
	//gradient PPPx side a


	float leny=ly_sc;
	//float lenx=lx;
	
	//gradient side a
	float gryax_sc=(ppp[0][2]-ppp[0][0])/(leny-1);
	float gryay_sc=(ppp[1][2]-ppp[1][0])/(leny-1);

	//gradient PPPx side b
	float grybx_sc=(ppp[0][3]-ppp[0][1])/(leny-1);
	float gryby_sc=(ppp[1][3]-ppp[1][1])/(leny-1);

	//initialization of ppp at side a and ppp at side b
	float ppp_xa=ppp[0][0];
	float ppp_xb=ppp[0][1];
    float ppp_ya=ppp[1][0];
    float ppp_yb=ppp[1][1];
	
    //System.out.println("ppp_ya="+ppp_ya+"    ppp_yb="+ppp_yb);
	
	//dominio down
	int y_sc=yini;
	float ya_ini=(int)(yini+ppp_ya/2);
	float yb_ini=(int)(yini+ppp_yb/2);
	
	
	//for (float ya=ya_ini,yb=yb_ini;ya<=yfin;ya+=ppp_ya,yb+=ppp_yb)
	for (float ya=ya_ini,yb=yb_ini;ya<=yfin;)
	{
		//System.out.println("ppp_ya="+ppp_ya+"    ppp_yb="+ppp_yb);
		float grx_sc=(ppp_xb-ppp_xa)/(lx_sc-1f); //lx_sc -1 steps
		
		float gry_sc=(yb-ya)/(lx_sc-1f); //lx_sc -1 steps
		//gry_sc=(yb-ya)/(lx_sc);
		
		
		
		//initialization of pppx at start of scanline
		float pppx=ppp_xa;
		//float pppy=ppp_ya;
		
		float xa=xini+pppx/2;
		
		//dominio original
		float y=(int)(ya);//+0.5f);
		float x=(int)xa;
				
		//System.out.println("xa:"+x+"    ya:"+y);
		for (int x_sc=xini;x_sc<=downsampled_xfin;x_sc++)
		{
			System.out.println("x:"+x+"    y:"+y);
			if (x>img.width-1) x=img.width-1;
			if (y>img.height-1) y=img.height-1;
			if (x<0) x=0;
			if (y<0) y=0;
			//System.out.println("x:"+x+"    y:"+y);
			//System.out.println("x_sc:"+x_sc+"    y_sc:"+y_sc);
			int sample=src_YUV[0][(int)y*img.width+(int)x];
			//if (y==yini) sample=255;
			//if (y==yfin) sample=255;
			//if (y_sc==downsampled_yfin) sample=255;
			result_YUV[0][y_sc*img.width+x_sc]=sample;
			
			x+=pppx;//ok
			y+=gry_sc;//OJO ESTE ERA EL ERROR: AQUI SE SUMA EL GRADIENTE Y NO EL PPP
			
			if (y>yfin) {
				//System.exit(0); //nunca ocurre
				// de todos modos como es sps puede no escogerse yfin perfectamente
				y=yfin;//nuevo 201709	
			}
			pppx+=grx_sc;
			//pppy+=gry_sc;

		}//x
		ppp_xa+=gryax_sc;
		ppp_xb+=grybx_sc;
		ppp_ya+=gryay_sc;
		ppp_yb+=gryby_sc;
		y_sc+=1; //listo para siguiente scanline
		//if (y_sc>=img.height) break;
		if (y_sc>downsampled_yfin) break;
		ya+=ppp_ya;
		yb+=ppp_yb;
		if (ya>yfin) ya=yfin;
		if (yb>yfin) yb=yfin;
		
		
		//if (y_sc>downsampled_yfin) y_sc=downsampled_yfin;
		//if (y>yfin) break;
	}//y


}




//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public void downsampleSPSOneShot( int[][] src_YUV, int[][] result_YUV)
{
	// we assume that the block has already rectangle shape, 
	// during this "Horizontal" processing we fill  the "horizontal_downsampled_YUV" array
	// gradient PPPx side a
	
	//initialization of ppp 
	float ppp_xa=ppp[0][0];
	float ppp_xb=ppp[0][1];
    float ppp_ya=ppp[1][0];
    float ppp_yb=ppp[1][1];
    float ppp_yc=ppp[1][0];
    
	//gradient side a
	float gr_a_pppx=(ppp[0][2]-ppp[0][0])/(ly_sc-1);
	float gr_a_pppy=(ppp[1][2]-ppp[1][0])/(ly_sc-1);

	//gradient side b
	float gr_b_pppx=(ppp[0][3]-ppp[0][1])/(ly_sc-1);
	float gr_b_pppy=(ppp[1][3]-ppp[1][1])/(ly_sc-1);

    //gradient side c. ojo aqui usamos lx_sc
    float gr_c_pppy=(ppp[1][1]-ppp[1][0])/(lx_sc-1);
    
    float ya=yini;//mundo real
    
	for (int y_sc=yini;y_sc<=downsampled_yfin;y_sc++)
	{
		ppp_yc=ppp[1][0];
		float gr_pppx=(ppp_xb-ppp_xa)/(lx_sc-1f);
		float gr_pppy=(ppp_yb-ppp_ya)/(lx_sc-1f);
	    
		//initialization of pppx at start of scanline
		float pppx=ppp_xa;
		float xa=xini;
		
		//initialization of pppy at start of scanline
		float pppy=ppp_ya;
		ya+=ppp_ya/2;
		
		//dominio original
		float y=ya;
		float x=xa;
		
		for (int x_sc=xini;x_sc<=downsampled_xfin;x_sc++)
		{
			x+=pppx/2;
			y=(int)(yini+ppp_yc/2+((pppy+ppp_yc)*(y_sc-yini))/2);
			
			result_YUV[0][y_sc*img.width+x_sc]=src_YUV[0][(int)y*img.width+(int)(x)];
			
			
			pppx+=gr_pppx;
			x+=pppx/2;
			
			ppp_yc+=gr_c_pppy;
			pppy+=gr_pppy;

		}//x
		ppp_xa+=gr_a_pppx;
		ppp_xb+=gr_b_pppx;
		ppp_ya+=gr_a_pppy;
		ppp_yb+=gr_b_pppy;
		
		ya+=ppp_ya/2;
		
	}//y


}

//****************************************************************************************
public void filterEPX2x(int u1,int u2 )
{
//esta funcion filtra por EPX la imagen 
	int[] im=img.interpolated_YUV[0];
	System.out.println("filtering EPX...");
	for (int y=yini; y<yfin;y++)
	{
		for (int x=xini; x<xfin-1;x++)
		{
			//filter1pixEPX2x(im,y,x,u); //filtra 1pixel
			//filter1pixEPX2x_002(im,y,x,u); //filtra 1pixel
			if (y>0 && x>1)
			filter1pixEPX2x_003(im,y,x,u1,u2); //filtra 1pixel
		}	
	}
	System.out.println("filtered !");
}
//****************
//****************************************************************************************
// est funcion filtra por epx selectivamente segun el pppx y pppy, pix a pix dentro del bloque
public void filterEPXP(int u1,int u2 , int by, int bx, PRblock[][] prbl)
{
//esta funcion filtra por EPX la imagen 
	int[] im=img.interpolated_YUV[0];
	System.out.println("filtering EPX...");
	
	//gradientes de PR
	float prax=prbl[by][bx].PRx;
	float prbx=prbl[by][bx+1].PRx;
	float pray=prbl[by][bx].PRy;
	float prby=prbl[by][bx+1].PRy;
	
	float prx=prax;
	float pry=pray;
	
	float gry_prax=(prbl[by+1][bx].PRx-prbl[by][bx].PRx)/(yfin-yini);
	float gry_prbx=(prbl[by+1][bx+1].PRx-prbl[by][bx+1].PRx)/(yfin-yini);
	float gry_pray=(prbl[by+1][bx].PRy-prbl[by][bx].PRy)/(yfin-yini);
	float gry_prby=(prbl[by+1][bx+1].PRy-prbl[by][bx+1].PRy)/(yfin-yini);
	
	
	//gradientes de PPP
	float pppax=ppp[0][0];
	float pppbx=ppp[0][1];
	float pppx=pppax;
	
	float pppay=ppp[1][0];
	float pppby=ppp[1][1];
	float pppy=pppay;
	
	
	float gry_pppax=(ppp[0][2]-ppp[0][0])/(yfin-yini);// constante, es el lado a
	float gry_pppbx=(ppp[0][1]-ppp[0][3])/(yfin-yini);// constante, es el lado b
	
	float gry_pppay=(ppp[1][2]-ppp[1][0])/(yfin-yini);// constante, es el lado a
	float gry_pppby=(ppp[1][1]-ppp[1][3])/(yfin-yini);// constante, es el lado b
	
	
	float umbral_ppp=1.5f;
	float umbral_pr=0.125f;
	
	
	for (int y=yini; y<=yfin;y++)
	{
		pppx=pppax;
		pppy=pppay;
		
		prx=prax;
		pry=pray;
		
		float grx_pppx=(pppbx-pppax)/(xfin-xini);//variable segun scanline
		float grx_pppy=(pppby-pppay)/(xfin-xini);//variable segun scanline
		
		float grx_prx=(prbx-prax)/(xfin-xini);//variable segun scanline
		float grx_pry=(prby-pray)/(xfin-xini);//variable segun scanline
		
		
		
		//System.out.println ("hola");
		for (int x=xini; x<=xfin;x++)
		{
			//filter1pixEPX2x(im,y,x,u); //filtra 1pixel
			//filter1pixEPX2x_002(im,y,x,u); //filtra 1pixel
			//if (y>0 && x>1)
			if (x>0 && y>0 && x<img.width-1 && y<img.height-1)
			{
				/*
			if (prx<=umbral_pr)
			{
				im[y*img.width+x]=(im[y*img.width+x-1]+im[(y)*img.width+x])/2;
				if (pry<=umbral_pr)
				{
					im[y*img.width+x]=(int)(0.33f*im[(y-1)*img.width+x]+0.67f*im[(y)*img.width+x]);
				}
				
			}
			else if (pry<=umbral_pr)
			{
				im[y*img.width+x]=(im[(y-1)*img.width+x]+im[(y)*img.width+x])/2;
			}
			*/
				
			  if (prx<=umbral_pr && pry<=umbral_pr)
			    {
				
				  filter1pixSoft(im,y,x);
				 // filter1pixEPX2x_003(im,y,x,u1,u2); //filtra 1pixel
				//System.out.println ("**********************************************");
			    }
			    
			  else if (pppx>=umbral_ppp && pppy>=umbral_ppp)
			    {
				  
			      filter1pixEPX2x_003(im,y,x,u1,u2); //filtra 1pixel
			     
			    }
			}
			pppx+=grx_pppx;
			pppy+=grx_pppy;
			
			prx+=grx_prx;
			pry+=grx_pry;
			
			//System.out.println ("prx:"+prx+ "     pry:"+pry);
		}	
		pppax+=gry_pppax;
		pppbx+=gry_pppbx;
		pppay+=gry_pppay;
		pppby+=gry_pppby;
		
		prax+=gry_prax;
		prbx+=gry_prbx;
		pray+=gry_pray;
		prby+=gry_prby;
		
		
	}
	System.out.println("filtered !");
}

//*************************************************
public void filter1pixSoft(int [] im, int y, int x)
{
	im[y*img.width+x]=(im[y*img.width+x-1]+im[y*img.width+x+1]+im[(y-1)*img.width+x]+im[(y+1)*img.width+x]+im[y*img.width+x])/5;
	
	//im[y*img.width+x]=(im[y*img.width+x-1]+im[(y-1)*img.width+x]+im[y*img.width+x])/3;
}


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
}//end class Block
