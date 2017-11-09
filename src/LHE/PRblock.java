package LHE;


/**
 * PRblock
 * 
 * @author josejavg
 *
 * PRBlock stores PR values of the center ( float values)
 *
 * after quantization, 4 possible values for each coordinate are allowed: 
 * therefore, 2 bits x 2 coord = 4 bits per PRblock
 * 
 *     +---------------------+
 *     |                     |
 *     |                     |
 *     |                     |
 *     |     (PRx,PRy)       |
 *     |         .           |
 *     |                     |
 *     |                     |
 *     |                     |
 *     +---------------------+
 *     
 */

public class PRblock {

	//static
	public static ImgUtil img;

	//shape
	public int xini;
	public int yini;
	public int xfin;
	public int yfin;

	//PR at the center , normalized values from 0..1
	public float PRx;
	public float PRy;
	
	//PR values non quantized
	public float nqPRx;
	public float nqPRy;
	//PR values non quantized
	public float[] HistnqPRx;
	public float[] HistnqPRy;
	
	
	//PR computation mode values [HOPS | LUM]
	public static String PR_MODE="HOPS"; 
	
	//******************************************************************
	public void quantizeGeometricalPR()
	{
		
		//previous backup of non-quantized PR values
		
		//nqPRx=PRx;
		//nqPRy=PRy;
		
		
		//System.out.println(" PR:"+PRx);
		//quantization based on equi-distant thresholds
		float levelnull=0.125f;//125f;//125f;//125f;//125f;//325f;//0.125f;//0.125f;//125f;//125f;//125f;//125f;//0.5f;//0.25f;
		//float levelmax=0.875f;//0.5f;//0.25f;
		
		float level0=0.25f;//25f;//0.5f;//0.25f;
		float level1=0.5f;//0.5f;//1.4f;//0.50f;
		float level2=0.75f;//0.75f;//1.5f;//0.75f;

		//de este modo cada intervalo gasta lo mismo que el anterior. doble rango pero mitad de ppp
		//level0=0.40f;//0.5f;//0.35f;//0.40f;//0.5f;//0.5f;//0.40f;
		//level1=0.65f;//0.75f;//0.70f;//0.65f;//0.7f;////0.75f;//0.7
		//level2=0.85f;//0.875f;//0.90f;//0.85f;//0.95f;//0.90f;//0.9
		
		
		float cuanto0=0.0f;
		float cuanto1=0.125f;//25f;
		float cuanto2=0.25f;//0.25f;;
		float cuanto3=0.5f;
		float cuanto4=1f;
		/*
		 cuanto0=(levelnull)/2f;;
		 cuanto1=(level0-levelnull)/2f;;
		 cuanto2=(level1-level0)/2f;;
		 cuanto3=(level2-level1)/2f;;
		 cuanto4=(1f-level2)/2f;
		*/
		/* cuanto0=0.0f;
		 cuanto1=0.11f;
		 cuanto2=0.33f;
		 cuanto3=0.66f;
		 cuanto4=1f;
		 */
		//PRx=(float)Math.sqrt(PRx);
		//PRy=(float)Math.sqrt(PRy);
		
		
		
		
		if (PRx<levelnull)PRx=cuanto0;//0.00f;
		else if (PRx<level0) PRx=cuanto1;//0.125f;//level0/2f;
		else if (PRx<level1) PRx=cuanto2;//0.25f;
		else if (PRx<level2) PRx=cuanto3;//0.5f;
		//else if (PRx<levelmax) PRx=0.75f;
		else PRx=cuanto4;//1f;//(1f+level2)/2f;

		if (PRy<levelnull)PRy=cuanto0;//0.00f;
		else if (PRy<level0) PRy=cuanto1;//0.125f;//level0/2f;
		else if (PRy<level1) PRy=cuanto2;//0.25f;
		else if (PRy<level2) PRy=cuanto3;//0.5f;
		//else if (PRy<levelmax) PRy=0.75f;
		else PRy=cuanto4;//1f;//(1f+level2)/2f;

		
		
		
		
	}
	//******************************************************************
	//******************************************************************
		public void quantizeGeometricalPR_ok_no_se_usa()
		{
			//quantization based on equi-distant thresholds
			float level0=0.25f;
			float level1=0.50f;
			float level2=0.75f;

			//de este modo cada intervalo gasta lo mismo que el anterior. doble rango pero mitad de ppp
			//level0=0.40f;//0.5f;//0.35f;//0.40f;//0.5f;//0.5f;//0.40f;
			//level1=0.65f;//0.75f;//0.70f;//0.65f;//0.7f;////0.75f;//0.7
			//level2=0.85f;//0.875f;//0.90f;//0.85f;//0.95f;//0.90f;//0.9
			
			if (PRx<level0) PRx=0.125f;//level0/2f;
			else if (PRx<level1) PRx=0.25f;
			else if (PRx<level2) PRx=0.5f;
			else PRx=1f;//(1f+level2)/2f;

			if (PRy<level0) PRy=0.125f;//level0/2f;
			else if (PRy<level1) PRy=0.25f;
			else if (PRy<level2) PRy=0.5f;
			else PRy=1f;//(1f+level2)/2f;

		}
		//******************************************************************
	public void quantizeLinearPR()
	{
		//quantization based on geometrical progression of thresholds with ratio=2
		float level0=0.25f;
		float level1=0.50f;
		float level2=0.75f;

		if (PRx<level0) PRx=level0/2f;
		else if (PRx<level1) PRx=(level1+level0)/2f;
		else if (PRx<level2) PRx=(level2+level1)/2f;
		else PRx=1f;//(1f+level2)/2f;

		if (PRy<level0) PRy=level0/2f;
		else if (PRy<level1) PRy=(level1+level0)/2f;
		else if (PRy<level2) PRy=(level2+level1)/2f;
		else PRy=1f;//(1f+level2)/2f;

	}
	public void quantizePureLinearPR()
	{
		//quantization based on geometrical progression of thresholds with ratio=2
		float level0=0.25f;
		float level1=0.50f;
		float level2=0.75f;

		if (PRx<level0) PRx=level0/2f;
		else if (PRx<level1) PRx=(level1+level0)/2f;
		else if (PRx<level2) PRx=(level2+level1)/2f;
		else PRx=(1f+level2)/2f;

		if (PRy<level0) PRy=level0/2f;
		else if (PRy<level1) PRy=(level1+level0)/2f;
		else if (PRy<level2) PRy=(level2+level1)/2f;
		else PRy=(1f+level2)/2f;

	}
	//*********************
	//******************************************************************
		public void quantizeRarePR()
		{
			//quantization based on geometrical progression of thresholds with ratio=2
			float level0=0.25f;
			float level1=0.50f;
			float level2=0.75f;

			if (PRx<level0) PRx=0.1f;
			else if (PRx<level1) PRx=0.3f;
			else if (PRx<level2) PRx=0.6f;
			else PRx=1f;//(1f+level2)/2f;

			if (PRy<level0) PRy=0.1f;
			else if (PRy<level1) PRy=0.3f;
			else if (PRy<level2) PRy=0.6f;
			else PRy=1f;//(1f+level2)/2f;

		}
	//******************************************************************
		
		//ESTA ES LA FUNCION CORRECTA 6/9/2017
	public void computePRmetrics()
   //public void computePRmetrics_OK()
	{
		
		if (PR_MODE.equals("LUM")) {computePRmetrics_LUM();
		//System.out.println ("Hola");
		return;}
		
		//PR computation normalized to 0..1

		//Grid class creates a grid of Blocks and PRBlocks.
		//PRBlocks coordinates are in 0..width and 0..height
		int last_hop=0;
		int hop=0;
		boolean hop_sign=true;
		boolean last_hop_sign=true;
		PRx=0;
		PRy=0;

		//float tune=1.0f;
		//PRx

		float Cx=0;
		float Cy=0; 
        
		//if (2>1) { System.out.println("hola");System.exit(0);}

		for (int y=yini;y<=yfin;y++)
		{
			if (y>0)
			{
				last_hop=img.hops[0][(y-1)*img.width+xini]-4;
				//last_hop_sign=(last_hop>0);
				last_hop_sign=(last_hop>=0); //NUEVO 19/3/2015 
				//System.out.println(" signo:"+last_hop_sign+ "hop:"+last_hop);
				
			}
			//horizontal scanlines
			for (int x=xini;x<=xfin;x++)
			{
				
				hop=img.hops[0][y*img.width+x]-4;//[0] is lumminance (Y)
				// hop value: -4....0....+4
				if (hop==0) continue; //h0 no sign
				//hop_sign=(hop>0);
				hop_sign=(hop>=0);//NUEVO 19/3/2015 
				//if (hop>0) hop_sign=true;
				//else hop_sign=false;
				//if (hop_sign!=last_hop_sign && last_hop!=0) {

				if ((hop_sign!=last_hop_sign && last_hop!=0) || hop==4 || hop==-4) {//NUEVO 19/3/2015 
				
				//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop>=3 || hop<=-3) {//NUEVO 19/3/2015 
						
					int weight=hop;
					if (weight<0)weight=-weight;
					//int weight=hop;
					//if (hop%2!=0) weight--; // possitive and negative must weigh the same [0..2..4..6]
					//weight=8-hop; //from 2 to 8
					//if (weight>7 ) tune=1;
					//else tune=1;
					//antes era 0 2 4 6, osea...max 6
					//int w=0;
					//if (weight!=0) w=4;
					//PRx+=w;//weight;
					PRx+=weight;
					//System.out.println ("hop:"+hop+"  lasth:"+last_hop+ "x:"+x);
					Cx++;

				}
				//System.out.println("scan");
				last_hop=hop;
				last_hop_sign=hop_sign;
			}
			//System.out.println("scan");
		}

		hop=0;
		hop_sign=true;
		last_hop=0;
		last_hop_sign=true;

		//System.out.println(" processing vscanlines  img.width"+img.width+"   xini:"+xini+"   xfin:"+xfin+"  yini:"+yini+"    yfin:"+yfin);
		for (int x=xini;x<=xfin;x++)
		{
			//System.out.println("x:"+x);
			if (x>0)
			{
			    //left pixel
				last_hop=img.hops[0][(yini)*img.width+x-1]-4;
				//last_hop_sign=(last_hop>0);
				last_hop_sign=(last_hop>=0);//NUEVO 19/3/2015 
			}
			//vertical scanlines
			for (int y=yini;y<=yfin;y++)
			{

				hop=img.hops[0][y*img.width+x]-4;//[0] is lumminance (Y)
				if (hop==0) continue; //h0 no sign
				//hop_sign=(hop>0);
				hop_sign=(hop>=0);//NUEVO 19/3/2015 
				//if (hop%2!=last_hop%2 && last_hop!=8) {
				//if (hop_sign!=last_hop_sign && last_hop!=0) {
				
				if ((hop_sign!=last_hop_sign && last_hop!=0) || hop==4 || hop==-4){
				//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop>=3 || hop<=-3){
					//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop!=4){
							
					
					int weight=hop;
					if (weight<0)weight=-weight;
					//if (hop%2!=0) weight--; // possitive and negative must weigh the same [0..2..4..6]
					//weight=8-hop; //from 2 to 8
					//int w=0;
					//if (weight!=0) w=4;
					//PRy+=w;//weight;
					PRy+=weight;
					//System.out.println ("hop:"+hop+"  lasth:"+last_hop);
					Cy++;
				}
				last_hop=hop;
				last_hop_sign=hop_sign;
			}

		}

		//System.out.println("numerador:"+PRx+" denominador:"+Cx*4);
		if (PRx>0) PRx=PRx/(Cx*4);
		if (PRy>0) PRy=PRy/(Cy*4);

		//if (PRx>0) PRx=PRx/((xfin-xini)*(yfin-yini)*4);
		//if (PRy>0) PRy=PRy/((xfin-xini)*(yfin-yini)*4);
		
		//if (PRx<0.01f || PRy<0.01f) System.out.println("NOTHING");
		if (PRx>1 || PRy>1) {
			System.out.println(" failure. imposible PR value > 1");
			System.exit(0);
		}

		//if (PRx==0.0) {
			//System.out.println("ha salido cero");
			//System.exit(0);
		//}
		//else System.out.println("no es cero");
		
    	//System.out.println("PRx:"+PRx+"  , PRy:"+PRy+"   xini:"+xini+"  xfin:"+xfin+"  yini:"+yini+"  yfin:"+yfin);
		//System.out.println((int)(PRx*100));
		//System.out.println((int)(PRy*100));
		float umbral=10.5f;//0.5f; //LATER
		if (PRx>umbral) PRx=0.5f;//1;
		if (PRy>umbral) PRy=0.5f;//1;
		
		//PRx=1-PRx;
		//PRy=1-PRy;
		
		//nqPRx=PRx;
		//nqPRy=PRy;
	}
//**********************************************************************************
public int log2(int dato)
{
	//if (dato/255 ==1) return 4;
	int signo=1;
	
	if (dato<0) {dato =-dato; signo=-1;}
	dato=dato+4;
	if (dato/64 >=1) return signo*4;
	if (dato/32 ==1) return signo*3;
	if (dato/16 ==1) return signo*2;
	if (dato/8 ==1) return signo*1;
	return 0;
	
}
 //******************************************************************
	
	//experimento con luminancias
	public void computePRmetrics_LUM()
	//public void computePRmetrics()
	{
		//PR computation normalized to 0..1

		//Grid class creates a grid of Blocks and PRBlocks.
		//PRBlocks coordinates are in 0..width and 0..height
		int last_hop=0;
		int hop=0;
		
		//------------nuevo-----
		int lum_dif=0;
		int last_lum_dif=0;//0;
		
	    boolean lum_sign=true;
	    boolean last_lum_sign=true;
		
	    int divisor=6;//8;
	    //int divisor2=4;
	    //int divisor_ini=4;
	    //int divisor_min=4;
	    int max=4;
		//-----------------
		
		//boolean hop_sign=true;
		//boolean last_hop_sign=true;
		PRx=0;
		PRy=0;

		//float tune=1.0f;
		//PRx

		float Cx=0;
		float Cy=0; 
        
		boolean dif_sign=true;//pos
		//if (2>1) { System.out.println("hola");System.exit(0);}

		int q0=4;//6;//4;
		int q1=8;//12;//10;
		int q2=24;//18;//30;
		int q3=72;//24;//80;
		
		for (int y=yini;y<=yfin;y++)
		{
			if (y>0)
			{
				//last_hop=img.hops[0][(y-1)*img.width+xini]-4;
				
				//lum_dif=0;//(img.YUV[0][(y)*img.width+xini]-img.YUV[0][(y-1)*img.width+xini])/divisor;
				
				int dif=(img.YUV[0][(y)*img.width+xini]-img.YUV[0][(y-1)*img.width+xini]);
				//System.out.println ("dif inicial:"+dif);
				//last_lum_dif=dif/divisor;
				
				//---------
				dif_sign=(dif>=0);
				if (dif<0) dif=-dif;
				if (dif<q0) dif=0;
				else if (dif<q1) dif=1;
				else if (dif<q2) dif=2;
				else if (dif<q3) dif=3;
				else dif=4;
				last_lum_dif=dif;
				//-----------
				
				
				
				//System.out.println ("dif inicial/6:"+last_lum_dif);
				//last_lum_dif=log2(dif);
				//if (last_lum_dif>1) last_lum_dif=last_lum_dif/2;
				
				
				//if (last_lum_dif==4) last_lum_dif=3;
				//if (last_lum_dif==-4) last_lum_dif=-3;
				
				if (last_lum_dif>max) last_lum_dif=max;
				if (last_lum_dif<-max) last_lum_dif=-max;
				
				//last_hop_sign=(last_hop>=0); //NUEVO 19/3/2015
				
				last_lum_sign=(last_lum_dif>=0);
				
				//------
				last_lum_sign=dif_sign;
				//-----
				
			}
			//horizontal scanlines
			//for (int x=xini;x<=xfin;x++)
			for (int x=xini;x<=xfin;x++) //para poder hacer lum_dif
			{
				
				int dif=0;
				
				if (x>0) dif=(img.YUV[0][(y)*img.width+x]-img.YUV[0][(y)*img.width+x-1]);
				//System.out.println ("dif:"+dif+    " x="+x);	
				//lum_dif=dif/divisor;
				
				//---------
				dif_sign=(dif>=0);
				if (dif<0) dif=-dif;
				if (dif<q0) dif=0;
				else if (dif<q1) dif=1;
				else if (dif<q2) dif=2;
				else if (dif<q3) dif=3;
				else dif=4;
				lum_dif=dif;
				//------------
				
				
				//System.out.println ("dif/6:"+lum_dif);
				//lum_dif=log2(dif);
				
				//if (lum_dif==4) lum_dif=3;
				//if (lum_dif==-4) lum_dif=-3;
				
				//if (lum_dif>1) lum_dif=lum_dif/2;
				
				if (lum_dif>max) lum_dif=max;
				if (lum_dif<-max) lum_dif=-max;
				//hop=img.hops[0][y*img.width+x]-4;//[0] is lumminance (Y)
				
				//int k= lum_dif; if (k<0) k=-k; divisor=divisor_ini+k;
				
				
				//System.out.println("lum_dif="+lum_dif);
				
				//if (lum_dif==0 && x>0) lum_dif=(img.YUV[0][(y)*img.width+x]-img.YUV[0][(y)*img.width+x-1])/divisor2;
				
			//	if (lum_dif<=1) {divisor--; if (divisor<divisor_min) divisor=divisor_min;}
			//	else divisor=10;
				
				if (lum_dif==0) continue; 
				
				// hop value: -4....0....+4
				//if (hop==0) continue; //h0 no sign
				
				//hop_sign=(hop>=0);//NUEVO 19/3/2015 
				lum_sign=(lum_dif>=0);
				
				//-------
				lum_sign=dif_sign;
				//-------
				
				
				//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop==4 || hop==-4) {//NUEVO 19/3/2015 
				
				if ((lum_sign!=last_lum_sign && last_lum_dif!=0) || lum_dif==max || lum_dif==-max ) {//NUEVO 19/3/2015 
							
					
					
				//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop>=3 || hop<=-3) {//NUEVO 19/3/2015 
						
					//int weight=hop;
					
					int weight=lum_dif;
					
					if (weight<0)weight=-weight;
					
					PRx+=weight;
					//System.out.println ("hop:"+hop+"  lasth:"+last_hop+ "x:"+x);
					Cx++;

				}
				//System.out.println("scan");
				//last_hop=hop;
				//last_hop_sign=hop_sign;
				
				
				last_lum_dif=lum_dif;
				last_lum_sign=lum_sign;
				
			}
			//System.out.println("scan");
		}

		//hop=0;
		//hop_sign=true;
		//last_hop=0;
		//last_hop_sign=true;

		
		lum_sign=true;
		
		lum_dif=0;
		last_lum_sign=true;
		last_lum_dif=0;//0;
		
		//System.out.println(" processing vscanlines  img.width"+img.width+"   xini:"+xini+"   xfin:"+xfin+"  yini:"+yini+"    yfin:"+yfin);
		for (int x=xini;x<=xfin;x++)
		{
			//System.out.println("x:"+x);
			if (x>0)
			{
			    //left pixel
				//last_hop=img.hops[0][(yini)*img.width+x-1]-4;
				
				//lum_dif=0;
				
				
				int dif=(img.YUV[0][(yini)*img.width+x]-img.YUV[0][(yini)*img.width+x-1]);
				
				//last_lum_dif=dif/divisor;
				//---------
				dif_sign=(dif>=0);
				if (dif<0) dif=-dif;
				if (dif<q0) dif=0;
				else if (dif<q1) dif=1;
				else if (dif<q2) dif=2;
				else if (dif<q3) dif=3;
				else dif=4;
				last_lum_dif=dif;
				//-----------
				
				//last_lum_dif=log2(dif);
				//if (last_lum_dif>1) last_lum_dif=last_lum_dif/2;
				//if (last_lum_dif==4) last_lum_dif=3;
				//if (last_lum_dif==-4) last_lum_dif=-3;
				
				if (last_lum_dif>max) last_lum_dif=max;
				if (last_lum_dif<-max) last_lum_dif=-max;
				
				//last_hop_sign=(last_hop>=0); //NUEVO 19/3/2015
				last_lum_sign=(last_lum_dif>=0);
				
				//----
				last_lum_sign=dif_sign;
				//----
				
				
				//last_hop_sign=(last_hop>0);
				//last_hop_sign=(last_hop>=0);//NUEVO 19/3/2015
				//last_lum_sign=true;
				//last_lum_dif=0;
				
			}
			//vertical scanlines
			//for (int y=yini;y<=yfin;y++)
			for (int y=yini;y<=yfin;y++)//para poder calcular dif
			{

				//hop=img.hops[0][y*img.width+x]-4;//[0] is lumminance (Y)
				//if (hop==0) continue; //h0 no sign
				
				int dif=0;
				if (y>0)dif=(img.YUV[0][y*img.width+x]-img.YUV[0][(y-1)*img.width+x]);
	            
				//---------------
				dif_sign=(dif>=0);
				if (dif<0) dif=-dif;
				if (dif<q0) dif=0;
				else if (dif<q1) dif=1;
				else if (dif<q2) dif=2;
				else if (dif<q3) dif=3;
				else dif=4;
				lum_dif=dif;
				//---------------
				
				//lum_dif=dif/divisor;
	            
	            //lum_dif=log2(dif);
	            
				//if (lum_dif==4) lum_dif=3;
				//if (lum_dif==-4) lum_dif=-3;
	           
				
				//if (lum_dif>1) lum_dif=lum_dif/2;
				
				if (lum_dif>max) lum_dif=max;
				if (lum_dif<-max) lum_dif=-max;
				//System.out.println("lum_dif="+lum_dif+"    div:"+divisor);
				
				//int k= lum_dif; if (k<0) k=-k; divisor=divisor_ini+k;
				//if (lum_dif==0 && y>0) lum_dif=(img.YUV[0][y*img.width+x]-img.YUV[0][(y-1)*img.width+x])/divisor2;
				
				
				//if (lum_dif<=1) {divisor--; if (divisor<divisor_min) divisor=divisor_min;}
				//else divisor=10;
				
				 if (lum_dif==0) continue; 
				
				//hop_sign=(hop>=0);//NUEVO 19/3/2015
				lum_sign=(lum_dif>=0);
				
				//------
				lum_sign=dif_sign;
				//--------
				
				//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop==4 || hop==-4){
				
					
				
				if ((lum_sign!=last_lum_sign && last_lum_dif!=0) || lum_dif==max || lum_dif==-max ) {//NUEVO 19/3/2015 
										
					
					//int weight=hop;
					
					int weight=lum_dif;
					if (weight<0)weight=-weight;
					
					PRy+=weight;
					//System.out.println ("hop:"+hop+"  lasth:"+last_hop);
					Cy++;
				}
				//last_hop=hop;
				
				last_lum_dif=lum_dif;
				
				//last_hop_sign=hop_sign;
				last_lum_sign=lum_sign;
			}

		}

		//System.out.println("numerador:"+PRx+" denominador:"+Cx*4);
		if (PRx>0) PRx=PRx/(Cx*max);
		if (PRy>0) PRy=PRy/(Cy*max);

		//if (PRx>0) PRx=PRx/((xfin-xini)*(yfin-yini)*4);
		//if (PRy>0) PRy=PRy/((xfin-xini)*(yfin-yini)*4);
		
		//if (PRx<0.01f || PRy<0.01f) System.out.println("NOTHING");
		if (PRx>1 || PRy>1) {
			System.out.println(" failure. imposible PR value > 1");
			System.exit(0);
		}

		//if (PRx==0.0) {
			//System.out.println("ha salido cero");
			//System.exit(0);
		//}
		//else System.out.println("no es cero");
		
    	//System.out.println("PRx:"+PRx+"  , PRy:"+PRy+"   xini:"+xini+"  xfin:"+xfin+"  yini:"+yini+"  yfin:"+yfin);
		//System.out.println((int)(PRx*100));
		//System.out.println((int)(PRy*100));
		float umbral=10.5f;//0.5f; //LATER
		if (PRx>umbral) PRx=0.5f;//1;
		if (PRy>umbral) PRy=0.5f;//1;
		
		//PRx=1-PRx;
		//PRy=1-PRy;
		
		//nqPRx=PRx;
		//nqPRy=PRy;
	}
//**********************************************************************************	
	public void computePRmetricsSPS()
	{
		//PR computation normalized to 0..1

		//Grid class creates a grid of Blocks and PRBlocks.
		//PRBlocks coordinates are in 0..width and 0..height
		int last_hop=0;
		int hop=0;
		boolean hop_sign=true;
		boolean last_hop_sign=true;
		PRx=0;
		PRy=0;

		//float tune=1.0f;
		//PRx

		float Cx=0;
		float Cy=0; 


		for (int y=yini;y<=yfin;y++)
		{
			if (y>0)
			{
				last_hop=img.hops[0][(y-1)*img.width+xini]-4;
				//last_hop_sign=(last_hop>0);
				last_hop_sign=(last_hop>=0); //NUEVO 19/3/2015 
				//System.out.println(" signo:"+last_hop_sign+ "hop:"+last_hop);
				
			}
			//horizontal scanlines
			//for (int x=xini;x<=xfin;x++)
			
				for (int x=xini;x<=xini+(xfin-xini)/2;x++)
			{
				
				hop=img.hops[0][y*img.width+x]-4;//[0] is lumminance (Y)
				// hop value: -4....0....+4
				if (hop==0) continue; //h0 no sign
				//hop_sign=(hop>0);
				hop_sign=(hop>=0);//NUEVO 19/3/2015 
				//if (hop>0) hop_sign=true;
				//else hop_sign=false;
				//if (hop_sign!=last_hop_sign && last_hop!=0) {

				if ((hop_sign!=last_hop_sign && last_hop!=0) || hop==4 || hop==-4) {//NUEVO 19/3/2015 
				
				//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop>=3 || hop<=-3) {//NUEVO 19/3/2015 
						
					int weight=hop;
					if (weight<0)weight=-weight;
					//int weight=hop;
					//if (hop%2!=0) weight--; // possitive and negative must weigh the same [0..2..4..6]
					//weight=8-hop; //from 2 to 8
					//if (weight>7 ) tune=1;
					//else tune=1;
					//antes era 0 2 4 6, osea...max 6
					//int w=0;
					//if (weight!=0) w=4;
					//PRx+=w;//weight;
					PRx+=weight;
					//System.out.println ("hop:"+hop+"  lasth:"+last_hop+ "x:"+x);
					Cx++;

				}
				//System.out.println("scan");
				last_hop=hop;
				last_hop_sign=hop_sign;
			}
			//System.out.println("scan");
		}

		hop=0;
		hop_sign=true;
		last_hop=0;
		last_hop_sign=true;

		//System.out.println(" processing vscanlines  img.width"+img.width+"   xini:"+xini+"   xfin:"+xfin+"  yini:"+yini+"    yfin:"+yfin);
		//for (int x=xini;x<=xfin;x++)
		for (int x=xini;x<=xini+(xfin-xini)/2;x++)
		{
			//System.out.println("x:"+x);
			if (x>0)
			{
			    //left pixel
				last_hop=img.hops[0][(yini)*img.width+x-1]-4;
				//last_hop_sign=(last_hop>0);
				last_hop_sign=(last_hop>=0);//NUEVO 19/3/2015 
			}
			//vertical scanlines
			for (int y=yini;y<=yfin;y++)
			{

				hop=img.hops[0][y*img.width+x]-4;//[0] is lumminance (Y)
				if (hop==0) continue; //h0 no sign
				//hop_sign=(hop>0);
				hop_sign=(hop>=0);//NUEVO 19/3/2015 
				//if (hop%2!=last_hop%2 && last_hop!=8) {
				//if (hop_sign!=last_hop_sign && last_hop!=0) {
				
				if ((hop_sign!=last_hop_sign && last_hop!=0) || hop==4 || hop==-4){
				//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop>=3 || hop<=-3){
					//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop!=4){
							
					
					int weight=hop;
					if (weight<0)weight=-weight;
					//if (hop%2!=0) weight--; // possitive and negative must weigh the same [0..2..4..6]
					//weight=8-hop; //from 2 to 8
					//int w=0;
					//if (weight!=0) w=4;
					//PRy+=w;//weight;
					PRy+=weight;
					//System.out.println ("hop:"+hop+"  lasth:"+last_hop);
					Cy++;
				}
				last_hop=hop;
				last_hop_sign=hop_sign;
			}

		}

		//System.out.println("numerador:"+PRx+" denominador:"+Cx*4);
		if (PRx>0) PRx=PRx/(Cx*4);
		if (PRy>0) PRy=PRy/(Cy*4);

		//if (PRx>0) PRx=PRx/((xfin-xini)*(yfin-yini)*4);
		//if (PRy>0) PRy=PRy/((xfin-xini)*(yfin-yini)*4);
		
		//if (PRx<0.01f || PRy<0.01f) System.out.println("NOTHING");
		if (PRx>1 || PRy>1) {
			System.out.println(" failure. imposible PR value > 1");
			System.exit(0);
		}

		//if (PRx==0.0) {
			//System.out.println("ha salido cero");
			//System.exit(0);
		//}
		//else System.out.println("no es cero");
		
    	//System.out.println("PRx:"+PRx+"  , PRy:"+PRy+"   xini:"+xini+"  xfin:"+xfin+"  yini:"+yini+"  yfin:"+yfin);
		//System.out.println((int)(PRx*100));
		//System.out.println((int)(PRy*100));
		float umbral=10.5f;//0.5f; //LATER
		if (PRx>umbral) PRx=0.5f;//1;
		if (PRy>umbral) PRy=0.5f;//1;
		
		//PRx=1-PRx;
		//PRy=1-PRy;
		
		//nqPRx=PRx;
		//nqPRy=PRy;
	}

	
	
	//*******************************************
	public void quantizeNaturalPR()
	{
		//quantization based on geometrical progression of thresholds with ratio=2
		float level0=0.2f;
		float level1=0.3f;
		float level2=0.4f;

		if (PRx<level0) PRx=0.125f;//level0/2f;
		else if (PRx<level1) PRx=0.25f;
		else if (PRx<level2) PRx=0.35f;
		else PRx=1f;//(1f+level2)/2f;

		if (PRy<level0) PRy=0.125f;//level0/2f;
		else if (PRy<level1) PRy=0.25f;
		else if (PRy<level2) PRy=0.35f;
		else PRy=1f;//(1f+level2)/2f;

	}
	//****************************************************
	//******************************************************************	
		public void computePRmetrics_experimental()
		{
			//PR computation normalized to 0..1

			//Grid class creates a grid of Blocks and PRBlocks.
			//PRBlocks coordinates are in 0..width and 0..height
			int last_hop=0;
			int hop=0;
			boolean hop_sign=true;
			boolean last_hop_sign=true;
			PRx=0;
			PRy=0;

			//float tune=1.0f;
			//PRx

			float Cx=0;
			float Cy=0; 


			for (int y=yini;y<=yfin;y++)
			{
				if (y>0)
				{
					last_hop=img.hops[0][(y-1)*img.width+xini]-4;
					//last_hop_sign=(last_hop>0);
					last_hop_sign=(last_hop>=0); //NUEVO 19/3/2015 
					//System.out.println(" signo:"+last_hop_sign+ "hop:"+last_hop);
					
				}
				//horizontal scanlines
				for (int x=xini;x<=xfin;x++)
				{
					
					hop=img.hops[0][y*img.width+x]-4;//[0] is lumminance (Y)
					// hop value: -4....0....+4
					if (hop==0) continue; //h0 no sign
					//hop_sign=(hop>0);
					hop_sign=(hop>=0);//NUEVO 19/3/2015 
					//if (hop>0) hop_sign=true;
					//else hop_sign=false;
					
					if (hop_sign!=last_hop_sign || hop==4 || hop==-4) {
					//if (hop_sign!=last_hop_sign && last_hop!=0) {

					//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop==4 || hop==-4) {//NUEVO 19/3/2015 
					
					//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop>=3 || hop<=-3) {//NUEVO 19/3/2015 
							
						int weight=hop;
						if (weight<0)weight=-weight;
						//int weight=hop;
						//if (hop%2!=0) weight--; // possitive and negative must weigh the same [0..2..4..6]
						//weight=8-hop; //from 2 to 8
						//if (weight>7 ) tune=1;
						//else tune=1;
						//antes era 0 2 4 6, osea...max 6
						//int w=0;
						//if (weight!=0) w=4;
						//PRx+=w;//weight;
						PRx+=weight;
						//System.out.println ("hop:"+hop+"  lasth:"+last_hop+ "x:"+x);
						Cx++;

					}
					//System.out.println("scan");
					last_hop=hop;
					last_hop_sign=hop_sign;
				}
				//System.out.println("scan");
			}

			hop=0;
			hop_sign=true;
			last_hop=0;
			last_hop_sign=true;

			//System.out.println(" processing vscanlines  img.width"+img.width+"   xini:"+xini+"   xfin:"+xfin+"  yini:"+yini+"    yfin:"+yfin);
			for (int x=xini;x<=xfin;x++)
			{
				//System.out.println("x:"+x);
				if (x>0)
				{
				    //left pixel
					last_hop=img.hops[0][(yini)*img.width+x-1]-4;
					//last_hop_sign=(last_hop>0);
					last_hop_sign=(last_hop>=0);//NUEVO 19/3/2015 
				}
				//vertical scanlines
				for (int y=yini;y<=yfin;y++)
				{

					hop=img.hops[0][y*img.width+x]-4;//[0] is lumminance (Y)
					if (hop==0) continue; //h0 no sign
					//hop_sign=(hop>0);
					hop_sign=(hop>=0);//NUEVO 19/3/2015 
					//if (hop%2!=last_hop%2 && last_hop!=8) {
					
					if (hop_sign!=last_hop_sign || hop==4 || hop==-4) {
					
					//if (hop_sign!=last_hop_sign && last_hop!=0) {
					
					//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop==4 || hop==-4){
					//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop>=3 || hop<=-3){
						//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop!=4){
								
						
						int weight=hop;
						if (weight<0)weight=-weight;
						//if (hop%2!=0) weight--; // possitive and negative must weigh the same [0..2..4..6]
						//weight=8-hop; //from 2 to 8
						//int w=0;
						//if (weight!=0) w=4;
						//PRy+=w;//weight;
						PRy+=weight;
						//System.out.println ("hop:"+hop+"  lasth:"+last_hop);
						Cy++;
					}
					last_hop=hop;
					last_hop_sign=hop_sign;
				}

			}

			//System.out.println("numerador:"+PRx+" denominador:"+Cx*4);
			if (PRx>0) PRx=PRx/(Cx*4);
			if (PRy>0) PRy=PRy/(Cy*4);

			//if (PRx>0) PRx=PRx/((xfin-xini)*(yfin-yini)*4);
			//if (PRy>0) PRy=PRy/((xfin-xini)*(yfin-yini)*4);
			
			//if (PRx<0.01f || PRy<0.01f) System.out.println("NOTHING");
			if (PRx>1 || PRy>1) {
				System.out.println(" failure. imposible PR value > 1");
				System.exit(0);
			}

			//if (PRx==0.0) {
				//System.out.println("ha salido cero");
				//System.exit(0);
			//}
			//else System.out.println("no es cero");
			
	    	//System.out.println("PRx:"+PRx+"  , PRy:"+PRy+"   xini:"+xini+"  xfin:"+xfin+"  yini:"+yini+"  yfin:"+yfin);
			//System.out.println((int)(PRx*100));
			//System.out.println((int)(PRy*100));
			float umbral=10.5f;//0.5f; //LATER
			if (PRx>umbral) PRx=0.5f;//1;
			if (PRy>umbral) PRy=0.5f;//1;
			
			//PRx=1-PRx;
			//PRy=1-PRy;
			
			//nqPRx=PRx;
			//nqPRy=PRy;
		}
		//*******************************************
	
		//**********************************************************************************	
		public void computePRmetrics4SPS(int stepy, int stepx)
		{
			//PR computation normalized to 0..1

			//Grid class creates a grid of Blocks and PRBlocks.
			//PRBlocks coordinates are in 0..width and 0..height
			int last_hop=0;
			int hop=0;
			boolean hop_sign=true;
			boolean last_hop_sign=true;
			PRx=0;
			PRy=0;

			
			float Cx=0;
			float Cy=0; 

			
			
			//      PRx COMPUTATION
			// ----------------------------------------
			// en vertical saltamos de stepy en stepy
			for (int y=yini;y<=yfin;y+=stepy)
			{
				if (y>0)
				{
					
					//last_hop=img.hops[0][(y-1)*img.width+xini]-4;
					last_hop=0; // como hay una distancia stepy no puedo considerar el de la linea superior
					last_hop_sign=(last_hop>=0); //NUEVO 19/3/2015 
					
					
				}
				//horizontal scanline
				//--------------------
				for (int x=xini;x<=xfin;x++)
				{
					
					hop=img.hops[0][y*img.width+x]-4;//[0] is lumminance (Y)
					// hop value: -4....0....+4
					if (hop==0) continue; //h0 no sign
					//hop_sign=(hop>0);
					hop_sign=(hop>=0);//NUEVO 19/3/2015 
					//if (hop>0) hop_sign=true;
					//else hop_sign=false;
					//if (hop_sign!=last_hop_sign && last_hop!=0) {

					if ((hop_sign!=last_hop_sign && last_hop!=0) || hop==4 || hop==-4) {//NUEVO 19/3/2015 
					
					//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop>=3 || hop<=-3) {//NUEVO 19/3/2015 
							
						int weight=hop;
						if (weight<0)weight=-weight;
						//int weight=hop;
						//if (hop%2!=0) weight--; // possitive and negative must weigh the same [0..2..4..6]
						//weight=8-hop; //from 2 to 8
						//if (weight>7 ) tune=1;
						//else tune=1;
						//antes era 0 2 4 6, osea...max 6
						//int w=0;
						//if (weight!=0) w=4;
						//PRx+=w;//weight;
						PRx+=weight;
						//System.out.println ("hop:"+hop+"  lasth:"+last_hop+ "x:"+x);
						Cx++;

					}
					//System.out.println("scan");
					last_hop=hop;
					last_hop_sign=hop_sign;
				}
				//System.out.println("scan");
			}

			hop=0;
			hop_sign=true;
			last_hop=0;
			last_hop_sign=true;

			//System.out.println(" processing vscanlines  img.width"+img.width+"   xini:"+xini+"   xfin:"+xfin+"  yini:"+yini+"    yfin:"+yfin);
			//for (int x=xini;x<=xfin;x++)
			
			//         PRy COMPUTATION
			// ----------------------------------
			
			
			for (int x=xini;x<=xfin;x+=stepx)
			{
				//System.out.println("x:"+x);
				if (x>0)
				{
				    //left pixel
					//last_hop=img.hops[0][(yini)*img.width+x-1]-4;
					
					last_hop=0;
					last_hop_sign=(last_hop>=0);//NUEVO 19/3/2015 
				}
				//vertical scanlines
				for (int y=yini;y<=yfin;y++)
				{

					hop=img.hops[0][y*img.width+x]-4;//[0] is lumminance (Y)
					if (hop==0) continue; //h0 no sign
					//hop_sign=(hop>0);
					hop_sign=(hop>=0);//NUEVO 19/3/2015 
					//if (hop%2!=last_hop%2 && last_hop!=8) {
					//if (hop_sign!=last_hop_sign && last_hop!=0) {
					
					if ((hop_sign!=last_hop_sign && last_hop!=0) || hop==4 || hop==-4){
					//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop>=3 || hop<=-3){
						//if ((hop_sign!=last_hop_sign && last_hop!=0) || hop!=4){
								
						
						int weight=hop;
						if (weight<0)weight=-weight;
						//if (hop%2!=0) weight--; // possitive and negative must weigh the same [0..2..4..6]
						//weight=8-hop; //from 2 to 8
						//int w=0;
						//if (weight!=0) w=4;
						//PRy+=w;//weight;
						PRy+=weight;
						//System.out.println ("hop:"+hop+"  lasth:"+last_hop);
						Cy++;
					}
					last_hop=hop;
					last_hop_sign=hop_sign;
				}

			}

			//System.out.println("numerador:"+PRx+" denominador:"+Cx*4);
			if (PRx>0) PRx=PRx/(Cx*4);
			if (PRy>0) PRy=PRy/(Cy*4);

			//if (PRx>0) PRx=PRx/((xfin-xini)*(yfin-yini)*4);
			//if (PRy>0) PRy=PRy/((xfin-xini)*(yfin-yini)*4);
			
			//if (PRx<0.01f || PRy<0.01f) System.out.println("NOTHING");
			if (PRx>1 || PRy>1) {
				System.out.println(" failure. imposible PR value > 1");
				System.exit(0);
			}

			//if (PRx==0.0) {
				//System.out.println("ha salido cero");
				//System.exit(0);
			//}
			//else System.out.println("no es cero");
			
	    	//System.out.println("PRx:"+PRx+"  , PRy:"+PRy+"   xini:"+xini+"  xfin:"+xfin+"  yini:"+yini+"  yfin:"+yfin);
			//System.out.println((int)(PRx*100));
			//System.out.println((int)(PRy*100));
			float umbral=10.5f;//0.5f; //LATER
			if (PRx>umbral) PRx=0.5f;//1;
			if (PRy>umbral) PRy=0.5f;//1;
			
			//PRx=1-PRx;
			//PRy=1-PRy;
			
			//nqPRx=PRx;
			//nqPRy=PRy;
		}

		
		
		//*******************************************
}

