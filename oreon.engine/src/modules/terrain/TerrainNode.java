package modules.terrain;

import core.buffers.PatchVBO;
import core.configs.Default;
import core.kernel.Camera;
import core.kernel.RenderContext;
import core.math.Vec2f;
import core.math.Vec3f;
import core.renderer.RenderInfo;
import core.renderer.Renderer;
import core.scene.GameObject;
import core.scene.Node;
import core.utils.Constants;

public class TerrainNode extends GameObject{

	private boolean isleaf;
	private TerrainConfig config;
	private int lod;
	private Vec2f location;
	private Vec3f worldPos;
	private Vec2f index;
	private float gap;
	private PatchVBO buffer;
	
	
	public TerrainNode(PatchVBO patchVBO, TerrainConfig terrConfig, Vec2f location, int lod, Vec2f index){
		
		this.buffer = patchVBO;
		this.isleaf = true;
		this.index = index;
		this.lod = lod;
		this.location = location;
		this.config = terrConfig;
		this.gap = 1f/(TerrainQuadtree.getRootNodes() * (float)(Math.pow(2, lod)));

		Vec3f localScaling = new Vec3f(gap,0,gap);
		Vec3f localTranslation = new Vec3f(location.getX(),0,location.getY());
		
		getLocalTransform().setScaling(localScaling);
		getLocalTransform().setTranslation(localTranslation);
		
		getWorldTransform().setScaling(terrConfig.getScaleXZ(), terrConfig.getScaleY(), terrConfig.getScaleXZ());
		getWorldTransform().getTranslation().setX(-terrConfig.getScaleXZ()/2f);
		getWorldTransform().getTranslation().setZ(-terrConfig.getScaleXZ()/2f);
		getWorldTransform().getTranslation().setY(0);
		
		Renderer mainRenderer = new Renderer();
		mainRenderer.setVbo(buffer);
		mainRenderer.setRenderInfo(new RenderInfo(new Default(),TerrainShader.getInstance()));
		
		Renderer wireframeRenderer = new Renderer();
		wireframeRenderer.setVbo(buffer);
		wireframeRenderer.setRenderInfo(new RenderInfo(new Default(),TerrainWireframeShader.getInstance()));
		
		addComponent(Constants.RENDERER_COMPONENT, mainRenderer);
		addComponent(Constants.WIREFRAME_RENDERER_COMPONENT, wireframeRenderer);
		
		computeWorldPos();
		updateQuadtree();
	}
	
	public void render()
	{
		if (isleaf)
		{	
			if (RenderContext.getInstance().isWireframe()){
				getComponents().get(Constants.WIREFRAME_RENDERER_COMPONENT).render();
			}
			else{
				getComponents().get(Constants.RENDERER_COMPONENT).render();
			}
		}
		
		for (Node node : getChildren()){
			node.render();
		}
	}
	
	public void updateQuadtree(){
		
		updateChildNodes();
		
		for (Node node : getChildren()){
			((TerrainNode) node).updateQuadtree();
		}
	}
	
	private void updateChildNodes(){
		
		float distance = (Camera.getInstance().getPosition().sub(worldPos)).length();
		
		if (distance < config.getLod_range()[lod]){
			add4ChildNodes(lod+1);
		}
		else if(distance >= config.getLod_range()[lod]){
			removeChildNodes();
		}
	}
	
	private void add4ChildNodes(int lod){
		
		if (isleaf){
			isleaf = false;
		}
		if(getChildren().size() == 0){
			for (int i=0; i<2; i++){
				for (int j=0; j<2; j++){
					addChild(new TerrainNode(buffer, config, location.add(new Vec2f(i*gap/2f,j*gap/2f)), lod, new Vec2f(i,j)));
				}
			}
		}
	}
	
	private void removeChildNodes(){
		
		if (!isleaf){
			isleaf = true;
		}
		if(getChildren().size() != 0){			
			getChildren().clear();
		}
}
	
	public void computeWorldPos(){
		
		Vec2f loc = location.add(gap/2f).mul(config.getScaleXZ()).sub(config.getScaleXZ()/2f);
		float height = getTerrainHeight(loc.getX(), loc.getY());
		this.worldPos = new Vec3f(loc.getX(),height,loc.getY());
	}
	
	public float getTerrainHeight(float x, float z){
		
		float h = 0;
		
		Vec2f pos = new Vec2f();
		pos.setX(x);
		pos.setY(z);
		pos = pos.add(config.getScaleXZ()/2f);
		pos = pos.div(config.getScaleXZ());
		Vec2f floor = new Vec2f((int) Math.floor(pos.getX()), (int) Math.floor(pos.getY()));
		pos = pos.sub(floor);
		pos = pos.mul(config.getHeightmap().getWidth());
		int x0 = (int) Math.floor(pos.getX());
		int x1 = x0 + 1;
		int z0 = (int) Math.floor(pos.getY());
		int z1 = z0 + 1;
		
		float h0 =  config.getHeightmapDataBuffer().get(config.getHeightmap().getWidth() * z0 + x0);
		float h1 =  config.getHeightmapDataBuffer().get(config.getHeightmap().getWidth() * z0 + x1);
		float h2 =  config.getHeightmapDataBuffer().get(config.getHeightmap().getWidth() * z1 + x0);
		float h3 =  config.getHeightmapDataBuffer().get(config.getHeightmap().getWidth() * z1 + x1);
		
		float percentU = pos.getX() - x0;
        float percentV = pos.getY() - z0;
        
        float dU, dV;
        if (percentU > percentV)
        {   // bottom triangle
            dU = h1 - h0;
            dV = h3 - h1;
        }
        else
        {   // top triangle
            dU = h3 - h2;
            dV = h2 - h0;
        }
        
        h = h0 + (dU * percentU) + (dV * percentV );
        h *= config.getScaleY();
		
		return h;
	}

	public Vec3f getWorldPos() {
		return worldPos;
	}

	public void setWorldPos(Vec3f worldPos) {
		this.worldPos = worldPos;
	}

	public Vec2f getLocation() {
		return location;
	}

	public void setLocation(Vec2f location) {
		this.location = location;
	}

	public TerrainConfig getConfig() {
		return config;
	}

	public void setConfig(TerrainConfig terrConfig) {
		this.config = terrConfig;
	}
	
	public int getLod() {
		return lod;
	}

	public void setLod(int lod) {
		this.lod = lod;
	}

	public Vec2f getIndex() {
		return index;
	}

	public void setIndex(Vec2f index) {
		this.index = index;
	}
	
	public float getGap(){
		return this.gap;
	}
	
	public TerrainNode getQuadtreeParent() {
		return (TerrainNode) getParent();
	}
}
