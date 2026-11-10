import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
const green=Color(0xFF087A35), pale=Color(0xFFF2F8F2);
void main()=>runApp(const AnnapurnaApp());
class Item{String name,cat,emoji;double price,stock;Item(this.name,this.cat,this.price,this.emoji,{this.stock=50});}
class MyApp extends AnnapurnaApp{const MyApp({super.key});}
class AnnapurnaApp extends StatefulWidget{const AnnapurnaApp({super.key});@override State<AnnapurnaApp> createState()=>_AppState();}
class _AppState extends State<AnnapurnaApp>{
 int tab=0;String cat='सर्व',search='';String ownerName='शेखर खरे',ownerPhone='';final cart=<String,double>{};final customPrices=<String,double>{};final orders=<String>[];final sales=<String>[];final purchases=<String>[];final wastages=<String>[];final khataEntries=<String>[];
 final items=<Item>[Item('टोमॅटो','भाजीपाला',40,'🍅'),Item('कांदा','भाजीपाला',30,'🧅'),Item('बटाटा','भाजीपाला',25,'🥔'),Item('भेंडी','भाजीपाला',50,'🥒'),Item('दुधी भोपळा','भाजीपाला',30,'🥬'),Item('पालक','भाजीपाला',20,'🥬'),Item('मेथी','भाजीपाला',15,'🌿'),Item('कोबी','भाजीपाला',25,'🥬'),Item('गाजर','भाजीपाला',40,'🥕'),Item('मिरची','भाजीपाला',60,'🌶️'),Item('सफरचंद','फळे',140,'🍎'),Item('केळी','फळे',50,'🍌'),Item('संत्रे','फळे',80,'🍊'),Item('द्राक्षे','फळे',100,'🍇'),Item('डाळिंब','फळे',120,'🍎'),Item('आंबा','फळे',100,'🥭'),Item('पेरू','फळे',60,'🍐'),Item('अननस','फळे',70,'🍍')];
 @override void initState(){super.initState();load();}
 Future<void> load()async{final p=await SharedPreferences.getInstance();Map<String,dynamic> read(String k)=>p.getString(k)==null?{}:jsonDecode(p.getString(k)!) as Map<String,dynamic>;cart.addAll(read('cart').map((k,v)=>MapEntry(k,(v as num).toDouble())));customPrices.addAll(read('prices').map((k,v)=>MapEntry(k,(v as num).toDouble())));final st=read('stock');for(final x in items){if(st[x.name] is num)x.stock=(st[x.name] as num).toDouble();}orders.addAll(p.getStringList('orders')??[]);sales.addAll(p.getStringList('sales')??[]);purchases.addAll(p.getStringList('purchases')??[]);wastages.addAll(p.getStringList('wastages')??[]);khataEntries.addAll(p.getStringList('khataEntries')??[]);ownerName=p.getString('ownerName')??'शेखर खरे';ownerPhone=p.getString('ownerPhone')??'';if(mounted)setState((){});}
 Future<void> save()async{final p=await SharedPreferences.getInstance();await p.setString('cart',jsonEncode(cart));await p.setString('prices',jsonEncode(customPrices));await p.setString('stock',jsonEncode({for(final x in items)x.name:x.stock}));await p.setStringList('orders',orders);await p.setStringList('sales',sales);await p.setStringList('purchases',purchases);await p.setStringList('wastages',wastages);await p.setStringList('khataEntries',khataEntries);await p.setString('ownerName',ownerName);await p.setString('ownerPhone',ownerPhone);}
 double price(Item p)=>customPrices[p.name]??p.price;
 double get total=>cart.entries.fold(0,(s,e)=>s+price(items.firstWhere((p)=>p.name==e.key))*e.value);
 void add(Item p,[double qty=.125]){setState(()=>cart[p.name]=(cart[p.name]??0)+qty);save();}
 void checkout(){
  if(cart.isEmpty)return;
  Navigator.push(context,MaterialPageRoute(builder:(_)=>DeliveryCheckout(
   amount:total,
   lines:cart.entries.map((e){final p=items.firstWhere((x)=>x.name==e.key);return '${p.name} • ${(e.value*1000).round()} ग्रॅम • ₹${(price(p)*e.value).toStringAsFixed(2)}';}).toList(),
   onConfirm:(name,phone,address,payment){
    final insufficient=cart.entries.where((e){final p=items.firstWhere((x)=>x.name==e.key);return e.value>p.stock;}).toList();
    if(insufficient.isNotEmpty){
     final e=insufficient.first;final p=items.firstWhere((x)=>x.name==e.key);
     ScaffoldMessenger.of(context).showSnackBar(SnackBar(content:Text('${p.name} साठी फक्त ${p.stock.toStringAsFixed(2)} किलो स्टॉक उपलब्ध आहे')));
     return false;
    }
    final amount=total;
    for(final e in cart.entries){final p=items.firstWhere((x)=>x.name==e.key);p.stock=(p.stock-e.value).clamp(0,999999).toDouble();}
    setState((){
     orders.insert(0,'ऑर्डर #${(1241+orders.length)} • ₹${amount.toStringAsFixed(2)} • ${name} • ${phone} • ${address} • ${payment} • स्थिती: नवीन');
     sales.insert(0,'विक्री • ₹${amount.toStringAsFixed(2)}');
     cart.clear();
    });
    save();
    return true;
   },
  )));
 }
 @override Widget build(BuildContext context)=>MaterialApp(debugShowCheckedModeBanner:false,title:'अन्नपूर्णा',theme:ThemeData(colorScheme:ColorScheme.fromSeed(seedColor:green),scaffoldBackgroundColor:pale,useMaterial3:true),home:Scaffold(appBar:AppBar(backgroundColor:green,foregroundColor:Colors.white,title:const Column(crossAxisAlignment:CrossAxisAlignment.start,children:[Text('अन्नपूर्णा',style:TextStyle(fontWeight:FontWeight.bold)),Text('Vegetable and Fruits',style:TextStyle(fontSize:11))]),actions:[IconButton(onPressed:()=>open('सूचना'),icon:const Icon(Icons.notifications_none))]),drawer:Drawer(child:ListView(children:[const DrawerHeader(decoration:BoxDecoration(color:green),child:Text('🧺 अन्नपूर्णा\\nदुकान व्यवस्थापन',style:TextStyle(color:Colors.white,fontSize:24))),...['डॅशबोर्ड','भाजीपाला व फळे','माझी पिशवी','माझ्या ऑर्डर्स','डिलिव्हरी पार्टनर','स्टॉक व्यवस्थापन','खरेदी नोंद','नासाडी नोंद','ग्राहक खाते','विक्री व रिपोर्ट','ताजा बाजार','अन्नपूर्णा AI','Photo Studio','सेटिंग्ज'].map((s)=>ListTile(title:Text(s),leading:const Icon(Icons.eco,color:green),onTap:(){Navigator.pop(context);open(s);} ))])),body:IndexedStack(index:tab,children:[home(),catalog(),cartPage(),profile()]),bottomNavigationBar:NavigationBar(selectedIndex:tab,onDestinationSelected:(v)=>setState(()=>tab=v),destinations:const [NavigationDestination(icon:Icon(Icons.home_outlined),label:'होम'),NavigationDestination(icon:Icon(Icons.grid_view),label:'भाजी / फळे'),NavigationDestination(icon:Icon(Icons.shopping_cart_outlined),label:'कार्ट'),NavigationDestination(icon:Icon(Icons.person_outline),label:'प्रोफाइल')]))); 
 Widget home()=>ListView(padding:const EdgeInsets.all(14),children:[Container(height:145,padding:const EdgeInsets.all(18),decoration:BoxDecoration(borderRadius:BorderRadius.circular(18),gradient:const LinearGradient(colors:[Color(0xFF05652B),Color(0xFF35A653)])),child:const Column(crossAxisAlignment:CrossAxisAlignment.start,mainAxisAlignment:MainAxisAlignment.center,children:[Text('ताज्या भाज्या, ताजी फळे...',style:TextStyle(color:Colors.white,fontSize:22,fontWeight:FontWeight.bold)),Text('आता तुमच्या मोबाईलवर !',style:TextStyle(color:Colors.white,fontSize:18)),Text('ताजे • विश्वासार्ह • घरपोच सेवा',style:TextStyle(color:Colors.white))])),const SizedBox(height:12),GridView.count(shrinkWrap:true,physics:const NeverScrollableScrollPhysics(),crossAxisCount:2,childAspectRatio:1.7,children:[tile('🥬','भाज्या',()=>setState(()=>tab=1)),tile('🍎','फळे',()=>setState(()=>tab=1)),tile('％','आजचे ऑफर',()=>open('आजचे ऑफर')),tile('🛒','माझ्या ऑर्डर्स',()=>open('माझ्या ऑर्डर्स'))]),const Text('लोकप्रिय उत्पादने',style:TextStyle(fontSize:18,fontWeight:FontWeight.bold)),GridView.count(shrinkWrap:true,physics:const NeverScrollableScrollPhysics(),crossAxisCount:3,children:items.take(6).map(card).toList()),Wrap(spacing:8,children:['ताजा बाजार','माझी पिशवी','अन्नपूर्णा AI','Photo Studio'].map((s)=>ActionChip(label:Text(s),onPressed:()=>open(s))).toList())]);
 Widget tile(String e,String s,VoidCallback f)=>Card(color:Colors.white,child:InkWell(onTap:f,child:Column(mainAxisAlignment:MainAxisAlignment.center,children:[Text(e,style:const TextStyle(fontSize:27)),Text(s,style:const TextStyle(fontWeight:FontWeight.bold))])));
 Widget catalog()=>Column(children:[Padding(padding:const EdgeInsets.all(12),child:TextField(onChanged:(v)=>setState(()=>search=v.trim()),decoration:InputDecoration(prefixIcon:const Icon(Icons.search),hintText:'भाजी किंवा फळ शोधा...',filled:true,fillColor:Colors.white,border:OutlineInputBorder(borderRadius:BorderRadius.circular(14),borderSide:BorderSide.none)))),Wrap(children:['सर्व','भाजीपाला','फळे'].map((s)=>ChoiceChip(label:Text(s),selected:cat==s,onSelected:(_)=>setState(()=>cat=s))).toList()),Expanded(child:GridView.count(padding:const EdgeInsets.all(10),crossAxisCount:2,childAspectRatio:.95,children:items.where((p)=>(cat=='सर्व'||p.cat==cat)&&(search.isEmpty||p.name.toLowerCase().contains(search.toLowerCase()))).map(card).toList()))]);
 Widget card(Item p)=>Card(color:Colors.white,child:Padding(padding:const EdgeInsets.all(8),child:Column(mainAxisAlignment:MainAxisAlignment.center,children:[
 Text(p.emoji,style:const TextStyle(fontSize:40)),
 Text(p.name,style:const TextStyle(fontWeight:FontWeight.bold)),
 Text('₹'+price(p).toStringAsFixed(0)+' / किलो',style:const TextStyle(color:green,fontWeight:FontWeight.bold)),
 FilledButton(onPressed:()=>showDialog(context:context,builder:(_){
  final custom=TextEditingController();
  return AlertDialog(title:Text(p.name+' • वजन निवडा'),content:SingleChildScrollView(child:Column(mainAxisSize:MainAxisSize.min,children:[
   Wrap(spacing:8,runSpacing:8,children:[125,250,500,1000].map((g)=>ActionChip(label:Text(g==1000?'1 किलो':g.toString()+' ग्रॅम'),onPressed:(){add(p,g/1000);Navigator.pop(context);})).toList()),
   const SizedBox(height:12),
   TextField(controller:custom,keyboardType:TextInputType.number,decoration:const InputDecoration(labelText:'कस्टम वजन (ग्रॅम)',hintText:'उदा. ३७५',border:OutlineInputBorder())),
   const SizedBox(height:8),
   FilledButton(onPressed:(){final g=double.tryParse(custom.text.trim());if(g==null||g<=0){ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content:Text('योग्य वजन ग्रॅममध्ये टाका')));return;}add(p,g/1000);Navigator.pop(context);},child:const Text('हे वजन कार्टमध्ये जोडा'))
  ])));
 }),style:FilledButton.styleFrom(backgroundColor:green),child:const Text('＋ वजन निवडा'))
 ])));
 Widget cartPage()=>ListView(padding:const EdgeInsets.all(14),children:[const Text('माझी पिशवी',style:TextStyle(fontSize:22,fontWeight:FontWeight.bold)),if(cart.isEmpty)const Padding(padding:EdgeInsets.all(30),child:Text('तुमची पिशवी रिकामी आहे 🛒')),...cart.entries.map((e){final p=items.firstWhere((x)=>x.name==e.key);return Card(color:Colors.white,child:ListTile(leading:Text(p.emoji,style:const TextStyle(fontSize:28)),title:Text(p.name),subtitle:Text((e.value*1000).round().toString()+' ग्रॅम • ₹'+price(p).toStringAsFixed(0)+'/kg'),trailing:Row(mainAxisSize:MainAxisSize.min,children:[IconButton(onPressed:(){setState((){if(e.value<=.125){cart.remove(e.key);}else{cart[e.key]=e.value-.125;}});save();},icon:const Icon(Icons.remove)),Text((e.value*1000).round().toString()+'g'),IconButton(onPressed:()=>add(p),icon:const Icon(Icons.add)),IconButton(onPressed:(){setState(()=>cart.remove(e.key));save();},icon:const Icon(Icons.delete_outline,color:Colors.red))])));}),Card(color:Colors.white,child:ListTile(title:const Text('एकूण रक्कम'),trailing:Text('₹'+total.toStringAsFixed(2),style:const TextStyle(fontSize:22,fontWeight:FontWeight.bold)))),FilledButton(onPressed:cart.isEmpty?null:checkout,child:const Text('ऑर्डर करा / चेकआउट'))]);
 Widget profile()=>ListView(padding:const EdgeInsets.all(14),children:[const CircleAvatar(radius:40,backgroundColor:Color(0xFFD7F0DF),child:Icon(Icons.person,size:48,color:green)),const Center(child:Text('शेखर खरे',style:TextStyle(fontSize:20,fontWeight:FontWeight.bold))),...['माझ्या ऑर्डर्स','पत्ता व्यवस्थापन','कूपन / सवलत','सेटिंग्ज','मदत आणि समर्थन','डिलिव्हरी पार्टनर','स्टॉक व्यवस्थापन','ग्राहक खाते'].map((s)=>Card(color:Colors.white,child:ListTile(title:Text(s),trailing:const Icon(Icons.chevron_right),onTap:()=>open(s))))]);
 void open(String title){
 if(title=='डिलिव्हरी पार्टनर'){
  Navigator.push(context,MaterialPageRoute(builder:(_)=>DeliveryPartnerPage(orders:orders,onUpdate:(i,status){setState((){final parts=orders[i].split(' • स्थिती: ');orders[i]=parts.first+' • स्थिती: '+status;});save();})));
  return;
 }
 Navigator.push(context,MaterialPageRoute(builder:(_)=>Detail(title:title,items:items,orders:orders,sales:sales,purchases:purchases,wastages:wastages,khataEntries:khataEntries,onRecord:(list,entry){setState(()=>list.insert(0,entry));save();},onPrice:(p,v){setState(()=>customPrices[p.name]=v);save();},onStock:(p,v){setState(()=>p.stock=(p.stock+v).clamp(0,999999).toDouble());save();} )));
}
}
class DeliveryCheckout extends StatefulWidget{
 const DeliveryCheckout({super.key,required this.amount,required this.lines,required this.onConfirm});
 final double amount;final List<String> lines;
 final bool Function(String,String,String,String) onConfirm;
 @override State<DeliveryCheckout> createState()=>_DeliveryCheckoutState();
}
class _DeliveryCheckoutState extends State<DeliveryCheckout>{
 final name=TextEditingController(),phone=TextEditingController(),address=TextEditingController(),landmark=TextEditingController();
 String payment='घरपोच पैसे (Cash on Delivery)';
 bool saving=false;
 @override void dispose(){name.dispose();phone.dispose();address.dispose();landmark.dispose();super.dispose();}
 @override Widget build(BuildContext context)=>Scaffold(
 appBar:AppBar(backgroundColor:green,foregroundColor:Colors.white,title:const Text('डिलिव्हरी तपशील')),
 body:ListView(padding:const EdgeInsets.all(16),children:[
  const Text('घरपोच ऑर्डरसाठी माहिती भरा',style:TextStyle(fontSize:20,fontWeight:FontWeight.bold)),
  const SizedBox(height:12),
  TextField(controller:name,decoration:const InputDecoration(labelText:'ग्राहकाचे नाव *',prefixIcon:Icon(Icons.person_outline),border:OutlineInputBorder())),
  const SizedBox(height:10),
  TextField(controller:phone,keyboardType:TextInputType.phone,maxLength:10,decoration:const InputDecoration(labelText:'मोबाईल नंबर *',prefixIcon:Icon(Icons.phone_android),border:OutlineInputBorder())),
  const SizedBox(height:10),
  TextField(controller:address,minLines:2,maxLines:3,decoration:const InputDecoration(labelText:'पूर्ण डिलिव्हरी पत्ता, नाशिक *',hintText:'घर क्रमांक, सोसायटी, परिसर',prefixIcon:Icon(Icons.location_on_outlined),border:OutlineInputBorder())),
  const SizedBox(height:10),
  TextField(controller:landmark,decoration:const InputDecoration(labelText:'जवळची खूण (ऐच्छिक)',border:OutlineInputBorder())),
  const SizedBox(height:16),
  const Text('पेमेंट पद्धत',style:TextStyle(fontWeight:FontWeight.bold,fontSize:16)),
  RadioListTile<String>(contentPadding:EdgeInsets.zero,value:'घरपोच पैसे (Cash on Delivery)',groupValue:payment,title:const Text('घरपोच पैसे द्या'),onChanged:(v)=>setState(()=>payment=v!)),
  RadioListTile<String>(contentPadding:EdgeInsets.zero,value:'UPI (पेमेंटची पुष्टी दुकानाकडून)',groupValue:payment,title:const Text('UPI (पेमेंटची पुष्टी दुकानाकडून)'),onChanged:(v)=>setState(()=>payment=v!)),
  const Divider(),
  const Text('तुमची ऑर्डर',style:TextStyle(fontSize:18,fontWeight:FontWeight.bold)),
  ...widget.lines.map((s)=>ListTile(dense:true,title:Text(s))),
  ListTile(title:const Text('एकूण रक्कम',style:TextStyle(fontWeight:FontWeight.bold)),trailing:Text('₹${widget.amount.toStringAsFixed(2)}',style:const TextStyle(fontSize:20,fontWeight:FontWeight.bold,color:green))),
  const SizedBox(height:10),
  FilledButton.icon(
   style:FilledButton.styleFrom(backgroundColor:green,padding:const EdgeInsets.all(16)),
   onPressed:saving?null:(){
    final n=name.text.trim(),ph=phone.text.trim(),ad=address.text.trim();
    if(n.isEmpty || !RegExp(r'^[6-9][0-9]{9}$').hasMatch(ph) || ad.isEmpty){
     ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content:Text('नाव, योग्य १० अंकी भारतीय मोबाईल नंबर आणि पूर्ण पत्ता भरा')));
     return;
    }
    setState(()=>saving=true);
    final confirmed = widget.onConfirm(n,ph,ad+(landmark.text.trim().isEmpty?'':' • खूण: ${landmark.text.trim()}'),payment);
    if (!confirmed) { setState(() => saving = false); return; }
    showDialog(context:context,barrierDismissible:false,builder:(_)=>AlertDialog(
     icon:const Icon(Icons.check_circle,color:green,size:48),
     title:const Text('ऑर्डर नोंदवली!'),
     content:const Text('तुमची ऑर्डर या फोनमध्ये जतन झाली आहे. दुकानाकडून पुष्टी मिळाल्यावर डिलिव्हरीची माहिती कळेल.'),
     actions:[FilledButton(onPressed:(){Navigator.pop(context);Navigator.pop(context);Navigator.pop(context);},child:const Text('ठीक आहे'))],
    ));
   },
   icon:const Icon(Icons.delivery_dining),label:const Text('ऑर्डर निश्चित करा'),
  ),
  const SizedBox(height:8),
  const Text('टीप: डिलिव्हरीचा वेळ आणि उपलब्धता दुकानाकडून निश्चित केली जाईल.',style:TextStyle(color:Colors.black54,fontSize:12)),
 ])
 );
}
class DeliveryPartnerPage extends StatefulWidget{
 const DeliveryPartnerPage({super.key,required this.orders,required this.onUpdate});
 final List<String> orders;final void Function(int,String) onUpdate;
 @override State<DeliveryPartnerPage> createState()=>_DeliveryPartnerPageState();
}
class _DeliveryPartnerPageState extends State<DeliveryPartnerPage>{
 static const stages=['नवीन','ऑर्डर स्वीकारली','पॅकिंग सुरू','डिलिव्हरीसाठी निघाली','पोहोचवली'];
 @override Widget build(BuildContext context)=>Scaffold(
 appBar:AppBar(backgroundColor:green,foregroundColor:Colors.white,title:const Text('डिलिव्हरी पार्टनर')),
 body:widget.orders.isEmpty
 ?const Center(child:Text('सध्या कोणतीही ऑर्डर नाही'))
 :ListView.builder(padding:const EdgeInsets.all(12),itemCount:widget.orders.length,itemBuilder:(context,i){
  final order=widget.orders[i];final parts=order.split(' • स्थिती: ');final status=parts.length>1?parts.last:'नवीन';
  return Card(color:Colors.white,child:Padding(padding:const EdgeInsets.all(12),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
   Text(parts.first,style:const TextStyle(fontWeight:FontWeight.bold)),
   const SizedBox(height:8),
   Row(children:[const Icon(Icons.delivery_dining,color:green),const SizedBox(width:6),Expanded(child:Text('सध्याची स्थिती: $status'))]),
   const SizedBox(height:8),
   DropdownButtonFormField<String>(value:stages.contains(status)?status:'नवीन',decoration:const InputDecoration(labelText:'ऑर्डरची स्थिती बदला',border:OutlineInputBorder()),items:stages.map((s)=>DropdownMenuItem(value:s,child:Text(s))).toList(),onChanged:(s){if(s!=null){widget.onUpdate(i,s);setState((){});}}),
  ])));
 })
 );
}
class Detail extends StatefulWidget{
 const Detail({super.key,required this.title,required this.items,required this.orders,required this.sales,required this.purchases,required this.wastages,required this.khataEntries,required this.onRecord,required this.onPrice,required this.onStock});
 final String title;final List<Item> items;final List<String> orders,sales,purchases,wastages,khataEntries;final void Function(List<String>,String) onRecord;final void Function(Item,double) onPrice,onStock;
 @override State<Detail> createState()=>_DetailState();
}
class _DetailState extends State<Detail>{
 Item? selected;final val=TextEditingController();
 @override void dispose(){val.dispose();super.dispose();}
 @override Widget build(BuildContext context){
  final t=widget.title;
  final isStock=t.contains('स्टॉक'),isPurchase=t.contains('खरेदी'),isWaste=t.contains('नासाडी');
  return Scaffold(appBar:AppBar(backgroundColor:green,foregroundColor:Colors.white,title:Text(t)),
   body:ListView(padding:const EdgeInsets.all(15),children:[
    if(isStock||isPurchase||isWaste)...[
     Card(color:Colors.white,child:Padding(padding:const EdgeInsets.all(12),child:Column(crossAxisAlignment:CrossAxisAlignment.start,children:[
      Text(isWaste?'नासाडी / खराब माल नोंदवा':isPurchase?'आजची खरेदी नोंदवा':'उपलब्ध स्टॉक',style:const TextStyle(fontSize:18,fontWeight:FontWeight.bold)),
      const SizedBox(height:10),
      DropdownButtonFormField<Item>(decoration:const InputDecoration(labelText:'उत्पादन निवडा',border:OutlineInputBorder()),items:widget.items.map((p)=>DropdownMenuItem(value:p,child:Text(p.name))).toList(),onChanged:(p)=>setState(()=>selected=p)),
      if(selected!=null)Padding(padding:const EdgeInsets.symmetric(vertical:8),child:Text('सध्याचा स्टॉक: '+selected!.stock.toStringAsFixed(2)+' किलो')),
      TextField(controller:val,keyboardType:const TextInputType.numberWithOptions(decimal:true),decoration:InputDecoration(labelText:isWaste?'नासाडीचे वजन (किलो)':'प्रमाण (किलो)',hintText:'उदा. 2.5',border:const OutlineInputBorder())),
      const SizedBox(height:10),
      SizedBox(width:double.infinity,child:FilledButton.icon(onPressed:(){
       final p=selected,q=double.tryParse(val.text.trim());
       if(p==null||q==null||q<=0){ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content:Text('उत्पादन निवडा आणि योग्य प्रमाण भरा')));return;}
       if(isWaste&&q>p.stock){ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content:Text('नासाडीचे प्रमाण उपलब्ध स्टॉकपेक्षा जास्त आहे')));return;}
       widget.onStock(p,isWaste?-q:q);final stamp=DateTime.now().toString().substring(0,16);widget.onRecord(isWaste?widget.wastages:widget.purchases,'$stamp • ${p.name} • ${q.toStringAsFixed(3)} किलो');val.clear();setState((){});ScaffoldMessenger.of(context).showSnackBar(SnackBar(content:Text(isWaste?'नासाडी नोंदवली':'स्टॉकमध्ये नोंद झाली')));
      },icon:Icon(isWaste?Icons.delete_outline:Icons.save),label:Text(isWaste?'नासाडी जतन करा':isPurchase?'खरेदी जतन करा':'स्टॉक वाढवा'))),
     ]))),
     const SizedBox(height:8),const Text('सर्व उत्पादनांचा सध्याचा स्टॉक',style:TextStyle(fontWeight:FontWeight.bold,fontSize:17)),
     ...widget.items.map((p)=>Card(color:Colors.white,child:ListTile(leading:Text(p.emoji,style:const TextStyle(fontSize:25)),title:Text(p.name),trailing:Text(p.stock.toStringAsFixed(2)+' kg',style:const TextStyle(fontWeight:FontWeight.bold))))),
     const SizedBox(height:12),Text(isWaste?'नासाडीचा इतिहास':'खरेदीचा इतिहास',style:const TextStyle(fontWeight:FontWeight.bold,fontSize:17)),
     ...(isWaste?widget.wastages:widget.purchases).map((entry)=>Card(color:Colors.white,child:ListTile(leading:Icon(isWaste?Icons.delete_outline:Icons.shopping_bag_outlined,color:green),title:Text(entry)))),
     if((isWaste?widget.wastages:widget.purchases).isEmpty)const Text('अजून नोंदी नाहीत.')
    ]
    else if(t.contains('सेटिंग')||t.contains('दर'))...[
     const Text('उत्पादनांचे विक्री दर (प्रति किलो)',style:TextStyle(fontSize:18,fontWeight:FontWeight.bold)),
     ...widget.items.map((p)=>Card(color:Colors.white,child:ListTile(title:Text(p.name),subtitle:Text('सध्याचा दर: ₹'+p.price.toStringAsFixed(2)+' / किलो'),trailing:IconButton(icon:const Icon(Icons.edit,color:green),onPressed:(){
      final c=TextEditingController(text:p.price.toString());
      showDialog(context:context,builder:(_)=>AlertDialog(title:Text(p.name+' चा दर बदला'),content:TextField(controller:c,keyboardType:const TextInputType.numberWithOptions(decimal:true),decoration:const InputDecoration(labelText:'नवीन दर (₹/किलो)')),actions:[
       TextButton(onPressed:()=>Navigator.pop(context),child:const Text('रद्द')),
       FilledButton(onPressed:(){final v=double.tryParse(c.text.trim());if(v==null||v<0){ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content:Text('योग्य दर भरा')));return;}widget.onPrice(p,v);Navigator.pop(context);setState((){});ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content:Text('दर जतन झाला')));},child:const Text('जतन करा'))
      ]));
     }))))
    ]
    else if(t.contains('ऑर्डर'))...[
     if(widget.orders.isEmpty)const Center(child:Padding(padding:EdgeInsets.all(30),child:Text('अजून कोणतीही ऑर्डर नाही'))),
     ...widget.orders.map((s)=>Card(color:Colors.white,child:ListTile(leading:const Icon(Icons.receipt_long,color:green),title:Text(s))))
    ]
    else if(t.contains('खाते'))...[
     const Text('ग्राहक खाते / खाता',style:TextStyle(fontSize:18,fontWeight:FontWeight.bold)),
     const Text('ग्राहकाची उधारी आणि जमा रक्कम येथे नोंदवा.',style:TextStyle(color:Colors.black54)),
     _KhataForm(onSave:(entry)=>widget.onRecord(widget.khataEntries,entry)),
     const SizedBox(height:12),
     const Text('खात्याचा इतिहास',style:TextStyle(fontWeight:FontWeight.bold,fontSize:17)),
     ...widget.khataEntries.map((s)=>Card(color:Colors.white,child:ListTile(leading:const Icon(Icons.account_balance_wallet_outlined,color:green),title:Text(s)))),
     if(widget.khataEntries.isEmpty)const Text('अजून उधारी किंवा जमा नोंद नाही.'),
     ...widget.sales.map((s)=>Card(color:Colors.white,child:ListTile(leading:const Icon(Icons.person_outline,color:green),title:Text(s))))
    ]
    else if(t.contains('रिपोर्ट')||t.contains('विक्री'))...[
     Text('एकूण नोंदी: '+widget.sales.length.toString(),style:const TextStyle(fontSize:18,fontWeight:FontWeight.bold)),
     ...widget.sales.map((s)=>Card(color:Colors.white,child:ListTile(leading:const Icon(Icons.currency_rupee,color:green),title:Text(s)))),
     if(widget.sales.isEmpty)const Text('विक्री नोंदी उपलब्ध नाहीत.')
    ]
    else if(t.contains('ताजा')||t.contains('बाजार'))...[
     const Text('आजचे ताजे उत्पादन आणि दर',style:TextStyle(fontSize:18,fontWeight:FontWeight.bold)),
     ...widget.items.map((p)=>Card(color:Colors.white,child:ListTile(leading:Text(p.emoji,style:const TextStyle(fontSize:25)),title:Text(p.name),trailing:Text('₹'+p.price.toStringAsFixed(0)+' / kg'))))
    ]
    else if(t.contains('AI'))...[
     const Icon(Icons.auto_awesome,color:green,size:48),const Text('अन्नपूर्णा AI',style:TextStyle(fontSize:20,fontWeight:FontWeight.bold)),
     const Text('भाजीपाला दर, उपलब्ध स्टॉक आणि विक्रीचे नियोजन यासाठी AI सहाय्य. AI उत्तरांसाठी सुरक्षित सर्व्हर/API जोडणी आवश्यक आहे.')
    ]
    else if(t.contains('Photo'))...[
     const Icon(Icons.photo_camera_outlined,color:green,size:48),const Text('Photo Studio',style:TextStyle(fontSize:20,fontWeight:FontWeight.bold)),
     const Text('उत्पादनांचे फोटो अपलोड व संपादन सुविधा पुढील आवृत्तीत जोडायची आहे.')
    ]
    else if(t.contains('ऑफर'))...[
     const Text('आजच्या ऑफर',style:TextStyle(fontSize:20,fontWeight:FontWeight.bold)),
     const Text('सध्या कोणतीही ऑफर जाहीर केलेली नाही. दुकानाचे दर सेटिंग्जमधून बदलू शकता.')
    ]
    else if(t.contains('सूचना'))...[
     const Icon(Icons.notifications_none,color:green,size:44),const Text('सूचना',style:TextStyle(fontSize:20,fontWeight:FontWeight.bold)),
     const Text('नवीन ऑर्डर आणि डिलिव्हरी अपडेट्स या विभागात दाखवले जातील.')
    ]
    else if(t.contains('पत्ता'))...[
     const Text('पत्ता व्यवस्थापन',style:TextStyle(fontSize:18,fontWeight:FontWeight.bold)),
     const Text('ऑर्डर करताना ग्राहकाचा पूर्ण डिलिव्हरी पत्ता भरता येतो.')
    ]
    else if(t.contains('मदत'))...[
     const Icon(Icons.support_agent,color:green,size:48),const Text('मदत आणि समर्थन',style:TextStyle(fontSize:20,fontWeight:FontWeight.bold)),
     const Text('ऑर्डर, स्टॉक किंवा दरांबाबत मदत हवी असल्यास दुकानाशी संपर्क साधा.')
    ]
    else const Text('हा विभाग पुढील आवृत्तीत अधिक सुविधांसह उपलब्ध होईल.')
   ])
  );
 }
}

class _KhataForm extends StatefulWidget {
 const _KhataForm({required this.onSave});
 final ValueChanged<String> onSave;
 @override State<_KhataForm> createState()=>_KhataFormState();
}
class _KhataFormState extends State<_KhataForm> {
 final name=TextEditingController(),phone=TextEditingController(),amount=TextEditingController(),note=TextEditingController();
 String kind='उधारी';
 @override void dispose(){name.dispose();phone.dispose();amount.dispose();note.dispose();super.dispose();}
 @override Widget build(BuildContext context)=>Card(color:Colors.white,child:Padding(padding:const EdgeInsets.all(12),child:Column(children:[
  TextField(controller:name,decoration:const InputDecoration(labelText:'ग्राहकाचे नाव',border:OutlineInputBorder())),
  const SizedBox(height:8),
  TextField(controller:phone,keyboardType:TextInputType.phone,maxLength:10,decoration:const InputDecoration(labelText:'मोबाईल नंबर (ऐच्छिक)',border:OutlineInputBorder())),
  DropdownButtonFormField<String>(value:kind,decoration:const InputDecoration(labelText:'नोंदीचा प्रकार'),items:const [DropdownMenuItem(value:'उधारी',child:Text('उधारी वाढवा')),DropdownMenuItem(value:'जमा',child:Text('पैसे जमा झाले'))],onChanged:(v)=>setState(()=>kind=v??'उधारी')),
  const SizedBox(height:8),
  TextField(controller:amount,keyboardType:const TextInputType.numberWithOptions(decimal:true),decoration:const InputDecoration(labelText:'रक्कम (₹)',border:OutlineInputBorder())),
  const SizedBox(height:8),
  TextField(controller:note,decoration:const InputDecoration(labelText:'तपशील (ऐच्छिक)',border:OutlineInputBorder())),
  const SizedBox(height:10),
  SizedBox(width:double.infinity,child:FilledButton.icon(onPressed:(){
   final n=name.text.trim(),v=double.tryParse(amount.text.trim()),ph=phone.text.trim();
   if(n.isEmpty||v==null||v<=0||(ph.isNotEmpty&&!RegExp(r'^[6-9][0-9]{9}$').hasMatch(ph))){ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content:Text('ग्राहकाचे नाव, योग्य रक्कम आणि योग्य मोबाईल नंबर भरा')));return;}
   final stamp=DateTime.now().toString().substring(0,16);
   widget.onSave('$stamp • $n • ${ph.isEmpty?'मोबाईल नाही':ph} • $kind • ₹${v.toStringAsFixed(2)} • ${note.text.trim()}');
   amount.clear();note.clear();ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content:Text('खात्याची नोंद जतन झाली')));
  },icon:const Icon(Icons.save),label:const Text('खात्यात नोंद जतन करा')))
 ])));
}
