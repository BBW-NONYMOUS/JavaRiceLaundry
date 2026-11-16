package laundry;

import components.Theme;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.time.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Read-only screen rendering. In-memory order fixtures never enter MySQL. */
public final class ScreenThemeChecks {
    private static void setUser(String role) throws Exception {
        Field f = Session.class.getDeclaredField("currentUser"); f.setAccessible(true);
        f.set(null, new User(1, "Preview Customer", "preview@example.com", "09123456789", role));
    }
    private static void render(JFrame frame, String name) throws Exception {
        if (frame.getContentPane().getPreferredSize().width != 1280 || frame.isResizable())
            throw new AssertionError(name + " must use the fixed Home canvas");
        frame.addNotify(); frame.validate();
        BufferedImage image = new BufferedImage(1280,760,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics(); frame.getContentPane().printAll(g); g.dispose();
        ImageIO.write(image,"png",Path.of("target/review/"+name+".png").toFile());
        frame.dispose();
    }
    private static Order fixture() {
        Order o=new Order();o.id=12;o.customerName="Preview Customer";o.customerEmail="preview@example.com";
        o.customerPhone="09123456789";o.service="Wash and Fold";o.icon="clothes.png";
        o.weightKg=5;o.pricePerKg=25;o.total=125;o.createdAt=LocalDateTime.of(2026,11,15,8,30);
        o.expectedCompletion=LocalDate.of(2026,11,16);o.paymentMethod=Order.CASH;
        o.paymentStatus=Order.UNPAID;o.status=Order.WASHING;o.staffNote="Handle whites separately.";
        o.statusTimes[0]=o.createdAt;o.statusTimes[1]=o.createdAt.plusHours(1);return o;
    }
    public static void main(String[] args) throws Exception {
        Theme.install();Files.createDirectories(Path.of("target/review"));
        SwingUtilities.invokeAndWait(() -> {
            try {
                setUser(User.CUSTOMER);
                render(new LoginForm(),"login");render(new RegisterForm(),"register");
                Session.setDraftWeight(5);
                render(new SelectServiceForm(),"select-service");render(new WeightForm(),"weight");
                render(new OrderSummaryForm(),"order-summary");render(new PaymentForm(),"payment");
                MyOrdersForm tracking=new MyOrdersForm();
                Field orders=MyOrdersForm.class.getDeclaredField("orders");orders.setAccessible(true);orders.set(tracking,List.of(fixture()));
                Field combo=MyOrdersForm.class.getDeclaredField("orderCombo");combo.setAccessible(true);
                ((JComboBox<?>)combo.get(tracking)).setSelectedIndex(-1);
                @SuppressWarnings("unchecked") JComboBox<String> picker=(JComboBox<String>)combo.get(tracking);
                picker.setModel(new DefaultComboBoxModel<>(new String[]{"Order #012"}));
                picker.setSelectedIndex(0);
                Field card=MyOrdersForm.class.getDeclaredField("orderCard");card.setAccessible(true);((JPanel)card.get(tracking)).setVisible(true);
                for(String name:new String[]{"emptyLabel","emptyButton","orderPickerLabel","orderCombo"}) {
                    Field field=MyOrdersForm.class.getDeclaredField(name);field.setAccessible(true);
                    ((JComponent)field.get(tracking)).setVisible(!name.startsWith("empty"));
                }
                render(tracking,"tracking");render(new ProfileForm(),"profile");
                setUser(User.ADMIN);
                render(new StaffDashboardForm(),"staff-dashboard");render(new StaffOrdersForm(),"manage-orders");
                render(new StaffServicesForm(),"manage-services");render(new StaffReportForm(),"reports");
                setUser(User.STAFF);
                StaffOrdersForm staff=new StaffOrdersForm();
                for(String name:new String[]{"staffServicesNav","staffReportNav"}) {
                    Field field=StaffOrdersForm.class.getDeclaredField(name);field.setAccessible(true);
                    if(((JComponent)field.get(staff)).isVisible())throw new AssertionError("Staff-only restrictions lost");
                }
                staff.dispose();Session.logout();
                System.out.println("PASS: all 12 restyled forms constructed and rendered; staff navigation restrictions preserved.");
            } catch(Exception e) { throw new RuntimeException(e); }
        });
    }
}
