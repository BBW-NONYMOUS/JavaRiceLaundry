"""Generate matching NetBeans metadata and Java layout from a single component tree.

Run only when intentionally replacing the layout; normal edits can use NetBeans Design.
Business logic lives in the separate CustomerDashboard.behavior template below.
"""
from pathlib import Path
import json
from xml.sax.saxutils import escape, quoteattr

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / "src/view/customer"
DEST.mkdir(parents=True, exist_ok=True)


def node(name, cls, bounds, **kw):
    return dict(name=name, cls=cls, bounds=bounds, **kw)


def label(name, text, bounds, size=13, bold=False, fg="#1B1B1B", **kw):
    return node(name, "javax.swing.JLabel", bounds, text=text, font=(size, bold), fg=fg, **kw)


def button(name, text, bounds, handler, primary=False):
    return node(name, "components.RoundedButton", bounds, text=text, font=(13, True),
                bg="#1E8E3E" if primary else "#FFFFFF", fg="#FFFFFF" if primary else "#145A32", event=handler)


sidebar = node("sidebar", "javax.swing.JPanel", (0, 0, 224, 760), bg="#FFFFFF", children=[
    label("logo", "", (79, 30, 64, 64), image="logo.png"),
    label("brand", "METRIC LAUNDRY", (20, 103, 184, 24), 17, True, "#145A32", center=True),
    label("brandTagline", "A little care. A lot of fresh.", (20, 132, 184, 20), 11, fg="#6B6B6B", center=True),
    button("homeButton", "Home", (20, 196, 184, 46), "home", True),
    button("placeOrderButton", "Place Order", (20, 252, 184, 46), "place"),
    button("trackButton", "Track Order", (20, 308, 184, 46), "track"),
    button("paymentButton", "Payment", (20, 364, 184, 46), "payment"),
    button("historyButton", "Order History", (20, 420, 184, 46), "history"),
    button("aboutButton", "About Us", (20, 476, 184, 46), "about"),
    button("profileButton", "My Profile", (20, 532, 184, 46), "profile"),
    button("logoutButton", "Log out", (20, 695, 184, 42), "logout"),
    label("sidebarLeaves", "", (0, 578, 170, 170), image="leaves.png"),
])
banner = node("welcomeBanner", "components.RoundedPanel", (248, 26, 1008, 150), bg="#E8F5E9", gradient="#C8E6C9", children=[
    label("eyebrow", "FRESH LAUNDRY, LESS EFFORT", (26, 18, 380, 18), 11, True, "#145A32"),
    label("welcomeTitle", "Welcome to Metric Laundry!", (26, 43, 725, 44), 30, True, "#145A32"),
    label("welcomeTagline", "Clean Clothes, Happy You!", (26, 94, 480, 25), 16, fg="#145A32"),
    label("basketImage", "", (754, -3, 240, 160), image="basket.png"),
])
service_children = [label("servicesTitle", "Our Services", (23, 18, 400, 26), 18, True),
                    button("viewServicesButton", "View All \u2192", (512, 13, 119, 36), "place")]
for i in range(3):
    x = 20 + i * 205
    service_children.append(node(f"serviceCard{i}", "components.RoundedPanel", (x, 59, 197, 200), bg="#F4F9F4", shadow=False, children=[
        label(f"serviceImage{i}", "", (28, 5, 140, 92), image=["washing.png", "dry-cleaning.png", "folding.png"][i]),
        label(f"serviceName{i}", ["Washing", "Dry Cleaning", "Folding"][i], (8, 99, 181, 25), 15, True, center=True),
        label(f"servicePrice{i}", "Loading rate...", (8, 127, 181, 22), 13, fg="#145A32", center=True),
        button(f"orderButton{i}", "Order Now  >", (26, 158, 145, 34), f"order{i}", True),
    ]))
services = node("servicesPanel", "components.RoundedPanel", (248, 196, 656, 278), children=service_children)
tracking = node("trackingPanel", "components.RoundedPanel", (924, 196, 332, 478), children=[
    label("trackingTitle", "Track Your Laundry", (23, 18, 286, 26), 18, True),
    label("trackingOrder", "Your latest order at a glance", (23, 50, 286, 20), 12, fg="#6B6B6B"),
    node("progress", "components.StepProgressBar", (24, 84, 282, 292), vertical=True),
    node("completionPanel", "components.RoundedPanel", (22, 386, 287, 68), bg="#E8F5E9", shadow=False, children=[
        label("completionTitle", "Estimated Completion", (16, 10, 253, 20), 12, True, "#145A32"),
        label("completionValue", "Place an order to get started", (16, 34, 253, 21), 13, fg="#145A32"),
    ]),
])
recent = node("recentPanel", "components.RoundedPanel", (248, 494, 656, 220), children=[
    label("recentTitle", "Your Recent Orders", (23, 17, 340, 26), 18, True),
    button("viewOrdersButton", "View All \u2192", (512, 12, 119, 36), "history"),
    node("ordersScroll", "javax.swing.JScrollPane", (20, 58, 616, 140), children=[
        node("ordersTable", "javax.swing.JTable", (0, 0, 0, 0)),
    ]),
])
thanks = node("thanksPanel", "components.RoundedPanel", (924, 690, 332, 44), bg="#1E8E3E", shadow=False, children=[
    label("thanksLabel", "Thank you for choosing Metric Laundry!", (15, 9, 308, 26), 12, True, "#FFFFFF"),
])
main = node("mainPanel", "javax.swing.JPanel", (0, 0, 1280, 760), bg="#F5F8F5", children=[
    sidebar, banner, services, tracking, recent, thanks,
    label("loadStatus", "Loading your services and orders...", (252, 725, 855, 22), 12, fg="#6B6B6B"),
    button("refreshButton", "Refresh", (1135, 736, 116, 24), "home"),
])


def walk(n):
    yield n
    for child in n.get("children", []):
        yield from walk(child)


nodes = list(walk(main))


def color(value):
    return "new java.awt.Color(0x" + value[1:] + ")"


def jstr(value):
    return json.dumps(value, ensure_ascii=True)


init = ["        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);",
        '        setTitle("Metric Laundry - Customer Dashboard");', "        setResizable(false);"]
for n in nodes:
    name, cls = n["name"], n["cls"]
    init.append(f"        {name} = new {cls}();")
    if cls.endswith("Panel"):
        init.append(f"        {name}.setLayout(null);")
    if "bg" in n: init.append(f"        {name}.setBackground({color(n['bg'])});")
    if "fg" in n: init.append(f"        {name}.setForeground({color(n['fg'])});")
    if "font" in n:
        size, bold = n["font"]
        init.append(f'        {name}.setFont(new java.awt.Font("Segoe UI", {1 if bold else 0}, {size}));')
    if "text" in n: init.append(f"        {name}.setText({jstr(n['text'])});")
    if n.get("center"): init.append(f"        {name}.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);")
    if "image" in n:
        init.append(f'        {name}.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/images/{n["image"]}")));')
    if "gradient" in n: init.append(f"        {name}.setGradientEnd({color(n['gradient'])});")
    if n.get("shadow") is False: init.append(f"        {name}.setShadow(false);")
    if n.get("vertical"): init.append(f"        {name}.setVertical(true);")
    if "event" in n:
        init.extend([f"        {name}.addActionListener(new java.awt.event.ActionListener() {{",
                     "            public void actionPerformed(java.awt.event.ActionEvent evt) {",
                     f"                {name}ActionPerformed(evt);", "            }", "        });"])


def attach(n):
    name = n["name"]
    for child in n.get("children", []):
        attach(child)
        cn = child["name"]
        if n["cls"].endswith("JScrollPane"):
            init.append(f"        {name}.setViewportView({cn});")
        else:
            init.append(f"        {name}.add({cn});")
            init.append(f"        {cn}.setBounds({', '.join(map(str, child['bounds']))});")


attach(main)
init += ["        mainPanel.setPreferredSize(new java.awt.Dimension(1280, 760));",
         "        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);", "        pack();",
         "        setLocationRelativeTo(null);"]
events = []
for n in nodes:
    if "event" in n:
        name = n["name"]
        events += [f"    private void {name}ActionPerformed(java.awt.event.ActionEvent evt) {{//GEN-FIRST:event_{name}ActionPerformed",
                   f'        navigate("{n["event"]}");', f"    }}//GEN-LAST:event_{name}ActionPerformed", ""]

behavior = (ROOT / "tools/CustomerDashboard.behavior").read_text(encoding="utf-8")
java = """package view.customer;

/** Customer home. Layout is paired with CustomerDashboard.form for NetBeans Design. */
public class CustomerDashboard extends javax.swing.JFrame {
""" + behavior + """
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
""" + "\n".join(init) + """
    }// </editor-fold>//GEN-END:initComponents

""" + "\n".join(events) + "\n    // Variables declaration - do not modify//GEN-BEGIN:variables\n" + "\n".join(
    f"    private {n['cls']} {n['name']};" for n in nodes) + "\n    // End of variables declaration//GEN-END:variables\n}\n"
(DEST / "CustomerDashboard.java").write_text(java, encoding="utf-8")


def prop(name, typ, value):
    return f'<Property name="{name}" type="{typ}" value={quoteattr(str(value))}/>'


def xml_node(n, root=False, viewport=False):
    container = "children" in n or n["cls"].endswith("Panel")
    tag = "Container" if container else "Component"
    parts = [f'<{tag} class="{n["cls"]}" name="{n["name"]}">', "<Properties>"]
    for key, xmlname in [("bg", "background"), ("fg", "foreground"), ("gradient", "gradientEnd")]:
        if key in n:
            hexval = n[key][1:]
            parts.append(f'<Property name="{xmlname}" type="java.awt.Color" editor="org.netbeans.beaninfo.editors.ColorEditor"><Color blue="{hexval[4:6]}" green="{hexval[2:4]}" red="{hexval[0:2]}" type="rgb"/></Property>')
    if "font" in n:
        size, bold = n["font"]
        parts.append(f'<Property name="font" type="java.awt.Font" editor="org.netbeans.beaninfo.editors.FontEditor"><Font name="Segoe UI" size="{size}" style="{1 if bold else 0}"/></Property>')
    if "text" in n: parts.append(prop("text", "java.lang.String", n["text"]))
    if n.get("center"): parts.append(prop("horizontalAlignment", "int", 0))
    if "image" in n:
        parts.append(f'<Property name="icon" type="javax.swing.Icon" editor="org.netbeans.modules.form.editors2.IconEditor"><Image iconType="3" name="/resources/images/{n["image"]}"/></Property>')
    if n.get("shadow") is False: parts.append(prop("shadow", "boolean", "false"))
    if n.get("vertical"): parts.append(prop("vertical", "boolean", "true"))
    if root:
        parts.append('<Property name="preferredSize" type="java.awt.Dimension" editor="org.netbeans.beaninfo.editors.DimensionEditor"><Dimension value="[1280, 760]"/></Property>')
    parts.append("</Properties>")
    if "event" in n:
        parts.append(f'<Events><EventHandler event="actionPerformed" listener="java.awt.event.ActionListener" parameters="java.awt.event.ActionEvent" handler="{n["name"]}ActionPerformed"/></Events>')
    if not viewport:
        if root:
            parts.append('<Constraints><Constraint layoutClass="org.netbeans.modules.form.compat2.layouts.DesignBorderLayout" value="org.netbeans.modules.form.compat2.layouts.DesignBorderLayout$BorderConstraintsDescription"><BorderConstraints direction="Center"/></Constraint></Constraints>')
        else:
            x,y,w,h = n["bounds"]
            parts.append(f'<Constraints><Constraint layoutClass="org.netbeans.modules.form.compat2.layouts.DesignAbsoluteLayout" value="org.netbeans.modules.form.compat2.layouts.DesignAbsoluteLayout$AbsoluteConstraintsDescription"><AbsoluteConstraints x="{x}" y="{y}" width="{w}" height="{h}"/></Constraint></Constraints>')
    if container:
        if n["cls"].endswith("JScrollPane"):
            parts.append('<Layout class="org.netbeans.modules.form.compat2.layouts.support.JScrollPaneSupportLayout"/>')
        else:
            parts.append('<Layout class="org.netbeans.modules.form.compat2.layouts.DesignAbsoluteLayout"><Property name="useNullLayout" type="boolean" value="true"/></Layout>')
        if n.get("children"):
            parts.append("<SubComponents>")
            parts += [xml_node(child, viewport=n["cls"].endswith("JScrollPane")) for child in n["children"]]
            parts.append("</SubComponents>")
    parts.append(f"</{tag}>")
    return "\n".join(parts)


form = '''<?xml version="1.0" encoding="UTF-8"?>
<Form version="1.9" maxVersion="1.9" type="org.netbeans.modules.form.forminfo.JFrameFormInfo">
<Properties>
<Property name="defaultCloseOperation" type="int" value="3"/>
<Property name="title" type="java.lang.String" value="Metric Laundry - Customer Dashboard"/>
<Property name="resizable" type="boolean" value="false"/>
</Properties>
<SyntheticProperties>
<SyntheticProperty name="formSizePolicy" type="int" value="1"/>
<SyntheticProperty name="generateCenter" type="boolean" value="true"/>
</SyntheticProperties>
<AuxValues>
<AuxValue name="FormSettings_autoResourcing" type="java.lang.Integer" value="0"/>
<AuxValue name="FormSettings_generateFQN" type="java.lang.Boolean" value="true"/>
<AuxValue name="FormSettings_layoutCodeTarget" type="java.lang.Integer" value="1"/>
<AuxValue name="FormSettings_listenerGenerationStyle" type="java.lang.Integer" value="0"/>
<AuxValue name="FormSettings_variablesLocal" type="java.lang.Boolean" value="false"/>
<AuxValue name="FormSettings_variablesModifier" type="java.lang.Integer" value="2"/>
</AuxValues>
<Layout class="org.netbeans.modules.form.compat2.layouts.DesignBorderLayout"/>
<SubComponents>
''' + xml_node(main, root=True) + "\n</SubComponents>\n</Form>\n"
from xml.dom import minidom
(DEST / "CustomerDashboard.form").write_text(minidom.parseString(form).toprettyxml(indent="  "), encoding="utf-8")
print(f"Generated matching Java + form with {len(nodes)} components")
