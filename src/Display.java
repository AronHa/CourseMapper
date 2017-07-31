import java.awt.Canvas;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Creates the user interface using JSwing
 * @author Aron Harder
 * @since 2017-02-18
 */
public class Display extends Thread implements MouseListener {
	final static int MAX_HOURS = 18; //The max number of credits per semester
	final static int MIN_HOURS = 12; //The minimum number of credits per semester
	final static int NUM_SEMESTERS = 8; //The number of semesters in the schedule

	private JFrame frame; //The frame for the main canvas
	private JFrame frame2; //The frame for the popup window that gets user input
	private Canvas canvas; //The canvas on which everything is painted
	private BufferStrategy strategy; //The strategy to update the graphics
	private Graphics graphics; //The graphics
	private JMenuBar menubar; //The top menu
	
	private Mapper m = new Mapper(); //Used to create schedule mappings
	private ArrayList<Course> courses; //A list of the courses
	private Course[][] mapping; //The mapping from which the schedule is created
	private Group[] groups; //The course groups
	private int group_index; //An index of where to add the next group
	private ArrayList<Requirement> reqs; //A list of the requirements
	private ArrayList<Course> no_semester = new ArrayList<Course>(); //A list of courses that are not in any semester
	private int[] reqs_display; //Which requirements to display
	
	private Hashtable<String,Integer> group_table = new Hashtable<String,Integer>(); //Keeps track of group information
	private Color[] colors = { //Proposed group colors
			new Color(255,128,128), //Red
			new Color(255,255,128), //Yellow
			new Color(128,255,128), //Green
			new Color(128,255,255), //Cyan
			new Color(128,128,255), //Blue
			new Color(255,128,255), //Magenta
			new Color(170,170,170), //Grey
			new Color(255,255,255)  //White
			};
	private Color elective_color = new Color(255,192,128); //The initial color of electives

	private static final int GRID_WIDTH = 800; //GRID_WIDTH of the window
	private static final int GRID_HEIGHT = 900; //Height of the window
	private static final int BARSIZE = 22; //Height of the bar at the top of the window
	private static final int MENUSIZE = 22; //Height of the top menu
	private static final int INFO_HEIGHT = 60; //Height of the top information
	private static final int LABEL_WIDTH = 200; //Width of the information in the side bar
	private static String FONT = "Helvetica"; //The font to use
	
	private Course info_course; //The course that the user most recently clicked on
	private Requirement info_req; //The requirement that the user most recently clicked on
	private String io_action = ""; //What input/output action to perform
	private boolean saving = false; //Whether we are currently exporting to PNG
	private int[] button_coords = {0,0,0,0}; //The coordinates of the "Edit Course/Requirement" button
	private Course drag_course; //Which course is being dragged
	private int[] drag_index = {-1,-1}; //The original place in the schedule for the course being dragged
	private int start_no_sem = 0; //The y-coordinate to start listing the no-semester courses
	
	private boolean isRunning; //Whether the program is running
	private boolean draw_graphics; //Whether to draw the schedule
	
	public Display(){
		courses = new ArrayList<Course>(); //Initialize the course arraylist
		
		reqs = new ArrayList<Requirement>(); //Initialize the requirement arraylist
		reqs.add(new Requirement("Add Requirement")); //Add the button that allows the user to add a requirement

		if (reqs.size() > 18){ //If there are more requirements than fit on 1 page, split it up
			reqs_display = new int[]{0,16};
		} else {
			reqs_display = new int[]{0,reqs.size()-1};
		}
		groups = new Group[7]; //Initialize the groups
		group_index = 0;
		for (Course c : courses){ //Find group names based on the initial courses
			if (group_table.get(c.get_group()) == null){
				groups[group_index] = new Group(c.get_group(),colors[group_index+1]);
				group_table.put(c.get_group(),group_index);
				group_table.put(String.valueOf(group_index), 0);
				group_index++;
			}
		}
		for (Course c : courses){ //Add the courses to the groups
			int gp = group_table.get(c.get_group());
			groups[gp].add_course(c);
		}
		if (courses.isEmpty()){ //If there are no courses, make one initial group
			groups[0] = new Group("Core",colors[1]);
			group_table.put("Core", group_index);
			group_table.put(String.valueOf(group_index), 0);
			group_index++;
		}
		
		update_schedule(); //Create an initial mapping
		
		draw_graphics = false; //false => Start in groups view; true => Start in schedule view
		
		frame = new JFrame("Course Mapper"); //Create a new JFrame
		frame.addWindowListener(new FrameClose()); //Listen for the window closing
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); //Don't do anything special when the frame closes
		frame.setVisible(true); //Show the frame
		frame.setResizable(false); //Don't let the user resize the window
		frame.setSize(GRID_WIDTH+LABEL_WIDTH,GRID_HEIGHT+INFO_HEIGHT+BARSIZE+MENUSIZE); //Set the frame's size
		
		ActionListener listener = new ActionListener(){ //The actions to perform when a menu item is clicked on 
			public void actionPerformed(ActionEvent evt){
				if (evt.getActionCommand().equals("Quit")){ //Quit
					System.exit(0);
				} else if (evt.getActionCommand().equals("Update Schedule")){ //Update Schedule
					create_update_warning();
				} else if (evt.getActionCommand().equals("View Schedule")){ //Switch views
					if (mapping != null){
						draw_graphics = true;
					}
				} else if (evt.getActionCommand().equals("View Groups")){ //Switch views
					draw_graphics = false;
				} else if (evt.getActionCommand().equals("Export to PNG")){ //Save image
					if (mapping != null){
						saving = true; //We are currently exporting the image
						boolean temp = draw_graphics; //Save the program state
						draw_graphics = true;
						BufferedImage image = new BufferedImage(GRID_WIDTH,GRID_HEIGHT+INFO_HEIGHT,BufferedImage.TYPE_INT_BGR); //The image to save
						Graphics2D g2 = (Graphics2D) image.createGraphics(); //Create a graphics object to draw with
						renderGraphics(g2); //Puts the graphics into the image
						g2.dispose();
						try {
							ImageIO.write(image,"png",new File("Schedule.png")); //Export the image
						} catch (IOException e){
							e.printStackTrace();
						}
						draw_graphics = temp; //Restore the program state
						saving = false; //We are no longer exporting the image
					}
				} else if (evt.getActionCommand().equals("Save")){ //Save all
					io_action = "Export all";
					find_file();
				} else if (evt.getActionCommand().equals("Load")){ //Load all
					io_action = "Import all";
					find_file();
				} else if (evt.getActionCommand().equals("New Requirement")){ //Create new requirement
					edit_requirement(new Requirement(), true);
				} else if (evt.getActionCommand().equals("New Course")){ //Create new course
					edit_course(new Course(),true);
				} else if (evt.getActionCommand().equals("New Group")){ //Create new group
					if (groups[6] == null){ //groups isn't filled
						edit_group(new Group(), true);
					}
				} else if (evt.getActionCommand().equals("About")){ //Create the about page
					create_about_page();
				} else if (evt.getActionCommand().equals("Requirements")){
					JMenuItem caller = (JMenuItem) evt.getSource(); //Find which "requirements" menu item was clicked
					JPopupMenu popup = (JPopupMenu) caller.getParent();
					JMenu menu = (JMenu) popup.getInvoker();
					if (menu.getText().equals("Export")){ //Save requirements
						io_action = "Export reqs";
						find_file();
					} else if (menu.getText().equals("Import")){ //Load requirements
						io_action = "Import reqs";
						find_file();
					} else if (menu.getText().equals("Help")){ //Requirements help page
						create_help_page("help/reqs_help.png");
					}
				} else if (evt.getActionCommand().equals("Courses")){
					JMenuItem caller = (JMenuItem) evt.getSource(); //Find which "courses" menu item was clicked
					JPopupMenu popup = (JPopupMenu) caller.getParent();
					JMenu menu = (JMenu) popup.getInvoker();
					if (menu.getText().equals("Export")){ //Save courses
						io_action = "Export courses";
						find_file();
					} else if (menu.getText().equals("Import")){ //Load courses
						io_action = "Import courses";
						find_file();
					} else if (menu.getText().equals("Help")){ //Courses help page
						create_help_page("help/course_help.png");
					}
				} else if (evt.getActionCommand().equals("Schedule")){
					JMenuItem caller = (JMenuItem) evt.getSource(); //Find which "schedule" menu item was clicked
					JPopupMenu popup = (JPopupMenu) caller.getParent();
					JMenu menu = (JMenu) popup.getInvoker();
					if (menu.getText().equals("Export")){ //Save schedule
						io_action = "Export mapping";
						find_file();
					} else if (menu.getText().equals("Import")){ //Load schedule
						io_action = "Import mapping";
						find_file();
					} else if (menu.getText().equals("Help")){ //Schedule help page
						create_help_page("help/schedule_help.png");
					}
				} else if (evt.getActionCommand().equals("Groups")){ //Groups help page
					create_help_page("help/groups_help.png");
				}
			}
		};
		
		menubar = new JMenuBar(); //Create a menu bar
		JMenu file = new JMenu("File"); //Create a file menu
		menubar.add(file);
		JMenuItem update = new JMenuItem("Update Schedule"); //Add these items to the file menu
		JMenuItem export_image = new JMenuItem("Export to PNG");
		JMenuItem save = new JMenuItem("Save");
		JMenu export = new JMenu("Export");
		JMenuItem ex_reqs = new JMenuItem("Requirements");
		JMenuItem ex_courses = new JMenuItem("Courses");
		JMenuItem ex_schedule = new JMenuItem("Schedule");
		JMenuItem load = new JMenuItem("Load");
		JMenu open = new JMenu("Import");
		JMenuItem im_reqs = new JMenuItem("Requirements");
		JMenuItem im_courses = new JMenuItem("Courses");
		JMenuItem im_schedule = new JMenuItem("Schedule");
		JMenuItem tab1 = new JMenuItem("View Schedule");
		JMenuItem tab2 = new JMenuItem("View Groups");
		JMenuItem quit = new JMenuItem("Quit");
		file.add(update);
		file.add(export_image);
		file.add(save);
		file.add(export);
		export.add(ex_reqs);
		export.add(ex_courses);
		export.add(ex_schedule);
		file.add(load);
		file.add(open);
		open.add(im_reqs);
		open.add(im_courses);
		open.add(im_schedule);
		file.addSeparator();
		file.add(tab1);
		file.add(tab2);
		file.addSeparator();
		file.add(quit);
		JMenu edit = new JMenu("Edit"); //Create an edit menu
		menubar.add(edit);
		JMenuItem add_r = new JMenuItem("New Requirement"); //Add these items to the edit menu
		JMenuItem add_c = new JMenuItem("New Course");
		JMenuItem add_g = new JMenuItem("New Group");
		edit.add(add_r);
		edit.add(add_c);
		edit.add(add_g);
		JMenu help = new JMenu("Help"); //Create a help menu
		menubar.add(help);
		JMenuItem help_r = new JMenuItem("Requirements"); //Add these items to the help menu
		JMenuItem help_c = new JMenuItem("Courses");
		JMenuItem help_g = new JMenuItem("Groups");
		JMenuItem help_s = new JMenuItem("Schedule");
		JMenuItem about = new JMenuItem("About");
		help.add(help_r);
		help.add(help_c);
		help.add(help_g);
		help.add(help_s);
		help.addSeparator();
		help.add(about);
		update.addActionListener(listener); //Make all the menu items know when they're clicked on
		export_image.addActionListener(listener);
		save.addActionListener(listener);
		ex_reqs.addActionListener(listener);
		ex_courses.addActionListener(listener);
		ex_schedule.addActionListener(listener);
		load.addActionListener(listener);
		im_reqs.addActionListener(listener);
		im_courses.addActionListener(listener);
		im_schedule.addActionListener(listener);
		tab1.addActionListener(listener);
		tab2.addActionListener(listener);
		quit.addActionListener(listener);
		add_r.addActionListener(listener);
		add_c.addActionListener(listener);
		add_g.addActionListener(listener);
		help_r.addActionListener(listener);
		help_c.addActionListener(listener);
		help_g.addActionListener(listener);
		help_s.addActionListener(listener);
		about.addActionListener(listener);
		
		frame.setJMenuBar(menubar); //Add the menubar to the frame
		
		canvas = new Canvas(); //Create the canvas for drawing on
		canvas.setSize(GRID_WIDTH+LABEL_WIDTH,GRID_HEIGHT+INFO_HEIGHT); //Set the canvas's size
		
		frame.add(canvas); //Add the canvas to the frame
		canvas.addMouseListener(this); //Make the canvas listen for clicks
		canvas.createBufferStrategy(2); //Create a buffer strategy
		do {
			strategy = canvas.getBufferStrategy();
		} while (strategy == null);
		
		frame.addWindowFocusListener(new WindowFocusListener(){ //If you click away from the popup window, get rid of it
			public void windowGainedFocus(WindowEvent evt){
				if (frame2 != null){
					frame2.dispose();
				}
			}
			public void windowLostFocus(WindowEvent evt){
			}
		});
		
		frame.setVisible(true); //This has to be at the end or the menubar may not appear
		isRunning = true; //Allow the graphics to start running
	}
	
	/**
	 * A class to close the window
	 */
	public class FrameClose extends WindowAdapter{
		@Override
		public void windowClosing(final WindowEvent e){ //This allows us to do things when the window closes
			System.exit(0);
		}
	}
	
	/**
	 * Returns the graphics from the buffer
	 * @return graphics
	 */
	public Graphics getBuffer(){
		if (graphics == null){
			try{
				graphics = strategy.getDrawGraphics();
			} catch(IllegalStateException e){
				return null;
			}
		}
		return graphics;
	}
	
	/**
	 * Updates the screen to create changes
	 * @return true if the screen content was lost
	 */
	private boolean updateScreen(){
		graphics.dispose(); //Get rid of the old graphics frame
		graphics = null;
		try {
			strategy.show(); //Show the next frame
			Toolkit.getDefaultToolkit().sync();
			return (!strategy.contentsLost());
		} catch (NullPointerException e){
			return true;
		} catch (IllegalStateException e){
			return true;
		}
	}
	
	/**
	 * Runs the graphics, updating with 34 fps
	 */
	public void run(){
		long fpsWait = (long) (1.0/30 * 1000);
		main: while(isRunning){
			long renderStart = System.nanoTime(); //We want to time how long this takes
			updateGraphics();
			
			do {
				Graphics bg = getBuffer(); //Get the graphics
				if (!isRunning){
					break main; //exit if the program is no longer running
				}
				if (!saving){
					boolean concurrent = true;
					while (concurrent == true){ //This prevents the program from crashing due to a concurrency error
						try{
							renderGraphics(bg); //draw the next set of graphics
							bg.dispose();
							concurrent = false;
						} catch (ConcurrentModificationException e){
							//This should be enough to prevent crashes from concurrency exceptions
							System.out.println("Concurrency Exception");
						}
					}

				}
			} while (!updateScreen()); //until the screen gets updated
			
			long renderTime = (System.nanoTime() - renderStart) / 1000000;
			try {
				Thread.sleep(Math.max(0, fpsWait - renderTime)); //Try to sleep until it's time to try drawing the next frame
			} catch (InterruptedException e){
				Thread.interrupted();
				break;
			}
			renderTime = (System.nanoTime() - renderStart) / 1000000;
		}
		frame.dispose(); //Get ride of the old frame
	}
	
	/**
	 * Draws on the canvas
	 * @param g
	 */
	public void renderGraphics(Graphics g){
		//This is where you draw the graphics
		
		//Draw the semester information
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight()); //Start with a blank canvas
		
		int height = 25;
		int offset = 5;

		if (draw_graphics == true){ //Schedule page
			//Draw the information along the top
			g.setColor(Color.BLACK);
			g.setFont(new Font(FONT,Font.PLAIN,24));
			g.drawString("First Year", 5, 25);
			g.drawLine(GRID_WIDTH/4, 0, GRID_WIDTH/4, INFO_HEIGHT);
			g.drawString("Second Year", GRID_WIDTH/4+5, 25);
			g.drawLine(2*GRID_WIDTH/4, 0, 2*GRID_WIDTH/4, INFO_HEIGHT);
			g.drawString("Third Year", GRID_WIDTH/2+5, 25);
			g.drawLine(3*GRID_WIDTH/4, 0, 3*GRID_WIDTH/4, INFO_HEIGHT);
			g.drawString("Fourth Year", 3*GRID_WIDTH/4+5, 25);
			g.drawLine(GRID_WIDTH, 0, GRID_WIDTH, INFO_HEIGHT);
			g.setFont(new Font(FONT,Font.PLAIN,14));
			int startYear = mapping[0][0].get_year();
			g.drawString("Fall "+startYear, 5, INFO_HEIGHT-5);
			g.drawString("Spring "+(startYear+1), GRID_WIDTH/8+5, INFO_HEIGHT-5);
			g.drawString("Fall "+(startYear+1), 2*GRID_WIDTH/8+5, INFO_HEIGHT-5);
			g.drawString("Spring "+(startYear+2), 3*GRID_WIDTH/8+5, INFO_HEIGHT-5);
			g.drawString("Fall "+(startYear+2), 4*GRID_WIDTH/8+5, INFO_HEIGHT-5);
			g.drawString("Spring "+(startYear+3), 5*GRID_WIDTH/8+5, INFO_HEIGHT-5);
			g.drawString("Fall "+(startYear+3), 6*GRID_WIDTH/8+5, INFO_HEIGHT-5);
			g.drawString("Spring "+(startYear+4), 7*GRID_WIDTH/8+5, INFO_HEIGHT-5);
			g.drawLine(0, INFO_HEIGHT-height, GRID_WIDTH, INFO_HEIGHT-height);
			g.setFont(new Font(FONT,Font.PLAIN,12));
			
			//Draw the grid
			g.setColor(Color.BLACK);
			for (int i = 1; i <= NUM_SEMESTERS; i++){ //Vertical lines
				g.drawLine(i*(GRID_WIDTH/NUM_SEMESTERS), INFO_HEIGHT-height, i*(GRID_WIDTH/NUM_SEMESTERS), GRID_HEIGHT+INFO_HEIGHT);
			}
			for (int i = 0; i < MAX_HOURS; i++){ //Horizontal lines
				if (i == 16){
					g.fillRect(0, i*(GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT-2, GRID_WIDTH, 4);
				} else {
					g.drawLine(0, i*(GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT, GRID_WIDTH, i*(GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT);
				}
			}
			
			//Draw the courses
			for (int i = 0; i < NUM_SEMESTERS; i++){
				System.out.println(mapping[i].length);
				int hours = 0;
				for (int j = 0; j < MAX_HOURS && mapping[i][j] != null; j++){
					if (drag_course == mapping[i][j]){
						hours+=mapping[i][j].get_sh();
						continue;
					}
					Color c;
					if (mapping[i][j].get_group().equals("Elective")){
						c = elective_color;
					} else {
						c = groups[group_table.get(mapping[i][j].get_group())].get_color();
					}
					//x = i*(GRID_WIDTH/NUM_SEMESTERS);
					//y = hours*(GRID_HEIGHT/MAX_HOURS);
					g.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),230)); //Make the color slightly translucent
					g.fillRoundRect(i*(GRID_WIDTH/NUM_SEMESTERS)+offset, hours*(GRID_HEIGHT/MAX_HOURS)+offset+INFO_HEIGHT,
							(GRID_WIDTH/NUM_SEMESTERS)-2*offset, (GRID_HEIGHT/MAX_HOURS)*(mapping[i][j].get_sh())-2*offset,5,5);
					if (mapping[i][j].should_warn() == true){
						g.setColor(Color.RED);
					} else {
						g.setColor(Color.BLACK);
					}
					g.drawRoundRect(i*(GRID_WIDTH/NUM_SEMESTERS)+offset, hours*(GRID_HEIGHT/MAX_HOURS)+offset+INFO_HEIGHT,
							(GRID_WIDTH/NUM_SEMESTERS)-2*offset, (GRID_HEIGHT/MAX_HOURS)*(mapping[i][j].get_sh())-2*offset,5,5);
					String name = mapping[i][j].get_name()+" ("+mapping[i][j].get_sh()+")";
					draw_wrapped_string(g,name,i*(GRID_WIDTH/NUM_SEMESTERS),(int) ((hours+0.5)*GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT,offset,
							(GRID_WIDTH/NUM_SEMESTERS)-2*offset-4,(int) ((GRID_HEIGHT/MAX_HOURS)*(mapping[i][j].get_sh()-0.5))-offset);
					hours+=mapping[i][j].get_sh();
				}
			}
		} else { //Groups page
			g.setColor(Color.BLACK);
			g.drawLine(GRID_WIDTH, 0, GRID_WIDTH, GRID_HEIGHT+INFO_HEIGHT); //Separate the info box from the groups
			//Display the Requirements
			//Draw the top information
			g.setColor(Color.BLACK);
			g.fillRect(GRID_WIDTH/NUM_SEMESTERS-1, 0, 3, GRID_HEIGHT+INFO_HEIGHT);
			g.setFont(new Font(FONT,Font.PLAIN,24));
			g.drawString("Reqs", 5, 37);
			g.setFont(new Font(FONT,Font.PLAIN,12));
			if (reqs_display[0] != 0){
				//Draw the previous button if it's needed
				g.setColor(colors[0]);
				g.fillPolygon(new int[]{2*offset,(int) (0.5*(GRID_WIDTH/NUM_SEMESTERS)),(GRID_WIDTH/NUM_SEMESTERS)-2*offset},
						new int[]{(GRID_HEIGHT/MAX_HOURS)-2*offset+INFO_HEIGHT-10,2*offset+INFO_HEIGHT,(GRID_HEIGHT/MAX_HOURS)-2*offset+INFO_HEIGHT-10},3);
				g.setColor(Color.BLACK);
				g.drawPolygon(new int[]{2*offset,(int) (0.5*(GRID_WIDTH/NUM_SEMESTERS)),(GRID_WIDTH/NUM_SEMESTERS)-2*offset},
						new int[]{(GRID_HEIGHT/MAX_HOURS)-2*offset+INFO_HEIGHT-10,2*offset+INFO_HEIGHT,(GRID_HEIGHT/MAX_HOURS)-2*offset+INFO_HEIGHT-10},3);
				g.drawString("Previous", (GRID_WIDTH/NUM_SEMESTERS)/2-24, (GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT+45);
				g.drawLine(0, INFO_HEIGHT,(GRID_WIDTH/NUM_SEMESTERS), INFO_HEIGHT);
			}
			int init_j = reqs_display[0]-reqs_display[0]%16; //Helps determine where to draw each requirement
			for (int j = reqs_display[0]; j <= reqs_display[1]; j++){
				//Draw the requirements
				g.setColor(colors[0]);
				g.fillRect(offset, (j-init_j)*(GRID_HEIGHT/MAX_HOURS)+offset+INFO_HEIGHT,
						(GRID_WIDTH/NUM_SEMESTERS)-2*offset, (GRID_HEIGHT/MAX_HOURS)-2*offset);
				g.setColor(Color.BLACK);
				g.drawRect(offset, (j-init_j)*(GRID_HEIGHT/MAX_HOURS)+offset+INFO_HEIGHT,
						(GRID_WIDTH/NUM_SEMESTERS)-2*offset, (GRID_HEIGHT/MAX_HOURS)-2*offset);
				g.drawLine(0, (j-init_j)*(GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT,
						(GRID_WIDTH/NUM_SEMESTERS), (j-init_j)*(GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT);

				//Draw the requirement "name"
				String name = "";
				for (int k = 0; k < reqs.get(j).get_courses().length; k++){
					if (k != 0){
						name+=", ";
					}
					name+=reqs.get(j).get_courses()[k];
				}
				draw_wrapped_string(g,name,0,(int) (((j-init_j)+0.5)*(GRID_HEIGHT/MAX_HOURS))+INFO_HEIGHT,offset,
						(GRID_WIDTH/NUM_SEMESTERS)-2*offset-4,(int) (0.5*(GRID_HEIGHT/MAX_HOURS))-offset);
			}
			if (reqs_display[1] != reqs.size()-1){
				//Draw the next button if it's needed
				g.setColor(colors[0]);
				g.fillPolygon(new int[]{2*offset,(int) (0.5*(GRID_WIDTH/NUM_SEMESTERS)),(GRID_WIDTH/NUM_SEMESTERS)-2*offset},
						new int[]{17*(GRID_HEIGHT/MAX_HOURS)+2*offset+INFO_HEIGHT+10,18*(GRID_HEIGHT/MAX_HOURS)-2*offset+INFO_HEIGHT,17*(GRID_HEIGHT/MAX_HOURS)+2*offset+INFO_HEIGHT+10},3);
				g.setColor(Color.BLACK);
				g.drawPolygon(new int[]{2*offset,(int) (0.5*(GRID_WIDTH/NUM_SEMESTERS)),(GRID_WIDTH/NUM_SEMESTERS)-2*offset},
						new int[]{17*(GRID_HEIGHT/MAX_HOURS)+2*offset+INFO_HEIGHT+10,18*(GRID_HEIGHT/MAX_HOURS)-2*offset+INFO_HEIGHT,17*(GRID_HEIGHT/MAX_HOURS)+2*offset+INFO_HEIGHT+10},3);
				g.drawString("Next", (int) (0.5*(GRID_WIDTH/NUM_SEMESTERS))-12, 18*(GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT-35);
				g.drawLine(0, 17*(GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT,
						(GRID_WIDTH/NUM_SEMESTERS), 17*(GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT);
			}
			//Display the Groups
			int last_group = 0;
			for (int i = 0; i < groups.length && groups[i] != null; i++){
				int x = i+1; //Since requirements are in the 0 position, we use x to shift the group to the right one place
				//Draw the top information
				g.setColor(Color.BLACK);
				g.drawLine((x+1)*(GRID_WIDTH/NUM_SEMESTERS), 0, (x+1)*(GRID_WIDTH/NUM_SEMESTERS), GRID_HEIGHT+INFO_HEIGHT);
				g.setFont(new Font(FONT,Font.PLAIN,24));
				String s = groups[i].get_name();
				FontMetrics metrics = g.getFontMetrics();
				while (metrics.stringWidth(s) > (int) (GRID_WIDTH/NUM_SEMESTERS)-2*offset){ //Make sure the group name fits in the box
					s = s.substring(0, s.length()-1);
				}
				g.drawString(s, x*GRID_WIDTH/8+5, 37);
				g.setFont(new Font(FONT,Font.PLAIN,12));
				ArrayList<Course> gp = groups[i].get_courses();
				if (groups[i].get_display()[0] != 0){
					//Draw the previous button if it's needed
					g.setColor(groups[i].get_color());
					g.fillPolygon(new int[]{x*(GRID_WIDTH/NUM_SEMESTERS)+2*offset,(int) ((x+0.5)*(GRID_WIDTH/NUM_SEMESTERS)),(x+1)*(GRID_WIDTH/NUM_SEMESTERS)-2*offset},
							new int[]{(GRID_HEIGHT/MAX_HOURS)-2*offset+INFO_HEIGHT-10,2*offset+INFO_HEIGHT,(GRID_HEIGHT/MAX_HOURS)-2*offset+INFO_HEIGHT-10},3);
					g.setColor(Color.BLACK);
					g.drawPolygon(new int[]{x*(GRID_WIDTH/NUM_SEMESTERS)+2*offset,(int) ((x+0.5)*(GRID_WIDTH/NUM_SEMESTERS)),(x+1)*(GRID_WIDTH/NUM_SEMESTERS)-2*offset},
							new int[]{(GRID_HEIGHT/MAX_HOURS)-2*offset+INFO_HEIGHT-10,2*offset+INFO_HEIGHT,(GRID_HEIGHT/MAX_HOURS)-2*offset+INFO_HEIGHT-10},3);
					g.drawString("Previous", (int) ((x+0.5)*(GRID_WIDTH/NUM_SEMESTERS))-24, (int)(0.5*(GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT+22));
					g.drawLine(x*(GRID_WIDTH/NUM_SEMESTERS), INFO_HEIGHT,
							(x+1)*(GRID_WIDTH/NUM_SEMESTERS), INFO_HEIGHT);
				}
				init_j = groups[i].get_display()[0]-groups[i].get_display()[0]%16; //Helps determine where to draw each course
				for (int j = groups[i].get_display()[0]; j <= groups[i].get_display()[1]; j++){
					//Draw the courses
					g.setColor(groups[i].get_color());
					g.fillRoundRect(x*(GRID_WIDTH/NUM_SEMESTERS)+offset, (j-init_j)*(GRID_HEIGHT/MAX_HOURS)+offset+INFO_HEIGHT,
							(GRID_WIDTH/NUM_SEMESTERS)-2*offset, (GRID_HEIGHT/MAX_HOURS)-2*offset,5,5);
					g.setColor(Color.BLACK);
					g.drawRoundRect(x*(GRID_WIDTH/NUM_SEMESTERS)+offset, (j-init_j)*(GRID_HEIGHT/MAX_HOURS)+offset+INFO_HEIGHT,
							(GRID_WIDTH/NUM_SEMESTERS)-2*offset, (GRID_HEIGHT/MAX_HOURS)-2*offset,5,5);
					g.drawLine(x*(GRID_WIDTH/NUM_SEMESTERS), (j-init_j)*(GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT,
							(x+1)*(GRID_WIDTH/NUM_SEMESTERS), (j-init_j)*(GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT);
					
					//Draw the course name
					String name = gp.get(j).get_name();
					if (!gp.get(j).get_name().equals("Add Course")){
						name+=" ("+gp.get(j).get_sh()+")";	
					}
					draw_wrapped_string(g,name,x*(GRID_WIDTH/NUM_SEMESTERS),(int) (((j-init_j)+0.5)*(GRID_HEIGHT/MAX_HOURS))+INFO_HEIGHT,offset,
							(GRID_WIDTH/NUM_SEMESTERS)-2*offset-4, (int) (0.5*(GRID_HEIGHT/MAX_HOURS))-offset);
				}
				if (groups[i].get_display()[1] != groups[i].get_courses().size()-1){
					//Draw the next button, if it's needed
					g.setColor(groups[i].get_color());
					g.fillPolygon(new int[]{x*(GRID_WIDTH/NUM_SEMESTERS)+2*offset,(int) ((x+0.5)*(GRID_WIDTH/NUM_SEMESTERS)),(x+1)*(GRID_WIDTH/NUM_SEMESTERS)-2*offset},
							new int[]{17*(GRID_HEIGHT/MAX_HOURS)+2*offset+INFO_HEIGHT+10,9*2*(GRID_HEIGHT/MAX_HOURS)-2*offset+INFO_HEIGHT,17*(GRID_HEIGHT/MAX_HOURS)+2*offset+INFO_HEIGHT+10},3);
					g.setColor(Color.BLACK);
					g.drawPolygon(new int[]{x*(GRID_WIDTH/NUM_SEMESTERS)+2*offset,(int) ((x+0.5)*(GRID_WIDTH/NUM_SEMESTERS)),(x+1)*(GRID_WIDTH/NUM_SEMESTERS)-2*offset},
							new int[]{17*(GRID_HEIGHT/MAX_HOURS)+2*offset+INFO_HEIGHT+10,9*2*(GRID_HEIGHT/MAX_HOURS)-2*offset+INFO_HEIGHT,17*(GRID_HEIGHT/MAX_HOURS)+2*offset+INFO_HEIGHT+10},3);
					g.drawString("Next", (int) ((x+0.5)*(GRID_WIDTH/NUM_SEMESTERS))-12, 18*(GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT-35);
					g.drawLine(x*(GRID_WIDTH/NUM_SEMESTERS), 17*(GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT,
							(x+1)*(GRID_WIDTH/NUM_SEMESTERS), 17*(GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT);
				}
				last_group++;
			}
			if (last_group < 7){
				//Draw the "New Group" button
				g.setColor(Color.BLACK);
				int x = last_group+1;
				g.drawLine((x+1)*(GRID_WIDTH/NUM_SEMESTERS), 0, (x+1)*(GRID_WIDTH/NUM_SEMESTERS), INFO_HEIGHT);
				g.drawLine(x*(GRID_WIDTH/NUM_SEMESTERS), INFO_HEIGHT, (x+1)*(GRID_WIDTH/NUM_SEMESTERS), INFO_HEIGHT);
				g.setFont(new Font(FONT,Font.PLAIN,18));
				g.drawString("New Group", x*GRID_WIDTH/8+5, 37);
				g.setFont(new Font(FONT,Font.PLAIN,12));
			}

		}
		
		//Draw the course info or req info (This is used in both screens, so it's put outside of the ifs)
		g.setColor(Color.WHITE);
		g.fillRect(GRID_WIDTH+2, 0, LABEL_WIDTH, INFO_HEIGHT+GRID_HEIGHT);
		g.setColor(Color.BLACK);
		int index = 0;
		if (info_course != null){
			//Display the course information
			int max_width = (LABEL_WIDTH-10);
			index = draw_wrapped_string(g,info_course.get_name(),GRID_WIDTH,15,offset,max_width,INFO_HEIGHT+GRID_HEIGHT);
			index++;
			g.drawString("Course Code: "+info_course.get_code(),GRID_WIDTH+10,15*index);
			index++;
			g.drawString("Study Hours: "+info_course.get_sh(),GRID_WIDTH+10,15*index);
			index++;
			//Edit Course button
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(GRID_WIDTH+10,15*(index-1)+5,max_width-10,20);
			g.setColor(Color.BLACK);
			g.drawRect(GRID_WIDTH+10,15*(index-1)+5,max_width-10,20);
			g.drawString("Edit Course", GRID_WIDTH+15, 15*index+5);
			button_coords = new int[]{GRID_WIDTH+10,15*(index-1)+5,GRID_WIDTH+max_width,15*(index-1)+25};
			index++;
		} else if (info_req != null){
			//Display the requirement information
			int max_width = (LABEL_WIDTH-10);
			String[] to_take = info_req.get_courses();
			if (to_take.length > 1){
				g.drawString("Take "+info_req.get_num()+" of these "+to_take.length+" courses:", GRID_WIDTH+10, 15);
			} else {
				g.drawString("Take this course:", GRID_WIDTH+10, 15);
			}
			index = 2;
			for (String s : to_take){
				Course c = m.find_course(courses.toArray(new Course[courses.size()]),s);
				if (c != null){ //If we can find the course, display it's name
					index+=draw_wrapped_string(g,c.get_name(),GRID_WIDTH,index*15,offset,LABEL_WIDTH,GRID_HEIGHT);
				} else { //Otherwise, just display the course code
					index+=draw_wrapped_string(g,s,GRID_WIDTH,index*15,offset,LABEL_WIDTH,GRID_HEIGHT);
				}
			}
			//Edit Requirement button
			g.setColor(Color.LIGHT_GRAY);
			g.fillRect(GRID_WIDTH+10,15*(index-1)+5,max_width-10,20);
			g.setColor(Color.BLACK);
			g.drawRect(GRID_WIDTH+10,15*(index-1)+5,max_width-10,20);
			g.drawString("Edit Requirement", GRID_WIDTH+15, 15*index+5);
			button_coords = new int[]{GRID_WIDTH+10,15*(index-1)+5,GRID_WIDTH+max_width,15*(index-1)+25};
			index++;
		}
		//Display courses not in a semester
		g.setColor(Color.BLACK);
		g.drawLine(GRID_WIDTH, 15*index, GRID_WIDTH+LABEL_WIDTH, 15*index);
		start_no_sem = 15*index;
		g.drawString("Courses not in any semester:", GRID_WIDTH+offset, start_no_sem+15);
		index = 0;
		for (Course c : no_semester){
			//Draw the courses
			Color color = groups[group_table.get(c.get_group())].get_color();
			g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),230)); //Make the color slightly translucent
			g.fillRoundRect(GRID_WIDTH+offset, (GRID_HEIGHT/MAX_HOURS)*index+start_no_sem+offset+15,
					(GRID_WIDTH/NUM_SEMESTERS)-2*offset, (GRID_HEIGHT/MAX_HOURS)-2*offset,5,5);
			g.setColor(Color.BLACK);
			g.drawRoundRect(GRID_WIDTH+offset, (GRID_HEIGHT/MAX_HOURS)*index+start_no_sem+offset+15,
					(GRID_WIDTH/NUM_SEMESTERS)-2*offset, (GRID_HEIGHT/MAX_HOURS)-2*offset,5,5);
						
			String name = c.get_name()+" ("+c.get_sh()+")";
			draw_wrapped_string(g,name,GRID_WIDTH,(GRID_HEIGHT/MAX_HOURS)*index+2*offset+start_no_sem+15*2,offset,
					(GRID_WIDTH/NUM_SEMESTERS)-2*offset-4,(int) ((GRID_HEIGHT/MAX_HOURS)*(0.5))-offset);

			index++;
		}
		
		//Draw the Update Schedule and Change View buttons
		g.setColor(Color.WHITE);
		g.fillRect(GRID_WIDTH+1, GRID_HEIGHT-(LABEL_WIDTH/2)+INFO_HEIGHT-offset, LABEL_WIDTH, LABEL_WIDTH);
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(GRID_WIDTH+offset, GRID_HEIGHT-(LABEL_WIDTH/2)+INFO_HEIGHT, LABEL_WIDTH-2*offset, LABEL_WIDTH/4-2*offset);
		g.fillRect(GRID_WIDTH+offset, GRID_HEIGHT-(LABEL_WIDTH/4)+INFO_HEIGHT, LABEL_WIDTH-2*offset, LABEL_WIDTH/4-2*offset);
		g.setColor(Color.BLACK);
		g.setFont(new Font(FONT,Font.PLAIN,18));
		g.drawString("Update Schedule", GRID_WIDTH+2*offset+20, GRID_HEIGHT-(LABEL_WIDTH/2)+INFO_HEIGHT+25);
		if (draw_graphics == true){
			g.drawString("View Groups", GRID_WIDTH+2*offset+40, GRID_HEIGHT-(LABEL_WIDTH/4)+INFO_HEIGHT+25);
		} else { 
			g.drawString("View Schedule", GRID_WIDTH+2*offset+30, GRID_HEIGHT-(LABEL_WIDTH/4)+INFO_HEIGHT+25);
		}
		g.setFont(new Font(FONT,Font.PLAIN,12));
		
		//Put the drag_course last so it appears on top of everything else
		if (drag_course != null){
			Course d = drag_course;
			Point p1 = MouseInfo.getPointerInfo().getLocation(); //Gets the location of the mouse on the screen
			Point p2 = frame.getLocationOnScreen(); //Gets the location of the frame on the screen
			int x = (int) p1.getX() - (int) p2.getX(); //The coordinates are the difference between these two values
			int y = (int) p1.getY() - (int) p2.getY();
			y-=(BARSIZE+MENUSIZE+INFO_HEIGHT); //Move the y up to account for the menu and top information
			x-=(GRID_WIDTH/NUM_SEMESTERS)/2-offset; //Center the course
			y-=2*offset; //y coord is near the top of the course, so it hangs down from the cursor
			Color c = groups[group_table.get(d.get_group())].get_color();
			g.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),230)); //Make the color slightly translucent
			//Draw the course at the coordinates of the mouse
			g.fillRoundRect(x+offset, y+offset+INFO_HEIGHT,
					(GRID_WIDTH/NUM_SEMESTERS)-2*offset, (GRID_HEIGHT/MAX_HOURS)*(d.get_sh())-2*offset,5,5);
			g.setColor(Color.BLACK);
			g.drawRoundRect(x+offset, y+offset+INFO_HEIGHT,
					(GRID_WIDTH/NUM_SEMESTERS)-2*offset, (GRID_HEIGHT/MAX_HOURS)*(d.get_sh())-2*offset,5,5);
			String name = d.get_name()+" ("+d.get_sh()+")";
			draw_wrapped_string(g,name,x,(int) (y+0.5*GRID_HEIGHT/MAX_HOURS)+INFO_HEIGHT,offset,
					(GRID_WIDTH/NUM_SEMESTERS)-2*offset-4, (int) ((GRID_HEIGHT/MAX_HOURS)*(d.get_sh()-0.5)) - offset);
		}

	}
	
	/**
	 * Runs the logic needed for the graphics
	 */
	public void updateGraphics(){
		//Not used
	}
	
	/**
	 * Adds elective courses to semesters that don't have enough courses. Removes courses from semesters with more than 18 sh
	 * @param semester
	 * @return semester containing electives
	 */
	private Course[] addElectives(Course[] semester){
		int hours = 0; //Keeps track of the credits in the semester
		for (int j = 0; j < semester.length; j++){
			if (semester[j] == null) { //We've reached the end of the courses. Add electives until we reach the minimum number of hours
				if (hours < MIN_HOURS){
					semester[j] = new Course(3,"Elective","Elective Course",2016,true,1,"Elective",new String[]{});
					hours+=3;
				} else {
					break;
				}
			} else if (semester[j].get_code().equals("Elective")){ //If the course we're at is an elective, we've reached the end of the actual courses.
				for (int k = j; k < semester.length; k++){ //Delete everything after this point
					semester[k] = null;
				}
				if (hours < MIN_HOURS){ //Add an elective if we haven't reached the minimum hours, to prevent a gap
					semester[j] = new Course(3,"Elective","Elective Course",2016,true,1,"Elective",new String[]{});
					hours+=3;
				} else {
					break;
				}
			} else if (hours+semester[j].get_sh() > 18){ //If a course would put us over 18 credits, put that course in no_semester
				no_semester.add(semester[j]);
				for (int k = j; k < semester.length-2; k++){
					semester[k] = semester[k+1];
				}
				j--;
			} else { //Increment the hours by the amount in the course
				hours+=semester[j].get_sh();
			}
		}
		return semester;
	}
	
	/**
	 * Compares the course list to the mapping, and puts the courses not in the mapping into no_semester
	 */
	@SuppressWarnings("unchecked")
	private void find_no_semester(){
		if (mapping == null){ //No mapping, do nothing
			return;
		}
		boolean has_cc = false;
		no_semester = (ArrayList<Course>) courses.clone(); //Clone the courses list
		for (Course[] sem : mapping){
			for (Course c : sem){
				if (c != null && c.get_name().equals("Cross Cultural")){
					has_cc = true;
				}
				no_semester.remove(c); //Remove all courses that are in the mapping
			}
		}
		if (has_cc == false){ //Add a cross cultural to no_semester if it isn't in the mapping
			no_semester.add(new Course(15,"CC101","Cross Cultural",2016,true,1,"Core",new String[]{"sophomore"}));
		}
	}
	
	/**
	 * Creates a new mapping based on the course and requirement lists
	 */
	@SuppressWarnings("unchecked")
	private void update_schedule(){
		ArrayList<Requirement> map_reqs = (ArrayList<Requirement>) reqs.clone();
		map_reqs.remove(map_reqs.size()-1); //Don't let the "Add Requirement" be an actual requirement. 
		mapping = m.create_mapping(courses.toArray(new Course[courses.size()]),map_reqs.toArray(new Requirement[map_reqs.size()]));
		if (mapping == null){
			draw_graphics = false; //There is no schedule to show, so force the user into the groups view
		} else {
			for (int i = 0; i < mapping.length; i++){
				for (int j = 0; j < mapping[i].length && mapping[i][j] != null; j++){
					mapping[i][j].set_warn(false); //There should be no problems, so remove all the warnings
				}
				mapping[i] = addElectives(mapping[i]); //Add electives to each semester
			}
		}
		find_no_semester(); //Find the courses not in the mapping
	}
	
	/**
	 * A method to display a string that wraps its text. The method returns the number of lines that the string uses
	 * @param g - the graphics to draw the string
	 * @param s - the string to draw
	 * @param x - the x position
	 * @param y - the y position
	 * @param offset - the amount to offset the string by
	 * @param max_width - the maximum allowed width of the string
	 * @param max_height - the maximum allowed height of the string
	 * @return index - the number of lines used
	 */
	private int draw_wrapped_string(Graphics g, String s, int x, int y, int offset, int max_width, int max_height){
		String[] to_display = s.split(" "); //Split the string into each word
		int index = 0;
		int next = 1;
		FontMetrics metrics = g.getFontMetrics();
		while (next < to_display.length){ //Join together the words until it gets too long for the specified width, then start a new line
			String join = to_display[index]+" "+to_display[next];
			if (metrics.stringWidth(join) < max_width){
				to_display[index] = join;
				to_display[next] = null;
				next++;
			} else {
				index = next;
				next++;
			}
		}
		index = 0;
		for (int k = 0; k < to_display.length; k++){
			//Display the text, so long as it isn't out the bottom of the box
			if (to_display[k] != null && index*15 < max_height){
				while (metrics.stringWidth(to_display[k]) > max_width){ //Don't let it display a single word that is too long for the box
					to_display[k] = to_display[k].substring(0,to_display[k].length()-1);
				}
				g.drawString(to_display[k], x+2*offset,y+index*15);
				index++;
			}
		}
		return index;
	}
	
	/**
	 * Checks if a course has any conflict with the semester it's in or its prerequisites
	 * @param c - the course to check
	 * @return whether the course has a conflict
	 */
	private boolean has_problem(Course c){
		int yr = (c.get_year()-m.get_start_year())*2; //When this course is first offered
		if (c.get_fall() == false){
			yr--;
		}
		int mapping_yr = NUM_SEMESTERS; //When this course is taken in the mapping
		for (int i = 0; i < mapping.length; i++){
			for (int j = 0; j < mapping[i].length && mapping[i][j] != null; j++){
				if (mapping[i][j] == c){
					mapping_yr = i;
					break;
				}
			}
		}
		if (mapping_yr%c.get_freq() != yr%c.get_freq()){ //If the course is taken in a semester it's not offered
			return true;
		}
		int num_prereqs = c.get_prereqs().size();
		for (String req : c.get_prereqs()){ //Make sure each of the prerequisites is fulfilled
			if (req.equals("freshman")){ //Should take as a freshman
				num_prereqs--;
			} else if (req.equals("sophomore") && mapping_yr >= 2){ //Need to be at least a sophomore
				num_prereqs--;
			} else if (req.equals("junior") && mapping_yr >= 4){ //Need to be at least a junior
				num_prereqs--;
			} else if (req.equals("senior") && mapping_yr >= 6){ //Need to be at least a senior
				num_prereqs--;
			} else { //Has a course prereq
				String[] aorb = req.split(" or "); //Prereqs that say take a or b will be in the form "MATH101 or MATH110"
				boolean found = false;
				for (String str : aorb){
					for (int i = 0; i < mapping_yr; i++){
						for (int j = 0; j < mapping[i].length && mapping[i][j] != null; j++)
						if (mapping[i][j].get_code().equals(str)){ //This prerequisite has been fulfilled
							num_prereqs--;
							found = true;
							break;
						}
					}
					if (found){
						break;
					}
				}
			}
		}
		if (num_prereqs != 0){ //If we haven't taken all the prerequisites for a course
			return true;
		}
		return false;
	}
	
	/**
	 * Writes data out to a file
	 * @param name - the file name
	 * @param arrayName - which array is being saved, used to create the file name
	 * @param data - the data to write out
	 * @throws FileNotFoundException
	 */
	public void save_to_file(String name,String arrayName,String data) throws FileNotFoundException{
		String fileName = "./files/"+name+"_"+arrayName+".txt"; //The file name
		PrintWriter out = new PrintWriter(fileName);
		for (String s : data.split("\n")){ //Write the data out to the file
			out.println(s);
		}
		out.close();
	}
	
	/**
	 * Read data in from a file
	 * @param name - the file name
	 * @param arrayName - which array is being read
	 * @throws FileNotFoundException
	 */
	public void load_from_file(String name,String arrayName) throws FileNotFoundException{
		String fileName = "./files/"+name+"_"+arrayName+".txt"; //The file name
		FileReader reader = new FileReader(fileName);
		Scanner in = new Scanner(reader);
		ArrayList<String> data = new ArrayList<String>(); 
		while (in.hasNextLine()){ //Pull the data in from the file
			data.add(in.nextLine());
		}
		in.close();
		if (arrayName.equals("mapping")){ //If you're reading in schedule data
			if (data.get(0).equals("")){
				mapping = null;
				return;
			} else if (mapping == null){ //Create an empty mapping if no mapping existed previously
				mapping = new Course[NUM_SEMESTERS][MAX_HOURS];
			}
			no_semester = new ArrayList<Course>();
			int sem_index = 0;
			int crs_index = 0;
			boolean missing_course = false; //If there is a course that should be in the mapping that is undefined
			mapping[sem_index] = new Course[MAX_HOURS];
			for (String crs : data){
				if (crs.equals("###")){ //Used as a separator in the file, means move to the next semester
					if (missing_course){ //If there is a course missing, add electives to that semester as needed
						addElectives(mapping[sem_index]);
					}
					//Move on to the next semester
					sem_index++;
					crs_index = 0;
					missing_course = false;
					if (sem_index < NUM_SEMESTERS){
						mapping[sem_index] = new Course[MAX_HOURS];
					}
				} else {
					if (sem_index < NUM_SEMESTERS){
						boolean added = false;
						if (crs.equals("CC101")){ //The cross cultural, not an actual course
							mapping[sem_index][crs_index] = new Course(15,"CC101","Cross Cultural",2016,true,1,"Core",new String[]{"sophomore"});
							added = true;
						} else if (crs.equals("Elective")){ //An elective, not an actual course
							mapping[sem_index][crs_index] = new Course(3,"Elective","Elective Course",2016,true,1,"Elective",new String[]{});
							added = true;
						} else { //A course to put in the mapping
							for (Course c : courses){
								if (c.get_code().equals(crs)){
									mapping[sem_index][crs_index] = c;
									added = true;
									break;
								}
							}
						}
						if (added){ //If the course was successfully added
							crs_index++;
						} else { //Else that course is missing from the courses list
							missing_course = true;
						}
					} else { //These are the courses that didn't fit into the mapping: the courses in no_semester
						if (crs.equals("CC101")){
							no_semester.add(new Course(15,"CC101","Cross Cultural",2016,true,1,"Core",new String[]{"sophomore"}));
						} else {
							for (Course c : courses){
								if (c.get_code().equals(crs)){
									no_semester.add(c);
									break;
								}
							}
						}
					}
				}
			}
			int sem = 0;
			while (mapping[sem][0] == null && sem < 8){ //Find the first non-empty semester, if it exists
				sem++;
			}
			if (sem < 8){ //This semester decides the starting year
				m.set_start_year(mapping[sem][0].get_year());
			}
		} else if (arrayName.equals("courses")){ //If you're reading in course data
			courses = new ArrayList<Course>();
			group_table = new Hashtable<String,Integer>();
			groups = new Group[7];
			group_index = 0;
			while (data.size() > 0 && data.get(0).startsWith("$")){ //While you're reading in group data
				String[] s = data.get(0).split(" ");
				int s_index = 0;
				String group_name = "";
				while (!s[s_index].startsWith("(")){ //Is part of the group name
					if (s_index != 0){
						group_name+=" ";
					}
					group_name+=s[s_index];
					s_index++;
				}
				group_name = group_name.substring(1); //Remove the leading "$"
				//Find the specified color of the group
				String[] rgb = s[s_index].substring(1,s[s_index].length()-1).split(",");
				groups[group_index] = new Group(group_name,new Color(Integer.valueOf(rgb[0]),Integer.valueOf(rgb[1]),Integer.valueOf(rgb[2])));
				group_table.put(group_name,group_index);
				group_table.put(String.valueOf(group_index), 0);
				group_index++;
				data.remove(0); //Move on to the next line
			}
			for (String crs : data){ //The course data
				String[] s = crs.split(","); //Split the line into various pieces of data
				int init_sh = Integer.valueOf(s[0]);
				String init_code = s[1];
				int index = 3;
				//Builds the name if there are commas in it
				while (index < s.length){
					try{
						Integer.valueOf(s[index]);
						break;
					} catch (NumberFormatException e){
						s[2] = String.join(",", s[2], s[index]);
						index++;
					}
				}
				String init_name = s[2];
				int init_year = Integer.valueOf(s[index++]);
				boolean init_fall = Boolean.valueOf(s[index++]);
				int init_freq = Integer.valueOf(s[index++]);
				String init_group = s[index++];
				String[] init_prereqs = s[index++].split("/"); //Prerequisites are separating by "/"
				boolean init_aorb = Boolean.valueOf(s[index++]);
				boolean init_warn = Boolean.valueOf(s[index++]);
				//Create a new course with the data pulled in from the file
				courses.add(new Course(init_sh,init_code,init_name,init_year,init_fall,init_freq,init_group,init_prereqs,init_aorb,init_warn));
			}
			for (Course c : courses){ //Put each of the courses into its group
				int gp = group_table.get(c.get_group());
				groups[gp].add_course(c);
			}
		} else if (arrayName.equals("reqs")){ //If you're reading in requirement data
			reqs = new ArrayList<Requirement>();
			for (String req : data){
				if (req.equals("")){
					continue;
				}
				String[] s = req.split(","); //Requirement consists of 2 parts, the course codes and the number of courses to take
				String[] init_courses = s[0].split("/"); //Course codes are separated by "/"
				int init_num = Integer.valueOf(s[1]);
				reqs.add(new Requirement(init_courses,init_num)); //Create a new requirement with the data pulled in from the file
			}
			reqs.add(new Requirement("Add Requirement")); //Add a "Add Requirement" button
			if (reqs.size() > 18){ //If there are more requirements than fit on 1 page, split it up
				reqs_display = new int[]{0,16};
			} else {
				reqs_display = new int[]{0,reqs.size()-1};
			}
		} else { //Unknown array name, can't do anything
			System.out.println("Warning: Unknown file");
		}
	}
	
	/**
	 * A method that makes a popup to add a new course or edit an existing course
	 * @param c - the course
	 * @param is_new - true if you're adding a new course, false if you're editing an existing course
	 */
	public void edit_course(Course c, boolean is_new){
		frame2 = new JFrame();
		frame2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); //Dispose of frame2 when it is closed
		frame2.setVisible(true);
		frame2.setResizable(false);
		Container pane = frame2.getContentPane();
		SpringLayout layout = new SpringLayout();
		pane.setLayout(layout);
		JLabel[] labels = new JLabel[]{ //Labels for each input area
				new JLabel("Course Name: "),
				new JLabel("Course Code: "),
				new JLabel("Study Hours: "),
				new JLabel("First Offered: "),
				new JLabel("Frequency Offered: "),
				new JLabel("Group: "),
				new JLabel("Prerequisites: ")
		};
		//Input areas
		JTextField name_field = new JTextField(c.get_name(),25);
		JTextField code_field = new JTextField(c.get_code(),7);
		JSpinner sh_spinner = new JSpinner(new SpinnerNumberModel(c.get_sh(),1,18,1));
		JSpinner year_spinner = new JSpinner(new SpinnerNumberModel(c.get_year(),1917,2117,1));
		year_spinner.setEditor(new JSpinner.NumberEditor(year_spinner,"#")); //Get rid of the delimiter to make it look like a year
		String[] fs_options = {"Fall","Spring"};
		JComboBox<String> fs_box = new JComboBox<String>(fs_options);
		if (c.get_fall() == true){
			fs_box.setSelectedIndex(0);
		} else {
			fs_box.setSelectedIndex(1);
		}
		String[] freq_options = {"Every Semester","Every Year","Every Other Year","Every 3 Years","Once"};
		JComboBox<String> freq_box = new JComboBox<String>(freq_options);
		if (c.get_freq() == 1){
			freq_box.setSelectedIndex(0);
		} else if (c.get_freq() == 2){
			freq_box.setSelectedIndex(1);
		} else if (c.get_freq() == 4){
			freq_box.setSelectedIndex(2);
		} else if (c.get_freq() == 6){
			freq_box.setSelectedIndex(3);
		} else {
			freq_box.setSelectedIndex(4);
		}
		String[] group_options = new String[group_index];
		for (int i = 0; i < groups.length && groups[i] != null; i++){
			group_options[i] = groups[i].get_name();
		}
		if (groups[0] == null){
			group_options[0] = "Core";
		}
		JComboBox<String> group_box = new JComboBox<String>(group_options);
		group_box.setSelectedItem(c.get_group());
		DefaultListModel<String> list_model = new DefaultListModel<String>(); //Needed for dynamic changing of the JList
		for (String s : c.get_prereqs()){
			list_model.addElement(s);
		}
		JList<String> prereqs = new JList<String>(list_model);
		prereqs.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		prereqs.setLayoutOrientation(JList.VERTICAL);
		JScrollPane list_scroller = new JScrollPane(prereqs);
		list_scroller.setPreferredSize(new Dimension(250,55));
		JButton remove_prereq = new JButton("Remove");
		JTextField prereq_field = new JTextField("",7);
		JButton add_prereq = new JButton("Add");
		//Buttons to update or delete the course, or cancel out of the page
		JButton update = new JButton("Update");
		JButton delete = new JButton("Delete");
		JButton cancel = new JButton("Cancel");
		
		for (int i = 0; i < labels.length; i++){
			pane.add(labels[i]); //Add the labels to the pane
			layout.putConstraint(SpringLayout.WEST, labels[i], 5, SpringLayout.WEST, pane); //Place the labels along the left side
		}
		//Add the input areas to the pane
		pane.add(name_field);
		pane.add(code_field);
		pane.add(sh_spinner);
		pane.add(year_spinner);
		pane.add(fs_box);
		pane.add(freq_box);
		pane.add(group_box);
		pane.add(list_scroller);
		pane.add(remove_prereq);
		pane.add(prereq_field);
		pane.add(add_prereq);
		pane.add(update);
		pane.add(delete);
		pane.add(cancel);
		//Place the input areas
		layout.putConstraint(SpringLayout.WEST, name_field, 5, SpringLayout.EAST, labels[0]);
		layout.putConstraint(SpringLayout.NORTH, name_field, 5, SpringLayout.NORTH, pane);
		layout.putConstraint(SpringLayout.NORTH, labels[0], 5, SpringLayout.NORTH, name_field);
		layout.putConstraint(SpringLayout.WEST, code_field, 5, SpringLayout.EAST, labels[1]);
		layout.putConstraint(SpringLayout.NORTH, code_field, 5, SpringLayout.SOUTH, name_field);
		layout.putConstraint(SpringLayout.NORTH, labels[1], 5, SpringLayout.NORTH, code_field);
		layout.putConstraint(SpringLayout.WEST, sh_spinner, 5, SpringLayout.EAST, labels[2]);
		layout.putConstraint(SpringLayout.NORTH, sh_spinner, 5, SpringLayout.SOUTH, code_field);
		layout.putConstraint(SpringLayout.NORTH, labels[2], 5, SpringLayout.NORTH, sh_spinner);
		layout.putConstraint(SpringLayout.WEST, fs_box, 5, SpringLayout.EAST, labels[3]);
		layout.putConstraint(SpringLayout.WEST, year_spinner, 5, SpringLayout.EAST, fs_box);
		layout.putConstraint(SpringLayout.NORTH, year_spinner, 5, SpringLayout.SOUTH, sh_spinner);
		layout.putConstraint(SpringLayout.NORTH, fs_box, 0, SpringLayout.NORTH, year_spinner);
		layout.putConstraint(SpringLayout.NORTH, labels[3], 5, SpringLayout.NORTH, year_spinner);
		layout.putConstraint(SpringLayout.WEST, freq_box, 5, SpringLayout.EAST, labels[4]);
		layout.putConstraint(SpringLayout.NORTH, freq_box, 5, SpringLayout.SOUTH, fs_box);
		layout.putConstraint(SpringLayout.NORTH, labels[4], 5, SpringLayout.NORTH, freq_box);
		layout.putConstraint(SpringLayout.WEST, group_box, 5, SpringLayout.EAST, labels[5]);
		layout.putConstraint(SpringLayout.NORTH, group_box, 5, SpringLayout.SOUTH, freq_box);
		layout.putConstraint(SpringLayout.NORTH, labels[5], 5, SpringLayout.NORTH, group_box);
		layout.putConstraint(SpringLayout.NORTH, labels[6], 5, SpringLayout.SOUTH, labels[5]);
		layout.putConstraint(SpringLayout.WEST, list_scroller, 5, SpringLayout.WEST, pane);
		layout.putConstraint(SpringLayout.NORTH, list_scroller, 5, SpringLayout.SOUTH, labels[6]);
		layout.putConstraint(SpringLayout.EAST, list_scroller, 0, SpringLayout.EAST, name_field);
		layout.putConstraint(SpringLayout.WEST, remove_prereq, 5, SpringLayout.WEST, pane);
		layout.putConstraint(SpringLayout.NORTH, remove_prereq, 5, SpringLayout.SOUTH, list_scroller);
		layout.putConstraint(SpringLayout.WEST, prereq_field, 5, SpringLayout.EAST, remove_prereq);
		layout.putConstraint(SpringLayout.NORTH, prereq_field, 0, SpringLayout.NORTH, remove_prereq);
		layout.putConstraint(SpringLayout.WEST, add_prereq, 5, SpringLayout.EAST, prereq_field);
		layout.putConstraint(SpringLayout.NORTH, add_prereq, 0, SpringLayout.NORTH, remove_prereq);
		layout.putConstraint(SpringLayout.WEST, update, 5, SpringLayout.WEST, pane);
		layout.putConstraint(SpringLayout.NORTH, update, 5, SpringLayout.SOUTH, add_prereq);
		layout.putConstraint(SpringLayout.WEST, delete, 5, SpringLayout.EAST, update);
		layout.putConstraint(SpringLayout.NORTH, delete, 5, SpringLayout.SOUTH, add_prereq);
		layout.putConstraint(SpringLayout.WEST, cancel, 5, SpringLayout.EAST, delete);
		layout.putConstraint(SpringLayout.NORTH, cancel, 5, SpringLayout.SOUTH, add_prereq);
		layout.putConstraint(SpringLayout.EAST, pane, 5, SpringLayout.EAST, name_field);
		layout.putConstraint(SpringLayout.SOUTH, pane, 5, SpringLayout.SOUTH, update);
		frame2.pack(); //Make the pane appear
		
		//What to do when a button is clicked on
		ActionListener listener2 = new ActionListener(){
			public void actionPerformed(ActionEvent evt){
				if (evt.getActionCommand().equals("Update")){ //Add/Update a course
					//Pull in the information from the input areas
					int init_sh = (int) sh_spinner.getValue();
					String init_code = code_field.getText().replace(" ","");
					String init_name = name_field.getText();
					int init_year = (int) year_spinner.getValue();
					boolean init_fall = true;
					if (fs_box.getSelectedIndex() == 1){
						init_fall = false;
					}
					int init_freq = 1;
					if (freq_box.getSelectedIndex() == 1){
						init_freq = 2;
					} else if (freq_box.getSelectedIndex() == 2){
						init_freq = 4;
					} else if (freq_box.getSelectedIndex() == 3){
						init_freq = 6;
					} else if (freq_box.getSelectedIndex() == 4){
						init_freq = 8;
					}
					String init_group = (String) group_box.getSelectedItem();
					String[] init_prereqs = new String[list_model.size()];
					for (int i = 0; i < list_model.size(); i++){
						init_prereqs[i] = list_model.get(i);
					}
					if (is_new == true){ //If you're making a new course
						//Create a new course, add it to the courses list and the proper group
						Course new_course = new Course(init_sh, init_code, init_name, init_year, init_fall, init_freq, init_group, init_prereqs);
						courses.add(new_course);
						int gp = group_table.get(init_group);
						groups[gp].add_course(new_course);
						no_semester.add(new_course); //Put it in no_semester since it's not in the mapping
					} else { //You're editing an existing course
						//Change the group containing the course if needed
						if (!c.get_group().equals(init_group)){
							groups[group_table.get(c.get_group())].remove_course(c);
							c.set_group(init_group);
							groups[group_table.get(init_group)].add_course(c);
						}
						//Set the remaining course properties to the new values
						c.set_sh(init_sh);
						c.set_code(init_code);
						c.set_name(init_name);
						c.set_year(init_year);
						c.set_fall(init_fall);
						c.set_freq(init_freq);
						c.set_prereqs(init_prereqs);
						if (mapping != null){
							for (int i = 0; i < mapping.length; i++){
								for (int j = 0; j < mapping[i].length && mapping[i][j] != null; j++){
									mapping[i][j].set_warn(has_problem(mapping[i][j])); //Check if editing the course created a problem
								}
								mapping[i] = addElectives(mapping[i]); //Add electives to the semester if needed
							}
						}
					}
					frame2.dispose();
				} else if (evt.getActionCommand().equals("Delete")){ //Delete the course
					if (is_new == false){
						courses.remove(c); //Remove from the list of courses
						groups[group_table.get(c.get_group())].remove_course(c); //Remove from the group
						info_course = null; //Since the info_course was the course being deleted, set it to null
					}
					frame2.dispose();
				} else if (evt.getActionCommand().equals("Add")){ //Adds a prerequisite
					String s = prereq_field.getText();
					if (!s.equals("") && !list_model.contains(s)){
						list_model.addElement(s);
					}
				} else if (evt.getActionCommand().equals("Remove")){ //Removes a prerequisite
					if (prereqs.getSelectedValue() != null){
						list_model.remove(prereqs.getSelectedIndex());
					}
				} else { //Clicked the "Cancel" button
					frame2.dispose();
				}
			}
		};

		//Make these buttons listen for clicks
		remove_prereq.addActionListener(listener2);
		add_prereq.addActionListener(listener2);
		update.addActionListener(listener2);
		delete.addActionListener(listener2);
		cancel.addActionListener(listener2);
	}
	
	/**
	 * A method that makes a popup to change the color of electives
	 * @param c - the Elective "course"
	 */
	public void edit_elective(Course c){
		frame2 = new JFrame();
		frame2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); //Dispose of frame2 when it is closed
		frame2.setVisible(true);
		frame2.setResizable(false);
		Container pane = frame2.getContentPane();
		SpringLayout layout = new SpringLayout();
		pane.setLayout(layout);
		
		JLabel[] labels = new JLabel[]{ //Labels for each input area
			new JLabel("Elective Color: "),
			new JLabel("Red"),
			new JLabel("Green"),
			new JLabel("Blue")
		};
		//The color sliders
		JSlider red_slider = new JSlider(JSlider.HORIZONTAL,0,255,elective_color.getRed());
		JLabel red_num = new JLabel(String.valueOf(elective_color.getRed()));
		JSlider green_slider = new JSlider(JSlider.HORIZONTAL,0,255,elective_color.getGreen());
		JLabel green_num = new JLabel(String.valueOf(elective_color.getGreen()));
		JSlider blue_slider = new JSlider(JSlider.HORIZONTAL,0,255,elective_color.getBlue());
		JLabel blue_num = new JLabel(String.valueOf(elective_color.getBlue()));
		JLabel color_label = new JLabel();
		color_label.setOpaque(true);
		color_label.setBackground(elective_color);
		//The buttons update the color, or cancel out of the page
		JButton update = new JButton("Update");
		JButton cancel = new JButton("Cancel");
		
		for (int i = 0; i < labels.length; i++){
			pane.add(labels[i]); //Add the labels to the pane
			layout.putConstraint(SpringLayout.WEST, labels[i], 5, SpringLayout.WEST, pane); //Place the labels along the left side
		}
		//Add the color sliders to the pane
		pane.add(red_slider);
		pane.add(red_num);
		pane.add(green_slider);
		pane.add(green_num);
		pane.add(blue_slider);
		pane.add(blue_num);
		pane.add(color_label);
		pane.add(update);
		pane.add(cancel);

		//Place the sliders and buttons
		layout.putConstraint(SpringLayout.NORTH, labels[0], 5, SpringLayout.NORTH, pane);
		layout.putConstraint(SpringLayout.NORTH, red_slider, 5, SpringLayout.SOUTH, labels[0]);
		layout.putConstraint(SpringLayout.WEST, red_slider, 5, SpringLayout.EAST, labels[2]);
		layout.putConstraint(SpringLayout.NORTH, red_num, 5, SpringLayout.NORTH, red_slider);
		layout.putConstraint(SpringLayout.WEST, red_num, 5, SpringLayout.EAST, red_slider);
		layout.putConstraint(SpringLayout.NORTH, labels[1], 5, SpringLayout.NORTH, red_slider);
		layout.putConstraint(SpringLayout.NORTH, green_slider, 5, SpringLayout.SOUTH, red_slider);
		layout.putConstraint(SpringLayout.WEST, green_slider, 5, SpringLayout.EAST, labels[2]);
		layout.putConstraint(SpringLayout.NORTH, green_num, 5, SpringLayout.NORTH, green_slider);
		layout.putConstraint(SpringLayout.WEST, green_num, 5, SpringLayout.EAST, green_slider);
		layout.putConstraint(SpringLayout.NORTH, labels[2], 5, SpringLayout.NORTH, green_slider);
		layout.putConstraint(SpringLayout.NORTH, blue_slider, 5, SpringLayout.SOUTH, green_slider);
		layout.putConstraint(SpringLayout.WEST, blue_slider, 5, SpringLayout.EAST, labels[2]);
		layout.putConstraint(SpringLayout.NORTH, blue_num, 5, SpringLayout.NORTH, blue_slider);
		layout.putConstraint(SpringLayout.WEST, blue_num, 5, SpringLayout.EAST, blue_slider);
		layout.putConstraint(SpringLayout.NORTH, labels[3], 5, SpringLayout.NORTH, blue_slider);
		layout.putConstraint(SpringLayout.NORTH, color_label, 0, SpringLayout.NORTH, red_slider);
		layout.putConstraint(SpringLayout.WEST, color_label, 35, SpringLayout.EAST, green_slider);
		layout.putConstraint(SpringLayout.SOUTH, color_label, 0, SpringLayout.SOUTH, blue_slider);
		layout.putConstraint(SpringLayout.EAST, color_label, 120, SpringLayout.EAST, green_slider);
		layout.putConstraint(SpringLayout.WEST, update, 5, SpringLayout.WEST, pane);
		layout.putConstraint(SpringLayout.NORTH, update, 5, SpringLayout.SOUTH, blue_slider);
		layout.putConstraint(SpringLayout.WEST, cancel, 5, SpringLayout.EAST, update);
		layout.putConstraint(SpringLayout.NORTH, cancel, 5, SpringLayout.SOUTH, blue_slider);
		layout.putConstraint(SpringLayout.SOUTH, pane, 5, SpringLayout.SOUTH, update);
		layout.putConstraint(SpringLayout.EAST, pane, 5, SpringLayout.EAST, color_label);
		
		frame2.pack(); //Make the pane appear
		
		update.addActionListener(new ActionListener(){ //What to do when the "Update" button is clicked on
			public void actionPerformed(ActionEvent evt){
				elective_color = new Color(red_slider.getValue(),green_slider.getValue(),blue_slider.getValue());
				frame2.dispose();
			}
		});
		cancel.addActionListener(new ActionListener(){ //What to do when the "Cancel" button is clicked on
			public void actionPerformed(ActionEvent evt){
				frame2.dispose();
			}
		});
		
		ChangeListener listener2 = new ChangeListener(){ //What to do when the sliders are changed
			public void stateChanged(ChangeEvent evt){
				//Update the displayed value of the slider
				if (evt.getSource() == red_slider){
					red_num.setText(String.valueOf(red_slider.getValue()));
				} else if (evt.getSource() == green_slider){
					green_num.setText(String.valueOf(green_slider.getValue()));
				} else if (evt.getSource() == blue_slider){
					blue_num.setText(String.valueOf(blue_slider.getValue()));
				}
				//Change the example color
				color_label.setBackground(new Color(red_slider.getValue(),green_slider.getValue(),blue_slider.getValue()));
			}
		};
		//Make these sliders listen for changes
		red_slider.addChangeListener(listener2);
		green_slider.addChangeListener(listener2);
		blue_slider.addChangeListener(listener2);
	}
	
	/**
	 * A method that makes a popup to add a new group or edit an existing group
	 * @param g - the group
	 * @param is_new - true if you're adding a new group, false if you're editing an existing group
	 */
	public void edit_group(Group g, boolean is_new){
		frame2 = new JFrame();
		frame2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); //Dispose of frame2 when it is closed
		frame2.setVisible(true);
		frame2.setResizable(false);
		Container pane = frame2.getContentPane();
		SpringLayout layout = new SpringLayout();
		pane.setLayout(layout);
		
		if (is_new == true){
			g.set_color(colors[group_index+1]);
		}
		
		JLabel[] labels = new JLabel[]{ //Labels for each input area
			new JLabel("Group Name: "),
			new JLabel("Group Color: "),
			new JLabel("Red"),
			new JLabel("Green"),
			new JLabel("Blue")
		};
		//The input areas
		JTextField name_field = new JTextField(g.get_name(),25);
		JSlider red_slider = new JSlider(JSlider.HORIZONTAL,0,255,g.get_color().getRed());
		JLabel red_num = new JLabel(String.valueOf(g.get_color().getRed()));
		JSlider green_slider = new JSlider(JSlider.HORIZONTAL,0,255,g.get_color().getGreen());
		JLabel green_num = new JLabel(String.valueOf(g.get_color().getGreen()));
		JSlider blue_slider = new JSlider(JSlider.HORIZONTAL,0,255,g.get_color().getBlue());
		JLabel blue_num = new JLabel(String.valueOf(g.get_color().getBlue()));
		JLabel color_label = new JLabel();
		color_label.setOpaque(true);
		color_label.setBackground(g.get_color());
		//The buttons to update or delete a group, or cancel out of the page
		JButton update = new JButton("Update");
		JButton delete = new JButton("Delete");
		JButton cancel = new JButton("Cancel");
		
		for (int i = 0; i < labels.length; i++){
			pane.add(labels[i]); //Add the labels to the pane
			layout.putConstraint(SpringLayout.WEST, labels[i], 5, SpringLayout.WEST, pane); //Place the labels along the left side
		}
		//Add the input areas to the pane
		pane.add(name_field);
		pane.add(red_slider);
		pane.add(red_num);
		pane.add(green_slider);
		pane.add(green_num);
		pane.add(blue_slider);
		pane.add(blue_num);
		pane.add(color_label);
		pane.add(update);
		pane.add(delete);
		pane.add(cancel);
		//Place the input areas
		layout.putConstraint(SpringLayout.NORTH, name_field, 5, SpringLayout.NORTH, pane);
		layout.putConstraint(SpringLayout.WEST, name_field, 5, SpringLayout.EAST, labels[0]);
		layout.putConstraint(SpringLayout.NORTH, labels[0], 5, SpringLayout.NORTH, name_field);
		layout.putConstraint(SpringLayout.NORTH, labels[1], 5, SpringLayout.SOUTH, labels[0]);
		layout.putConstraint(SpringLayout.NORTH, red_slider, 5, SpringLayout.SOUTH, labels[1]);
		layout.putConstraint(SpringLayout.WEST, red_slider, 5, SpringLayout.EAST, labels[3]);
		layout.putConstraint(SpringLayout.NORTH, red_num, 5, SpringLayout.NORTH, red_slider);
		layout.putConstraint(SpringLayout.WEST, red_num, 5, SpringLayout.EAST, red_slider);
		layout.putConstraint(SpringLayout.NORTH, labels[2], 5, SpringLayout.NORTH, red_slider);
		layout.putConstraint(SpringLayout.NORTH, green_slider, 5, SpringLayout.SOUTH, red_slider);
		layout.putConstraint(SpringLayout.WEST, green_slider, 5, SpringLayout.EAST, labels[3]);
		layout.putConstraint(SpringLayout.NORTH, green_num, 5, SpringLayout.NORTH, green_slider);
		layout.putConstraint(SpringLayout.WEST, green_num, 5, SpringLayout.EAST, green_slider);
		layout.putConstraint(SpringLayout.NORTH, labels[3], 5, SpringLayout.NORTH, green_slider);
		layout.putConstraint(SpringLayout.NORTH, blue_slider, 5, SpringLayout.SOUTH, green_slider);
		layout.putConstraint(SpringLayout.WEST, blue_slider, 5, SpringLayout.EAST, labels[3]);
		layout.putConstraint(SpringLayout.NORTH, blue_num, 5, SpringLayout.NORTH, blue_slider);
		layout.putConstraint(SpringLayout.WEST, blue_num, 5, SpringLayout.EAST, blue_slider);
		layout.putConstraint(SpringLayout.NORTH, labels[4], 5, SpringLayout.NORTH, blue_slider);
		layout.putConstraint(SpringLayout.NORTH, color_label, 0, SpringLayout.NORTH, red_slider);
		layout.putConstraint(SpringLayout.WEST, color_label, 35, SpringLayout.EAST, green_slider);
		layout.putConstraint(SpringLayout.SOUTH, color_label, 0, SpringLayout.SOUTH, blue_slider);
		layout.putConstraint(SpringLayout.EAST, color_label, -5, SpringLayout.EAST, name_field);
		layout.putConstraint(SpringLayout.WEST, update, 5, SpringLayout.WEST, pane);
		layout.putConstraint(SpringLayout.NORTH, update, 5, SpringLayout.SOUTH, blue_slider);
		layout.putConstraint(SpringLayout.WEST, delete, 5, SpringLayout.EAST, update);
		layout.putConstraint(SpringLayout.NORTH, delete, 5, SpringLayout.SOUTH, blue_slider);
		layout.putConstraint(SpringLayout.WEST, cancel, 5, SpringLayout.EAST, delete);
		layout.putConstraint(SpringLayout.NORTH, cancel, 5, SpringLayout.SOUTH, blue_slider);
		layout.putConstraint(SpringLayout.SOUTH, pane, 5, SpringLayout.SOUTH, update);
		layout.putConstraint(SpringLayout.EAST, pane, 5, SpringLayout.EAST, name_field);
		
		frame2.pack(); //Make the pane appear
		
		update.addActionListener(new ActionListener(){ //What to do when the "update" button is clicked
			public void actionPerformed(ActionEvent evt){
				if (is_new == false){
					//Update the group with the new properties
					g.set_name(name_field.getText());
					g.set_color(new Color(red_slider.getValue(),green_slider.getValue(),blue_slider.getValue()));
				} else {
					//Create a new group
					groups[group_index] = new Group(name_field.getText(),new Color(red_slider.getValue(),green_slider.getValue(),blue_slider.getValue()));
					group_table.put(name_field.getText(),group_index);
					group_table.put(String.valueOf(group_index), 0);
					group_index++;
				}
				frame2.dispose();
			}
		});
		delete.addActionListener(new ActionListener(){ //What to do when the "delete" button is clicked
			public void actionPerformed(ActionEvent evt){
				//Delete the group
				if (is_new == false){
					int index = group_table.get(g.get_name());
					groups[index] = null;
					while (index < groups.length-1){
						groups[index] = groups[index+1];
						if (groups[index+1] != null){
							group_table.put(groups[index+1].get_name(),index);
						} else {
							break;
						}
						index++;
					}
					group_index--;
				}
				frame2.dispose();
			}
		});
		cancel.addActionListener(new ActionListener(){ //What to do when the "cancel" button is clicked
			public void actionPerformed(ActionEvent evt){
				frame2.dispose();
			}
		});
		
		ChangeListener listener2 = new ChangeListener(){ //What to do when the sliders are changed
			public void stateChanged(ChangeEvent evt){
				//Update the displayed value of the slider
				if (evt.getSource() == red_slider){
					red_num.setText(String.valueOf(red_slider.getValue()));
				} else if (evt.getSource() == green_slider){
					green_num.setText(String.valueOf(green_slider.getValue()));
				} else if (evt.getSource() == blue_slider){
					blue_num.setText(String.valueOf(blue_slider.getValue()));
				}
				//Change the example color
				color_label.setBackground(new Color(red_slider.getValue(),green_slider.getValue(),blue_slider.getValue()));
			}
		};
		//Make the sliders listen for changes
		red_slider.addChangeListener(listener2);
		green_slider.addChangeListener(listener2);
		blue_slider.addChangeListener(listener2);
	}
	
	/**
	 * A method that makes a popup to add a new requirement or edit an existing requirement
	 * @param r - the requirement
	 * @param is_new - true if you're adding a new requirement, false if you're editing an existing requirement
	 */
	public void edit_requirement(Requirement r, boolean is_new){
		frame2 = new JFrame();
		frame2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); //Dispose of frame2 when it is closed
		frame2.setVisible(true);
		frame2.setResizable(false);
		Container pane = frame2.getContentPane();
		SpringLayout layout = new SpringLayout();
		pane.setLayout(layout);
		
		DefaultListModel<String> list_model = new DefaultListModel<String>(); //Needed for dynamic changing of the JList
		for (String s : r.get_courses()){
			list_model.addElement(s);
		}

		//Labels for the input areas
		JLabel num_label1 = new JLabel("Take");
		JLabel num_label2 = new JLabel("of these courses:");
		//The input areas
		SpinnerNumberModel num_model = new SpinnerNumberModel(1,1,1,1);
		if (list_model.size() > 0){
			num_model.setMaximum(list_model.size());		
		}
		JSpinner num_spinner = new JSpinner(num_model);
		JList<String> r_courses = new JList<String>(list_model);
		r_courses.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		r_courses.setLayoutOrientation(JList.VERTICAL);
		JScrollPane list_scroller = new JScrollPane(r_courses);
		list_scroller.setPreferredSize(new Dimension(250,55));
		JButton remove_course = new JButton("Remove");
		JTextField course_field = new JTextField("",7);
		JButton add_course = new JButton("Add");
		//The buttons to update or delete a requirement, or cancel out of the page
		JButton update = new JButton("Update");
		JButton delete = new JButton("Delete");
		JButton cancel = new JButton("Cancel");
		
		//Add the components to the pane
		pane.add(num_label1);
		pane.add(num_label2);
		pane.add(num_spinner);
		pane.add(list_scroller);
		pane.add(remove_course);
		pane.add(course_field);
		pane.add(add_course);
		pane.add(update);
		pane.add(delete);
		pane.add(cancel);
		
		//Place the components in the pane
		layout.putConstraint(SpringLayout.WEST, num_label1, 5, SpringLayout.WEST, pane);
		layout.putConstraint(SpringLayout.WEST, num_spinner, 5, SpringLayout.EAST, num_label1);
		layout.putConstraint(SpringLayout.NORTH, num_spinner, 5, SpringLayout.NORTH, pane);
		layout.putConstraint(SpringLayout.WEST, num_label2, 5, SpringLayout.EAST, num_spinner);
		layout.putConstraint(SpringLayout.NORTH, num_label1, 5, SpringLayout.NORTH, num_spinner);
		layout.putConstraint(SpringLayout.NORTH, num_label2, 5, SpringLayout.NORTH, num_spinner);
		layout.putConstraint(SpringLayout.WEST, list_scroller, 5, SpringLayout.WEST, pane);
		layout.putConstraint(SpringLayout.NORTH, list_scroller, 5, SpringLayout.SOUTH, num_spinner);
		layout.putConstraint(SpringLayout.WEST, remove_course, 5, SpringLayout.WEST, pane);
		layout.putConstraint(SpringLayout.NORTH, remove_course, 5, SpringLayout.SOUTH, list_scroller);
		layout.putConstraint(SpringLayout.WEST, course_field, 5, SpringLayout.EAST, remove_course);
		layout.putConstraint(SpringLayout.NORTH, course_field, 5, SpringLayout.SOUTH, list_scroller);
		layout.putConstraint(SpringLayout.WEST, add_course, 5, SpringLayout.EAST, course_field);
		layout.putConstraint(SpringLayout.NORTH, add_course, 5, SpringLayout.SOUTH, list_scroller);
		layout.putConstraint(SpringLayout.WEST, update, 5, SpringLayout.WEST, pane);
		layout.putConstraint(SpringLayout.NORTH, update, 5, SpringLayout.SOUTH, remove_course);
		layout.putConstraint(SpringLayout.WEST, delete, 5, SpringLayout.EAST, update);
		layout.putConstraint(SpringLayout.NORTH, delete, 5, SpringLayout.SOUTH, remove_course);
		layout.putConstraint(SpringLayout.WEST, cancel, 5, SpringLayout.EAST, delete);
		layout.putConstraint(SpringLayout.NORTH, cancel, 5, SpringLayout.SOUTH, remove_course);
		layout.putConstraint(SpringLayout.SOUTH, pane, 5, SpringLayout.SOUTH, update);
		layout.putConstraint(SpringLayout.EAST, pane, 5, SpringLayout.EAST, add_course);

		frame2.pack(); //Make the pane appear
		
		ActionListener listener2 = new ActionListener(){ //What to do when the buttons are clicked
			public void actionPerformed(ActionEvent evt){
				if (evt.getActionCommand().equals("Update")){ //Update/Create a requirement
					if ((int) num_spinner.getValue() == list_model.size()){
						if (is_new == false){
							reqs.remove(r); //If we were editing a requirement, take that one out because we made it into several requirements
							info_req = null;
							reqs_display[1]--;
						}
						//Make a requirement for each course code listed
						for (int i = 0; i < list_model.size(); i++){
							if (reqs.size() == 0){
								reqs.add(new Requirement(list_model.get(i)));
							} else {
								reqs.add(reqs.size()-1,new Requirement(list_model.get(i)));
							}
							reqs_display[1]++; //Need to display another requirement
							int extras = 0;
							if (reqs_display[0] == 0){ //First set can have 1 extra in it
								extras++;
							}
							if (reqs.size()-1 <= reqs_display[0]+17){
								extras++;
							}
							 if (reqs_display[1] > reqs_display[0]+15+extras){ //If our interval is too big
								 reqs_display[1] = reqs_display[0]+15+extras; //Make it the proper size
							} else if (reqs_display[1] > reqs.size()-1){
								reqs_display[1] = reqs.size()-1;
							}
						}
					} else if (list_model.size() > 0) {
						String[] init_courses = new String[list_model.size()];
						for (int i = 0; i < list_model.size(); i++){
							init_courses[i] = list_model.get(i);
						}
						if (is_new == true){ //Add a new requirement
							if (reqs.size() == 0){
								reqs.add(new Requirement(init_courses,(int) num_spinner.getValue()));
							} else {
								reqs.add(reqs.size()-1,new Requirement(init_courses,(int) num_spinner.getValue()));
							}
							reqs_display[1]++; //Need to display another requirement
							int extras = 0;
							if (reqs_display[0] == 0){ //First set can have 1 extra in it
								extras++;
							}
							if (reqs.size()-1 <= reqs_display[0]+17){
								extras++;
							}
							 if (reqs_display[1] > reqs_display[0]+15+extras){ //If our interval is too big
								 reqs_display[1] = reqs_display[0]+15+extras; //Make it the proper size
							} else if (reqs_display[1] > reqs.size()-1){
								reqs_display[1] = reqs.size()-1;
							}
						} else { //Update an existing requirement with new values
							r.set_courses(init_courses);
							r.set_num((int) num_spinner.getValue());
						}
					}
					frame2.dispose();
				} else if (evt.getActionCommand().equals("Delete")){ //Delete a requirement
					if (is_new == false){
						info_req = null;
						reqs.remove(r);
						//Decrement the display value as needed
						if (reqs_display[1] == reqs.size()-2){
							reqs_display[1] = reqs.size()-1;
						} else if (reqs_display[1] >= reqs.size()){
							reqs_display[1]--;
						}
						if (reqs_display[1] == reqs_display[0]){
							int temp = reqs_display[1];
							if (reqs_display[0] > 0){
								reqs_display[0]-=16;
								reqs_display[1] = reqs_display[0]+15;
								if (reqs_display[0] == 1){
									reqs_display[0] = 0;
								}
								if (reqs_display[1] > reqs.size()-1){
									reqs_display[1] = reqs.size()-1;
								}
							}
							reqs_display[1] = temp;
						}
					}
					frame2.dispose();
				} else if (evt.getActionCommand().equals("Add")){ //Add a course code to the requirement
					String s = course_field.getText().replace(" ", "");
					if (!s.equals("") && !list_model.contains(s)){
						list_model.addElement(s);
					}
					num_model.setMaximum(list_model.size());
				} else if (evt.getActionCommand().equals("Remove")){ //Remove a course code from the requirement
					if (r_courses.getSelectedValue() != null){
						list_model.remove(r_courses.getSelectedIndex());
					}
					if (list_model.size() > 0){
						num_model.setMaximum(list_model.size());
					}
				} else { //The "Cancel" button
					frame2.dispose();
				}
			}
		};
		
		//Make the buttons listen for clicks
		remove_course.addActionListener(listener2);
		add_course.addActionListener(listener2);
		update.addActionListener(listener2);
		delete.addActionListener(listener2);
		cancel.addActionListener(listener2);
	}
	
	/**
	 * Opens a popup to find a file, using the io_action variable to decide what to do with it
	 */
	public void find_file(){
		String[] action = io_action.split(" ");
		frame2 = new JFrame();
		frame2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); //Dispose of frame2 when it is closed
		frame2.setVisible(true);
		frame2.setResizable(false);
		Container pane = frame2.getContentPane();
		SpringLayout layout = new SpringLayout();
		pane.setLayout(layout);
		
		DefaultListModel<String> list_model = new DefaultListModel<String>();
		File directory = new File("./files/");
		if (!directory.exists()){
			directory.mkdir();
		}
		//Find all the desired files in the directory ./files and display them
		File[] files = directory.listFiles(new FileFilter(){
			@Override
			public boolean accept(File pathname) {
				if (action[1].equals("all")){
					return pathname.getName().endsWith("mapping.txt");					
				} else {
					return pathname.getName().endsWith(action[1]+".txt");
				}
			}
		});
		String to_remove = action[1];
		if (action[1].equals("all")){
			to_remove = "mapping";
		}
		for (File f : files){
			String fname = f.getName();
			list_model.addElement(fname.substring(0,fname.length()-to_remove.length()-5)+fname.substring(fname.length()-4,fname.length()));
		}
		//The components in the pane
		JList<String> file_list = new JList<String>(list_model);
		JScrollPane list_scroller = new JScrollPane(file_list);
		list_scroller.setPreferredSize(new Dimension(250,105)); //6 files
		JTextField file_name = new JTextField("");
		JButton io = new JButton(action[0]);
		JButton cancel = new JButton("Cancel");
		
		//Add the components to the pane
		pane.add(list_scroller);
		pane.add(file_name);
		pane.add(io);
		pane.add(cancel);
		
		//Place the components in the pane
		layout.putConstraint(SpringLayout.WEST, list_scroller, 5, SpringLayout.WEST, pane);
		layout.putConstraint(SpringLayout.NORTH, list_scroller, 5, SpringLayout.NORTH, pane);
		layout.putConstraint(SpringLayout.EAST, pane, 5, SpringLayout.EAST, list_scroller);
		layout.putConstraint(SpringLayout.NORTH, file_name, 5, SpringLayout.SOUTH, list_scroller);
		layout.putConstraint(SpringLayout.WEST, file_name, 0, SpringLayout.WEST, list_scroller);
		layout.putConstraint(SpringLayout.EAST, file_name, 0, SpringLayout.EAST, list_scroller);
		layout.putConstraint(SpringLayout.NORTH, io, 5, SpringLayout.SOUTH, file_name);
		layout.putConstraint(SpringLayout.WEST, io, 0, SpringLayout.WEST, list_scroller);
		layout.putConstraint(SpringLayout.NORTH, cancel, 5, SpringLayout.SOUTH, file_name);
		layout.putConstraint(SpringLayout.WEST, cancel, 0, SpringLayout.EAST, io);
		layout.putConstraint(SpringLayout.SOUTH, pane, 5, SpringLayout.SOUTH, io);

		frame2.pack(); //Make the pane appear
		
		ActionListener listener2 = new ActionListener(){ //What to do when the buttons are clicked
			public void actionPerformed(ActionEvent evt){
				if (evt.getActionCommand().equals("Export")){ //Save a file
					String fname = file_name.getText();
					if (fname.endsWith(".txt")){
						fname = fname.substring(0,fname.length()-4);
					}
					if (action[1].equals("courses") || action[1].equals("all")){ //Save the courses
						String data = "";
						//Save the group data
						for (Group g : groups){
							if (g == null){
								break;
							}
							String s = "$"+g.get_name()+" ("+g.get_color().getRed()+","+g.get_color().getGreen()+","+g.get_color().getBlue()+")";
							data+=s+"\n";
						}
						//Save the course data
						for (Course c : courses){
							String prereq = "";
							for (int i = 0; i < c.get_prereqs().size(); i++){
								if (i != 0){
									prereq+="/";
								}
								prereq+=c.get_prereqs().get(i);
							}
							String s = c.get_sh()+","+c.get_code()+","+c.get_name()+","+c.get_year()+","+c.get_fall()+","+
							c.get_freq()+","+c.get_group()+","+prereq+","+c.is_aorb()+","+c.should_warn();
							data+=s+"\n";
						}
						try{
							save_to_file(fname,"courses",data); //Write the file
						} catch (FileNotFoundException e){
							System.out.println("Error: File not found");
						}
					}
					if (action[1].equals("reqs") || action[1].equals("all")){ //Save the requirements
						String data = "";
						//Save the requirement data
						for (Requirement r : reqs){
							if (r.get_courses()[0].equals("Add Requirement")){
								continue;
							}
							String crs = "";
							for (int i = 0; i < r.get_courses().length; i++){
								if (i != 0){
									crs+="/";
								}
								crs+=r.get_courses()[i];
							}
							String s = crs+","+r.get_num();
							data+=s+"\n";
						}
						try{
							save_to_file(fname,"reqs",data); //Write the file
						} catch (FileNotFoundException e){
							System.out.println("Error: File not found");
						}
					}
					if (action[1].equals("mapping") || action[1].equals("all")){ //Save the schedule
						String data = "";
						//Save the mapping data
						if (mapping != null){
							for (Course[] sem : mapping){
								for (Course c : sem){
									if (c == null){
										break;
									}
									data+=c.get_code()+"\n";
								}
								data+="###\n"; //Character to separate semesters
							}
						}
						for (Course c : no_semester){
							data+=c.get_code()+"\n";
						}
						try{
							save_to_file(fname,"mapping",data); //Write the file
						} catch (FileNotFoundException e){
							System.out.println("Error: File not found");
						}
					}
					frame2.dispose();
				} else if (evt.getActionCommand().equals("Import")){ //Load a file
					String fname = file_name.getText();
					if (fname.endsWith(".txt")){
						fname = fname.substring(0,fname.length()-4);
					}
					if (action[1].equals("courses") || action[1].equals("all")){ //Load courses
						try{
							load_from_file(fname,"courses");
						} catch (FileNotFoundException e){
							System.out.println("Error: File not found");
						}
					}
					if (action[1].equals("reqs") || action[1].equals("all")){ //Load requirements
						try{
							load_from_file(fname,"reqs");
						} catch (FileNotFoundException e){
							System.out.println("Error: File not found");
						}
					}
					if (action[1].equals("mapping") || action[1].equals("all")){ //Load schedule
						try{
							load_from_file(fname,"mapping");
						} catch (FileNotFoundException e){
							System.out.println("Error: File not found");
						}
					}
					frame2.dispose();
				} else if (evt.getActionCommand().equals("Cancel")){ //Cancel out of the page
					frame2.dispose();
				}
			}
		};
		
		//Make buttons listen for clicks
		io.addActionListener(listener2);
		cancel.addActionListener(listener2);
		
		//Make the file list listen for clicks, and set the input to the name of the file clicked on
		file_list.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				file_name.setText(file_list.getSelectedValue());
			}
			
		});
	}
	
	/**
	 * Create a popup that warns the user before updating the schedule
	 */
	public void create_update_warning(){
		frame2 = new JFrame();
		frame2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); //Dispose of frame2 when it is closed
		frame2.setVisible(true);
		frame2.setResizable(false);
		Container pane = frame2.getContentPane();
		SpringLayout layout = new SpringLayout();
		pane.setLayout(layout);
		
		String message = "Updating the schedule will erase all modifications you have made to it. Are you sure you want to do this?";
		//Split the message into several lines
		String[] to_display = message.split(" ");
		JLabel[] text = new JLabel[to_display.length];
		int index = 0;
		int next = 1;
		while (next < to_display.length){
			FontMetrics metrics = getBuffer().getFontMetrics();
			String join = to_display[index]+" "+to_display[next];
			if (metrics.stringWidth(join) < 275){
				to_display[index] = join;
				to_display[next] = null;
				next++;
			} else {
				index = next;
				next++;
			}
		}
		index = 0;
		for (int k = 0; k < to_display.length; k++){
			if (to_display[k] != null){
				text[index] = new JLabel(to_display[k]);
				index++;
			}
		}
		
		//Buttons to update the schedule, or cancel out of the page
		JButton ok = new JButton("Ok");
		JButton cancel = new JButton("Cancel");
		
		//Add the message and buttons to the pane
		index = 0;
		while (index < text.length && text[index] != null){
			pane.add(text[index]);
			if (index == 0){
				layout.putConstraint(SpringLayout.WEST, text[index], 5, SpringLayout.WEST, pane);
				layout.putConstraint(SpringLayout.NORTH, text[index], 5, SpringLayout.NORTH, pane);
			} else {
				layout.putConstraint(SpringLayout.WEST, text[index], 5, SpringLayout.WEST, pane);
				layout.putConstraint(SpringLayout.NORTH, text[index], 5, SpringLayout.SOUTH, text[index-1]);
			}
			index++;
		}
		pane.add(ok);
		pane.add(cancel);
		
		//Place the message and buttons in the pane
		layout.putConstraint(SpringLayout.NORTH, ok, 5, SpringLayout.SOUTH, text[index-1]);
		layout.putConstraint(SpringLayout.WEST, ok, 5, SpringLayout.WEST, pane);
		layout.putConstraint(SpringLayout.NORTH, cancel, 5, SpringLayout.SOUTH, text[index-1]);
		layout.putConstraint(SpringLayout.WEST, cancel, 5, SpringLayout.EAST, ok);
		layout.putConstraint(SpringLayout.SOUTH, pane, 5, SpringLayout.SOUTH, ok);
		layout.putConstraint(SpringLayout.EAST, pane, 300, SpringLayout.WEST, text[0]);
		
		frame2.pack(); //Make the pane appear
		
		ActionListener listener = new ActionListener(){ //What to do when the buttons are clicked
			public void actionPerformed(ActionEvent evt){
				if (evt.getActionCommand().equals("Ok")){ //Update the schedule
					update_schedule();
					frame2.dispose();
				} else if (evt.getActionCommand().equals("Cancel")){ //Cancel out of the page
					frame2.dispose();
				}
			}
		};
		//Make the buttons listen for clicks
		ok.addActionListener(listener);
		cancel.addActionListener(listener);
	}
	
	/**
	 * Creates a popup that provides information about the author
	 */
	public void create_about_page(){
		frame2 = new JFrame();
		frame2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); //Dispose of frame2 when it is closed
		frame2.setVisible(true);
		frame2.setResizable(false);
		Container pane = frame2.getContentPane();
		SpringLayout layout = new SpringLayout();
		pane.setLayout(layout);
		
		String message = "CourseMapper 1.0\nCreated by Aron Harder\ngithub.com/AronHa";
		//Split the message into several lines
		String[] to_display = message.split("\n");
		JLabel[] text = new JLabel[to_display.length];
		for (int i = 0; i < to_display.length; i++){
			text[i] = new JLabel(to_display[i]);
		}
		
		JButton ok = new JButton("Ok"); //Button to cancel out of the page
		
		//Add the components to the pane 
		for (int i = 0; i < text.length; i++){
			pane.add(text[i]);
			if (i == 0){
				layout.putConstraint(SpringLayout.WEST, text[i], 5, SpringLayout.WEST, pane);
				layout.putConstraint(SpringLayout.NORTH, text[i], 5, SpringLayout.NORTH, pane);
			} else {
				layout.putConstraint(SpringLayout.WEST, text[i], 5, SpringLayout.WEST, pane);
				layout.putConstraint(SpringLayout.NORTH, text[i], 5, SpringLayout.SOUTH, text[i-1]);
			}
		}
		pane.add(ok);
		
		//Place the components in the pane
		layout.putConstraint(SpringLayout.NORTH, ok, 5, SpringLayout.SOUTH, text[text.length-1]);
		layout.putConstraint(SpringLayout.WEST, ok, 5, SpringLayout.WEST, pane);
		layout.putConstraint(SpringLayout.SOUTH, pane, 5, SpringLayout.SOUTH, ok);
		layout.putConstraint(SpringLayout.EAST, pane, 300, SpringLayout.WEST, text[0]);
		
		frame2.pack(); //Make the pane appear
		
		ok.addActionListener(new ActionListener(){ //Cancel out of the page when the button is clicked
			public void actionPerformed(ActionEvent evt){
				frame2.dispose();
			}
		});
	}
	
	/**
	 * Create a popup that displays a help page
	 * @param file_path - the location of the image to display
	 */
	public void create_help_page(String file_path){
		frame2 = new JFrame();
		frame2.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); //Dispose of frame2 when it is closed
		frame2.setVisible(true);
		frame2.setResizable(false);
		Container pane = frame2.getContentPane();
		SpringLayout layout = new SpringLayout();
		pane.setLayout(layout);
		
		try { //Display the image if it exists
			BufferedImage help_img = ImageIO.read(new File(file_path));
			JLabel pic = new JLabel(new ImageIcon(help_img));
			JButton cancel = new JButton("Cancel");
			
			//Add the components to the pane
			frame2.add(pic);
			frame2.add(cancel);
			
			//Place the components in the pane
			layout.putConstraint(SpringLayout.NORTH, pic, 5, SpringLayout.NORTH, pane);
			layout.putConstraint(SpringLayout.WEST, pic, 5, SpringLayout.WEST, pane);
			layout.putConstraint(SpringLayout.NORTH, cancel, 5, SpringLayout.SOUTH, pic);
			layout.putConstraint(SpringLayout.WEST, cancel, 5, SpringLayout.WEST, pane);
			layout.putConstraint(SpringLayout.SOUTH, pane, 5, SpringLayout.SOUTH, cancel);
			layout.putConstraint(SpringLayout.EAST, pane, 5, SpringLayout.EAST, pic);

			frame2.pack(); //Make the pane appear

			cancel.addActionListener(new ActionListener(){ //Cancel out of the page when the button is clicked
				public void actionPerformed(ActionEvent evt){
					frame2.dispose();
				}
			});
		} catch (IOException e){ //Image did not exist. Exit out of the page
			e.printStackTrace();
			frame2.dispose();
		}
	}
	
	/**
	 * What to do when the mouse gets clicked on the canvas
	 */
	public void mouseClicked(MouseEvent evt) {
		int x = evt.getX(); //Get the coordinates of the mouse click
		int y = evt.getY();
		
		if (x >= button_coords[0] && x <= button_coords[2] && y >= button_coords[1] && y <= button_coords[3]){
			//If the "Edit Course/Requirement" button was clicked
			if (info_req != null){
				edit_requirement(info_req, false); //Edit the requirement
			} else if (info_course != null){
				if (info_course.get_code().equals("Elective")){
					edit_elective(info_course); //Edit the elective color
				} else {
					edit_course(info_course,false); //Edit the course
				}
			}
			return;
		} else if (x >= GRID_WIDTH+5 && x <= GRID_WIDTH+LABEL_WIDTH-5 && y >= GRID_HEIGHT-(LABEL_WIDTH/2)+INFO_HEIGHT && y <= GRID_HEIGHT-(LABEL_WIDTH/4)+INFO_HEIGHT-10){
			//If the "Update Schedule" button was clicked
			create_update_warning();
			return;
		} else if (x >= GRID_WIDTH+5 && x <= GRID_WIDTH+LABEL_WIDTH-5 && y >= GRID_HEIGHT-(LABEL_WIDTH/4)+INFO_HEIGHT && y <= GRID_HEIGHT+INFO_HEIGHT-10){
			//If the "View Groups/Schedule" button was clicked
			if (draw_graphics == false && mapping != null){
				draw_graphics = true;
			} else if (draw_graphics == true){
				draw_graphics = false;
			}
			return;
		} else if (x > GRID_WIDTH){
			//If the click is in the side information bar
			if (y < start_no_sem+20){
				return; //ignore the click if it is above the no_semester courses
			}
			int grid_y = (y-start_no_sem-20)/(GRID_HEIGHT/MAX_HOURS); //Get the index of the course clicked on
			if (grid_y < no_semester.size() && grid_y >= 0){
				info_course = no_semester.get(grid_y); //Set the info_course to the course clicked on
			}
			return;
		}
		if (draw_graphics == true){ //Schedule View
			if (y < INFO_HEIGHT){
				return; //ignore the click if it is in the top information bar
			} else {
				int grid_x = x/(GRID_WIDTH/NUM_SEMESTERS); //Get the semester clicked on
				int grid_y = (y-INFO_HEIGHT)/(GRID_HEIGHT/MAX_HOURS); //Get the hour in the semester clicked on
				int hour = 0;
				//Find the course at that grid location
				for (Course c : mapping[grid_x]){
					if (c == null){
						info_course = null; //No course was clicked on; make info_course null
						break;
					} else {
						hour+=c.get_sh();
						if (hour > grid_y){
							//Set info_course to the course clicked on
							info_req = null;
							info_course = c;
							break;
						}
					}
				}
			}		
		} else { // Groups View
			int grid_x = x/(GRID_WIDTH/NUM_SEMESTERS); //Find the group clicked on
			int grid_y = (y-INFO_HEIGHT)/(GRID_HEIGHT/MAX_HOURS); //Find the index within the group clicked on
			int grid_y_offset = 0; //Offset because of previous button
			if (grid_x == 0){ //Requirements
				if (y < INFO_HEIGHT){
					return; //ignore the click if it is in the top information bar
				}
				if (reqs_display[0] != 0){
					grid_y_offset--; //If there is a previous button
				}
				if (reqs.get(grid_x) == null || grid_y+reqs_display[0]+grid_y_offset >= reqs.size()){
					return; //ignore the click if it is below the Add Requirement button
				} else {
					if (grid_y == 0 && reqs_display[0] != 0){ //The Previous button was clicked
						//Decrement the requirements to display
						if (reqs_display[0] > 0){
							reqs_display[0]-=16;
							reqs_display[1] = reqs_display[0]+15;
							if (reqs_display[0] == 1){
								reqs_display[0] = 0;
							}
							if (reqs_display[1] > reqs.size()-1){
								reqs_display[1] = reqs.size()-1;
							}
						}
					} else if (grid_y == 17 && reqs_display[1] < reqs.size()-1){ //The Next button was clicked
						//Increment the requirements to display
						if (reqs_display[1] < reqs.size()-2){
							if (reqs_display[0] == 0){
								reqs_display[0]++;
							}
							reqs_display[0]+=16;
							reqs_display[1] = reqs_display[0]+15;
							if (reqs_display[1] >= reqs.size()-2){
								reqs_display[1] = reqs.size()-1;
							}
						}
					} else { //A requirement was clicked on
						if (reqs.get(grid_y+grid_y_offset+reqs_display[0]).get_courses()[0].equals("Add Requirement")){ //The Add Requirement button was clicked
							edit_requirement(new Requirement(), true);
						} else {
							//Set info_req to the requirement clicked on
							info_course = null;
							info_req = reqs.get(grid_y+grid_y_offset+reqs_display[0]);
						}
					}
				}
			} else { //Groups
				grid_x--; //Decrement grid_x to adjust for requirements being in the first column
				if (y < INFO_HEIGHT){ //If the click was in the top information bar
					if (grid_x >= groups.length){
						return; //ignore the click if it is in the side information bar
					}
					if (groups[grid_x] == null && (grid_x == 0 || groups[grid_x-1] != null)){
						edit_group(new Group(),true); //Create a new group if the New Group button was clicked on
					} else if (groups[grid_x] == null && groups[grid_x-1] == null){
							return; //ignore the click if it is past the New Group button
					} else {
						edit_group(groups[grid_x],false); //Edit the group if the group name was clicked on
					}
				} else {
					if (grid_x >= groups.length || groups[grid_x] == null){
						return; //ignore the click if it is in the side information bar or there is no group in that column
					}
					if (groups[grid_x].get_display()[0] != 0){
						grid_y_offset--; //If there is a previous button
					}
					if (grid_y+groups[grid_x].get_display()[0]+grid_y_offset >= groups[grid_x].get_courses().size()){ //Clicking past the last course in that group
						return; //ignore the click
					} else {
						if (grid_x == -1){ //Shouldn't ever happen, is when a requirement was clicked on
							System.out.println("Req");
						} else {
							if (grid_y == 0 && groups[grid_x].get_display()[0] != 0){ //The Previous button was clicked
								groups[grid_x].decrement_display(); //Decrement the courses to display
							} else if (grid_y == 17 && groups[grid_x].get_display()[1] < groups[grid_x].get_courses().size()-1){ //The Next button was clicked
								groups[grid_x].increment_display(); //Increment the courses to display
							} else { //A course was clicked
								if (groups[grid_x].get_courses().get(grid_y+grid_y_offset+groups[grid_x].get_display()[0]).get_name().equals("Add Course")){
									//Create a new course if the Add Course button was clicked
									Course c = new Course();
									c.set_group(groups[grid_x].get_name());
									edit_course(c,true);
								} else {
									//Set info_course to the course clicked on
									info_req = null;
									info_course = groups[grid_x].get_courses().get(grid_y+grid_y_offset+groups[grid_x].get_display()[0]);
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * What to do when the mouse is pressed on the canvas
	 */
	public void mousePressed(MouseEvent evt) {
		int x = evt.getX(); // Get the coordinates of the mouse press
		int y = evt.getY();
		
		if (draw_graphics == true){ //Schedule View
			if (x > GRID_WIDTH && x < GRID_WIDTH+(GRID_WIDTH/NUM_SEMESTERS)){ //No semester courses;
				if (y < start_no_sem+20){
					return; //ignore the press if it is above the first course in no_semester
				}
				int grid_y = (y-start_no_sem-20)/(GRID_HEIGHT/MAX_HOURS); //Find the index of the course the mouse was on
				if (grid_y < no_semester.size()){
					//If the mouse was on a course, make that course the course being dragged
					drag_course = no_semester.get(grid_y);
					drag_index[0] = mapping.length;
					drag_index[1] = grid_y;
				}
			} else if (x >= GRID_WIDTH+(GRID_WIDTH/NUM_SEMESTERS)){
				return; //ignore the press if it is past the No Semester courses
			} else if (y < INFO_HEIGHT){
				return; //ignore the press if it is in the top information bar
			} else {
				int grid_x = x/(GRID_WIDTH/NUM_SEMESTERS); //Find the semester where the press occurred
				int grid_y = (y-INFO_HEIGHT)/(GRID_HEIGHT/MAX_HOURS); //Find the hour where the press occurred
				int hour = 0;
				//Find the course at that location
				for (int i = 0; i < mapping[grid_x].length && mapping[grid_x][i] != null; i++){
					hour+=mapping[grid_x][i].get_sh();
					if (hour > grid_y){
						//Set that course to the drag course
						drag_course = mapping[grid_x][i];
						drag_index[0] = grid_x;
						drag_index[1] = i;
						break;
					}
				}
				//Don't let the user drag electives
				if (drag_course != null && drag_course.get_code().equals("Elective")){
					drag_course = null;
					drag_index[0] = -1;
					drag_index[1] = -1;
				}
			}
		}
	}
	/**
	 * What to do when the mouse is released on the canvas
	 */
	public void mouseReleased(MouseEvent evt) {
		int x = evt.getX(); //Get the coordinates of the mouse release
		int y = evt.getY();
		if (draw_graphics == true){ //Schedule View
			if (drag_course == null){
				return; //ignore the release if there is no course being dragged
			}
			if (x > GRID_WIDTH){
				if (drag_index[0] < mapping.length){
					//If the course was dragged into the No Semester area, add that course to no_semester and remove it from mapping
					no_semester.add(drag_course);
					mapping[drag_index[0]][drag_index[1]] = null;
					for (int i = drag_index[1]; i < mapping[drag_index[0]].length-1; i++){
						mapping[drag_index[0]][i] = mapping[drag_index[0]][i+1];
					}
					mapping[drag_index[0]] = addElectives(mapping[drag_index[0]]);
				}
				//No longer dragging a course
				drag_course = null;
				drag_index[0] = -1;
				drag_index[1] = -1;
				return;
			} else if (y < INFO_HEIGHT){
				//ignore if the course was dragged into the top information bar
				drag_course = null;
				drag_index[0] = -1;
				drag_index[1] = -1;
				return;
			} else {
				int grid_x = x/(GRID_WIDTH/NUM_SEMESTERS); //Find the semester that the course was dragged to
				if (grid_x < 0 || grid_x >= mapping.length){
					//ignore if the course was dragged somewhere that isn't a semester
					//This shouldn't happen, but sometimes does when the mouse leaves the canvas.
					drag_course = null;
					drag_index[0] = -1;
					drag_index[1] = -1;
					return;
				}
				int grid_y = (y-INFO_HEIGHT)/(GRID_HEIGHT/MAX_HOURS); //Find the hour that the course was dragged to
				if (grid_y < 0 || grid_y >= MAX_HOURS){
					//ignore if the course was dragged somewhere above or below the semester
					//This shouldn't happen, but sometimes does when the mouse leaves the canvas.
					drag_course = null;
					drag_index[0] = -1;
					drag_index[1] = -1;
					return;
				}
				//Delete the course from the semester it used to be in
				if (drag_index[0] >= mapping.length){ //Course was in no_semesters
					no_semester.remove(drag_index[1]);
				} else { //Course was in the mapping
					mapping[drag_index[0]][drag_index[1]] = null;
					for (int i = drag_index[1]; i < mapping[drag_index[0]].length-1; i++){
						mapping[drag_index[0]][i] = mapping[drag_index[0]][i+1];
					}
					mapping[drag_index[0]] = addElectives(mapping[drag_index[0]]); //Add electives if needed
				}
				//Designate the course to the placed where it was dragged
				int hour = 0;
				int start_index;
				for (start_index = 0; start_index < mapping[grid_x].length && mapping[grid_x][start_index] != null; start_index++){
					if (mapping[grid_x][start_index].get_code().equals("Elective")){
						break;
					} else if (hour+mapping[grid_x][start_index].get_sh() > grid_y){
						break;
					} else {
						hour+=mapping[grid_x][start_index].get_sh();
					}
				}
				hour+=drag_course.get_sh();
				int end_index;
				if (hour > 18){	//If dragging a course causes it to go over 18 credits, designate it to be put back
					grid_x = drag_index[0];
					start_index = drag_index[1];
					if (grid_x < mapping.length){
						end_index = mapping[grid_x].length-1;
					} else {
						end_index = no_semester.size();
					}
				} else {
					end_index = start_index;
					while (mapping[grid_x][end_index] != null){
						hour+=mapping[grid_x][end_index].get_sh();
						end_index++;
					}
				}
				//Putting the course back into no_semester if needed
				if (hour > 18 && grid_x >= mapping.length){
					no_semester.add(start_index,drag_course);
				} else { //Add the course to the designated place
					while (end_index > start_index){
						mapping[grid_x][end_index] = mapping[grid_x][end_index-1];
						end_index--;
					}
					mapping[grid_x][start_index] = drag_course;
					mapping[grid_x] = addElectives(mapping[grid_x]); //Add electives if you got rid of a really big course
				}
				for (int i = 0; i < NUM_SEMESTERS; i++){
					for (int j = 0; j < mapping[i].length && mapping[i][j] != null; j++){
						mapping[i][j].set_warn(has_problem(mapping[i][j]));
					}
				}
				//The course is no longer being dragged
				drag_course = null;
				drag_index[0] = -1;
				drag_index[1] = -1;
			}
		}
	}
	/**
	 * If the mouse leaves the frame, stop dragging the course
	 */
	public void mouseExited(MouseEvent evt) {
		drag_course = null;
		drag_index[0] = -1;
		drag_index[1] = -1;
	}
	/**
	 * This method is not used, but it's needed in order to implement mouseListener
	 */
	public void mouseEntered(MouseEvent evt) {}
}
