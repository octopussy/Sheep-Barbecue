package de.swagner.ld25.com.badlogic.gdx.graphics.g3d.model.skeleton;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;

public class SkeletonModelGpuSkinned extends SkeletonModel {
	public final static String BoneIndexAttribue = "a_boneIndex";
	public final static String BoneWeightAttribue = "a_boneWeight";

	public SkeletonModelGpuSkinned(Skeleton skeleton, SkeletonSubMesh[] subMeshes) {
		super(skeleton,subMeshes);
	}
	
	// Factory Method to create a SkeletonModelGpuSkinned from a SkeletonModel
	// This will destroy the skeletonModel passed in.
	public static SkeletonModel CreateFromSkeletonModel(SkeletonModel skeletonModel){
		if (Gdx.gl20 == null){
			return skeletonModel;
		}
		SkeletonModelGpuSkinned model = new SkeletonModelGpuSkinned(skeletonModel.skeleton, skeletonModel.subMeshes);
		model.setupGpuSkin();
		return model;
	}
	
	public void setupGpuSkin(){
		for (int i = 0; i < subMeshes.length; i++) {
			setupGpuSkin(subMeshes[i]);
		}
	}
	
	private void setupGpuSkin(SkeletonSubMesh subMesh){
		VertexAttributes oldAttributes =  subMesh.mesh.getVertexAttributes();
		final int oldAttributeCount = oldAttributes.size();
		VertexAttribute[] attributeArray = new VertexAttribute[oldAttributeCount+2];
		for(int i=0;i<oldAttributeCount;i++){
			attributeArray[i] = oldAttributes.get(i);
		}
		final int boneIndex = oldAttributeCount;
		final int weightIndex = oldAttributeCount+1;
		attributeArray[boneIndex] = new VertexAttribute(Usage.Generic, 4, BoneIndexAttribue);
		attributeArray[weightIndex] = new VertexAttribute(Usage.Generic, 4, BoneWeightAttribue);
		VertexAttributes newAttributes = new VertexAttributes(attributeArray);
		
		//TODO: not sure if I want to generate a new mesh. But VertexAttributes was final inside mesh
		Mesh newMesh = new Mesh(true, subMesh.mesh.getMaxVertices(), subMesh.mesh.getMaxIndices(), newAttributes);
		
		final int stride = subMesh.mesh.getVertexSize() / 4;
		final int newStride = newMesh.getVertexSize() / 4;
		final int numVertices = subMesh.mesh.getNumVertices();
		int idx = 0;
		int newIdx = 0;
		int bidx = -1;
		int widx = -1;
		for(int i=0;i<newAttributes.size();i++)
		{
			VertexAttribute a = newAttributes.get(i);
			if(a.alias.equals(BoneIndexAttribue)){
				bidx = a.offset/4;
			} else if(a.alias.equals(BoneWeightAttribue)){
				widx = a.offset/4;
			}
		}
		
		if(bidx <0 || widx < 0){
			throw new IllegalArgumentException("Need Shader with bone index and bone wieght vectors to use GPU skinning");
		}
		
		final float[] vertices = subMesh.vertices;
		final float[] skinnedVertices = new float[newStride * numVertices];

		for (int i = 0; i < numVertices; i++, idx += stride, newIdx += newStride, bidx += newStride, widx += newStride) {
			final int[] boneIndices = subMesh.boneAssignments[i];
			final float[] boneWeights = subMesh.boneWeights[i];
			
			System.arraycopy(vertices, idx, skinnedVertices, newIdx, stride);
			
			skinnedVertices[bidx] = boneIndices.length>0?boneIndices[0]:0;
			skinnedVertices[bidx + 1] = boneIndices.length>1?boneIndices[1]:0;
			skinnedVertices[bidx + 2] = boneIndices.length>2?boneIndices[2]:0;
			skinnedVertices[bidx + 3] = boneIndices.length>3?boneIndices[3]:0;
			
			skinnedVertices[widx] = boneWeights.length>0?boneWeights[0]:0;
			skinnedVertices[widx + 1] = boneWeights.length>1?boneWeights[1]:0;
			skinnedVertices[widx + 2] = boneWeights.length>2?boneWeights[2]:0;
			skinnedVertices[widx + 3] = boneWeights.length>3?boneWeights[3]:0;
		}

		newMesh.setVertices(skinnedVertices);
		newMesh.setIndices(subMesh.indices);
		subMesh.mesh.dispose();
		subMesh.mesh = newMesh;
		subMesh.skinnedVertices = null;
		subMesh.vertices = skinnedVertices;
	}
	
	@Override
	public void setAnimation(String animation, float time, boolean loop) {
		skeleton.setAnimation(animation, time);
	}
}
