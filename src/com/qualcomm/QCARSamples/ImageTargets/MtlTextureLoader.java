package com.qualcomm.QCARSamples.ImageTargets;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import android.content.res.AssetManager;

import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;

public class MtlTextureLoader {

	private static List<String> readFileNames(InputStream file){
		List<String> fileNames = new ArrayList<String>();
		Scanner input;
		input = new Scanner(file);
		while (input.hasNext()) {
			String s = input.nextLine();
			if (s.startsWith("map_Kd")) {
				String filename = s.substring(6, s.length());
				filename = filename.trim();
				if (filename.startsWith("-s")) {

					filename = filename.substring(filename.lastIndexOf(' ') + 1, filename.length());
				}

				//System.out.println(filename);
				if(!fileNames.contains(filename)){
					fileNames.add(filename);
				}
			}
		}
		input.close();
		return fileNames;
	}
	
	public static boolean loadTexturesFromAssets(String mtlFile, AssetManager mngr){
		return loadTexturesFromAssets(mtlFile, ".", mngr);
	}

	public static boolean loadTexturesFromAssets(String mtlFile, String path, AssetManager mngr){
		try {
			List<String> fileNames = readFileNames(mngr.open(mtlFile));
			for(int i =0; i<fileNames.size(); i++){
				Texture t = new Texture(mngr.open(fileNames.get(i)));
				t.compress();
				if (!TextureManager.getInstance().containsTexture(fileNames.get(i)))
					TextureManager.getInstance().addTexture(fileNames.get(i), t);
			}
			return true;

		} catch (FileNotFoundException e) {			
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}
