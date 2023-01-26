package graphRec;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

@Route("")
public class MainView extends VerticalLayout {

	private List<String> listItems;
	private Image image;
	private Grid<Recommendation> grid;
	private RecommendationsRestController recController;
	
	public MainView() {
		
		this.grid = new Grid<>(Recommendation.class);
		this.recController = new RecommendationsRestController();
		
		// defining viewed movies for the dropdown
		listItems = new ArrayList<>();
		listItems.add("Apocalypse Now (1979) | 1208");
		listItems.add("Back to the Future (1985) | 1270");
		listItems.add("Star Wars: Episode IV - A New Hope (1977) | 260");
		listItems.add("Star Wars: Episode V - The Empire Strikes Back (1980) | 1196");
		listItems.add("Shawshank Redemption The (1994) | 318");
		listItems.add("Matrix The (1999) | 2571");
		listItems.add("Forrest Gump (1994) | 356");
		listItems.add("Kill Bill: Vol. 2 (2004) | 7438");
		listItems.add("Pulp Fiction (1994) | 296");
		
		image = new Image();
		image.setHeight("300px");
		
		add(getList(),grid);
	}
	
	private Component getList() {
		
		var layout = new HorizontalLayout();
		
		ListBox<String> listSelect = new ListBox<>();
		listSelect.setItems(listItems);
		layout.add(listSelect);
		
		listSelect.addValueChangeListener(click ->{
			String[] selectedValues = listSelect.getValue().split(" \\| ");
			int movieID = Integer.parseInt(selectedValues[1]);
			image.setSrc(getImageFilename(movieID));
			
			List<Recommendation> recs = recController.findRealtimeRecsByMovieId(movieID);
			grid.setItems(recs);
		});

		layout.add(image);
		
		return layout;
	}
	
	private StreamResource getImageFilename(int movieID) {
		
		String filename = "movie_" + movieID + ".jpeg";
		return new StreamResource(filename,
				() -> getClass().getResourceAsStream("/images/" + filename));
	}
}
