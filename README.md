<p align="center"><img src="./images/ai_builder_logo.png" alt="Logo" width="200"></p>
<h1 align="center">
	Mine Arena
</h1>
<p>A Forge mod built for Minecraft Java 1.21.1 that allows you to build anything in Minecraft using the power of AI. The AI uses a text prompt to generate a 3D model of your idea, which you can then place into your Minecraft world. Alternatively, you can import your own 3D model files.</p>
<p>Models are automatically converted into a voxelized, blocky representations complete with textures and color matching. Players can scale, rotate, and position models in-game using intuitive keyboard and mouse controls, or through commands for more fine-tuned adjustments. </p>
<p>This mod supports .obj and .stl file formats, and provides a real-time preview of the model as it is transformed and placed within the world.</p>

## Demo

Below are screenshots demonstrating AI model generation and placement in Minecraft.

### Generation in Progress
![Model Generation](./images/generation_in_progress.png)

### Generation Result
![Generated Model](./images/generated_result.png)

## The builds below were all generated using this mod:

### Steampunk Airship
![Steampunk Airship](./images/steampunk_pirate_ship.png)

### Greek Statue
![Greek Statue](./images/greek_statue.png)

### Bee
![Bee](./images/bee.png)

### Iron Man Helmet
![Iron Man Helmet](./images/iron_man_helmet.png)

<h2>How to Install/Compile</h2>
<p>To install this mod, simply copy the <code>mine_arena-1.0.0.jar</code> file into the mods folder of your Minecraft directory.
</p>
<p>If you would like to compile the .jar file yourself, navigate to the root directory of the project and run <code>gradlew build</code> from the command line.</p>

<h2>How to Use</h2>
<h3>Loading a Model</h3>
<p>Upon launching the game, a <code>models</code> folder will be created in your Minecraft directory. Place your 3D models and textures in this folder.</p>
<p>To load a model in-game:</p>
<ol>
    <li>Open your world.</li>
    <li>Run the command: <code>/model load &lt;filename&gt;</code>. The mod will list all valid filenames in your <code>models</code> folder.</li>
</ol>

<h3>Generating a Model</h3>
<ol>
    <li>To generate models using AI, an API key from Meshy is required. This involves creating an account on their website, and purchasing a subscription to get access to their API. This can be done here: <a href="https://www.meshy.ai/settings/api">Meshy API Page</a></li>
    <li>After acquiring an API key, go into the config folder of your Minecraft directory, and open "mine_arena-common.toml". Paste your API key here.</li>
    <li>Once you load up your world, run the command <code>model generate &lt;prompt&gt;</code> to generate a model.
</ol>

<h3>Manipulating Models with the Model Hammer</h3>
<p>The <strong>Model Hammer</strong> is your primary tool for interacting with models in the game. You can perform various transformations (scaling, rotating, and moving) with keyboard and mouse controls while holding the hammer.</p>

<ul>
	<li><strong>Placing the Model</strong>: Right-click with the hammer to place the model where you want it.</li>
	<li><strong>Transformations</strong>: Press the following keys to change transformation modes:
		<ul>
			<li><strong>S</strong>: Scale</li>
			<li><strong>R</strong>: Rotate</li>
			<li><strong>M</strong>: Move</li>
		</ul>
		Then, scroll your mouse wheel to apply the transformation.
	</li>
	<li><strong>Axis Selection</strong>: Choose which axis to manipulate by pressing the corresponding keys:
		<ul>
			<li><strong>X</strong>: X-axis</li>
			<li><strong>Y</strong>: Y-axis</li>
			<li><strong>Z</strong>: Z-axis</li>
		</ul>
	</li>
	<li><strong>Placement Controls</strong>:
		<ul>
			<li>Press <strong>P</strong> to place the model.</li>
			<li>Press <strong>U</strong> to unplace the model.</li>
		</ul>
		These key bindings are configurable in the settings menu.
	</li>
</ul>

<h3>Manipulating Models via Commands</h3>
<p>You can also manipulate models using in-game commands. The <code>/model</code> command offers several subcommands for loading, transforming, and adjusting models.</p>

<h4><code>/model load &lt;filename&gt;</code></h4>
<p>Loads a model from the <code>models</code> folder. Supported formats: <code>.stl</code> and <code>.obj</code>.</p>

<h4><code>/model generate...</code></h4>
<ul>
	<li><code>/model generate &lt;prompt&gt; [&lt;model_name&gt;]</code>: Generates a 3D model from the given text prompt. If a model name is specified, will save the 3D model and textures to files using that name.</li>
	<li><code>/model generate cancel</code>: Cancels the current model generation if it exists. Useful for if generation is taking a long time or not responding.</li>
</ul>

<h4><code>/model place</code></h4>
<p>Places the currently loaded model at the player's position.</p>

<h4><code>/model undo</code></h4>
<p>Undoes the last model placement action.</p>

<h4><code>/model scale ...</code></h4>
<ul>
	<li><code>/model scale &lt;scale&gt;</code>: Scales the model uniformly in all dimensions by the given number.</li>
	<li><code>/model scale &lt;x-scale&gt; &lt;y-scale&gt; &lt;z-scale&gt;</code>: Scales the model independently along each axis.</li>
	<li><code>/model scale &lt;axis&gt; &lt;scale&gt;</code>: Scales the model along the specified axis (<code>x</code>, <code>y</code>, or <code>z</code>).</li>
</ul>

<h4><code>/model rotate ...</code></h4>
<ul>
	<li><code>/model rotate &lt;x-angle&gt; &lt;y-angle&gt; &lt;z-angle&gt;</code>: Rotates the model by the given angles around the X, Y, and Z axes.</li>
	<li><code>/model rotate &lt;axis&gt; &lt;angle&gt;</code>: Rotates the model around the specified axis by the given angle.</li>
</ul>

<h4><code>/model move &lt;distance&gt; [&lt;direction&gt;]</code></h4>
<p>Moves the model by the specified distance. If no direction is specified, the model will move in the direction the player is facing.</p>
<p>Valid directions: <code>up</code>, <code>down</code>, <code>north</code>, <code>east</code>, <code>south</code>, <code>west</code>.</p>

<h3>Changing Model Appearance</h3>
<p>There are three possible rendering previews that can be switched between by pressing <strong>V</strong>.
</p>
<ul>
    <li><strong>Bounding Box</strong> (Default) [Low Performance Impact]</li>
    <li><strong>Mesh/Wireframe</strong> [Medium Performance Impact]</li>
    <li><strong>BLocks Preview</strong> [High Performance Impact]</li>
</ul>