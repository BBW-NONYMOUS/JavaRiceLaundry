"""Migrate existing NetBeans forms to the Home design, then regenerate matching layouts.

Existing business methods and event handlers are preserved. The theme marker prevents
repeated scaling; later runs regenerate Java from the current .form metadata.
"""
from pathlib import Path
import copy
import json
import re
import xml.etree.ElementTree as E

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'src/laundry'
GREEN, DARK, LIGHT, ACCENT, BG, TEXT, MUTED, BORDER = (
    '#1E8E3E', '#145A32', '#E8F5E9', '#C8E6C9', '#F5F8F5', '#1B1B1B', '#6B6B6B', '#E3EBE4')
PALETTE = {
    (37,99,235): GREEN, (30,64,175): DARK, (15,23,42): TEXT, (30,41,59): TEXT,
    (51,65,85): TEXT, (100,116,139): MUTED, (148,163,184): MUTED,
    (226,232,240): BORDER, (248,250,252): BG, (219,234,254): LIGHT,
    (239,246,255): LIGHT, (191,219,254): ACCENT,
    (180,83,9): '#8A6D00', (254,243,199): '#FFF3B0', (253,230,138): '#FFF3B0',
    (248,251,255): LIGHT, (241,245,249): LIGHT, (14,165,233): '#56A468',
    (22,163,74): GREEN, (21,128,61): '#1E6B2F', (220,252,231): ACCENT,
    (187,247,208): ACCENT, (71,85,105): MUTED,
}


def properties(n):
    p = n.find('Properties')
    if p is None: p = E.SubElement(n, 'Properties')
    return p


def set_prop(n, name, typ, value=None, child=None):
    props = properties(n)
    for old in list(props):
        if old.get('name') == name: props.remove(old)
    p = E.SubElement(props, 'Property', name=name, type=typ)
    if value is not None: p.set('value', str(value).lower() if isinstance(value, bool) else str(value))
    if child is not None: p.append(child)
    return p


def del_prop(n, name):
    for p in list(properties(n)):
        if p.get('name') == name: properties(n).remove(p)


def set_color(n, name, hexval):
    rgb = hexval.lstrip('#')
    p = set_prop(n, name, 'java.awt.Color', child=E.Element('Color', red=rgb[0:2], green=rgb[2:4], blue=rgb[4:6], type='rgb'))
    p.set('editor', 'org.netbeans.beaninfo.editors.ColorEditor')


def font(n, size=13, bold=False):
    p = set_prop(n, 'font', 'java.awt.Font', child=E.Element('Font', name='Segoe UI', size=str(size), style='1' if bold else '0'))
    p.set('editor', 'org.netbeans.beaninfo.editors.FontEditor')


def text(n, value): set_prop(n, 'text', 'java.lang.String', value)


def icon(n, path):
    p = set_prop(n, 'icon', 'javax.swing.Icon', child=E.Element('Image', iconType='3', name=path))
    p.set('editor', 'org.netbeans.modules.form.editors2.IconEditor')


def absolute(n, bounds):
    c = n.find('Constraints')
    if c is not None: n.remove(c)
    c = E.SubElement(n, 'Constraints')
    c = E.SubElement(c, 'Constraint', layoutClass='org.netbeans.modules.form.compat2.layouts.DesignAbsoluteLayout',
                     value='org.netbeans.modules.form.compat2.layouts.DesignAbsoluteLayout$AbsoluteConstraintsDescription')
    E.SubElement(c, 'AbsoluteConstraints', **dict(zip(('x','y','width','height'), map(str, bounds))))


def layout(n):
    l = E.SubElement(n, 'Layout', {'class': 'org.netbeans.modules.form.compat2.layouts.DesignAbsoluteLayout'})
    E.SubElement(l, 'Property', name='useNullLayout', type='boolean', value='true')


def panel(name, bounds, bg='#FFFFFF', shadow=True):
    n = E.Element('Container', {'class': 'components.RoundedPanel', 'name': name})
    set_color(n, 'background', bg)
    set_prop(n, 'shadow', 'boolean', shadow)
    absolute(n, bounds); layout(n); E.SubElement(n, 'SubComponents')
    return n


def label(name, value, bounds, size=13, bold=False, fg=TEXT):
    n = E.Element('Component', {'class': 'javax.swing.JLabel', 'name': name})
    text(n, value); font(n, size, bold); set_color(n, 'foreground', fg); absolute(n, bounds)
    return n


def find(root, name): return next(n for n in root.iter() if n.get('name') == name and n.tag in ('Component','Container'))


def bound(n):
    a = n.find('./Constraints/Constraint/AbsoluteConstraints')
    return [int(a.get(k)) for k in ('x','y','width','height')]


def scale(n, sx, sy):
    a = n.find('./Constraints/Constraint/AbsoluteConstraints')
    if a is not None:
        for k in ('x','width'): a.set(k, str(max(1 if k=='width' else 0, round(int(a.get(k))*sx))))
        for k in ('y','height'): a.set(k, str(max(1 if k=='height' else 0, round(int(a.get(k))*sy))))
    for child in n.findall('./SubComponents/*'): scale(child, sx, sy)


def add_action(n, body, handlers, source):
    name = n.get('name') + 'ActionPerformed'
    events = n.find('Events')
    if events is not None: n.remove(events)
    events = E.SubElement(n, 'Events')
    E.SubElement(events, 'EventHandler', event='actionPerformed', listener='java.awt.event.ActionListener',
                 parameters='java.awt.event.ActionEvent', handler=name)
    if not re.search(r'void\s+' + name + r'\(', source):
        handlers.append(f'    private void {name}(java.awt.event.ActionEvent evt) {{//GEN-FIRST:event_{name}\n'
                        f'        {body}\n    }}//GEN-LAST:event_{name}\n')


def rebuild_sidebar(root, staff, handlers, source):
    side = find(root, 'sidebarPanel'); absolute(side, (0,0,224,760))
    side.set('class', 'javax.swing.JPanel'); del_prop(side,'border')
    set_color(side, 'background', DARK if staff else '#FFFFFF')
    children = side.find('SubComponents')
    existing = {n.get('name'): n for n in children}
    for n in list(children): children.remove(n)
    mark = existing['brandLabel']; absolute(mark,(80,30,64,64)); text(mark,'')
    icon(mark, '/resources/images/ui/logo_white.png' if staff else '/resources/images/logo.png')
    children.append(mark)
    brand = label('sidebarBrand','METRIC LAUNDRY',(12,103,200,24),17,True,'#FFFFFF' if staff else DARK)
    set_prop(brand,'horizontalAlignment','int',0); children.append(brand)
    tagline = label('sidebarTagline', 'Clean Clothes, Happy You!', (12,132,200,20),11,False,ACCENT if staff else MUTED)
    set_prop(tagline,'horizontalAlignment','int',0); children.append(tagline)
    if staff:
        role = existing['roleLabel']; absolute(role,(12,157,200,20)); font(role,11,True)
        set_color(role,'foreground',ACCENT); set_prop(role,'horizontalAlignment','int',0); children.append(role)
        entries = [('staffHomeNav','Dashboard','home','Nav.STAFF_HOME'),
                   ('staffOrdersNav','Manage Orders','orders','Nav.STAFF_ORDERS'),
                   ('staffServicesNav','Manage Services','services','Nav.STAFF_SERVICES'),
                   ('staffReportNav','Reports','report','Nav.STAFF_REPORT')]
    else:
        entries = [('homeNav','Home','home','Nav.HOME'), ('servicesNav','Place Order','orders','Nav.SERVICES'),
                   ('ordersNav','Track Order','track','Nav.ORDERS'),('paymentNav','Payment','payment','Nav.ORDERS'),
                   ('historyNav','Order History','history','Nav.ORDERS'),('aboutNav','About Us','info',None),
                   ('profileNav','My Profile','profile','Nav.PROFILE')]
    for i,(name,title,asset,target) in enumerate(entries):
        n = existing.get(name, E.Element('Component', {'name':name}))
        n.set('class','components.RoundedButton'); text(n,title); font(n,13,True)
        active = 'active' in E.tostring(n, encoding='unicode')
        # Existing forms identify the active item with their active icon.
        set_color(n,'background',GREEN if active else DARK if staff else '#FFFFFF')
        set_color(n,'foreground','#FFFFFF' if active or staff else DARK)
        for prop in ('border','opaque','borderPainted','contentAreaFilled','focusPainted','cursor'): del_prop(n,prop)
        set_prop(n,'horizontalAlignment','int',2); set_prop(n,'iconTextGap','int',12)
        icon(n,f'/resources/images/ui/{asset}{"_white" if active or staff else ""}.png')
        absolute(n,(20,196+i*56,184,46))
        body = 'Ui.showAbout(this);' if target is None else f'Nav.go(this, {target});'
        add_action(n, body, handlers, source); children.append(n)
    leaf = label('sidebarLeaves','',(0,578,170,170)); icon(leaf,'/resources/images/leaves.png')
    logout = existing['logoutNav']; logout.set('class','components.RoundedButton')
    text(logout,'Log out'); font(logout,13,True); absolute(logout,(20,695,184,42))
    for prop in ('border','opaque','cursor'): del_prop(logout,prop)
    set_color(logout,'background',DARK if staff else '#FFFFFF'); set_color(logout,'foreground','#FFFFFF' if staff else DARK)
    set_prop(logout,'horizontalAlignment','int',2); icon(logout,f'/resources/images/ui/logout{"_white" if staff else ""}.png')
    add_action(logout,'Nav.logout(this);',handlers,source); children.append(logout)
    children.append(leaf)


def authentication(root, register):
    main = find(root,'mainPanel'); children=main.find('SubComponents')
    existing = {n.get('name'):n for n in children}
    for n in list(children): children.remove(n)
    left = panel('brandPanel',(24,24,552,712),LIGHT,False); set_color(left,'gradientEnd',ACCENT)
    lc=left.find('SubComponents')
    logo=existing['logoLabel']; absolute(logo,(36,90,480,100)); icon(logo,'/resources/images/ui/logo.png'); lc.append(logo)
    brand=existing['appNameLabel']; text(brand,'METRIC LAUNDRY'); font(brand,30,True); set_color(brand,'foreground',DARK)
    absolute(brand,(36,220,480,50)); lc.append(brand)
    tagline=label('brandTagline','Clean Clothes, Happy You!',(36,278,480,30),18,False,DARK)
    set_prop(tagline,'horizontalAlignment','int',0);lc.append(tagline)
    basket=label('basketArtwork','',(156,334,240,160)); icon(basket,'/resources/images/basket.png');lc.append(basket)
    leaf=label('authLeaves','',(12,526,170,170));icon(leaf,'/resources/images/leaves.png');lc.append(leaf)
    note=label('brandNote','Thoughtful care for your everyday essentials.',(80,612,410,28),14,False,DARK);lc.append(note)
    children.append(left)
    card = panel('authCard',(608,40 if register else 100,624,680 if register else 560))
    cc=card.find('SubComponents')
    title=existing['createLabel' if register else 'welcomeLabel']; absolute(title,(72,24 if register else 40,480,42));font(title,30,True)
    set_prop(title,'horizontalAlignment','int',2);set_color(title,'foreground',DARK);cc.append(title)
    subtitle=existing['registerSubtitleLabel' if register else 'loginSubtitleLabel'];absolute(subtitle,(72,74 if register else 94,480,24))
    set_prop(subtitle,'horizontalAlignment','int',2);cc.append(subtitle)
    boxes=['nameBox','emailBox','phoneBox','passwordBox'] if register else ['emailBox','passwordBox']
    captions=['Full name','Email address','Mobile number','Password'] if register else ['Email address','Password']
    for i,(boxname,caption) in enumerate(zip(boxes,captions)):
        y=(132+i*84) if register else (180+i*92)
        cc.append(label(boxname+'Caption',caption,(72,y-27,480,22),13,True))
        box=existing[boxname]
        for n in box.findall('./SubComponents/*'): scale(n,480/380,1)
        absolute(box,(72,y,480,48));cc.append(box)
    submit=existing['registerButton' if register else 'loginButton'];absolute(submit,(72,498 if register else 354,480,48));cc.append(submit)
    prompt=existing['loginPromptLabel' if register else 'registerPromptLabel'];absolute(prompt,(72,570 if register else 430,214,24));cc.append(prompt)
    link=existing['loginLink' if register else 'registerLink'];absolute(link,(296,570 if register else 430,230,24));cc.append(link)
    children.append(card)
    if not register:
        hint=existing['demoHintLabel'];text(hint,'One secure sign-in for customers, staff and administrators.')
        absolute(hint,(635,688,580,24));font(hint,12);set_color(hint,'foreground',MUTED);children.append(hint)


def migrate(root, name, source, handlers):
    main=find(root,'mainPanel'); set_color(main,'background',BG)
    set_prop(main,'preferredSize','java.awt.Dimension',child=E.Element('Dimension',value='[1280, 760]'))
    properties(main).find("Property[@name='preferredSize']").set('editor','org.netbeans.beaninfo.editors.DimensionEditor')
    for c in root.iter('Color'):
        if c.get('type')=='rgb':
            rgb=tuple(int(c.get(k,'0'),16) for k in ('red','green','blue'))
            if rgb in PALETTE:
                value=PALETTE[rgb][1:]
                for k,v in zip(('red','green','blue'),(value[0:2],value[2:4],value[4:6])):c.set(k,v)
    for n in list(root.iter()):
        if n.tag not in ('Container','Component'):continue
        nm=n.get('name','');cls=n.get('class','')
        if cls=='javax.swing.JPanel' and nm not in ('mainPanel','sidebarPanel'):
            n.set('class','components.RoundedPanel')
            if nm.endswith('Box'):
                set_prop(n,'shadow','boolean',False)
            else:
                del_prop(n,'border');set_prop(n,'shadow','boolean',not nm.startswith('notice'))
            set_color(n,'background',LIGHT if nm.startswith('notice') else '#FFFFFF')
        if cls=='javax.swing.JButton':
            n.set('class','components.RoundedButton');font(n,13,True)
            for prop in ('border','borderPainted','contentAreaFilled','focusPainted','opaque'):del_prop(n,prop)
            primary=not any(word in nm.lower() for word in ('back','password','refresh','document','receipt'))
            set_color(n,'background',GREEN if primary else LIGHT)
            set_color(n,'foreground','#FFFFFF' if primary else DARK)
        for f in n.findall('./Properties/Property/Font'):
            size=int(f.get('size','13'))
            f.set('size',str(30 if size>=26 else 26 if size>=24 else 18 if size>=19 else 16 if size>=16 else 13 if size>=13 else size))
        for image in n.findall('./Properties/Property/Image'):
            image.set('name',image.get('name','').replace('/laundry/icons/','/resources/images/ui/'))
    if name in ('LoginForm','RegisterForm'):
        authentication(root,name=='RegisterForm')
    else:
        staff=name.startswith('Staff')
        for n in list(main.find('SubComponents')):
            if n.get('name')=='sidebarPanel':continue
            x,y,w,h=bound(n)
            for child in n.findall('./SubComponents/*'):scale(child,1.26,760/680)
            absolute(n,(round(248+(x-260)*1.26),round(y*760/680),round(w*1.26),round(h*760/680)))
        rebuild_sidebar(root,staff,handlers,source)
        header=panel('pageBanner',(248,24,1008,84),LIGHT,False);set_color(header,'gradientEnd',ACCENT)
        children=main.find('SubComponents');children.append(header)
        title=find(root,'titleLabel');subtitle=find(root,'subtitleLabel')
        children.remove(title);children.remove(subtitle)
        absolute(title,(24,6,710,40));font(title,28,True);set_color(title,'foreground',DARK)
        absolute(subtitle,(24,48,780,24));font(subtitle,13);set_color(subtitle,'foreground',DARK)
        header.find('SubComponents').extend([title,subtitle])
        # Put auxiliary header controls above the banner with room for the title.
        controls={
            'MyOrdersForm':{'orderPickerLabel':(858,28,48,30),'orderCombo':(910,28,220,32),'refreshButton':(1140,28,100,32)},
            'StaffDashboardForm':{'dateLabel':(900,32,332,24),'refreshButton':(1120,67,112,30)},
            'StaffOrdersForm':{'refreshButton':(1120,44,112,38)},
            'StaffReportForm':{'periodCombo':(1030,44,200,34)},
        }
        for nm,bounds in controls.get(name,{}).items():absolute(find(root,nm),bounds)
        if name=='MyOrdersForm': absolute(title,(24,6,540,40))
        if name=='StaffDashboardForm':absolute(title,(24,6,610,40))
    aux=root.find('AuxValues')
    E.SubElement(aux,'AuxValue',name='MetricLaundry_greenTheme',type='java.lang.String',value='1')


def java_color(c):
    rgb=[int(c.get(k,'0'),16) for k in ('red','green','blue')]
    return 'new java.awt.Color('+', '.join(map(str,rgb))+')'


def property_java(var,p):
    name,typ,value=p.get('name'),p.get('type'),p.get('value')
    if name=='buttonGroup':return [f'        {p.find("ComponentRef").get("name")}.add({var});']
    if typ=='java.lang.String':arg=json.dumps(value or '',ensure_ascii=True)
    elif typ in ('boolean','int'):arg=value
    elif typ=='java.awt.Color':arg=java_color(p.find('Color'))
    elif typ=='java.awt.Font':
        f=p.find('Font');arg=f'components.Theme.font({f.get("size")}, {str(f.get("style")=="1").lower()})'
    elif typ=='java.awt.Dimension':arg='new java.awt.Dimension('+p.find('Dimension').get('value').strip('[]')+')'
    elif typ=='javax.swing.Icon':arg=f'new javax.swing.ImageIcon(getClass().getResource({json.dumps(p.find("Image").get("name"))}))'
    elif typ=='java.awt.Cursor':arg='java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)'
    elif typ=='javax.swing.ComboBoxModel':
        vals=[json.dumps(s.get('value','')) for s in p.findall('./StringArray/StringItem')]
        arg='new javax.swing.DefaultComboBoxModel<>(new String[]{'+', '.join(vals)+'})'
    elif typ=='javax.swing.border.Border':
        b=p.find('Border');line=b.find('LineBorder');matte=b.find('MatteColorBorder');empty=b.find('EmptyBorder')
        if line is not None:
            c=line.find('Color');arg=f'new javax.swing.border.LineBorder({java_color(c)}, {line.get("thickness","1")}, {line.get("roundedCorners","false")})'
        elif matte is not None:
            arg='javax.swing.BorderFactory.createMatteBorder('+', '.join(matte.get(k,'0') for k in ('top','left','bottom','right'))+', '+java_color(matte.find('Color'))+')'
        elif empty is not None:
            arg='javax.swing.BorderFactory.createEmptyBorder('+', '.join(empty.get(k,'0') for k in ('top','left','bottom','right'))+')'
        else:raise ValueError('Unsupported border '+E.tostring(b,encoding='unicode'))
    else:raise ValueError(f'Unsupported property {name}: {typ}')
    return [f'        {var}.set{name[0].upper()+name[1:]}({arg});']


def generate(root,path,source,handlers):
    components=[n for n in root.iter() if n.tag in ('Container','Component')]
    lines=[]
    for n in components:
        cls=n.get('class')
        lines.append(f'        {n.get("name")} = new {cls}{"<>" if cls=="javax.swing.JComboBox" else ""}();')
    for p in root.findall('./Properties/Property'):lines.extend(property_java('',p))
    # Root properties apply to the frame itself, not an empty variable.
    lines=[line.replace('        .set','        set') for line in lines]

    def build(n):
        name=n.get('name')
        for p in n.findall('./Properties/Property'):lines.extend(property_java(name,p))
        l=n.find('Layout')
        if l is not None and 'DesignAbsoluteLayout' in l.get('class',''):lines.append(f'        {name}.setLayout(null);')
        for ev in n.findall('./Events/EventHandler'):
            listener=ev.get('listener'); method=ev.get('event');param=ev.get('parameters')
            adapter=listener.replace('Listener','Adapter') if listener.endswith(('MouseListener','KeyListener','FocusListener')) else listener
            lines.extend([f'        {name}.add{listener.rsplit(".",1)[1]}(new {adapter}() {{',
                          f'            public void {method}({param} evt) {{ {ev.get("handler")}(evt); }}',
                          '        });'])
        for child in n.findall('./SubComponents/*'):
            build(child)
            cn=child.get('name')
            if n.get('class')=='javax.swing.JScrollPane':lines.append(f'        {name}.setViewportView({cn});')
            else:
                lines.append(f'        {name}.add({cn});')
                a=child.find('./Constraints/Constraint/AbsoluteConstraints')
                if a is not None:lines.append(f'        {cn}.setBounds('+', '.join(a.get(k) for k in ('x','y','width','height'))+');')
    main=find(root,'mainPanel');build(main)
    lines+=['        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);','        pack();','        setLocationRelativeTo(null);']
    method='    private void initComponents() {\n'+'\n'.join(lines)+'\n    }// </editor-fold>//GEN-END:initComponents'
    source,n=re.subn(r'    private void initComponents\(\) \{.*?//GEN-END:initComponents',lambda m:method,source,flags=re.S)
    if n!=1:raise ValueError('Layout block missing: '+path.name)
    fields='    // Variables declaration - do not modify//GEN-BEGIN:variables\n'+'\n'.join(
        f'    private {n.get("class")}{"<String>" if n.get("class")=="javax.swing.JComboBox" else ""} {n.get("name")};' for n in components)+'\n    // End of variables declaration//GEN-END:variables'
    source,n=re.subn(r'    // Variables declaration.*?//GEN-END:variables',lambda m:fields,source,flags=re.S)
    if n!=1:raise ValueError('Field block missing: '+path.name)
    if handlers:source=source.replace('    // Variables declaration','\n'.join(handlers)+'\n    // Variables declaration',1)
    if 'Ui.finishFrame(this);' not in source:source=source.replace('        initComponents();','        initComponents();\n        Ui.finishFrame(this);',1)
    source=source.replace('Ui.BLUE','Ui.GREEN').replace('/laundry/icons/','/resources/images/ui/')
    path.write_text(source,encoding='utf-8')


for form in sorted(SRC.glob('*.form')):
    if form.stem=='DashboardForm':continue
    tree=E.parse(form);root=tree.getroot();java=form.with_suffix('.java');source=java.read_text(encoding='utf-8')
    handlers=[]
    if not any(n.get('name')=='MetricLaundry_greenTheme' for n in root.findall('./AuxValues/AuxValue')):
        backup=ROOT/'target/style-migration/original';backup.mkdir(parents=True,exist_ok=True)
        (backup/form.name).write_bytes(form.read_bytes());(backup/java.name).write_bytes(java.read_bytes())
        migrate(root,form.stem,source,handlers)
    for n in root.iter():
        if n.get('class')=='laundry.BarChart':del_prop(n,'border')
        if n.get('name')=='statusBadge' and n.tag=='Component':
            n.set('class','components.StatusBadgeLabel');set_prop(n,'opaque','boolean',False);del_prop(n,'border')
    if form.stem=='MyOrdersForm':
        order_card=find(root,'orderCard');children=order_card.find('SubComponents')
        if not any(n.get('name')=='trackingProgress' for n in children):
            for n in list(children):
                if re.fullmatch(r'step\d+(Icon|Name|Status|Line)',n.get('name','')):children.remove(n)
            progress=E.Element('Component',{'class':'components.StepProgressBar','name':'trackingProgress'})
            absolute(progress,(30,210,948,110));children.append(progress)
        text(find(root,'titleLabel'),'Order Tracking')
        text(find(root,'subtitleLabel'),'Follow your laundry and review your past orders.')
        replacement='''        trackingProgress.setCurrentStep(order.getStatus() + 1);
        String[] recordedTimes = new String[6];
        recordedTimes[0] = order.getCreatedAt().format(Ui.TIME);
        for (int step = 0; step <= order.getStatus(); step++) {
            java.time.LocalDateTime when = order.getStatusTime(step);
            if (when != null) recordedTimes[step + 1] = when.format(Ui.TIME);
        }
        trackingProgress.setTimestamps(recordedTimes);
        '''
        source=re.sub(r'        javax\.swing\.JLabel\[\] icons = .*?        (?=customerValueLabel\.setText)',
                      lambda m:replacement,source,flags=re.S)
    E.indent(tree,space='  ');tree.write(form,encoding='UTF-8',xml_declaration=True)
    generate(root,java,source,handlers)
    print('Updated '+form.stem+' Java + form')

# Runtime helpers, diagrams, and receipt HTML must use the same palette too.
for java in SRC.glob('*.java'):
    source=java.read_text(encoding='utf-8').replace('Ui.BLUE','Ui.GREEN')
    def rgb(m):
        values=tuple(map(int,m.groups())); mapped=PALETTE.get(values)
        return 'new '+('java.awt.' if 'java.awt.' in m.group(0) else '')+'Color(0x'+mapped[1:]+')' if mapped else m.group(0)
    source=re.sub(r'new (?:java\.awt\.)?Color\((\d+),\s*(\d+),\s*(\d+)\)',rgb,source)
    for old,new in {'#2563eb':GREEN,'#64748b':MUTED,'#1e40af':DARK}.items():source=source.replace(old,new)
    source=source.replace('/laundry/icons/','/resources/images/ui/')
    java.write_text(source,encoding='utf-8')
print('Runtime styling and receipt colors updated')
