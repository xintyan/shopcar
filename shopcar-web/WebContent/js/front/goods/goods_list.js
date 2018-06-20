$(function() {//ajax 实现 post 
	//<button id="addCar-${goods.gid}" class="btn btn-primary btn-sm">
	//意思是button下的 id 同伙 -来才分
	$("button[id*=addCar-]").each(function(){
		var gid = $(this).attr("id").split("-")[1] ;
		//调用这个的属性id  通过split进行拆分
		$(this).on("click",function(){//为button绑定单击时间 
			// console.log("*** gid = " + gid) ;
			$.post("pages/front/center/shopcar/ShopcarActionFront!add.action",{"gid":gid},
					//post请求方式      下面是请求action路径                                                                                                                                                                             
					//{"gid":gid}  键值对   key value  
	
					function(data){//传参把deta的值传进来
				operateAlert(data.trim()=="true","购物车添加成功！","购物车添加失败！") ;
				//data是js调用action返回的内容，true的话显示购物车添加成功！

			},"text") ;
		}) ;
	}) ; 
}) 