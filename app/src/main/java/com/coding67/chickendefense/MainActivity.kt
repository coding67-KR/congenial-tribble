package com.coding67.chickendefense

import android.app.Activity
import android.media.MediaPlayer
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.graphics.*
import java.util.Random
import kotlin.math.hypot

class MainActivity : Activity() {
    private lateinit var game: ChickenGameView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(1024, 1024)
        game = ChickenGameView()
        setContentView(game)
    }
    inner class ChickenGameView : View(this@MainActivity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rnd = Random()
        private val chickens = mutableListOf<UnitData>()
        private val enemies = mutableListOf<EnemyData>()
        private var eggs = 180
        private var hp = 100f
        private var wave = 1
        private var running = false
        private var waveSpawned = 0
        private var waveTotal = 0
        private var spawnTimer = 0f
        private var eggBomb = true
        private var selected: String? = null
        private var frame = 0
        private var lastTime = System.nanoTime()
        private var screen = 0
        private var music: MediaPlayer? = null
        private val cache = HashMap<String, Bitmap>()

        private data class UnitData(val type:String, var x:Float, var y:Float, var cooldown:Float=0f)
        private data class EnemyData(val kind:String, var x:Float, var y:Float, var hp:Float, val maxHp:Float, val speed:Float)

        init { isFocusable = true; music = MediaPlayer.create(this@MainActivity, assets.openFd("bgm.wav")); music?.isLooping=true; music?.start() }
        private fun bmp(name:String): Bitmap = cache.getOrPut(name) { BitmapFactory.decodeStream(assets.open(name)) }
        override fun onDraw(c: Canvas) { super.onDraw(c); if (screen == 0) drawMenu(c) else drawGame(c) }
        private fun drawMenu(c: Canvas) {
            c.drawBitmap(bmp("start_menu.png"), null, Rect(0,0,width,height), paint)
            paint.color = Color.argb(235,35,29,23); c.drawRoundRect(width*.10f,height*.08f,width*.90f,height*.30f,28f,28f,paint)
            paint.color = Color.WHITE; paint.textAlign=Paint.Align.CENTER; paint.typeface=Typeface.DEFAULT_BOLD
            paint.textSize = height*.075f; c.drawText("닭 디펜스",width/2f,height*.18f,paint)
            paint.textSize = height*.028f; paint.typeface=Typeface.DEFAULT; c.drawText("농장을 지키는 닭들의 반격",width/2f,height*.235f,paint)
            paint.color = Color.rgb(229,155,66); c.drawRoundRect(width*.34f,height*.70f,width*.66f,height*.84f,24f,24f,paint)
            paint.color=Color.WHITE; paint.typeface=Typeface.DEFAULT_BOLD; paint.textSize=height*.035f; c.drawText("게임 시작",width/2f,height*.785f,paint)
        }
        private fun drawGame(c: Canvas) {
            paint.shader = LinearGradient(0f,0f,0f,height,Color.rgb(190,220,157),Color.rgb(140,190,116),Shader.TileMode.CLAMP); c.drawRect(0f,0f,width.toFloat(),height.toFloat(),paint); paint.shader=null
            paint.color=Color.rgb(199,175,128); c.drawRoundRect(width*.06f,height*.42f,width*.94f,height*.60f,50f,50f,paint)
            paint.color=Color.rgb(223,201,160); paint.strokeWidth=6f; for(i in 0..10)c.drawLine(width*.08f+i*width*.084f,height*.51f,width*.11f+i*width*.084f,height*.51f,paint)
            val hx=width*.50f; val hy=height*.51f; paint.color=Color.rgb(229,194,125); c.drawRoundRect(hx-74,hy-55,hx+74,hy+55,22f,22f,paint); paint.color=Color.rgb(182,94,77); val roof=Path(); roof.moveTo(hx-88,hy-53); roof.lineTo(hx,hy-115); roof.lineTo(hx+88,hy-53); roof.close(); c.drawPath(roof,paint)
            drawHud(c); chickens.forEach { drawUnit(c,it) }; enemies.forEach { drawEnemy(c,it) }
            if (!running && waveSpawned==0) drawHint(c)
        }
        private fun drawHud(c:Canvas){
            paint.color=Color.argb(235,255,250,240); c.drawRoundRect(20f,18f,270f,82f,20f,20f,paint); c.drawRoundRect(width-490f,18f,width-20f,82f,20f,20f,paint)
            paint.textAlign=Paint.Align.LEFT; paint.color=Color.rgb(60,50,40); paint.typeface=Typeface.DEFAULT_BOLD; paint.textSize=26f
            c.drawBitmap(bmp("ui_egg.png"),null,RectF(32f,30f,65f,63f),paint); c.drawText("$eggs",75f,57f,paint)
            c.drawBitmap(bmp("ui_heart.png"),null,RectF(135f,30f,168f,63f),paint); c.drawText("${hp.toInt()}",178f,57f,paint)
            c.drawBitmap(bmp("ui_wave.png"),null,RectF(width-475f,30f,width-442f,63f),paint); c.drawText("WAVE $wave",width-430f,57f,paint)
            val labels=listOf("토종닭" to "normal","병아리" to "fast","수탉" to "power","빙닭" to "ice","왕닭" to "king"); labels.forEachIndexed{ i,pair -> val x=20f+i*160; paint.color=Color.argb(240,255,250,240); c.drawRoundRect(x,height-105,x+145,height-20,18f,18f,paint); c.drawBitmap(bmp("chicken_${pair.second}_0.png"),null,RectF(x+8,height-98,x+62,height-44),paint); paint.color=Color.rgb(60,50,40);paint.textSize=17f;c.drawText(pair.first,x+68,height-68,paint);paint.textSize=14f;paint.color=Color.rgb(93,156,101);c.drawText(cost(pair.second).toString()+" 🥚",x+68,height-44,paint)}
            paint.color=Color.rgb(229,155,66); c.drawRoundRect(width-185f,height-105,width-25f,height-55,16f,16f,paint); paint.color=Color.WHITE;paint.textAlign=Paint.Align.CENTER;paint.textSize=18f;c.drawText("▶ 웨이브 시작",width-105f,height-72f,paint)
            paint.color=Color.rgb(93,156,101); c.drawRoundRect(width-185f,height-48,width-25f,height-20,14f,14f,paint); paint.color=Color.WHITE;paint.textSize=16f;c.drawText(if(eggBomb)"🥚 알폭탄" else "사용 완료",width-105f,height-29f,paint)
        }
        private fun drawUnit(c:Canvas,u:UnitData){ val name="chicken_${u.type}_${(frame/18)%2}.png"; c.drawBitmap(bmp(name),null,RectF(u.x-45,u.y-45,u.x+45,u.y+45),paint) }
        private fun drawEnemy(c:Canvas,e:EnemyData){ val b=bmp("enemy_${e.kind}.png"); c.drawBitmap(b,null,RectF(e.x-38,e.y-38,e.x+38,e.y+38),paint); paint.color=Color.argb(210,245,230,220);c.drawRect(e.x-32,e.y-50,e.x+32,e.y-43,paint);paint.color=Color.rgb(207,107,85);c.drawRect(e.x-32,e.y-50,e.x-32+64*(e.hp/e.maxHp),e.y-43,paint)}
        private fun drawHint(c:Canvas){ paint.color=Color.argb(220,255,250,240);c.drawRoundRect(width*.34f,height*.08f,width*.66f,height*.17f,18f,18f,paint);paint.color=Color.rgb(60,50,40);paint.textAlign=Paint.Align.CENTER;paint.textSize=20f;c.drawText("닭을 골라 필드에 배치하세요!",width/2f,height*.135f,paint) }
        private fun cost(t:String)=when(t){"normal"->40;"fast"->55;"power"->65;"ice"->90;else->150}
        override fun onTouchEvent(ev: MotionEvent): Boolean { if(ev.action!=MotionEvent.ACTION_UP)return true; val x=ev.x; val y=ev.y
            if(screen==0){screen=1;return true}
            if(y>height-120){ if(x>width-195){if(y>height-60){if(eggBomb && running){eggBomb=false;enemies.forEach{it.hp-=60f}}}else if(!running){startWave()};return true}}
            if(y>height-130 && x<width-200){val i=(x/160f).toInt().coerceIn(0,4);selected=listOf("normal","fast","power","ice","king")[i];return true}
            if(selected!=null && y>height*.18f && y<height*.82f){val t=selected!!;if(eggs>=cost(t)){eggs-=cost(t);chickens.add(UnitData(t,x,y));selected=null}}
            return true }
        private fun startWave(){running=true;waveSpawned=0;waveTotal=8+wave*3;spawnTimer=0f;eggBomb=true}
        private fun update(){if(!running)return;val now=System.nanoTime(); val dt=((now-lastTime)/1e9).toFloat().coerceAtMost(.05f);lastTime=now;spawnTimer-=dt;if(waveSpawned<waveTotal&&spawnTimer<=0f){spawnTimer=.55f;spawnEnemy();waveSpawned++}
            chickens.forEach{u->u.cooldown-=dt;if(u.cooldown<=0f){val target=enemies.minByOrNull{hypot((it.x-u.x).toDouble(),(it.y-u.y).toDouble())};if(target!=null&&hypot((target.x-u.x).toDouble(),(target.y-u.y).toDouble())<190){target.hp-=when(u.type){"fast"->7f;"power"->26f;"ice"->10f;"king"->18f;else->12f};if(u.type=="king")enemies.filter{hypot((it.x-target.x).toDouble(),(it.y-target.y).toDouble())<55}.forEach{it.hp-=8f};u.cooldown=when(u.type){"fast"->.35f;"power"->1.15f;"ice"->.8f;"king"->.6f;else->.75f}}}}
            enemies.removeAll{e->if(e.hp<=0){eggs+=6;true}else false}; enemies.forEach{e->val dx=width/2f-e.x;val dy=height*.51f-e.y;val d=hypot(dx.toDouble(),dy.toDouble()).toFloat();if(d<70){hp-=dt*e.speed*.06f}else if(d>1){e.x+=dx/d*e.speed*dt;e.y+=dy/d*e.speed*dt}}
            if(hp<=0){running=false;hp=0f} else if(waveSpawned>=waveTotal&&enemies.isEmpty()){running=false;eggs+=35+wave*5;wave++}
        }
        private fun spawnEnemy(){val k=listOf("rat","fox","wolf","bear","eagle")[rnd.nextInt(5)];val side=rnd.nextInt(4);val x=if(side<2)rnd.nextFloat()*width else if(side==2)-30f else width+30f;val y=if(side>=2)rnd.nextFloat()*height*.7f+height*.15f else if(side==0)-30f else height+30f;val hp0=(22+wave*5)*when(k){"fox"->1.7f;"wolf"->2.6f;"bear"->5f;"eagle"->2.2f;else->1f};enemies.add(EnemyData(k,x,y,hp0,hp0,40f*when(k){"rat"->1.4f;"fox"->1.1f;"wolf"->.85f;"bear"->.55f;else->1.25f}))}
        private val ticker = object : Runnable { override fun run() { frame++; update(); invalidate(); postDelayed(this, 16L) } }
        override fun onAttachedToWindow() { super.onAttachedToWindow(); post(ticker) }
        override fun onDetachedFromWindow() { removeCallbacks(ticker); music?.release(); music=null; super.onDetachedFromWindow() }
    }
}
